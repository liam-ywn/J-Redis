package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;

public class TTLCommand implements RedisCommand {
    @Override
    public String execute(RespArray args, dev.yewintnaing.handler.ClientHandler client) {
        var elements = args.elements();
        if (elements.size() < 2) {
            return "-ERR wrong number of arguments for 'ttl' command\r\n";
        }

        String key = ((RespBulkString) elements.get(1)).asUtf8();
        long ttl = RedisStorage.getTTL(key);

        return ":" + ttl + "\r\n";
    }
}
