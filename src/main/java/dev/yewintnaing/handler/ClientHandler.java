package dev.yewintnaing.handler;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespParser;
import dev.yewintnaing.protocol.RespType;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import dev.yewintnaing.logic.CommandProcessor;
import dev.yewintnaing.logic.PubSubManager;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final CommandProcessor commandProcessor;
    private final Set<String> subscriptions = ConcurrentHashMap.newKeySet();
    private final BlockingQueue<String> outputQueue = new LinkedBlockingQueue<>(50); // Smaller queue for faster
                                                                                     // detection
    private static final int MAX_SUBSCRIPTIONS = 100;
    private Thread writerThread;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.commandProcessor = new CommandProcessor();
    }

    @Override
    public void run() {
        // Start writer thread
        writerThread = Thread.ofVirtual().start(this::writeLoop);

        try (InputStream in = new BufferedInputStream(socket.getInputStream())) {
            while (!socket.isClosed()) {
                RespType request = RespParser.readResp(in);
                if (request == null) {
                    break;
                } // Client disconnected

                try {
                    if (!(request instanceof RespArray commandArray)) {
                        writeError("Protocol error: expected array");
                        continue;
                    }

                    String respResponse = commandProcessor.handle(commandArray, this);
                    if (respResponse != null) {
                        sendMessage(respResponse);
                    }

                } catch (Exception e) {
                    writeError(e.getMessage());
                    System.err.println("Protocol error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + e.getMessage());
        } finally {
            close();
        }
    }

    private void writeLoop() {
        try (OutputStream out = new BufferedOutputStream(socket.getOutputStream())) {
            while (!socket.isClosed()) {
                String message = outputQueue.poll(5, java.util.concurrent.TimeUnit.SECONDS);
                if (message == null) {
                    continue; // Timeout, check if socket closed
                }

                long startTime = System.currentTimeMillis();
                out.write(message.getBytes(StandardCharsets.UTF_8));
                out.flush();
                long writeTime = System.currentTimeMillis() - startTime;

                // If write takes too long, client is slow
                if (writeTime > 1000) { // 1 second threshold
                    System.err.println(
                            "Client " + socket + " write took " + writeTime + "ms. Disconnecting slow client.");
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // Socket closed or error
        } finally {
            close();
        }
    }

    public boolean addSubscription(String channel) {
        if (subscriptions.size() >= MAX_SUBSCRIPTIONS) {
            return false;
        }
        return subscriptions.add(channel);
    }

    public void removeSubscription(String channel) {
        subscriptions.remove(channel);
    }

    public Set<String> getSubscriptions() {
        return subscriptions;
    }

    public void sendMessage(String message) {
        if (socket.isClosed())
            return;
        if (!outputQueue.offer(message)) {
            System.err.println("Client " + socket + " is too slow. Disconnecting.");
            close();
        }
    }

    private void writeError(String msg) {
        String safe = (msg == null || msg.isBlank()) ? "ERR" : msg;
        sendMessage("-ERR " + safe + "\r\n");
    }

    private void close() {
        try {
            PubSubManager.getInstance().unsubscribeAll(this);
            socket.close();
            if (writerThread != null) {
                writerThread.interrupt();
            }
        } catch (IOException e) {
            // Ignore
        }
    }
}
