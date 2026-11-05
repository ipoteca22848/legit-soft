package com.example.auravisuals.util.friend;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;

public final class FriendHubConnection {
    static final HttpClient HTTP = HttpClient.newBuilder().version(Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(8L)).build();
    final AtomicReference<WebSocket> socketRef = new AtomicReference<>();
    final AtomicLong generation = new AtomicLong();
    volatile boolean connecting;
    Consumer<JsonObject> onIncoming;
    Runnable onConnected;
    Runnable onDisconnected;

    public void setOnIncoming(Consumer<JsonObject> cb) {
        this.onIncoming = cb;
    }

    public void setOnConnected(Runnable cb) {
        this.onConnected = cb;
    }

    public void setOnDisconnected(Runnable cb) {
        this.onDisconnected = cb;
    }

    public void connect(String urlString, String login, String avatarUrl) {
        String url = urlString == null ? "" : urlString.trim();
        if (!url.isEmpty() && (url.startsWith("ws://") || url.startsWith("wss://"))) {
            this.disconnect();
            long attempt = this.generation.incrementAndGet();
            this.connecting = true;

            URI uri;
            try {
                uri = URI.create(url);
            } catch (IllegalArgumentException var9) {
                this.connecting = false;
                return;
            }

            HTTP.newWebSocketBuilder().buildAsync(uri, new FriendHubConnection$1(this, attempt, login, avatarUrl)).whenComplete((ws, ex) -> {
                if (ex != null && this.generation.get() == attempt) {
                    this.connecting = false;
                    this.socketRef.set(null);
                    MinecraftClient.getInstance().execute(() -> {
                        if (this.onDisconnected != null) {
                            this.onDisconnected.run();
                        }
                    });
                }
            });
        }
    }

    void handleIncomingJson(String raw) {
        try {
            JsonElement el = JsonParser.parseString(raw);
            if (!el.isJsonObject()) {
                return;
            }

            if (this.onIncoming != null) {
                this.onIncoming.accept(el.getAsJsonObject());
            }
        } catch (Exception var3) {
        }
    }

    public boolean sendJson(JsonObject obj) {
        WebSocket ws = this.socketRef.get();
        if (ws == null) {
            return false;
        }

        try {
            ws.sendText(obj.toString(), true);
            return true;
        } catch (Exception var4) {
            return false;
        }
    }

    public void disconnect() {
        this.generation.incrementAndGet();
        WebSocket ws = this.socketRef.getAndSet(null);
        if (ws != null) {
            try {
                ws.sendClose(1000, "disconnect");
            } catch (Exception var3) {
            }
        }

        this.connecting = false;
    }

    public boolean isConnected() {
        return this.socketRef.get() != null;
    }

    public boolean isConnecting() {
        return this.connecting;
    }
}
