package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespString;
import dev.yewintnaing.storage.RedisStorage;

public class LLenCommand implements RedisCommand {

    @Override
    public String execute(RespArray args) {

        var elements = args.elements();

        if (elements.get(1) instanceof RespString(String key)) {

            var optSize = RedisStorage.getLength(key);

            return optSize.map(aLong -> ":" + aLong + "\r\n").orElse("-WRONGTYPE" + "\r\n");
        }

        return "-ERR Invalid key format\r\n";
    }

}
