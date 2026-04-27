package dev.yewintnaing.storage;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class RedisStorage {

    private static final ConcurrentHashMap<String, RedisValue> DATA = new ConcurrentHashMap<>();

    public static java.util.Map<String, RedisValue> getSnapshot() {
        return new java.util.HashMap<>(DATA);
    }

    public static void clear() {
        DATA.clear();
    }

    public static long incr(String key) {

        var data = (LongValue) DATA.compute(key, (k, old) -> {
            if (old == null) {
                return new LongValue(1L);
            }

            if (old instanceof LongValue(long value)) {
                return new LongValue(value + 1);
            }

            if (old instanceof StringValue value) {

                try {
                    long longValue = Long.parseLong(value.value());
                    return new LongValue(longValue + 1);
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid type!");
                }

            }

            throw new IllegalStateException("Invalid type!");

        });

        return data.value();

    }

    public static void putString(String key, String value) {

        DATA.put(key, new StringValue(value, 0));
    }

    public static Optional<String> getString(String key) {

        RedisValue value = DATA.get(key);

        if (value == null) {
            return Optional.empty();
        }

        if (value.isExpired()) {
            DATA.remove(key);
            return Optional.empty();
        }

        if (value instanceof StringValue s) {
            return Optional.of(s.value());
        }

        if (value instanceof LongValue l) {
            return Optional.of(String.valueOf(l.value()));
        }

        return Optional.empty();
    }

    public static Optional<Long> getLength(String key) {

        RedisValue value = DATA.get(key);

        if (value == null) {
            return Optional.empty();
        }

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

        if (value == null) {
            return Optional.empty();
        }

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

        // DATA.computeIfAbsent(key, s -> new StringValue(value, 0));

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
        });

    }

    public static void pushListRight(String key, String value) {

        DATA.compute(key, (k, old) -> {

            if (old == null) {
                var listValue = new ListValue(new ConcurrentLinkedDeque<>(), 0);
                listValue.value().addLast(value);
                return listValue;
            }

            if (old instanceof ListValue listValue) {
                listValue.value().addLast(value);
                return listValue;
            }

            throw new IllegalStateException("Invalid type!");
        });

    }

    public static Optional<String> popListRight(String key) {

        String[] resultHolder = new String[1];

        DATA.computeIfPresent(key, (k, old) -> {
            if (old.isExpired()) {
                return null;
            }

            if (old instanceof ListValue listValue) {
                var deque = listValue.value();

                resultHolder[0] = deque.pollLast();

                return deque.isEmpty() ? null : listValue;
            }

            throw new IllegalStateException("Invalid type!");
        });

        return Optional.ofNullable(resultHolder[0]);

    }

    public static int hset(String key, String field, String value) {

        DATA.compute(key, (k, old) -> {
            if (old == null) {
                var map = new java.util.concurrent.ConcurrentHashMap<String, String>();
                map.put(field, value);
                return new HashValue(map, 0);
            }

            if (old instanceof HashValue hv) {
                hv.value().put(field, value);
                return hv;
            }

            throw new IllegalStateException("Invalid type!");
        });

        return 1;
    }

    public static Optional<String> hget(String key, String field) {

        RedisValue value = DATA.get(key);

        if (value == null || value.isExpired()) {
            if (value != null)
                DATA.remove(key);
            return Optional.empty();
        }

        if (value instanceof HashValue hv) {
            return Optional.ofNullable(hv.value().get(field));
        }

        throw new IllegalStateException("Invalid type!");
    }

    public static int hdel(String key, String field) {

        int[] result = new int[1];

        DATA.computeIfPresent(key, (k, old) -> {
            if (old instanceof HashValue hv) {
                if (hv.value().remove(field) != null) {
                    result[0] = 1;
                }
                return hv.value().isEmpty() ? null : hv;
            }
            return old;
        });

        return result[0];
    }

    public static long hlen(String key) {

        RedisValue value = DATA.get(key);

        if (value == null || value.isExpired()) {
            if (value != null)
                DATA.remove(key);
            return 0;
        }

        if (value instanceof HashValue hv) {
            return hv.value().size();
        }

        throw new IllegalStateException("Invalid type!");
    }

    public static Optional<java.util.Map<String, String>> hgetall(String key) {

        RedisValue value = DATA.get(key);

        if (value == null || value.isExpired()) {
            if (value != null)
                DATA.remove(key);
            return Optional.empty();
        }

        if (value instanceof HashValue hv) {
            return Optional.of(hv.value());
        }

        throw new IllegalStateException("Invalid type!");
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
                case HashValue hashValue -> {
                    return new HashValue(hashValue.value(), expiryTime);
                }
                case SetValue setValue -> {
                    return new SetValue(setValue.value(), expiryTime);
                }
                case ZSetValue zSetValue -> {
                    return new ZSetValue(zSetValue.memberScores(), zSetValue.index(), expiryTime);
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

        if (value == null) {
            return Optional.empty();
        }

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
        List<String> result = deque.stream()
                .skip(normalizedStart)
                .limit(normalizedStop - normalizedStart + 1)
                .toList();

        return Optional.of(result);
    }

    public static int del(String... keys) {
        int removed = 0;
        for (String key : keys) {
            if (DATA.remove(key) != null) {
                removed++;
            }
        }
        return removed;
    }

    public static int exists(String... keys) {
        int count = 0;
        for (String key : keys) {
            RedisValue value = DATA.get(key);
            if (value != null) {
                if (value.isExpired()) {
                    DATA.remove(key);
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    public static List<String> keys(String pattern) {
        // Simple pattern matching: supports only '*' for now
        if ("*".equals(pattern)) {
            removeExpired();
            return DATA.keySet().stream().toList();
        }
        return List.of();
    }

    public static long getTTL(String key) {
        RedisValue value = DATA.get(key);
        if (value == null) {
            return -2;
        }
        if (value.isExpired()) {
            DATA.remove(key);
            return -2;
        }
        long ttl = value.expiryTime();
        if (ttl == 0) {
            return -1;
        }
        long remaining = (ttl - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    public static int sadd(String key, String... members) {
        var addedCount = new int[1];
        DATA.compute(key, (k, old) -> {
            if (old == null || old.isExpired()) {
                var set = java.util.concurrent.ConcurrentHashMap.<String>newKeySet();
                for (String member : members) {
                    if (set.add(member)) {
                        addedCount[0]++;
                    }
                }
                return new SetValue(set, 0);
            }

            if (old instanceof SetValue sv) {
                for (String member : members) {
                    if (sv.value().add(member)) {
                        addedCount[0]++;
                    }
                }
                return sv;
            }

            throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
        });
        return addedCount[0];
    }

    public static int srem(String key, String... members) {
        var removedCount = new int[1];
        DATA.computeIfPresent(key, (k, old) -> {
            if (old.isExpired()) {
                return null;
            }
            if (old instanceof SetValue sv) {
                for (String member : members) {
                    if (sv.value().remove(member)) {
                        removedCount[0]++;
                    }
                }
                return sv.value().isEmpty() ? null : sv;
            }
            throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
        });
        return removedCount[0];
    }

    public static Optional<java.util.Set<String>> smembers(String key) {
        RedisValue value = DATA.get(key);
        if (value == null || value.isExpired()) {
            if (value != null) DATA.remove(key);
            return Optional.empty();
        }
        if (value instanceof SetValue sv) {
            return Optional.of(sv.value());
        }
        throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
    }

    public static boolean sismember(String key, String member) {
        RedisValue value = DATA.get(key);
        if (value == null || value.isExpired()) {
            if (value != null) DATA.remove(key);
            return false;
        }
        if (value instanceof SetValue sv) {
            return sv.value().contains(member);
        }
        throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
    }

    public static int scard(String key) {
        RedisValue value = DATA.get(key);
        if (value == null || value.isExpired()) {
            if (value != null) DATA.remove(key);
            return 0;
        }
        if (value instanceof SetValue sv) {
            return sv.value().size();
        }
        throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
    }

    public static int zadd(String key, double score, String member) {
        var added = new int[1];

        DATA.compute(key, (k, old) -> {
            if (old == null || old.isExpired()) {
                var memberScores = new ConcurrentHashMap<String, Double>();
                var skipList = new SkipList();
                memberScores.put(member, score);
                skipList.insert(member, score);
                added[0] = 1;
                return new ZSetValue(memberScores, skipList, 0);
            }

            if (old instanceof ZSetValue zSetValue) {
                Double existingScore = zSetValue.memberScores().get(member);
                if (existingScore != null) {
                    if (Double.compare(existingScore, score) == 0) {
                        return zSetValue;
                    }
                    zSetValue.index().delete(member, existingScore);
                } else {
                    added[0] = 1;
                }

                zSetValue.memberScores().put(member, score);
                zSetValue.index().insert(member, score);
                return zSetValue;
            }

            throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
        });

        return added[0];
    }

    public static Optional<List<String>> zrange(String key, long start, long stop, boolean withScores) {
        RedisValue value = DATA.get(key);
        if (value == null || value.isExpired()) {
            if (value != null) {
                DATA.remove(key);
            }
            return Optional.empty();
        }

        if (!(value instanceof ZSetValue zSetValue)) {
            throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        int size = zSetValue.index().size();
        int normalizedStart = normalizeRangeIndex(start, size);
        int normalizedStop = normalizeRangeIndex(stop, size);

        if (normalizedStart >= size || normalizedStop < normalizedStart) {
            return Optional.of(List.of());
        }

        List<String> response = new java.util.ArrayList<>();
        for (SkipList.Node node : zSetValue.index().range(normalizedStart, normalizedStop)) {
            response.add(node.member);
            if (withScores) {
                response.add(formatScore(node.score));
            }
        }
        return Optional.of(response);
    }

    public static int zrem(String key, String... members) {
        var removed = new int[1];

        DATA.computeIfPresent(key, (k, old) -> {
            if (old.isExpired()) {
                return null;
            }

            if (old instanceof ZSetValue zSetValue) {
                for (String member : members) {
                    Double score = zSetValue.memberScores().remove(member);
                    if (score != null) {
                        zSetValue.index().delete(member, score);
                        removed[0]++;
                    }
                }
                return zSetValue.memberScores().isEmpty() ? null : zSetValue;
            }

            throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
        });

        return removed[0];
    }

    public static Optional<String> zscore(String key, String member) {
        RedisValue value = DATA.get(key);
        if (value == null || value.isExpired()) {
            if (value != null) {
                DATA.remove(key);
            }
            return Optional.empty();
        }

        if (value instanceof ZSetValue zSetValue) {
            Double score = zSetValue.memberScores().get(member);
            return score == null ? Optional.empty() : Optional.of(formatScore(score));
        }

        throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
    }

    public static int zcard(String key) {
        RedisValue value = DATA.get(key);
        if (value == null || value.isExpired()) {
            if (value != null) {
                DATA.remove(key);
            }
            return 0;
        }

        if (value instanceof ZSetValue zSetValue) {
            return zSetValue.memberScores().size();
        }

        throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
    }

    private static int normalizeRangeIndex(long index, int size) {
        long normalized = index < 0 ? size + index : index;
        if (normalized < 0) {
            return 0;
        }
        if (normalized >= size) {
            return size;
        }
        return (int) normalized;
    }

    private static String formatScore(double score) {
        if (score == Math.rint(score)) {
            return String.format(java.util.Locale.ROOT, "%.1f", score);
        }
        return Double.toString(score);
    }

}
