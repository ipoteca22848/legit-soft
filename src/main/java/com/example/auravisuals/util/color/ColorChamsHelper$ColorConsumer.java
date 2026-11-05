package com.example.auravisuals.util.color;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack.Entry;

public class ColorChamsHelper$ColorConsumer implements VertexConsumer {
    final VertexConsumer delegate;
    final int r;
    final int g;
    final int b;
    final int a;
    final boolean isItem;

    ColorChamsHelper$ColorConsumer(VertexConsumer delegate, int r, int g, int b, int a, boolean isItem) {
        this.delegate = delegate;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.isItem = isItem;
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z) {
        this.delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        if (!this.isItem) {
            this.delegate.vertex(x, y, z, color, u, v, overlay, light, normalX, normalY, normalZ);
        } else {
            int out = ColorChamsHelper.multiplyPackedArgb(color, this.r, this.g, this.b, this.a);
            this.delegate.vertex(x, y, z, out, u, v, overlay, light, normalX, normalY, normalZ);
        }
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        this.delegate.color(red * this.r / 255, green * this.g / 255, blue * this.b / 255, alpha * this.a / 255);
        return this;
    }

    @Override
    public VertexConsumer color(float red, float green, float blue, float alpha) {
        this.delegate.color(red * this.r / 255.0F, green * this.g / 255.0F, blue * this.b / 255.0F, alpha * this.a / 255.0F);
        return this;
    }

    @Override
    public VertexConsumer color(int argb) {
        if (!this.isItem) {
            return this.color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >> 24 & 0xFF);
        }

        this.delegate.color(ColorChamsHelper.multiplyPackedArgb(argb, this.r, this.g, this.b, this.a));
        return this;
    }

    @Override
    public VertexConsumer lineWidth(float width) {
        this.delegate.lineWidth(width);
        return this;
    }

    @Override
    public void quad(Entry matrixEntry, BakedQuad quad, float red, float green, float blue, float alpha, int light, int overlay) {
        if (!this.isItem) {
            this.delegate.quad(matrixEntry, quad, red, green, blue, alpha, light, overlay);
        } else {
            this.delegate
                .quad(matrixEntry, quad, red * this.r / 255.0F, green * this.g / 255.0F, blue * this.b / 255.0F, alpha * this.a / 255.0F, light, overlay);
        }
    }

    @Override
    public void quad(Entry matrixEntry, BakedQuad quad, float[] brightnesses, float red, float green, float blue, float alpha, int[] lights, int overlay) {
        if (!this.isItem) {
            this.delegate.quad(matrixEntry, quad, brightnesses, red, green, blue, alpha, lights, overlay);
        } else {
            this.delegate
                .quad(
                    matrixEntry,
                    quad,
                    brightnesses,
                    red * this.r / 255.0F,
                    green * this.g / 255.0F,
                    blue * this.b / 255.0F,
                    alpha * this.a / 255.0F,
                    lights,
                    overlay
                );
        }
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
