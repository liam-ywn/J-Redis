package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;

public class DelCommand implements RedisCommand {
    @Override
    public String execute(RespArray args, dev.yewintnaing.handler.ClientHandler client) {
        var elements = args.elements();
        if (elements.size() < 2) {
            return "-ERR wrong number of arguments for 'del' command\r\n";
        }

        String[] keys = elements.stream()
                .skip(1)
                .filter(e -> e instanceof RespBulkString)
                .map(e -> ((RespBulkString) e).asUtf8())
                .toArray(String[]::new);

        int removed = RedisStorage.del(keys);
        return ":" + removed + "\r\n";
    }

    @Override
    public boolean isWriteCommand() {
        return true;
    }
}
