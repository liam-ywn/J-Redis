package dev.yewintnaing.storage;

import java.util.Map;

public record ZSetValue(Map<String, Double> memberScores, SkipList index, long expiryTime) implements RedisValue {
    @Override
    public long expiryTime() {
        return expiryTime;
    }
}
