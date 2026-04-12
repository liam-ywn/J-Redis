package dev.yewintnaing.storage;

import dev.yewintnaing.RedisServer;
import dev.yewintnaing.logic.LRangeCommand;
import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceTest {

    private static final Path AOF_PATH = Paths.get("appendOnlyFile.aof");

    @BeforeEach
    void setUp() throws IOException {
        PersistenceManager.shutdown(); // Ensure clean state
        Files.deleteIfExists(AOF_PATH);
        PersistenceManager.initAofStream();
        RedisStorage.clear();
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(AOF_PATH);
    }

    @Test
    void testAofLogAndRead() throws IOException {
        RespArray cmd = createCommand("SET", "key1", "val1");
        PersistenceManager.log(cmd);

        PersistenceManager.sync();

        List<RespArray> history = PersistenceManager.readAof();
        assertFalse(history.isEmpty());
        assertEquals("SET", ((RespBulkString) history.get(0).elements().get(0)).asUtf8());
        assertEquals("key1", ((RespBulkString) history.get(0).elements().get(1)).asUtf8());
        assertEquals("val1", ((RespBulkString) history.get(0).elements().get(2)).asUtf8());
    }

    @Test
    void testAofRewrite() throws IOException {
        // Perform multiple writes to the same key
        RedisStorage.putString("user:1", "old_name");
        PersistenceManager.log(createCommand("SET", "user:1", "old_name"));

        RedisStorage.putString("user:1", "new_name");
        PersistenceManager.log(createCommand("SET", "user:1", "new_name"));

        PersistenceManager.sync();
        long sizeBefore = Files.size(AOF_PATH);

        PersistenceManager.rewriteAof();

        long sizeAfter = Files.size(AOF_PATH);
        assertTrue(sizeAfter < sizeBefore,
                "Rewritten AOF (" + sizeAfter + ") should be smaller than original (" + sizeBefore + ")");

        List<RespArray> history = PersistenceManager.readAof();
        // Should only have 1 SET command for user:1 now
        assertEquals(1,
                history.stream().filter(c -> ((RespBulkString) c.elements().get(1)).asUtf8().equals("user:1")).count());
    }

    @Test
    void testAofRewriteRecoveryPreservesListWithoutDuplicatingLog() throws IOException {
        RedisStorage.pushList("tasks", "third");
        PersistenceManager.log(createCommand("LPUSH", "tasks", "third"));

        RedisStorage.pushList("tasks", "second");
        PersistenceManager.log(createCommand("LPUSH", "tasks", "second"));

        RedisStorage.pushList("tasks", "first");
        PersistenceManager.log(createCommand("LPUSH", "tasks", "first"));

        PersistenceManager.sync();
        PersistenceManager.rewriteAof();
        long sizeBeforeRecovery = Files.size(AOF_PATH);

        RedisStorage.clear();
        RedisServer.recovery();
        PersistenceManager.sync();

        String result = new LRangeCommand().execute(new RespArray(List.of(
                new RespBulkString("LRANGE".getBytes()),
                new RespBulkString("tasks".getBytes()),
                new RespBulkString("0".getBytes()),
                new RespBulkString("-1".getBytes()))), null);

        assertEquals("*3\r\n$5\r\nfirst\r\n$6\r\nsecond\r\n$5\r\nthird\r\n", result);
        assertEquals(sizeBeforeRecovery, Files.size(AOF_PATH),
                "Recovery should replay AOF without appending duplicate commands");
    }

    @Test
    void testAofRewriteRecoveryPreservesSortedSet() throws IOException {
        RedisStorage.zadd("leaders", 10.0, "alice");
        PersistenceManager.log(createCommand("ZADD", "leaders", "10.0", "alice"));

        RedisStorage.zadd("leaders", 20.0, "carol");
        PersistenceManager.log(createCommand("ZADD", "leaders", "20.0", "carol"));

        RedisStorage.zadd("leaders", 15.0, "bob");
        PersistenceManager.log(createCommand("ZADD", "leaders", "15.0", "bob"));

        PersistenceManager.sync();
        PersistenceManager.rewriteAof();
        RedisStorage.clear();
        RedisServer.recovery();

        assertEquals(List.of("alice", "bob", "carol"),
                RedisStorage.zrange("leaders", 0, -1, false).orElseThrow());
        assertEquals(List.of("alice", "10.0", "bob", "15.0", "carol", "20.0"),
                RedisStorage.zrange("leaders", 0, -1, true).orElseThrow());
    }

    private RespArray createCommand(String... parts) {
        java.util.List<dev.yewintnaing.protocol.RespType> elements = new java.util.ArrayList<>();
        for (String part : parts) {
            elements.add(new RespBulkString(part.getBytes(StandardCharsets.UTF_8)));
        }
        return new RespArray(elements);
    }
}
