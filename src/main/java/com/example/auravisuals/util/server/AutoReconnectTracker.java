package com.example.auravisuals.util.server;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public final class AutoReconnectTracker {
    private static ServerInfo lastServer;
    private static boolean manualDisconnect;
    private static boolean reconnectPending;
    private static boolean reconnectAttempted;
    private static long disconnectedAt;

    private AutoReconnectTracker() {
    }

    public static void captureCurrentServer(MinecraftClient client) {
        ServerInfo current = client.getCurrentServerEntry();
        manualDisconnect = false;
        reconnectPending = false;
        reconnectAttempted = false;
        disconnectedAt = 0L;
        if (current == null) {
            lastServer = null;
        } else {
            ServerInfo snapshot = new ServerInfo(current.name, current.address, current.getServerType());
            snapshot.copyWithSettingsFrom(current);
            lastServer = snapshot;
        }
    }

    public static void markManualDisconnect() {
        manualDisconnect = true;
        reconnectPending = false;
        reconnectAttempted = false;
        disconnectedAt = 0L;
    }

    public static void handleDisconnect() {
        if (!manualDisconnect && lastServer != null) {
            reconnectPending = true;
            reconnectAttempted = false;
            disconnectedAt = System.currentTimeMillis();
        } else {
            reconnectPending = false;
            reconnectAttempted = false;
        }
    }

    public static boolean canAttemptReconnect(long delayMs) {
        return reconnectPending && !reconnectAttempted && lastServer != null && System.currentTimeMillis() - disconnectedAt >= delayMs;
    }

    public static void markReconnectAttempted() {
        reconnectAttempted = true;
        manualDisconnect = false;
    }

    public static ServerInfo getLastServer() {
        if (lastServer == null) {
            return null;
        }

        ServerInfo snapshot = new ServerInfo(lastServer.name, lastServer.address, lastServer.getServerType());
        snapshot.copyWithSettingsFrom(lastServer);
        return snapshot;
    }
}
