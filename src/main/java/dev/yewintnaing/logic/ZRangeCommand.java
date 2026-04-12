package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;

public class ZRangeCommand implements RedisCommand {
    @Override
    public String execute(RespArray args, dev.yewintnaing.handler.ClientHandler client) {
        var elements = args.elements();
        if (elements.size() < 4 || elements.size() > 5) {
            return "-ERR wrong number of arguments for 'zrange' command\r\n";
        }

        String key = ((RespBulkString) elements.get(1)).asUtf8();
        boolean withScores = elements.size() == 5 &&
                "WITHSCORES".equalsIgnoreCase(((RespBulkString) elements.get(4)).asUtf8());

        if (elements.size() == 5 && !withScores) {
            return "-ERR syntax error\r\n";
        }

        try {
            long start = Long.parseLong(((RespBulkString) elements.get(2)).asUtf8());
            long stop = Long.parseLong(((RespBulkString) elements.get(3)).asUtf8());

            return RedisStorage.zrange(key, start, stop, withScores)
                    .map(values -> {
                        StringBuilder sb = new StringBuilder();
                        sb.append("*").append(values.size()).append("\r\n");
                        for (String value : values) {
                            sb.append("$").append(value.length()).append("\r\n")
                                    .append(value).append("\r\n");
                        }
                        return sb.toString();
                    })
                    .orElse("*0\r\n");
        } catch (NumberFormatException e) {
            return "-ERR value is not an integer or out of range\r\n";
        } catch (IllegalStateException e) {
            return "-ERR " + e.getMessage() + "\r\n";
        }
    }
}
