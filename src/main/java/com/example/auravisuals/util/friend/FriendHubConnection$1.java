package com.example.auravisuals.util.friend;

import com.google.gson.JsonObject;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import net.minecraft.client.MinecraftClient;

public class FriendHubConnection$1 implements Listener {
    final FriendHubConnection this$0;
    final long val$attempt;
    final String val$login;
    final String val$avatarUrl;

    FriendHubConnection$1(FriendHubConnection this$0, long param2, String nullx, String nullxx) {
        this.this$0 = this$0;
        this.val$attempt = param2;
        this.val$login = nullx;
        this.val$avatarUrl = nullxx;
    }

    @Override
    public void onOpen(WebSocket ws) {
        if (this.this$0.generation.get() != this.val$attempt) {
            ws.abort();
        } else {
            this.this$0.connecting = false;
            this.this$0.socketRef.set(ws);
            ws.request(1L);
            JsonObject auth = new JsonObject();
            auth.addProperty("type", "auth");
            auth.addProperty("login", this.val$login);
            auth.addProperty("avatar_url", this.val$avatarUrl != null ? this.val$avatarUrl : "");

            try {
                ws.sendText(auth.toString(), true);
            } catch (Exception var4) {
            }

            MinecraftClient.getInstance().execute(() -> {
                if (this.this$0.onConnected != null) {
                    this.this$0.onConnected.run();
                }
            });
        }
    }

    @Override
    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
        if (this.this$0.generation.get() != this.val$attempt) {
            return CompletableFuture.completedFuture(null);
        }

        ws.request(1L);
        String text = data.toString();
        MinecraftClient.getInstance().execute(() -> this.this$0.handleIncomingJson(text));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
        if (this.this$0.generation.get() != this.val$attempt) {
            return CompletableFuture.completedFuture(null);
        }

        this.this$0.socketRef.compareAndSet(ws, null);
        this.this$0.connecting = false;
        MinecraftClient.getInstance().execute(() -> {
            if (this.this$0.onDisconnected != null) {
                this.this$0.onDisconnected.run();
            }
        });
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket ws, Throwable err) {
        if (this.this$0.generation.get() == this.val$attempt) {
            this.this$0.socketRef.compareAndSet(ws, null);
            this.this$0.connecting = false;
            MinecraftClient.getInstance().execute(() -> {
                if (this.this$0.onDisconnected != null) {
                    this.this$0.onDisconnected.run();
                }
            });
        }
    }
}
