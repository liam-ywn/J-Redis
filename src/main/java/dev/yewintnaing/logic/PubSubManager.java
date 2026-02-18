package dev.yewintnaing.logic;

import dev.yewintnaing.handler.ClientHandler;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class PubSubManager {
    private static final PubSubManager INSTANCE = new PubSubManager();
    private final ConcurrentHashMap<String, Set<ClientHandler>> channels = new ConcurrentHashMap<>();
    private static final int MAX_TOTAL_SUBSCRIPTIONS = 1000;
    private final java.util.concurrent.atomic.AtomicInteger totalSubscriptions = new java.util.concurrent.atomic.AtomicInteger(
            0);

    private PubSubManager() {
    }

    public static PubSubManager getInstance() {
        return INSTANCE;
    }

    public void subscribe(String channel, ClientHandler client) {
        if (totalSubscriptions.get() >= MAX_TOTAL_SUBSCRIPTIONS) {
            throw new RuntimeException("Max total subscriptions reached");
        }

        if (!client.addSubscription(channel)) {
            throw new RuntimeException("Max client subscriptions reached");
        }

        if (channels.computeIfAbsent(channel, k -> new CopyOnWriteArraySet<>()).add(client)) {
            totalSubscriptions.incrementAndGet();
        }
    }

    public void unsubscribe(String channel, ClientHandler client) {
        client.removeSubscription(channel);
        Set<ClientHandler> subscribers = channels.get(channel);
        if (subscribers != null) {
            if (subscribers.remove(client)) {
                totalSubscriptions.decrementAndGet();
            }
            if (subscribers.isEmpty()) {
                channels.remove(channel);
            }
        }
    }

    public void unsubscribeAll(ClientHandler client) {
        for (String channel : client.getSubscriptions()) {
            unsubscribe(channel, client);
        }
    }

    public int publish(String channel, String message) {
        Set<ClientHandler> subscribers = channels.get(channel);
        if (subscribers == null || subscribers.isEmpty()) {
            return 0;
        }

        // RESPONSIBILITY: Redis Pub/Sub message format
        // *3\r\n$7\r\nmessage\r\n$<channel_len>\r\n<channel>\r\n$<msg_len>\r\n<message>\r\n
        String encodedMessage = "*3\r\n$7\r\nmessage\r\n$" + channel.length() + "\r\n" + channel + "\r\n$"
                + message.length() + "\r\n" + message + "\r\n";

        int count = 0;
        for (ClientHandler client : subscribers) {
            client.sendMessage(encodedMessage);
            count++;
        }
        return count;
    }
}
