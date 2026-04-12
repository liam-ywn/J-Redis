package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZSetCommandTest {

    @BeforeEach
    void setUp() {
        RedisStorage.clear();
    }

    @Test
    void testZAddAndZRange() {
        ZAddCommand zadd = new ZAddCommand();
        ZRangeCommand zrange = new ZRangeCommand();

        String addResult = zadd.execute(new RespArray(List.of(
                new RespBulkString("ZADD".getBytes()),
                new RespBulkString("leaders".getBytes()),
                new RespBulkString("10".getBytes()),
                new RespBulkString("alice".getBytes()),
                new RespBulkString("20".getBytes()),
                new RespBulkString("carol".getBytes()),
                new RespBulkString("15".getBytes()),
                new RespBulkString("bob".getBytes()))), null);

        assertEquals(":3\r\n", addResult);
        assertEquals("*3\r\n$5\r\nalice\r\n$3\r\nbob\r\n$5\r\ncarol\r\n",
                zrange.execute(new RespArray(List.of(
                        new RespBulkString("ZRANGE".getBytes()),
                        new RespBulkString("leaders".getBytes()),
                        new RespBulkString("0".getBytes()),
                        new RespBulkString("-1".getBytes()))), null));
    }

    @Test
    void testZAddUpdatesScoreWithoutIncreasingCardinality() {
        ZAddCommand zadd = new ZAddCommand();
        ZCardCommand zcard = new ZCardCommand();
        ZScoreCommand zscore = new ZScoreCommand();
        ZRangeCommand zrange = new ZRangeCommand();

        assertEquals(":1\r\n", zadd.execute(new RespArray(List.of(
                new RespBulkString("ZADD".getBytes()),
                new RespBulkString("leaders".getBytes()),
                new RespBulkString("10".getBytes()),
                new RespBulkString("alice".getBytes()))), null));

        assertEquals(":0\r\n", zadd.execute(new RespArray(List.of(
                new RespBulkString("ZADD".getBytes()),
                new RespBulkString("leaders".getBytes()),
                new RespBulkString("30".getBytes()),
                new RespBulkString("alice".getBytes()))), null));

        assertEquals(":1\r\n", zcard.execute(new RespArray(List.of(
                new RespBulkString("ZCARD".getBytes()),
                new RespBulkString("leaders".getBytes()))), null));

        assertEquals("$4\r\n30.0\r\n", zscore.execute(new RespArray(List.of(
                new RespBulkString("ZSCORE".getBytes()),
                new RespBulkString("leaders".getBytes()),
                new RespBulkString("alice".getBytes()))), null));

        assertEquals("*2\r\n$5\r\nalice\r\n$4\r\n30.0\r\n",
                zrange.execute(new RespArray(List.of(
                        new RespBulkString("ZRANGE".getBytes()),
                        new RespBulkString("leaders".getBytes()),
                        new RespBulkString("0".getBytes()),
                        new RespBulkString("-1".getBytes()),
                        new RespBulkString("WITHSCORES".getBytes()))), null));
    }

    @Test
    void testZRem() {
        RedisStorage.zadd("leaders", 10.0, "alice");
        RedisStorage.zadd("leaders", 15.0, "bob");

        ZRemCommand zrem = new ZRemCommand();
        ZCardCommand zcard = new ZCardCommand();

        assertEquals(":1\r\n", zrem.execute(new RespArray(List.of(
                new RespBulkString("ZREM".getBytes()),
                new RespBulkString("leaders".getBytes()),
                new RespBulkString("alice".getBytes()))), null));

        assertEquals(":1\r\n", zcard.execute(new RespArray(List.of(
                new RespBulkString("ZCARD".getBytes()),
                new RespBulkString("leaders".getBytes()))), null));
    }

    @Test
    void testZSetTTL() throws InterruptedException {
        RedisStorage.zadd("leaders", 10.0, "alice");
        RedisStorage.setExpiry("leaders", 1);

        ZCardCommand zcard = new ZCardCommand();
        assertEquals(":1\r\n", zcard.execute(new RespArray(List.of(
                new RespBulkString("ZCARD".getBytes()),
                new RespBulkString("leaders".getBytes()))), null));

        Thread.sleep(1100);

        assertEquals(":0\r\n", zcard.execute(new RespArray(List.of(
                new RespBulkString("ZCARD".getBytes()),
                new RespBulkString("leaders".getBytes()))), null));
    }
}
