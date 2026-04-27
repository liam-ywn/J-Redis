package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.protocol.RespType;
import dev.yewintnaing.storage.RedisStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListCommandTest {

    @BeforeEach
    void setUp() {
        RedisStorage.clear();
    }

    @Test
    void rpopRemovesLastElementAndDeletesEmptyListKey() {
        RedisStorage.pushListRight("tasks", "one");

        RPopCommand rpop = new RPopCommand();
        assertEquals("$3\r\none\r\n", rpop.execute(command("RPOP", "tasks"), null));
        assertEquals("$-1\r\n", rpop.execute(command("RPOP", "tasks"), null));

        ExistsCommand exists = new ExistsCommand();
        assertEquals(":0\r\n", exists.execute(command("EXISTS", "tasks"), null));
    }

    @Test
    void rpopRejectsExtraArguments() {
        RPopCommand rpop = new RPopCommand();

        assertEquals("-ERR wrong number of arguments for 'rpop' command\r\n",
                rpop.execute(command("RPOP", "tasks", "extra"), null));
    }

    private RespArray command(String... parts) {
        List<RespType> elements = List.of(parts).stream()
                .map(part -> (RespType) new RespBulkString(part.getBytes()))
                .toList();
        return new RespArray(elements);
    }
}
