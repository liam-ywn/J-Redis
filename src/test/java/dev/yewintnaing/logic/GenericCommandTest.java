package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GenericCommandTest {

    @Test
    void testDelAndExists() {
        RedisStorage.putString("key1", "val1");
        RedisStorage.putString("key2", "val2");

        ExistsCommand exists = new ExistsCommand();
        String existsResult = exists.execute(new RespArray(List.of(
                new RespBulkString("EXISTS".getBytes()),
                new RespBulkString("key1".getBytes()),
                new RespBulkString("key2".getBytes()),
                new RespBulkString("key3".getBytes()))), null);
        assertEquals(":2\r\n", existsResult);

        DelCommand del = new DelCommand();
        String delResult = del.execute(new RespArray(List.of(
                new RespBulkString("DEL".getBytes()),
                new RespBulkString("key1".getBytes()),
                new RespBulkString("key3".getBytes()))), null);
        assertEquals(":1\r\n", delResult);

        existsResult = exists.execute(new RespArray(List.of(
                new RespBulkString("EXISTS".getBytes()),
                new RespBulkString("key1".getBytes()))), null);
        assertEquals(":0\r\n", existsResult);
    }

    @Test
    void testKeys() {
        RedisStorage.putString("abc", "1");
        RedisStorage.putString("def", "2");

        KeysCommand keys = new KeysCommand();
        String result = keys.execute(new RespArray(List.of(
                new RespBulkString("KEYS".getBytes()),
                new RespBulkString("*".getBytes()))), null);

        assertTrue(result.contains("abc"));
        assertTrue(result.contains("def"));
    }

    @Test
    void testTTL() throws InterruptedException {
        String key = "ttlkey";
        RedisStorage.putString(key, "val");

        TTLCommand ttl = new TTLCommand();
        assertEquals(":-1\r\n", ttl.execute(new RespArray(List.of(
                new RespBulkString("TTL".getBytes()),
                new RespBulkString(key.getBytes()))), null));

        RedisStorage.setExpiry(key, 10);
        String ttlResult = ttl.execute(new RespArray(List.of(
                new RespBulkString("TTL".getBytes()),
                new RespBulkString(key.getBytes()))), null);
        assertTrue(ttlResult.startsWith(":"));
        long remaining = Long.parseLong(ttlResult.substring(1, ttlResult.length() - 2));
        assertTrue(remaining <= 10 && remaining > 0);

        RedisStorage.setExpiry(key, 1);
        Thread.sleep(1100);
        assertEquals(":-2\r\n", ttl.execute(new RespArray(List.of(
                new RespBulkString("TTL".getBytes()),
                new RespBulkString(key.getBytes()))), null));
    }
}
