package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;
import java.util.Optional;

class HGetCommand implements RedisCommand {
    @Override
    public String execute(RespArray args, dev.yewintnaing.handler.ClientHandler client) {
        if (args.elements().size() != 3) {
            return "-ERR wrong number of arguments for 'hget' command\r\n";
        }

        String key = ((RespBulkString) args.elements().get(1)).asUtf8();
        String field = ((RespBulkString) args.elements().get(2)).asUtf8();

        try {
            Optional<String> value = RedisStorage.hget(key, field);
            if (value.isEmpty()) {
                return "$-1\r\n";
            }
            byte[] bytes = value.get().getBytes();
            return "$" + bytes.length + "\r\n" + value.get() + "\r\n";
        } catch (IllegalStateException e) {
            return "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n";
        }
    }

    @Override
    public boolean isWriteCommand() {
        return false;
    }
}
