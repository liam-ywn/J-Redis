package dev.yewintnaing.logic;

import dev.yewintnaing.handler.ClientHandler;
import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.protocol.RespType;

import java.util.List;
import java.util.Set;

public class UnsubscribeCommand implements RedisCommand {
    @Override
    public String execute(RespArray args, ClientHandler client) {
        List<RespType> elements = args.elements();

        // If no channels specified, unsubscribe from all
        if (elements.size() == 1) {
            Set<String> subscriptions = client.getSubscriptions();
            if (subscriptions.isEmpty()) {
                // Return a single unsubscribe message with 0 count
                return "*3\r\n$11\r\nunsubscribe\r\n$-1\r\n:0\r\n";
            }

            StringBuilder response = new StringBuilder();
            // Need to copy to avoid ConcurrentModificationException if unsubscribeAll isn't
            // used
            String[] subs = subscriptions.toArray(new String[0]);
            for (String channel : subs) {
                PubSubManager.getInstance().unsubscribe(channel, client);
                response.append("*3\r\n");
                response.append("$11\r\nunsubscribe\r\n");
                response.append("$").append(channel.length()).append("\r\n").append(channel).append("\r\n");
                response.append(":").append(client.getSubscriptions().size()).append("\r\n");
            }
            return response.toString();
        }

        StringBuilder response = new StringBuilder();
        for (int i = 1; i < elements.size(); i++) {
            RespType element = elements.get(i);
            if (element instanceof RespBulkString channelName) {
                String channel = channelName.asUtf8();
                PubSubManager.getInstance().unsubscribe(channel, client);

                response.append("*3\r\n");
                response.append("$11\r\nunsubscribe\r\n");
                response.append("$").append(channel.length()).append("\r\n").append(channel).append("\r\n");
                response.append(":").append(client.getSubscriptions().size()).append("\r\n");
            }
        }

        return response.toString();
    }
}
