package dev.yewintnaing.logic;

import dev.yewintnaing.handler.ClientHandler;
import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class PubSubTest {

    private PubSubManager pubSubManager;

    @BeforeEach
    void setUp() {
        pubSubManager = PubSubManager.getInstance();
        // Since it's a singleton, we might need to clear it or ensure it's in a clean
        // state
        // However, there's no clear method. We'll just use unique channel names for
        // each test.
    }

    private static class MockClientHandler extends ClientHandler {
        public List<String> sentMessages = new ArrayList<>();
        private final Set<String> mockSubscriptions = ConcurrentHashMap.newKeySet();

        public MockClientHandler() {
            super(null);
        }

        @Override
        public void sendMessage(String message) {
            sentMessages.add(message);
        }

        @Override
        public boolean addSubscription(String channel) {
            return mockSubscriptions.add(channel);
        }

        @Override
        public void removeSubscription(String channel) {
            mockSubscriptions.remove(channel);
        }

        @Override
        public Set<String> getSubscriptions() {
            return mockSubscriptions;
        }

        @Override
        public void run() {
        }
    }

    @Test
    void testSubscribeAndPublish() {
        MockClientHandler client = new MockClientHandler();
        String channel = "test-channel-" + System.nanoTime();

        pubSubManager.subscribe(channel, client);

        int receivers = pubSubManager.publish(channel, "hello");
        assertEquals(1, receivers);
        assertEquals(1, client.sentMessages.size());
        assertTrue(client.sentMessages.get(0).contains("hello"));
        assertTrue(client.sentMessages.get(0).contains(channel));
    }

    @Test
    void testUnsubscribe() {
        MockClientHandler client = new MockClientHandler();
        String channel = "test-channel-unsub-" + System.nanoTime();

        pubSubManager.subscribe(channel, client);
        pubSubManager.unsubscribe(channel, client);

        int receivers = pubSubManager.publish(channel, "hello");
        assertEquals(0, receivers);
    }

    @Test
    void testUnsubscribeCommand() {
        MockClientHandler client = new MockClientHandler();
        String channel = "test-channel-cmd-" + System.nanoTime();

        pubSubManager.subscribe(channel, client);

        UnsubscribeCommand command = new UnsubscribeCommand();
        RespArray args = new RespArray(List.of(
                new RespBulkString("UNSUBSCRIBE".getBytes()),
                new RespBulkString(channel.getBytes())));

        String response = command.execute(args, client);
        assertTrue(response.contains("unsubscribe"));
        assertTrue(response.contains(channel));

        int receivers = pubSubManager.publish(channel, "hello");
        assertEquals(0, receivers);
    }

    @Test
    void testUnsubscribeAll() {
        MockClientHandler client = new MockClientHandler();
        String channel1 = "ch1-" + System.nanoTime();
        String channel2 = "ch2-" + System.nanoTime();

        pubSubManager.subscribe(channel1, client);
        pubSubManager.subscribe(channel2, client);

        pubSubManager.unsubscribeAll(client);

        assertEquals(0, pubSubManager.publish(channel1, "msg"));
        assertEquals(0, pubSubManager.publish(channel2, "msg"));
    }
}
