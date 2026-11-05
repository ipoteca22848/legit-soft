package com.example.auravisuals.util.render;

import com.example.auravisuals.compat.AuraRender;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class GlowRenderer {
    public static void drawGlowingCircle(MatrixStack matrices, float radius, int color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = (color >> 24 & 0xFF) / 255.0F;
        buffer.vertex(matrix, 0.0F, 0.0F, 0.0F).color(r, g, b, a);
        int segments = 24;

        for (int i = 0; i <= segments; i++) {
            double angle = i * Math.PI * 2.0 / segments;
            float x = (float)Math.cos(angle) * radius;
            float y = (float)Math.sin(angle) * radius;
            buffer.vertex(matrix, x, y, 0.0F).color(r, g, b, 0.0F);
        }

        try {
            AuraRender.drawColor(buffer, AuraRender.POS_COLOR_TRI_FAN);
        } catch (Exception var16) {
        }
    }

    public static void drawVolumetricLine(MatrixStack matrices, Vec3d start, Vec3d end, float width, int color, Vec3d cameraPos) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = (color >> 24 & 0xFF) / 255.0F;
        Vec3d dir = end.subtract(start).normalize();
        Vec3d toCam = start.subtract(cameraPos).normalize();
        Vec3d right = dir.crossProduct(toCam).normalize().multiply(width * 0.5);
        Vector3f pos1 = new Vector3f((float)(start.x + right.x), (float)(start.y + right.y), (float)(start.z + right.z));
        Vector3f pos2 = new Vector3f((float)(start.x - right.x), (float)(start.y - right.y), (float)(start.z - right.z));
        new Vector3f((float)(end.x + right.x), (float)(end.y + right.y), (float)(end.z + right.z));
        new Vector3f((float)(end.x - right.x), (float)(end.y - right.y), (float)(end.z - right.z));
        buffer.vertex(matrix, pos1.x, pos1.y, pos1.z).color(r, g, b, 0.0F);
        buffer.vertex(matrix, pos2.x, pos2.y, pos2.z).color(r, g, b, a);
    }

    public static void renderRibbon(BufferBuilder buffer, Matrix4f matrix, Vec3d start, Vec3d end, float width, int color, Vec3d cameraPos) {
        Vec3d dir = end.subtract(start).normalize();
        Vec3d viewDir = start.subtract(cameraPos).normalize();
        Vec3d right = dir.crossProduct(viewDir).normalize().multiply(width * 0.5);
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = (color >> 24 & 0xFF) / 255.0F;
        buffer.vertex(matrix, (float)(start.x + right.x), (float)(start.y + right.y), (float)(start.z + right.z)).color(r, g, b, 0.0F);
        buffer.vertex(matrix, (float)start.x, (float)start.y, (float)start.z).color(r, g, b, a);
        buffer.vertex(matrix, (float)(end.x + right.x), (float)(end.y + right.y), (float)(end.z + right.z)).color(r, g, b, 0.0F);
        buffer.vertex(matrix, (float)end.x, (float)end.y, (float)end.z).color(r, g, b, a);
        buffer.vertex(matrix, (float)(start.x - right.x), (float)(start.y - right.y), (float)(start.z - right.z)).color(r, g, b, 0.0F);
        buffer.vertex(matrix, (float)start.x, (float)start.y, (float)start.z).color(r, g, b, a);
        buffer.vertex(matrix, (float)(end.x - right.x), (float)(end.y - right.y), (float)(end.z - right.z)).color(r, g, b, 0.0F);
        buffer.vertex(matrix, (float)end.x, (float)end.y, (float)end.z).color(r, g, b, a);
    }
}
