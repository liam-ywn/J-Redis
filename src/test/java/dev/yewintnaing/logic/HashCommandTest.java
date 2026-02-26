package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HashCommandTest {

    @BeforeEach
    void setUp() {
        // Clear storage if needed, but since it's static and we don't have a clear
        // method,
        // we'll just use unique keys.
    }

    @Test
    void testHSetAndHGet() {
        String key = "myhash";
        String field = "name";
        String value = "yewint";

        RespArray hsetArgs = new RespArray(List.of(
                new RespBulkString("HSET".getBytes()),
                new RespBulkString(key.getBytes()),
                new RespBulkString(field.getBytes()),
                new RespBulkString(value.getBytes())));

        HSetCommand hset = new HSetCommand();
        assertEquals(":1\r\n", hset.execute(hsetArgs, null));

        RespArray hgetArgs = new RespArray(List.of(
                new RespBulkString("HGET".getBytes()),
                new RespBulkString(key.getBytes()),
                new RespBulkString(field.getBytes())));

        HGetCommand hget = new HGetCommand();
        assertEquals("$6\r\nyewint\r\n", hget.execute(hgetArgs, null));
    }

    @Test
    void testHLenAndHGetAll() {
        String key = "myhash2";

        RedisStorage.hset(key, "f1", "v1");
        RedisStorage.hset(key, "f2", "v2");

        HLenCommand hlen = new HLenCommand();
        assertEquals(":2\r\n", hlen.execute(new RespArray(List.of(
                new RespBulkString("HLEN".getBytes()),
                new RespBulkString(key.getBytes()))), null));

        HGetAllCommand hgetall = new HGetAllCommand();
        String result = hgetall.execute(new RespArray(List.of(
                new RespBulkString("HGETALL".getBytes()),
                new RespBulkString(key.getBytes()))), null);

        assertTrue(result.contains("f1"));
        assertTrue(result.contains("v1"));
        assertTrue(result.contains("f2"));
        assertTrue(result.contains("v2"));
        assertTrue(result.startsWith("*4\r\n"));
    }

    @Test
    void testHDel() {
        String key = "myhash3";
        RedisStorage.hset(key, "f1", "v1");

        HDelCommand hdel = new HDelCommand();
        assertEquals(":1\r\n", hdel.execute(new RespArray(List.of(
                new RespBulkString("HDEL".getBytes()),
                new RespBulkString(key.getBytes()),
                new RespBulkString("f1".getBytes()))), null));

        assertEquals(":0\r\n", hlen(key));
    }

    @Test
    void testHashTTL() throws InterruptedException {
        String key = "ttlhash";
        RedisStorage.hset(key, "field", "value");

        ExpireCommand expire = new ExpireCommand();
        assertEquals(":1\r\n", expire.execute(new RespArray(List.of(
                new RespBulkString("EXPIRE".getBytes()),
                new RespBulkString(key.getBytes()),
                new RespBulkString("1".getBytes()))), null));

        // Should still exist
        assertEquals(":1\r\n", hlen(key));

        // Wait for expiration
        Thread.sleep(1100);

        // Should be gone
        assertEquals(":0\r\n", hlen(key));
    }

    private String hlen(String key) {
        return new HLenCommand().execute(new RespArray(List.of(
                new RespBulkString("HLEN".getBytes()),
                new RespBulkString(key.getBytes()))), null);
    }
}
