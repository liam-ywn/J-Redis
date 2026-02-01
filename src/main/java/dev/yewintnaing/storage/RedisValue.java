package dev.yewintnaing.storage;

public sealed interface RedisValue permits StringValue, ListValue {
    public long expiryTime();

    default boolean isExpired() {
        return expiryTime() != 0 && System.currentTimeMillis() > expiryTime();
    }
}