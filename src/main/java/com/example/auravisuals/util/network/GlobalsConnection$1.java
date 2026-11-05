package com.example.auravisuals.util.network;

import com.example.auravisuals.module.misc.Globals;
import com.example.auravisuals.util.registry.GlobalsRegistry;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import net.minecraft.client.MinecraftClient;

public class GlobalsConnection$1 implements Listener {
    final GlobalsConnection this$0;

    GlobalsConnection$1(GlobalsConnection this$0) {
        this.this$0 = this$0;
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        this.this$0.connecting = false;
        this.this$0.socketRef.set(webSocket);
        webSocket.request(1L);
        String openedName = this.this$0.pendingName;
        GlobalsConnection.sendHello(webSocket, openedName);
        MinecraftClient.getInstance().execute(() -> GlobalsRegistry.add(openedName));
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        webSocket.request(1L);
        String text = data.toString();
        MinecraftClient.getInstance().execute(() -> GlobalsConnection.handleIncomingJson(text));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        if (this.this$0.socketRef.compareAndSet(webSocket, null)) {
            this.this$0.connecting = false;
            Globals.rotateUrl();
            MinecraftClient.getInstance().execute(GlobalsRegistry::clear);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        if (this.this$0.socketRef.compareAndSet(webSocket, null)) {
            this.this$0.connecting = false;
            Globals.rotateUrl();
            MinecraftClient.getInstance().execute(GlobalsRegistry::clear);
        } else {
            this.this$0.connecting = false;
        }
    }
}
