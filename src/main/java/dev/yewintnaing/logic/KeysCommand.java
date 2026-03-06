package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;

import java.util.List;

public class KeysCommand implements RedisCommand {
    @Override
    public String execute(RespArray args, dev.yewintnaing.handler.ClientHandler client) {
        var elements = args.elements();
        if (elements.size() < 2) {
            return "-ERR wrong number of arguments for 'keys' command\r\n";
        }

        String pattern = ((RespBulkString) elements.get(1)).asUtf8();
        List<String> keys = RedisStorage.keys(pattern);

        StringBuilder sb = new StringBuilder();
        sb.append("*").append(keys.size()).append("\r\n");
        for (String key : keys) {
            sb.append("$").append(key.length()).append("\r\n").append(key).append("\r\n");
        }
        return sb.toString();
    }
}
