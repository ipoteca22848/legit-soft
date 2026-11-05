package com.example.auravisuals.util.chat;

import com.google.gson.JsonObject;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import net.minecraft.client.MinecraftClient;

public class ChatHubConnection$1 implements Listener {
    final ChatHubConnection this$0;
    final long val$attempt;
    final String val$login;
    final String val$role;
    final String val$avatarUrl;

    ChatHubConnection$1(ChatHubConnection this$0, long param2, String nullx, String nullxx, String nullxxx) {
        this.this$0 = this$0;
        this.val$attempt = param2;
        this.val$login = nullx;
        this.val$role = nullxx;
        this.val$avatarUrl = nullxxx;
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
            auth.addProperty("role", this.val$role);
            auth.addProperty("avatar_url", this.val$avatarUrl);

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
