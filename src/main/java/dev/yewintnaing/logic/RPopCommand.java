package dev.yewintnaing.logic;

import dev.yewintnaing.handler.ClientHandler;
import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;

public class RPopCommand implements RedisCommand {

    @Override
    public String execute(RespArray args, ClientHandler client) {
        if (args.elements().size() != 2) {
            return "-ERR wrong number of arguments for 'rpop' command\r\n";
        }

        String key = ((RespBulkString) args.elements().get(1)).asUtf8();

        return RedisStorage.popListRight(key)
                .map(val -> "$" + val.length() + "\r\n" + val + "\r\n")
                .orElse("$-1\r\n");
    }

    @Override
    public boolean isWriteCommand() {
        return true;
    }
}
