package dev.yewintnaing.logic;

import dev.yewintnaing.handler.ClientHandler;
import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespBulkString;
import dev.yewintnaing.protocol.RespType;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class SubscribeCommand implements RedisCommand {
    @Override
    public String execute(RespArray args, ClientHandler client) {
        List<RespType> elements = args.elements();
        if (elements.size() < 2) {
            return "-ERR at least 1 channel required\r\n";
        }

        StringBuilder response = new StringBuilder();
        int subscribedCount = 0; // In a real Redis, this tracks total subscriptions for the client.
                                 // For simplicity, we just increment per channel in this command.
                                 // To be more accurate, ClientHandler should track its own subscription count.

        for (int i = 1; i < elements.size(); i++) {
            RespType element = elements.get(i);
            if (element instanceof RespBulkString channelName) {
                String channel = channelName.asUtf8();
                try {
                    PubSubManager.getInstance().subscribe(channel, client);
                    subscribedCount++; // This is loose; normally it's total subs for client.

                    // Push subscription message immediately
                    // *3\r\n$9\r\nsubscribe\r\n$<len>\r\n<channel>\r\n:<count>\r\n
                    response.append("*3\r\n");
                    response.append("$9\r\nsubscribe\r\n");
                    response.append("$").append(channel.length()).append("\r\n").append(channel).append("\r\n");
                    response.append(":").append(subscribedCount).append("\r\n");
                } catch (RuntimeException e) {
                    return "-ERR " + e.getMessage() + "\r\n";
                }
            }
        }

        // SUBSCRIBE is unique: it pushes messages. The return of execute() might be
        // sent by ClientHandler loop.
        // But since we are pushing multiple messages (one per channel), we can return
        // the whole thing concatenated.
        return response.toString();
    }
}
