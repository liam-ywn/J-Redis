package dev.yewintnaing.storage;

public record StringValue(String value, long expiryTime) implements RedisValue {
    @Override
    public long expiryTime() {
        return expiryTime;
    }
}