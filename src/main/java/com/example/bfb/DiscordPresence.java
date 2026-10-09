package com.example.bfb;

import net.fabricmc.loader.api.FabricLoader;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DiscordPresence {
    private static Thread thread;
    private static volatile boolean running;

    private DiscordPresence() {
    }

    public static boolean start() {
        if (running) return true;
        String clientId = clientId();
        if (clientId.isBlank()) return false;
        running = true;
        thread = new Thread(() -> connect(clientId), "bfb-discord");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    public static void stop() {
        running = false;
        thread = null;
    }

    private static String clientId() {
        try {
            Path file = FabricLoader.getInstance().getConfigDir().resolve("undetected").resolve("discord.txt");
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                Files.writeString(file, "");
                return "";
            }
            String raw = Files.readString(file).trim();
            int line = raw.indexOf('\n');
            if (line >= 0) raw = raw.substring(0, line).trim();
            StringBuilder digits = new StringBuilder();
            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);
                if (c >= '0' && c <= '9') digits.append(c);
            }
            return digits.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static void connect(String clientId) {
        for (int i = 0; i < 10 && running; i++) {
            try (RandomAccessFile pipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw")) {
                send(pipe, 0, "{\"v\":1,\"client_id\":\"" + clientId + "\"}");
                String activity = "{\"cmd\":\"SET_ACTIVITY\",\"nonce\":\"bfb\",\"args\":{\"pid\":" + ProcessHandle.current().pid()
                        + ",\"activity\":{\"type\":0,\"details\":\"Undetected\",\"state\":\"Minecraft 1.21.11\"}}}";
                send(pipe, 1, activity);
                read(pipe);
                while (running) {
                    Thread.sleep(15000);
                    send(pipe, 1, activity);
                    send(pipe, 3, null);
                }
                return;
            } catch (Exception ignored) {
            }
        }
        running = false;
    }

    private static void send(RandomAccessFile pipe, int opcode, String json) throws Exception {
        if (json == null) json = "{}";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(opcode);
        buffer.putInt(data.length);
        buffer.put(data);
        pipe.write(buffer.array());
    }

    private static void read(RandomAccessFile pipe) throws Exception {
        byte[] header = new byte[8];
        pipe.readFully(header);
        int length = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
        if (length > 0) pipe.readFully(new byte[length]);
    }
}
