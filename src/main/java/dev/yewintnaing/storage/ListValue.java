package dev.yewintnaing.storage;

import java.util.Deque;

public record ListValue(Deque<String> value, long expiryTime) implements RedisValue {
    @Override
    public long expiryTime() {
        return expiryTime;
    }
}
