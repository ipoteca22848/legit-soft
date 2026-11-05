package com.example.auravisuals.util.friendscreen;

import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class FriendScreenClient$1 implements Listener {
    final StringBuilder incoming;
    final ByteArrayOutputStream incomingBinary;
    final FriendScreenClient this$0;

    FriendScreenClient$1(FriendScreenClient this$0) {
        this.this$0 = this$0;
        this.incoming = new StringBuilder();
        this.incomingBinary = new ByteArrayOutputStream();
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        this.this$0.socket.set(webSocket);
        this.this$0.clearFrameSendPending();
        this.this$0.state = FriendScreenClient$State.CONNECTED;
        webSocket.request(1L);
        String minecraftName = FriendScreenClient.minecraftName();
        String identity = FriendScreenClient.identity();
        this.this$0.authenticatedIdentity = FriendScreenClient.key(identity);
        JsonObject auth = new JsonObject();
        auth.addProperty("type", "auth");
        auth.addProperty("login", identity);
        auth.addProperty("account_login", FriendScreenClient.accountLogin());
        auth.addProperty("minecraft_name", minecraftName);
        webSocket.sendText(auth.toString(), true);
        this.this$0.notifyDataChanged();
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        this.incoming.append(data);
        webSocket.request(1L);
        if (last) {
            String complete = this.incoming.toString();
            this.incoming.setLength(0);
            this.this$0.dispatch(complete);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        byte[] chunk = new byte[data.remaining()];
        data.get(chunk);
        this.incomingBinary.writeBytes(chunk);
        webSocket.request(1L);
        if (last) {
            byte[] packet = this.incomingBinary.toByteArray();
            this.incomingBinary.reset();
            this.this$0.dispatchBinaryFrame(packet);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        if (this.this$0.socket.compareAndSet(webSocket, null)) {
            this.this$0.state = FriendScreenClient$State.DISCONNECTED;
            this.this$0.authenticatedIdentity = "";
            this.this$0.clearFrameSendPending();
            this.this$0.markOffline();
            this.this$0.notifyDataChanged();
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        if (this.this$0.socket.compareAndSet(webSocket, null)) {
            this.this$0.state = FriendScreenClient$State.DISCONNECTED;
            this.this$0.authenticatedIdentity = "";
            this.this$0.clearFrameSendPending();
            this.this$0.markOffline();
            this.this$0.notifyDataChanged();
        }
    }
}
