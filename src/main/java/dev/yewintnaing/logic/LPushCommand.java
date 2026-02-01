package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespString;
import dev.yewintnaing.storage.RedisStorage;

public class LPushCommand implements RedisCommand {
    @Override
    public String execute(RespArray args) {

        if (args.elements().size() < 3)
            return "-ERR LPUSH requires key and value\r\n";

        String key = ((RespString) args.elements().get(1)).value();
        String val = ((RespString) args.elements().get(2)).value();

        RedisStorage.pushList(key, val);

        return "+OK\r\n";
    }
}
