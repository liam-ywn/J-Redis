package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;

public class ZAddCommand implements RedisCommand {
    @Override
    public String execute(RespArray args, dev.yewintnaing.handler.ClientHandler client) {
        var elements = args.elements();
        if (elements.size() < 4 || (elements.size() - 2) % 2 != 0) {
            return "-ERR wrong number of arguments for 'zadd' command\r\n";
        }

        String key = ((RespBulkString) elements.get(1)).asUtf8();
        int added = 0;

        try {
            for (int i = 2; i < elements.size(); i += 2) {
                double score = Double.parseDouble(((RespBulkString) elements.get(i)).asUtf8());
                String member = ((RespBulkString) elements.get(i + 1)).asUtf8();
                added += RedisStorage.zadd(key, score, member);
            }
            return ":" + added + "\r\n";
        } catch (NumberFormatException e) {
            return "-ERR value is not a valid float\r\n";
        } catch (IllegalStateException e) {
            return "-ERR " + e.getMessage() + "\r\n";
        }
    }

    @Override
    public boolean isWriteCommand() {
        return true;
    }
}
