package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;
import java.util.Map;
import java.util.Optional;

class HGetAllCommand implements RedisCommand {
    @Override
    public String execute(RespArray args, dev.yewintnaing.handler.ClientHandler client) {
        if (args.elements().size() != 2) {
            return "-ERR wrong number of arguments for 'hgetall' command\r\n";
        }

        String key = ((RespBulkString) args.elements().get(1)).asUtf8();

        try {
            Optional<Map<String, String>> hash = RedisStorage.hgetall(key);
            if (hash.isEmpty()) {
                return "*0\r\n";
            }

            Map<String, String> map = hash.get();
            StringBuilder sb = new StringBuilder();
            sb.append("*").append(map.size() * 2).append("\r\n");

            for (Map.Entry<String, String> entry : map.entrySet()) {
                String field = entry.getKey();
                String value = entry.getValue();

                sb.append("$").append(field.length()).append("\r\n").append(field).append("\r\n");
                sb.append("$").append(value.length()).append("\r\n").append(value).append("\r\n");
            }

            return sb.toString();
        } catch (IllegalStateException e) {
            return "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n";
        }
    }

    @Override
    public boolean isWriteCommand() {
        return false;
    }
}
