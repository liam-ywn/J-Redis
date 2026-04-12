package dev.yewintnaing.storage;

public sealed interface RedisValue permits ListValue, LongValue, StringValue, HashValue, SetValue, ZSetValue {
    public long expiryTime();

    default boolean isExpired() {
        return expiryTime() != 0 && System.currentTimeMillis() > expiryTime();
    }
}
