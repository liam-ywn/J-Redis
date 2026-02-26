package dev.yewintnaing.storage;

import java.util.Map;

public record HashValue(Map<String, String> value, long expiryTime) implements RedisValue {
    @Override
    public long expiryTime() {
        return expiryTime;
    }
}
