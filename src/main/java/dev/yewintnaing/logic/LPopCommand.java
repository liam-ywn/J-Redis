package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespString;
import dev.yewintnaing.storage.RedisStorage;

public class LPopCommand implements RedisCommand {
    @Override
    public String execute(RespArray args) {
        if (args.elements().size() < 2)
            return "-ERR LPOP requires a key\r\n";

        String key = ((RespString) args.elements().get(1)).value();

        return RedisStorage.popList(key)
                .map(val -> "$" + val.length() + "\r\n" + val + "\r\n")
                .orElse("$-1\r\n");
    }
}