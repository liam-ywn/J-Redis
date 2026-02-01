package dev.yewintnaing.storage;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CleanerTask {

    public static void init() {
        var scheduler = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().name("redis-clearner").factory());

        scheduler.scheduleAtFixedRate(RedisStorage::removeExpired, 5, 5, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();

            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException _) {
                scheduler.shutdownNow();
            }
        }));

    }
}
