package dev.yewintnaing.storage;

public record LongValue(long value) implements RedisValue {

    @Override
    public long expiryTime() {

        return 0;
    }

}