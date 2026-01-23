package dev.yewintnaing.storage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

public class RedisStorage {
    
    // Internal record to keep the logic encapsulated
    public record ExpiringValue(String value, long expiryTime) {
        public boolean isExpired() {
            return expiryTime != 0 && System.currentTimeMillis() > expiryTime;
        }
    }

    private static final ConcurrentHashMap<String, ExpiringValue> DATA = new ConcurrentHashMap<>();

    public static void put(String key, String value, long expiryTime) {
        DATA.put(key, new ExpiringValue(value, expiryTime));
    }

    public static void put(String key, String value) {
        DATA.put(key, new ExpiringValue(value, 0));
    }



    public static Optional<String> get(String key) {
        ExpiringValue entry = DATA.get(key);
        
        if (entry == null) return Optional.empty();

        if (entry.isExpired()) {
            DATA.remove(key); // Passive cleanup
            return Optional.empty();
        }

        return Optional.of(entry.value());
    }

    public static boolean setExpiry(String key, long seconds) {
        long expiryTime = System.currentTimeMillis() + (seconds * 1000L);
        
        // Atomic update: only update if the key actually exists
        ExpiringValue updated = DATA.computeIfPresent(key, (k, old) -> 
            new ExpiringValue(old.value(), expiryTime));
            
        return updated != null;
    }

    // This is where your background cleaner will call
    public static void removeExpired() {
        long now = System.currentTimeMillis();
        DATA.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}