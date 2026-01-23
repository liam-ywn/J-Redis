package dev.yewintnaing.handler;

import dev.yewintnaing.protocol.RespArray;
import dev.yewintnaing.protocol.RespType;

import java.io.*;
import java.net.Socket;

import static dev.yewintnaing.logic.CommandProcessor.handle;
import static dev.yewintnaing.protocol.RespParser.readResp;

public class ClientHandler {

    public static void handleClient(Socket socket) {
        try (socket;
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            while (true) {
                try {
                    // 1. READ: Parse the incoming RESP request
                    RespType request = readResp(reader);

                    // 2. PROCESS: Handle the command (e.g., SET, GET, PING)
                    if (request instanceof RespArray commandArray) {
                        String response = handle(commandArray);

                        // 3. RESPOND: Write the RESP formatted result back
                        writer.write(response);
                        writer.flush();
                    }
                } catch (EOFException e) {
                    // Normal disconnection
                    System.out.println("Client disconnected.");
                    break;
                } catch (Exception e) {
                    writer.write("-ERR " + e.getMessage() + "\r\n");
                    writer.flush();
                    System.err.println("Protocol error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + e.getMessage());
        }
    }


}
