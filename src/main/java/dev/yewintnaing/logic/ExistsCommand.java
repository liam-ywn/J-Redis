package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;

public class ExistsCommand implements RedisCommand {
    @Override
    public String execute(RespArray args, dev.yewintnaing.handler.ClientHandler client) {
        var elements = args.elements();
        if (elements.size() < 2) {
            return "-ERR wrong number of arguments for 'exists' command\r\n";
        }

        String[] keys = elements.stream()
                .skip(1)
                .filter(e -> e instanceof RespBulkString)
                .map(e -> ((RespBulkString) e).asUtf8())
                .toArray(String[]::new);

        int count = RedisStorage.exists(keys);
        return ":" + count + "\r\n";
    }
}
