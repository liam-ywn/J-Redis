package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;

public class ZRemCommand implements RedisCommand {
    @Override
    public String execute(RespArray args, dev.yewintnaing.handler.ClientHandler client) {
        var elements = args.elements();
        if (elements.size() < 3) {
            return "-ERR wrong number of arguments for 'zrem' command\r\n";
        }

        String key = ((RespBulkString) elements.get(1)).asUtf8();
        String[] members = elements.stream()
                .skip(2)
                .map(element -> ((RespBulkString) element).asUtf8())
                .toArray(String[]::new);

        try {
            return ":" + RedisStorage.zrem(key, members) + "\r\n";
        } catch (IllegalStateException e) {
            return "-ERR " + e.getMessage() + "\r\n";
        }
    }

    @Override
    public boolean isWriteCommand() {
        return true;
    }
}
