package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;

class HLenCommand implements RedisCommand {
    @Override
    public String execute(RespArray args, dev.yewintnaing.handler.ClientHandler client) {
        if (args.elements().size() != 2) {
            return "-ERR wrong number of arguments for 'hlen' command\r\n";
        }

        String key = ((RespBulkString) args.elements().get(1)).asUtf8();

        try {
            long length = RedisStorage.hlen(key);
            return ":" + length + "\r\n";
        } catch (IllegalStateException e) {
            return "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n";
        }
    }

    @Override
    public boolean isWriteCommand() {
        return false;
    }
}
