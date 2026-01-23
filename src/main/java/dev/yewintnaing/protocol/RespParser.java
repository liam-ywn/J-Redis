package dev.yewintnaing.protocol;

import java.io.*;
import java.util.*;

public class RespParser {
    public static RespType readResp(BufferedReader reader) throws IOException {
        int prefix = reader.read();
        if (prefix == -1) throw new EOFException();

        return switch ((char) prefix) {
            case '+' -> new RespString(reader.readLine());
            case '$' -> readBulkString(reader);
            case '*' -> readArray(reader);
            // Add ':' for Integers or '-' for Errors later
            default -> throw new IOException("Unknown prefix: " + (char) prefix);
        };
    }

    private static RespType readBulkString(BufferedReader reader) throws IOException {
        int length = Integer.parseInt(reader.readLine());
        if (length == -1) return new RespNull();

        char[] buffer = new char[length];
        reader.read(buffer, 0, length);
        reader.readLine(); // Consume CRLF
        return new RespString(new String(buffer));
    }

    private static RespType readArray(BufferedReader reader) throws IOException {
        int count = Integer.parseInt(reader.readLine());
        List<RespType> elements = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            elements.add(readResp(reader));
        }
        return new RespArray(elements);
    }
}