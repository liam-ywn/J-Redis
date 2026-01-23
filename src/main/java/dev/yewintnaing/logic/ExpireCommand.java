package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespString;
import dev.yewintnaing.storage.RedisStorage;

class ExpireCommand implements RedisCommand {
    @Override
    public String execute(RespArray args) {

        String key = ((RespString) args.elements().get(1)).value();
        long seconds = Long.parseLong(((RespString) args.elements().get(2)).value());


        return RedisStorage.setExpiry(key, seconds) ? ":1\r\n" : ":0\r\n";
    }
}
