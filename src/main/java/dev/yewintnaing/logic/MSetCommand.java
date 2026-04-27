package dev.yewintnaing.logic;

import dev.yewintnaing.handler.ClientHandler;
import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.storage.RedisStorage;

public class MSetCommand implements RedisCommand {
    @Override
    public String execute(RespArray args, ClientHandler client) {

        if (args.elements().size() < 3) {
            return "-ERR wrong number of arguments for 'mset' command\r\n";
        }

        var noOfKeyAndValue = (args.elements().size() + 1) / 2;

        if ((args.elements().size() + 1) % 2 != 0) {
            return "-ERR wrong number of arguments for 'mset' command\r\n";
        }


        var index = 1;
        for (int i = 0; i < noOfKeyAndValue - 1; i++) {


            if (args.elements().get(index) instanceof RespBulkString key &&
                args.elements().get(index + 1) instanceof RespBulkString value
            ) {
                RedisStorage.putString(key.asUtf8(), value.asUtf8());
                index += 2;
            }


        }


        return "+OK\r\n";
    }


    @Override
    public boolean isWriteCommand() {
        return true;
    }
}
