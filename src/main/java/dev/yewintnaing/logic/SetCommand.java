package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespString;
import dev.yewintnaing.storage.RedisStorage;

class SetCommand implements RedisCommand {
    @Override
    public String execute(RespArray args) {

        String key = ((RespString) args.elements().get(1)).value();
        String val = ((RespString) args.elements().get(2)).value();

        RedisStorage.put(key, val);


        return "+OK\r\n";
    }
}
