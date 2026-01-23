package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;

class PingCommand implements RedisCommand {
    @Override
    public String execute(RespArray args) {

        return "+PONG\r\n";
    }
}
