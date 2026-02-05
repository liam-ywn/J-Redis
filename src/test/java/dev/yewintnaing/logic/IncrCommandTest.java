package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IncrCommandTest {

    private IncrCommand incrCommand;

    @BeforeEach
    void setUp() {
        incrCommand = new IncrCommand();

    }

    @Test
    void testIncrNewKey() {
        String key = "test_incr_new_key_" + System.currentTimeMillis();
        RespArray args = new RespArray(List.of(
                new RespBulkString("INCR".getBytes()),
                new RespBulkString(key.getBytes())));

        String result = incrCommand.execute(args);
        assertEquals(":1\r\n", result);

        // Incr again
        result = incrCommand.execute(args);
        assertEquals(":2\r\n", result);
    }

    @Test
    void testIncrExistingStringInteger() {
        String key = "test_incr_existing_int_" + System.currentTimeMillis();
        RedisStorage.putString(key, "10");

        RespArray args = new RespArray(List.of(
                new RespBulkString("INCR".getBytes()),
                new RespBulkString(key.getBytes())));

        String result = incrCommand.execute(args);
        assertEquals(":11\r\n", result);
    }

    @Test
    void testIncrInvalidType() {
        String key = "test_incr_invalid_type_" + System.currentTimeMillis();
        RedisStorage.putString(key, "not-a-number");

        RespArray args = new RespArray(List.of(
                new RespBulkString("INCR".getBytes()),
                new RespBulkString(key.getBytes())));

        String result = incrCommand.execute(args);
        assertEquals("-ERR value is not an integer or out of range\r\n", result);
    }

    @Test
    void testMissingArguments() {
        RespArray args = new RespArray(List.of(
                new RespBulkString("INCR".getBytes())));

        String result = incrCommand.execute(args);
        assertEquals("-ERR wrong number of arguments for 'incr' command\r\n", result);
    }
}
