package dev.yewintnaing.logic;

import dev.yewintnaing.handler.ClientHandler;
import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;

public class MGetCommand implements RedisCommand {
    @Override
    public String execute(RespArray args, ClientHandler client) {

        // mget key [key ...]
        if (args.elements().size() < 2) {
            return "-ERR wrong number of arguments for 'mget' command\r\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("*");
        sb.append(args.elements().size() - 1);
        sb.append("\r\n");

        for (int i = 1; i < args.elements().size(); i++) {
            var element = args.elements().get(i);
            if (element instanceof RespBulkString key) {
                sb
                        .append(
                                RedisStorage.getString(key.asUtf8())
                                        .map(s -> "$" + s.length() + "\r\n" + s + "\r\n")
                                        .orElse("$-1\r\n")
                        );
            } else {
                sb.append("$-1\r\n");
            }


        }

        return sb.toString();
    }
}
