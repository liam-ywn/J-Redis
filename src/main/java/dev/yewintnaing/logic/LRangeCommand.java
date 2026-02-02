package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespString;
import dev.yewintnaing.storage.RedisStorage;

public class LRangeCommand implements RedisCommand {
    @Override
    public String execute(RespArray args) {

        var elements = args.elements();

        if (elements.get(1) instanceof RespString(String key)) {

            long start = Long.parseLong(((RespString) elements.get(2)).value());
            long stop = Long.parseLong(((RespString) elements.get(3)).value());


            return RedisStorage.getListRange(key, start, stop)
                    .map(list -> {
                        StringBuilder sb = new StringBuilder();
                        sb.append("*").append(list.size()).append("\r\n");
                        for (String item : list) {
                            sb.append("$").append(item.length()).append("\r\n")
                                    .append(item).append("\r\n");
                        }
                        return sb.toString();
                    })
                    .orElse("*0\r\n");


        }

        return "";
    }
}
