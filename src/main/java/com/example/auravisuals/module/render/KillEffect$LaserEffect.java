package com.example.auravisuals.module.render;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.theme.ThemeManager;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class KillEffect$LaserEffect {
    public final Vec3d position;
    public final long startTime;

    public KillEffect$LaserEffect(KillEffect killEffect, Vec3d position, long startTime) {
        this.position = position;
        this.startTime = startTime;
    }

    public void render(WorldRenderContext context, float progress) {
        int color = ThemeManager.INSTANCE.getVisualModulesColor();
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        float globalAlpha = progress < 0.15F ? progress / 0.15F : (progress > 0.75F ? (1.0F - progress) / 0.25F : 1.0F);
        MatrixStack matrices = context.matrixStack();
        Vec3d cam = context.camera().getCameraPos();
        float yaw = context.camera().getYaw();
        float pitch = context.camera().getPitch();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (int i = 0; i < 40; i++) {
            float t = i / 40.0F;
            float y = t * 4.0F;
            float radius = 1.0F * MathHelper.sin((float)(Math.PI * t));
            float sliceAlpha = (1.0F - Math.abs(2.0F * t - 1.0F) * 0.25F) * globalAlpha;
            Vec3d pos = this.position.add(0.0, y, 0.0);
            this.appendGlow(buffer, matrices, cam, yaw, pitch, pos, radius * 2.1F, r, g, b, sliceAlpha * 0.22F);
            this.appendGlow(buffer, matrices, cam, yaw, pitch, pos, radius * 1.15F, r, g, b, sliceAlpha * 0.48F);
            this.appendGlow(buffer, matrices, cam, yaw, pitch, pos, radius * 0.55F, r, g, b, sliceAlpha * 0.85F);
        }

        for (int var22 = 0; var22 < 10; var22++) {
            float t = var22 / 10.0F;
            float spread = 1.0F - t;
            float bottomRadius = 3.6F * spread;
            float bottomAlpha = spread * spread * globalAlpha * 0.38F;
            Vec3d bPos = this.position.add(0.0, t * 0.45F, 0.0);
            this.appendGlow(buffer, matrices, cam, yaw, pitch, bPos, bottomRadius, r, g, b, bottomAlpha);
            this.appendGlow(buffer, matrices, cam, yaw, pitch, bPos, bottomRadius * 0.35F, r, g, b, bottomAlpha * 1.7F);
        }

        try {
            AuraRender.drawTextured(buffer, AuraRender.POS_TEX_COLOR_QUADS_ADD, KillEffect.BLOOM_TEX);
        } catch (Exception var20) {
        }
    }

    public void appendGlow(BufferBuilder buffer, MatrixStack matrices, Vec3d cam, float yaw, float pitch, Vec3d pos, float size, int r, int g, int b, float a) {
        if (!(a <= 0.01F)) {
            matrices.push();

            try {
                matrices.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                Matrix4f matrix = matrices.peek().getPositionMatrix();
                float half = size * 0.5F;
                int ai = Math.max(0, Math.min(255, (int)(a * 255.0F)));
                buffer.vertex(matrix, -half, -half, 0.0F).texture(0.0F, 1.0F).color(r, g, b, ai);
                buffer.vertex(matrix, -half, half, 0.0F).texture(0.0F, 0.0F).color(r, g, b, ai);
                buffer.vertex(matrix, half, half, 0.0F).texture(1.0F, 0.0F).color(r, g, b, ai);
                buffer.vertex(matrix, half, -half, 0.0F).texture(1.0F, 1.0F).color(r, g, b, ai);
            } finally {
                matrices.pop();
            }
        }
    }
}
