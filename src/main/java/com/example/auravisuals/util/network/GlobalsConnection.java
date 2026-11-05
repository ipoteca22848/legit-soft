package com.example.auravisuals.util.network;

import com.example.auravisuals.module.misc.Globals;
import com.example.auravisuals.util.registry.GlobalsRegistry;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.HttpClient.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.MinecraftClient;

public final class GlobalsConnection {
    static final HttpClient HTTP = HttpClient.newBuilder().version(Version.HTTP_1_1).build();
    final AtomicReference<WebSocket> socketRef = new AtomicReference<>();
    volatile boolean connecting;
    volatile String pendingUrl;
    volatile String pendingName;

    public void connect(String urlString, String displayName) {
        String trimmed = urlString == null ? "" : urlString.trim();
        if (!trimmed.isEmpty() && (trimmed.startsWith("ws://") || trimmed.startsWith("wss://"))) {
            this.pendingUrl = trimmed;
            String name = displayName == null ? "" : displayName.trim();
            this.pendingName = name.isEmpty() ? "User" : name;
            this.disconnect();
            this.connecting = true;

            URI uri;
            try {
                uri = URI.create(trimmed);
            } catch (IllegalArgumentException var7) {
                this.connecting = false;
                return;
            }

            HTTP.newWebSocketBuilder().buildAsync(uri, new GlobalsConnection$1(this)).whenComplete((ws, throwable) -> {
                this.connecting = false;
                if (throwable != null) {
                    this.socketRef.set(null);
                    Globals.rotateUrl();
                }
            });
        }
    }

    public void sendHello(String name) {
        WebSocket ws = this.socketRef.get();
        if (ws != null) {
            this.pendingName = name;
            sendHello(ws, name);
        }
    }

    static void sendHello(WebSocket ws, String name) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "globals_hello");
        o.addProperty("name", name);

        try {
            ws.sendText(o.toString(), true);
        } catch (Exception var4) {
        }
    }

    static void handleIncomingJson(String raw) {
        try {
            JsonElement el = JsonParser.parseString(raw);
            if (!el.isJsonObject()) {
                return;
            }

            JsonObject o = el.getAsJsonObject();
            String type = o.has("type") ? o.get("type").getAsString() : "";
            if ("globals_sync".equals(type) || "sync".equals(type)) {
                applySync(o);
            }
        } catch (Exception var4) {
        }
    }

    static void applySync(JsonObject o) {
        List<String> list = new ArrayList<>();
        if (o.has("players") && o.get("players").isJsonArray()) {
            JsonArray arr = o.getAsJsonArray("players");

            for (int i = 0; i < arr.size(); i++) {
                if (arr.get(i).isJsonPrimitive()) {
                    list.add(arr.get(i).getAsString());
                }
            }
        } else if (o.has("names") && o.get("names").isJsonArray()) {
            JsonArray arr = o.getAsJsonArray("names");

            for (int ix = 0; ix < arr.size(); ix++) {
                if (arr.get(ix).isJsonPrimitive()) {
                    list.add(arr.get(ix).getAsString());
                }
            }
        }

        GlobalsRegistry.replaceAll(list);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            String current = Globals.getDisplayName(mc);
            GlobalsRegistry.add(current);
        }
    }

    public void disconnect() {
        WebSocket ws = this.socketRef.getAndSet(null);
        if (ws != null) {
            try {
                ws.sendClose(1000, "disconnect");
            } catch (Exception var3) {
            }
        }

        this.connecting = false;
        GlobalsRegistry.clear();
    }

    public void sendPing() {
        WebSocket ws = this.socketRef.get();
        if (ws != null) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "ping");

            try {
                ws.sendText(o.toString(), true);
            } catch (Exception var4) {
            }
        }
    }

    public boolean isConnected() {
        return this.socketRef.get() != null;
    }

    public boolean isConnecting() {
        return this.connecting;
    }
}
