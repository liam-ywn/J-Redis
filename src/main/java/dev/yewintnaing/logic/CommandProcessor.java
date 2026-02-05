package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.PersistenceManager;

import java.util.Map;
import java.util.HashMap;

public class CommandProcessor {
    private static final Map<String, RedisCommand> COMMANDS = new HashMap<>();

    static {
        COMMANDS.put("PING", new PingCommand());
        COMMANDS.put("SET", new SetCommand());
        COMMANDS.put("GET", new GetCommand());
        COMMANDS.put("EXPIRE", new ExpireCommand());
        COMMANDS.put("LPUSH", new LPushCommand());
        COMMANDS.put("LPOP", new LPopCommand());
        COMMANDS.put("LLEN", new LLenCommand());
        COMMANDS.put("INCR", new IncrCommand());
        COMMANDS.put("LRANGE", new LRangeCommand());
    }

    public static String handle(RespArray request) {
        var elements = request.elements();
        String cmdName = ((RespBulkString) elements.getFirst()).asUtf8().toUpperCase();

        RedisCommand command = COMMANDS.get(cmdName);



        if (command == null) {
            return "-ERR unknown command '" + cmdName + "'\r\n";
        }

        if (command.isWriteCommand()) {
            PersistenceManager.log(request);
        }

        return command.execute(request);
    }
}