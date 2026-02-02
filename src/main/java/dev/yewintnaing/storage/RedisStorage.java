package dev.yewintnaing.storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import java.util.Optional;

public class RedisStorage {

    private static final ConcurrentHashMap<String, RedisValue> DATA = new ConcurrentHashMap<>();

    public static void putString(String key, String value) {
        DATA.put(key, new StringValue(value, 0));
    }

    public static Optional<String> getString(String key) {
        RedisValue value = DATA.get(key);

        if (value == null)
            return Optional.empty();

        if (value.isExpired()) {
            DATA.remove(key);
            return Optional.empty();
        }

        if (value instanceof StringValue s) {
            return Optional.of(s.value());
        }

        return Optional.empty();
    }

    public static Optional<Long> getLength(String key) {
        RedisValue value = DATA.get(key);

        if (value == null)
            return Optional.empty();

        if (value.isExpired()) {
            DATA.remove(key);
            return Optional.of(0L);
        }

        if (value instanceof ListValue listValue) {
            return Optional.of((long) listValue.value().size());
        }

        return Optional.of(0L);
    }

    public static Optional<ListValue> getList(String key) {
        RedisValue value = DATA.get(key);

        if (value == null)
            return Optional.empty();

        if (value.isExpired()) {
            DATA.remove(key);
            return Optional.empty();
        }

        if (value instanceof ListValue listValue) {
            return Optional.of(listValue);
        }

        return Optional.empty();
    }

    public static void pushList(String key, String value) {

//        DATA.computeIfAbsent(key, s ->  new StringValue(value, 0));

        DATA.compute(key, (k, old) -> {

                    if (old == null) {
                        var listValue = new ListValue(new ConcurrentLinkedDeque<>(), 0);
                        listValue.value().addFirst(value);
                        return listValue;
                    }

                    if (old instanceof ListValue listValue) {
                        listValue.value().addFirst(value);
                        return listValue;
                    }

                    throw new IllegalStateException("Invalid type!");
                }
        );


    }

    public static Optional<String> popList(String key) {

        String[] resultHolder = new String[1];

        DATA.computeIfPresent(key, (k, old) -> {
            if (old instanceof ListValue listValue) {

                var deque = listValue.value();

                if (!deque.isEmpty()) {
                    resultHolder[0] = deque.pollFirst();
                }

                return deque.isEmpty() ? null : listValue;
            }
            return old;
        });

        return Optional.ofNullable(resultHolder[0]);
    }

    public static boolean setExpiry(String key, long seconds) {
        long expiryTime = System.currentTimeMillis() + (seconds * 1000L);

        RedisValue updated = DATA.computeIfPresent(key, (k, old) -> {

            switch (old) {
                case StringValue stringValue -> {
                    return new StringValue(stringValue.value(), expiryTime);
                }
                case ListValue listValue -> {
                    return new ListValue(listValue.value(), expiryTime);
                }
                default -> throw new IllegalStateException("Invalid Type");
            }
        });

        return updated != null;
    }

    public static void removeExpired() {
        DATA.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public static Optional<List<String>> getListRange(String key, long start, long stop) {

        RedisValue value = DATA.get(key);

        if (value == null)
            return Optional.empty();

        if (!(value instanceof ListValue listValue)) {
            throw new IllegalStateException("Invalid Type!");
        }

        var deque = listValue.value();

        var size = deque.size();

        // 1. Normalize Indices (Handle negatives)
        long normalizedStart = (start < 0) ? Math.max(0, size + start) : start;
        long normalizedStop = (stop < 0) ? size + stop : stop;

        // 2. Bound Check
        if (normalizedStart >= size || normalizedStart > normalizedStop) {
            return Optional.of(List.of());
        }

        // Ensure we don't go past the end of the list
        normalizedStop = Math.min(normalizedStop, size - 1);

        // 3. Extract the Range
        // Using streams to skip and limit is much cleaner than a manual loop with peek()
        List<String> result = deque.stream()
                .skip(normalizedStart)
                .limit(normalizedStop - normalizedStart + 1)
                .toList();

        return Optional.of(result);
    }


}