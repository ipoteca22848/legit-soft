package com.example.auravisuals.module.render;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class KillEffect$KillGhost {
    public final Vec3d origin;
    public final float width;
    public final float height;
    public final long startTime = System.currentTimeMillis();
    public double yOffset = 0.0;

    public KillEffect$KillGhost(KillEffect killEffect, LivingEntity entity) {
        this.origin = entity.getEntityPos();
        this.width = entity.getWidth();
        this.height = entity.getHeight();
    }

    public boolean update() {
        long elapsed = System.currentTimeMillis() - this.startTime;
        if (elapsed > 2200L) {
            return true;
        }

        this.yOffset = this.yOffset - 0.018 * KillEffect.speed.getValue();
        return false;
    }

    public boolean render(WorldRenderContext context) {
        long elapsed = System.currentTimeMillis() - this.startTime;
        if (elapsed > 2200L) {
            return true;
        }

        float progress = MathHelper.clamp((float)elapsed / 2200.0F, 0.0F, 1.0F);
        float fade = 1.0F - progress;
        Vec3d cameraPos = context.camera().getCameraPos();
        double x = this.origin.x - cameraPos.x;
        double y = this.origin.y + this.yOffset - cameraPos.y;
        double z = this.origin.z - cameraPos.z;
        float halfW = Math.max(0.25F, this.width * 0.6F);
        float boxH = Math.max(1.0F, this.height) * (1.0F - progress * 0.15F);
        int outlineA = Math.max(0, Math.min(255, (int)(170.0F * fade)));
        MatrixStack matrices = context.matrixStack();
        matrices.push();

        try {
            matrices.translate(x, y, z);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder outline = tessellator.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            this.appendOutline(outline, matrix, halfW, boxH, 255, 255, 255, outlineA);
            AuraRender.drawColorLines(outline, AuraRender.POS_COLOR_LINES, 2.2F);
        } finally {
            matrices.pop();
        }

        return false;
    }

    public void appendBox(BufferBuilder buffer, Matrix4f matrix, float halfW, float boxH, int r, int g, int b, int a) {
        float minX = -halfW;
        float maxX = halfW;
        float minZ = -halfW;
        float maxZ = halfW;
        float minY = 0.0F;
        float maxY = boxH;
        this.quad(buffer, matrix, minX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        this.quad(buffer, matrix, minX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        this.side(buffer, matrix, minX, minY, minZ, minX, maxY, maxZ, r, g, b, a);
        this.side(buffer, matrix, maxX, minY, minZ, maxX, maxY, maxZ, r, g, b, a);
        this.front(buffer, matrix, minX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        this.front(buffer, matrix, minX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
    }

    public void quad(BufferBuilder buffer, Matrix4f matrix, float x1, float y, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
        buffer.vertex(matrix, x1, y, z1).color(r, g, b, a);
        buffer.vertex(matrix, x1, y, z2).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a);
    }

    public void side(BufferBuilder buffer, Matrix4f matrix, float x, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
        buffer.vertex(matrix, x, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x, y2, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a);
    }

    public void front(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z, float x2, float y2, float z2, int r, int g, int b, int a) {
        buffer.vertex(matrix, x1, y1, z).color(r, g, b, a);
        buffer.vertex(matrix, x1, y2, z).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a);
    }

    public void appendOutline(BufferBuilder buffer, Matrix4f matrix, float halfW, float boxH, int r, int g, int b, int a) {
        float minX = -halfW;
        float maxX = halfW;
        float minZ = -halfW;
        float maxZ = halfW;
        float minY = 0.0F;
        float maxY = boxH;
        this.line(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        this.line(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        this.line(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        this.line(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);
        this.line(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        this.line(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        this.line(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        this.line(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        this.line(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        this.line(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        this.line(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        this.line(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    public void line(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
    }
}
