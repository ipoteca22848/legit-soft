package com.example.auravisuals.util.friendlabels;

import com.google.gson.JsonObject;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import net.minecraft.client.MinecraftClient;

public class FriendLabelsClient$1 implements Listener {
    final StringBuilder buffer;
    final FriendLabelsClient this$0;

    FriendLabelsClient$1(FriendLabelsClient this$0) {
        this.this$0 = this$0;
        this.buffer = new StringBuilder();
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        this.this$0.socket.set(webSocket);
        this.this$0.state = FriendLabelsClient$State.CONNECTED;
        this.this$0.identity = FriendLabelsClient.key(FriendLabelsClient.minecraftName());
        JsonObject auth = new JsonObject();
        auth.addProperty("type", "auth");
        auth.addProperty("login", FriendLabelsClient.minecraftName());
        auth.addProperty("account_login", FriendLabelsClient.accountLogin());
        auth.addProperty("minecraft_name", FriendLabelsClient.minecraftName());
        webSocket.sendText(auth.toString(), true);

        JsonObject pending;
        while ((pending = this.this$0.pendingMessages.poll()) != null) {
            webSocket.sendText(pending.toString(), true);
        }

        webSocket.request(1L);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        this.buffer.append(data);
        if (last) {
            String raw = this.buffer.toString();
            this.buffer.setLength(0);
            MinecraftClient.getInstance().execute(() -> this.this$0.handle(raw));
        }

        webSocket.request(1L);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int status, String reason) {
        this.this$0.disconnected(webSocket);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        this.this$0.disconnected(webSocket);
    }
}
