package com.example.auravisuals.util.render;

import net.minecraft.client.render.VertexConsumer;

public final class HandSilhouetteMask$WhiteConsumer implements VertexConsumer {
    final VertexConsumer delegate;

    HandSilhouetteMask$WhiteConsumer(VertexConsumer delegate) {
        this.delegate = delegate;
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z) {
        this.delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer lineWidth(float width) {
        this.delegate.lineWidth(width);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        this.delegate.color(255, 255, 255, 255);
        return this;
    }

    @Override
    public VertexConsumer color(int argb) {
        this.delegate.color(-1);
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        this.delegate.texture(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        this.delegate.overlay(u, v);
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        this.delegate.light(15728880);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        this.delegate.normal(x, y, z);
        return this;
    }
}
