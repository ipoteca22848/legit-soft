package com.example.auravisuals.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;

public final class WorldRenderContext {
    private final net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext delegate;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public WorldRenderContext(net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext delegate) {
        this.delegate = delegate;
    }

    public MatrixStack matrixStack() {
        return this.delegate.matrices();
    }

    public Camera camera() {
        return this.mc.gameRenderer.getCamera();
    }

    public RenderTickCounter tickCounter() {
        return this.mc.getRenderTickCounter();
    }

    public VertexConsumerProvider consumers() {
        return this.delegate.consumers();
    }

    public OrderedRenderCommandQueue commandQueue() {
        return this.delegate.commandQueue();
    }

    public net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext delegate() {
        return this.delegate;
    }
}
