package dev.yewintnaing.storage;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.protocol.RespParser;
import dev.yewintnaing.protocol.RespType;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PersistenceManager {

    private static final Path PATH_OF_AOF = Paths.get("appendOnlyFile.aof");
    private static final Path PATH_OF_AOF_TEMP = Paths.get("appendOnlyFile.aof.tmp");

    private static final BlockingQueue<RespArray> logQueue = new LinkedBlockingQueue<>(10000);
    private static OutputStream aofOut;
    private static volatile boolean running = true;
    private static volatile boolean writerRunning = false;
    private static volatile boolean fsyncRunning = false;

    private static final Object lock = new Object();

    static {
        initAofStream();
    }

    public static void initAofStream() {
        try {
            synchronized (lock) {
                running = true;
                if (aofOut != null) {
                    aofOut.close();
                }
                aofOut = new BufferedOutputStream(
                        Files.newOutputStream(
                                PATH_OF_AOF,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.APPEND));
                startBackgroundWriter();
                startFsyncTask();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to open appendOnlyFile.aof", e);
        }
    }

    public static void shutdown() {
        running = false;
        try {
            synchronized (lock) {
                if (aofOut != null) {
                    aofOut.flush();
                    aofOut.close();
                    aofOut = null;
                }
                writerRunning = false;
                fsyncRunning = false;
            }
        } catch (IOException e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }

    private static void startBackgroundWriter() {
        synchronized (lock) {
            if (writerRunning)
                return;
            writerRunning = true;
        }
        Thread thread = new Thread(() -> {
            while (running || !logQueue.isEmpty()) {
                try {
                    RespArray command = logQueue.poll(50, TimeUnit.MILLISECONDS);
                    if (command != null) {
                        synchronized (lock) {
                            if (aofOut != null) {
                                aofOut.write(convertToRespBytes(command));
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    System.err.println("Failed to write to AOF: " + e.getMessage());
                }
            }
            synchronized (lock) {
                writerRunning = false;
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static void startFsyncTask() {
        synchronized (lock) {
            if (fsyncRunning)
                return;
            fsyncRunning = true;
        }
        Thread thread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(1000);
                    synchronized (lock) {
                        if (aofOut != null) {
                            aofOut.flush();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    System.err.println("Failed to flush AOF: " + e.getMessage());
                }
            }
            synchronized (lock) {
                fsyncRunning = false;
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public static void log(RespArray respArray) {
        if (!logQueue.offer(respArray)) {
            // If queue is full, we might want to block or drop.
            // In Redis, it usually blocks the client if AOF is lagging too much.
            try {
                logQueue.put(respArray);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void sync() {
        while (!logQueue.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // Also ensure it's flushed to disk
        try {
            synchronized (lock) {
                if (aofOut != null) {
                    aofOut.flush();
                }
            }
        } catch (IOException e) {
            System.err.println("Sync flush failed: " + e.getMessage());
        }
    }

    private static byte[] convertToRespBytes(RespArray command) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<RespType> elements = command.elements();
        out.write(("*" + elements.size() + "\r\n").getBytes(StandardCharsets.US_ASCII));

        for (RespType element : elements) {
            if (!(element instanceof RespBulkString bulk)) {
                throw new IOException("Only bulk string elements supported");
            }
            byte[] data = bulk.data();
            out.write(("$" + data.length + "\r\n").getBytes(StandardCharsets.US_ASCII));
            out.write(data);
            out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
        }
        return out.toByteArray();
    }

    public static List<RespArray> readAof() throws IOException {
        if (!Files.exists(PATH_OF_AOF)) {
            return new ArrayList<>();
        }
        List<RespArray> commands = new ArrayList<>();
        try (InputStream in = new BufferedInputStream(Files.newInputStream(PATH_OF_AOF))) {
            RespType resp;
            while ((resp = RespParser.readResp(in)) != null) {
                if (resp instanceof RespArray arr) {
                    commands.add(arr);
                }
            }
        }
        return commands;
    }

    public static synchronized void rewriteAof() {
        System.out.println("Starting AOF rewrite...");
        try (OutputStream tempOut = new BufferedOutputStream(Files.newOutputStream(PATH_OF_AOF_TEMP,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            for (var entry : RedisStorage.getSnapshot().entrySet()) {
                String key = entry.getKey();
                RedisValue value = entry.getValue();
                if (value.isExpired())
                    continue;

                List<RespArray> commands = serializeToCommands(key, value);
                for (RespArray cmd : commands) {
                    tempOut.write(convertToRespBytes(cmd));
                }
            }
            tempOut.flush();

            // Swap files
            aofOut.close();
            Files.move(PATH_OF_AOF_TEMP, PATH_OF_AOF, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            initAofStream();
            System.out.println("AOF rewrite completed.");
        } catch (IOException e) {
            System.err.println("AOF rewrite failed: " + e.getMessage());
        }
    }

    private static List<RespArray> serializeToCommands(String key, RedisValue value) {
        List<RespArray> cmds = new ArrayList<>();
        if (value instanceof StringValue s) {
            cmds.add(createCommand("SET", key, s.value()));
        } else if (value instanceof LongValue l) {
            cmds.add(createCommand("SET", key, String.valueOf(l.value())));
        } else if (value instanceof ListValue lv) {
            for (String item : lv.value()) {
                cmds.add(createCommand("RPUSH", key, item));
            }
        } else if (value instanceof HashValue hv) {
            for (var entry : hv.value().entrySet()) {
                cmds.add(createCommand("HSET", key, entry.getKey(), entry.getValue()));
            }
        } else if (value instanceof SetValue sv) {
            if (!sv.value().isEmpty()) {
                List<String> command = new ArrayList<>();
                command.add("SADD");
                command.add(key);
                command.addAll(sv.value());
                cmds.add(createCommand(command.toArray(String[]::new)));
            }
        } else if (value instanceof ZSetValue zv) {
            for (SkipList.Node node : zv.index().range(0, zv.index().size() - 1)) {
                cmds.add(createCommand("ZADD", key, Double.toString(node.score), node.member));
            }
        }

        if (value.expiryTime() > 0) {
            long seconds = (value.expiryTime() - System.currentTimeMillis()) / 1000;
            if (seconds > 0) {
                cmds.add(createCommand("EXPIRE", key, String.valueOf(seconds)));
            }
        }
        return cmds;
    }

    private static RespArray createCommand(String... parts) {
        List<RespType> elements = new ArrayList<>();
        for (String part : parts) {
            elements.add(new RespBulkString(part.getBytes(StandardCharsets.UTF_8)));
        }
        return new RespArray(elements);
    }
}
