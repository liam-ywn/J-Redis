package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespString;
import dev.yewintnaing.storage.RedisStorage;

public class GetCommand implements RedisCommand {
    @Override
    public String execute(RespArray args) {

        String key = ((RespString) args.elements().get(1)).value();

        return RedisStorage
                .get(key)
                .map((val) -> "$" + val.length() + "\r\n" + val + "\r\n")
                .orElse("$-1\r\n");
    }
}
