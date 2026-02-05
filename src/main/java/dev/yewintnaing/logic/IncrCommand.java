package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;

public class IncrCommand implements RedisCommand {

    @Override
    public String execute(RespArray args) {

        var elements = args.elements();

        if (elements.size() < 2) {
            return "-ERR wrong number of arguments for 'incr' command\r\n";
        }

        if (elements.getLast() instanceof RespBulkString bulkString) {
            String key = bulkString.asUtf8();

            try {
                var result = RedisStorage.incr(key);
                return ":" + result + "\r\n";
            } catch (IllegalStateException | NumberFormatException e) {
                return "-ERR value is not an integer or out of range\r\n";
            }
        }

        return "-ERR Invalid key format\r\n";
    }

    @Override
    public boolean isWriteCommand() {

        return true;
    }

}
