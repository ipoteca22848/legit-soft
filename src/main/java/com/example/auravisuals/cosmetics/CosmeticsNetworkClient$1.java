package com.example.auravisuals.cosmetics;

import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import net.minecraft.client.MinecraftClient;

public class CosmeticsNetworkClient$1 implements Listener {
    final CosmeticsNetworkClient this$0;

    CosmeticsNetworkClient$1(CosmeticsNetworkClient this$0) {
        this.this$0 = this$0;
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        this.this$0.connecting.set(false);
        this.this$0.socketRef.set(webSocket);
        webSocket.request(1L);
        this.this$0.sendHello();
        String pe = this.this$0.pendingEmotion;
        if (pe != null) {
            this.this$0.pendingEmotion = null;
            this.this$0.sendEmotion(pe);
        }
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        webSocket.request(1L);
        String text = data.toString();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            mc.execute(() -> this.this$0.handleMessage(text));
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        this.this$0.socketRef.compareAndSet(webSocket, null);
        this.this$0.connecting.set(false);
        this.this$0.currentUrlIndex = (this.this$0.currentUrlIndex + 1) % CosmeticsNetworkClient.SERVER_URLS.length;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        this.this$0.socketRef.compareAndSet(webSocket, null);
        this.this$0.connecting.set(false);
        this.this$0.currentUrlIndex = (this.this$0.currentUrlIndex + 1) % CosmeticsNetworkClient.SERVER_URLS.length;
    }
}
