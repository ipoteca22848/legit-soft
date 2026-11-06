package com.example.auravisuals.module.misc;

import com.example.auravisuals.util.ClientProfile;
import com.example.auravisuals.util.network.GlobalsConnection;
import com.example.auravisuals.util.registry.GlobalsRegistry;
import net.minecraft.client.MinecraftClient;

public final class Globals {
    public static final String[] SERVER_URLS = new String[]{"ws://152.89.199.138:7655", "wss://auravisuals.space/ws/globals"};
    private static int urlIndex = 0;
    private static final long RECONNECT_MS = 10000L;
    private static final long PING_MS = 25000L;
    private static final GlobalsConnection connection = new GlobalsConnection();
    private static long lastReconnectAttemptMs;
    private static long lastPingMs;
    private static boolean initialized = false;
    private static String lastSentName = "";

    private Globals() {
    }

    public static void shutdown() {connection.disconnect();GlobalsRegistry.clear();}

    public static boolean shouldShowIcon() {
        return com.anvil.client.AnvilRuntime.isActive();
    }

    public static String getDisplayName(MinecraftClient mc) {
        String profileName = ClientProfile.getUsername();
        if (profileName != null && !profileName.isBlank() && !profileName.equalsIgnoreCase("User")) {
            return profileName.trim();
        }

        if (mc != null && mc.player != null) {
            String playerName;
            if (mc.player.getGameProfile() != null && mc.player.getGameProfile().name() != null && !(playerName = mc.player.getGameProfile().name()).isBlank()) {
                return playerName.trim();
            }

            if (mc.player.getName() != null && !(playerName = mc.player.getName().getString()).isBlank()) {
                return playerName.trim();
            }
        }

        String sessionName;
        return mc != null && mc.getSession() != null && (sessionName = mc.getSession().getUsername()) != null && !sessionName.isBlank()
            ? sessionName.trim()
            : "User";
    }

    public static void init() {
        if (!initialized) {
            initialized = true;
            MinecraftClient mc = MinecraftClient.getInstance();
            String displayName;
            lastSentName = displayName = getDisplayName(mc);
            GlobalsRegistry.clear();
            GlobalsRegistry.add(displayName);
            connection.connect(SERVER_URLS[urlIndex], displayName);
        }
    }

    public static void rotateUrl() {
        urlIndex = (urlIndex + 1) % SERVER_URLS.length;
    }

    public static void onAccountChanged(String newName) {
        if (newName != null && !newName.isBlank()) {
            String name;
            lastSentName = name = newName.trim();
            GlobalsRegistry.add(name);
            if (connection.isConnected()) {
                connection.sendHello(name);
            } else if (!connection.isConnecting()) {
                connection.connect(SERVER_URLS[urlIndex], name);
            }
        }
    }

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        String displayName = getDisplayName(mc);
        GlobalsRegistry.add(displayName);
        long now = System.currentTimeMillis();
        if (connection.isConnected()) {
            if (!displayName.equalsIgnoreCase(lastSentName)) {
                lastSentName = displayName;
                connection.sendHello(displayName);
            }

            if (now - lastPingMs >= 25000L) {
                lastPingMs = now;
                connection.sendPing();
            }
        } else if (!connection.isConnecting()) {
            if (now - lastReconnectAttemptMs >= 10000L) {
                lastReconnectAttemptMs = now;
                lastSentName = displayName;
                GlobalsRegistry.add(displayName);
                connection.connect(SERVER_URLS[urlIndex], displayName);
            }
        }
    }
}
