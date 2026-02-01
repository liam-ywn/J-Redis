package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespString;
import dev.yewintnaing.storage.RedisStorage;

public class GetCommand implements RedisCommand {
    @Override
    public String execute(RespArray args) {

        var elements = args.elements();

        if (elements.size() < 2) {
            return "-ERR wrong number of arguments for 'get' command\r\n";
        }

        if (elements.get(1) instanceof RespString(String key)) {
            return RedisStorage
                    .getString(key)
                    .map((val) -> "$" + val.length() + "\r\n" + val + "\r\n")
                    .orElse("$-1\r\n");
        }

        return "-ERR Invalid key format\r\n";
    }
}
