package dev.yewintnaing.logic;

import dev.yewintnaing.protocol.RespArray;

public interface RedisCommand {
    String execute(RespArray args);
}