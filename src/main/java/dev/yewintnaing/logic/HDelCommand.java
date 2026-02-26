package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;

class HDelCommand implements RedisCommand {
    @Override
    public String execute(RespArray args, dev.yewintnaing.handler.ClientHandler client) {
        if (args.elements().size() != 3) {
            return "-ERR wrong number of arguments for 'hdel' command\r\n";
        }

        String key = ((RespBulkString) args.elements().get(1)).asUtf8();
        String field = ((RespBulkString) args.elements().get(2)).asUtf8();

        try {
            int result = RedisStorage.hdel(key, field);
            return ":" + result + "\r\n";
        } catch (IllegalStateException e) {
            return "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n";
        }
    }

    @Override
    public boolean isWriteCommand() {
        return true;
    }
}
