package com.example.auravisuals.util.integration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public final class DiscordRPC {
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static volatile RandomAccessFile pipe;
    static volatile boolean connected;
    static volatile boolean running;
    static ScheduledExecutorService executor;
    static DiscordRPC$Config config;
    static long startEpoch;
    static String lastPayload = "";

    DiscordRPC() {
    }

    public static synchronized void start() {
        if (!running && System.getProperty("os.name", "").toLowerCase().contains("win")) {
            config = loadConfig();
            if (config == null) {
                config = new DiscordRPC$Config();
            }

            running = true;
            startEpoch = System.currentTimeMillis();
            executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "bamber client-DiscordRPC");
                t.setDaemon(true);
                return t;
            });
            executor.scheduleWithFixedDelay(DiscordRPC::tick, 1L, 5L, TimeUnit.SECONDS);
        }
    }

    public static synchronized void shutdown() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        closePipe();
    }

    static void tick() {
        try {
            if (!connected) {
                connectAndHandshake();
            }

            if (connected) {
                sendActivity();
            }
        } catch (Throwable var1) {
            connected = false;
            closePipe();
        }
    }

    static void connectAndHandshake() {
        for (int i = 0; i < 10; i++) {
            try {
                RandomAccessFile p = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                pipe = p;
                String handshake = "{\"v\":1,\"client_id\":\"" + esc(config.clientId) + "\"}";
                write(0, handshake);
                readFrame();
                connected = true;
                return;
            } catch (Throwable var3) {
                closePipe();
            }
        }
    }

    static void sendActivity() throws Exception {
        String payload = buildPayload();
        if (!payload.equals(lastPayload)) {
            lastPayload = payload;
            write(1, payload);
            readFrame();
        }
    }

    static String buildPayload() {
        String details = config.detailsOverride != null && !config.detailsOverride.isEmpty() ? config.detailsOverride : versionName();
        String state = serverState();
        StringBuilder activity = new StringBuilder();
        activity.append('{');
        activity.append("\"details\":\"").append(esc(details)).append("\",");
        activity.append("\"state\":\"").append(esc(state)).append("\",");
        activity.append("\"timestamps\":{\"start\":").append(startEpoch).append("},");
        int online = onlinePlayers();
        if (online > 0) {
            activity.append("\"party\":{\"id\":\"auravisual\",\"size\":[").append(online).append(',').append(online).append("]},");
        }

        StringBuilder assets = new StringBuilder("{");
        boolean hasAsset = false;
        if (notEmpty(config.largeImage)) {
            assets.append("\"large_image\":\"").append(esc(config.largeImage)).append("\"");
            hasAsset = true;
        }

        if (notEmpty(config.largeText)) {
            if (hasAsset) {
                assets.append(',');
            }

            assets.append("\"large_text\":\"").append(esc(config.largeText)).append("\"");
            hasAsset = true;
        }

        if (notEmpty(config.smallImage)) {
            if (hasAsset) {
                assets.append(',');
            }

            assets.append("\"small_image\":\"").append(esc(config.smallImage)).append("\"");
            hasAsset = true;
        }

        if (notEmpty(config.smallText)) {
            if (hasAsset) {
                assets.append(',');
            }

            assets.append("\"small_text\":\"").append(esc(config.smallText)).append("\"");
            hasAsset = true;
        }

        assets.append('}');
        if (hasAsset) {
            activity.append("\"assets\":").append(assets).append(',');
        }

        StringBuilder buttons = new StringBuilder("[");
        boolean hasBtn = false;
        if (notEmpty(config.button1Label) && notEmpty(config.button1Url)) {
            buttons.append("{\"label\":\"").append(esc(config.button1Label)).append("\",\"url\":\"").append(esc(config.button1Url)).append("\"}");
            hasBtn = true;
        }

        if (notEmpty(config.button2Label) && notEmpty(config.button2Url)) {
            if (hasBtn) {
                buttons.append(',');
            }

            buttons.append("{\"label\":\"").append(esc(config.button2Label)).append("\",\"url\":\"").append(esc(config.button2Url)).append("\"}");
            hasBtn = true;
        }

        buttons.append(']');
        if (hasBtn) {
            activity.append("\"buttons\":").append(buttons).append(',');
        }

        if (activity.charAt(activity.length() - 1) == ',') {
            activity.setLength(activity.length() - 1);
        }

        activity.append('}');
        long pid = ProcessHandle.current().pid();
        return "{\"cmd\":\"SET_ACTIVITY\",\"nonce\":\"" + UUID.randomUUID() + "\",\"args\":{\"pid\":" + pid + ",\"activity\":" + activity + "}}";
    }

    static String versionName() {
        try {
            return SharedConstants.getGameVersion().name();
        } catch (Throwable var1) {
            return "Minecraft";
        }
    }

    static String serverState() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null) {
                return "В меню";
            }

            if (mc.world == null || mc.player == null) {
                return "В главном меню";
            }

            if (mc.isInSingleplayer()) {
                return "Одиночная игра";
            }

            ServerInfo entry = mc.getCurrentServerEntry();
            String address = entry != null ? entry.address : "сервер";
            return "Сервер: " + address;
        } catch (Throwable var3) {
            return "В игре";
        }
    }

    static int onlinePlayers() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.world == null || mc.isInSingleplayer()) {
                return 0;
            }

            if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerList() != null) {
                return mc.getNetworkHandler().getPlayerList().size();
            }
        } catch (Throwable var1) {
        }

        return 0;
    }

    static void write(int op, String json) throws Exception {
        RandomAccessFile p = pipe;
        if (p == null) {
            throw new IllegalStateException("pipe closed");
        }

        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(op);
        buf.putInt(data.length);
        buf.put(data);
        p.write(buf.array());
    }

    static void readFrame() {
        try {
            RandomAccessFile p = pipe;
            if (p == null) {
                return;
            }

            byte[] header = new byte[8];
            p.readFully(header);
            ByteBuffer hb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            int op = hb.getInt();
            int len = hb.getInt();
            if (len > 0 && len < 1048576) {
                byte[] body = new byte[len];
                p.readFully(body);
            }

            if (op == 2) {
                connected = false;
                closePipe();
            }
        } catch (Throwable var6) {
            connected = false;
            closePipe();
        }
    }

    static void closePipe() {
        RandomAccessFile p = pipe;
        pipe = null;
        lastPayload = "";
        if (p != null) {
            try {
                p.close();
            } catch (Throwable var2) {
            }
        }
    }

    static DiscordRPC$Config loadConfig() {
        try {
            Path path = FabricLoader.getInstance().getConfigDir().resolve("bamber client_discord.json");
            if (Files.exists(path)) {
                String text = Files.readString(path, StandardCharsets.UTF_8);
                DiscordRPC$Config c = (DiscordRPC$Config)GSON.fromJson(text, DiscordRPC$Config.class);
                return c != null ? c : new DiscordRPC$Config();
            } else {
                DiscordRPC$Config def = new DiscordRPC$Config();
                Files.createDirectories(path.getParent());
                Files.writeString(path, GSON.toJson(def), StandardCharsets.UTF_8);
                return def;
            }
        } catch (Throwable var3) {
            return new DiscordRPC$Config();
        }
    }

    static boolean notEmpty(String s) {
        return s != null && !s.isEmpty();
    }

    static String esc(String s) {
        if (s == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder(s.length() + 8);

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", Integer.valueOf(c)));
                    } else {
                        sb.append(c);
                    }
            }
        }

        return sb.toString();
    }
}
