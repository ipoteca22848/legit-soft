package com.example.auravisuals.module.render;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.theme.ThemeManager;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class Cubes extends Module {
    static final Identifier GLOW_TEX = Identifier.of("auravisuals", "textures/glow.png");
    static final float DEFAULT_SPAWN_RADIUS = 12.0F;
    static final float PARTICLE_SIZE = 0.18F;
    static final float PARTICLE_SPEED = 0.25F;
    static final float TICK_SIMULATION_SCALE = 3.0F;
    static final float GLOW_INTENSITY = 1.7F;
    static final byte[][] CUBE_EDGES = new byte[][]{
        {-1, -1, -1, 1, -1, -1},
        {1, -1, -1, 1, -1, 1},
        {1, -1, 1, -1, -1, 1},
        {-1, -1, 1, -1, -1, -1},
        {-1, 1, -1, 1, 1, -1},
        {1, 1, -1, 1, 1, 1},
        {1, 1, 1, -1, 1, 1},
        {-1, 1, 1, -1, 1, -1},
        {-1, -1, -1, -1, 1, -1},
        {1, -1, -1, 1, 1, -1},
        {1, -1, 1, 1, 1, 1},
        {-1, -1, 1, -1, 1, 1}
    };
    static final byte[][] TRIANGLE_EDGES = new byte[][]{{0, 1}, {0, 2}, {0, 3}, {0, 4}, {1, 2}, {2, 3}, {3, 4}, {4, 1}};
    static final float[] GLOW_SCALES = new float[]{10.0F, 6.0F, 3.5F};
    static final float[] GLOW_ALPHA_SCALES = new float[]{0.06F, 0.14F, 0.25F};
    public static final ModeSetting animation = new ModeSetting("Анимация", "Разлет", "Разлет", "Падение");
    public static final ModeSetting shape = new ModeSetting("Форма", "Кубы", "Кубы", "Треугольники");
    public static final NumberSetting count = new NumberSetting("Количество", 30.0, 5.0, 100.0, 1.0);
    public static final NumberSetting size = new NumberSetting("Размер", 1.0, 0.1, 3.0, 0.1);
    public static final NumberSetting speed = new NumberSetting("Скорость", 1.0, 0.1, 5.0, 0.1);
    public static final NumberSetting distance = new NumberSetting("Дальность от игрока", 12.0, 5.0, 24.0, 0.5);
    public static final NumberSetting glowStrength = new NumberSetting("Сила свечения", 1.0, 0.5, 1.5, 0.05);
    public static final BooleanSetting useCustomColor = new BooleanSetting("Свой цвет", false);
    public static final ColorSetting color = new ColorSetting("Цвет", new Color(120, 200, 255), () -> useCustomColor.getValue());
    final List<Cubes$CubeParticle> cubes = new ArrayList<>();
    final List<Cubes$CubeParticle> visibleCubes = new ArrayList<>();
    final Random random = new Random();
    boolean lastAttackPressed;
    float cr;
    float cg;
    float cb;

    public Cubes() {
        super("Cubes", "3D Кубы по миру", Category.VISUALS);
        this.addSettings(animation, shape, count, size, speed, distance, glowStrength, useCustomColor, color);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.cubes.clear();
    }

    @Override
    public void onDisable() {
        this.cubes.clear();
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.mc.player != null && this.mc.world != null) {
            Camera camera = context.camera();
            boolean attackPressed = this.mc.options.attackKey.isPressed();
            if (attackPressed && !this.lastAttackPressed) {
                this.applyHitImpulseFromCrosshair(camera);
            }

            this.lastAttackPressed = attackPressed;
            this.renderCubes(context, camera);
        }
    }

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.world != null) {
            this.updateCubes();
        }
    }

    int themeColor() {
        return useCustomColor.getValue() ? color.getValue() & 16777215 : ThemeManager.INSTANCE.getVisualModulesColor() & 16777215;
    }

    void applyHitImpulseFromCrosshair(Camera camera) {
        if (!this.cubes.isEmpty() && camera != null) {
            Vec3d origin = camera.getCameraPos();
            float yaw = (float)Math.toRadians(camera.getYaw());
            float pitch = (float)Math.toRadians(camera.getPitch());
            double dirX = -MathHelper.sin(yaw) * MathHelper.cos(pitch);
            double dirY = -MathHelper.sin(pitch);
            double dirZ = MathHelper.cos(yaw) * MathHelper.cos(pitch);
            Cubes$CubeParticle best = null;
            double bestT = Double.MAX_VALUE;
            int sz = this.cubes.size();

            for (int i = 0; i < sz; i++) {
                Cubes$CubeParticle p = this.cubes.get(i);
                double opX = p.x - origin.x;
                double opY = p.y - origin.y;
                double opZ = p.z - origin.z;
                double t = opX * dirX + opY * dirY + opZ * dirZ;
                double closestZ;
                double dz;
                double closestY;
                double dy;
                double closestX;
                double dx;
                if (!(t < 0.0)
                    && !(t > 128.0)
                    && !(
                        (dx = p.x - (closestX = origin.x + dirX * t)) * dx
                                + (dy = p.y - (closestY = origin.y + dirY * t)) * dy
                                + (dz = p.z - (closestZ = origin.z + dirZ * t)) * dz
                            > 1.32
                    )
                    && !(t >= bestT)) {
                    bestT = t;
                    best = p;
                }
            }

            if (best != null) {
                double force = 0.08 * speed.getFloatValue();
                best.vx = (float)(best.vx + dirX * force);
                best.vy = (float)(best.vy + (dirY * force + 0.02));
                best.vz = (float)(best.vz + dirZ * force);
            }
        }
    }

    void updateCubes() {
        int target = count.getIntValue();
        int currentSize = this.cubes.size();
        if (currentSize < target) {
            int toAdd = Math.min(target - currentSize, 10);

            for (int i = 0; i < toAdd; i++) {
                this.cubes.add(this.spawnCube());
            }
        } else if (currentSize > target) {
            this.cubes.subList(target, currentSize).clear();
        }

        float spd = 0.25F * speed.getFloatValue() * 3.0F;
        float maxR = distance.getFloatValue();
        boolean falling = animation.is("Падение");
        Vec3d playerPos = this.mc.player.getEntityPos();
        double recycleRadius = maxR * 1.75;
        double maxRSq = recycleRadius * recycleRadius;

        for (int i = this.cubes.size() - 1; i >= 0; i--) {
            Cubes$CubeParticle p = this.cubes.get(i);
            p.prevX = p.x;
            p.prevY = p.y;
            p.prevZ = p.z;
            p.prevRotX = p.rotX;
            p.prevRotY = p.rotY;
            p.prevRotZ = p.rotZ;
            if (falling) {
                p.wobblePhase += 0.06F * spd;
                p.x = p.x + (p.vx * spd + Math.sin(p.wobblePhase + p.wobbleOffset) * 0.0024F * spd);
                p.y = p.y + p.vy * spd;
                p.z = p.z + (p.vz * spd + Math.cos(p.wobblePhase * 0.8F + p.wobbleOffset) * 0.002F * spd);
                p.vy = Math.max(p.vy - 8.0E-5F * spd, -0.032F);
                p.rotX = p.rotX + p.rotSpeedX * 0.2F * spd;
                p.rotY = p.rotY + p.rotSpeedY * 0.2F * spd;
                p.rotZ = p.rotZ + p.rotSpeedZ * 0.2F * spd;
            } else {
                p.x = p.x + p.vx * spd;
                p.y = p.y + p.vy * spd;
                p.z = p.z + p.vz * spd;
                p.rotX = p.rotX + p.rotSpeedX * spd;
                p.rotY = p.rotY + p.rotSpeedY * spd;
                p.rotZ = p.rotZ + p.rotSpeedZ * spd;
                p.vx *= 0.995F;
                p.vy *= 0.995F;
                p.vz *= 0.995F;
            }

            p.life--;
            double dx = p.x - playerPos.x;
            double dy = p.y - playerPos.y;
            double dz = p.z - playerPos.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (p.life <= 0 || distSq > maxRSq || falling && p.y < playerPos.y - 2.5) {
                this.cubes.remove(i);
                this.cubes.add(this.spawnCube());
            }
        }
    }

    void renderCubes(WorldRenderContext context, Camera camera) {
        if (this.mc.player != null) {
            MatrixStack ms = context.matrixStack();
            Vec3d cam = camera.getCameraPos();
            float s = 0.18F * size.getFloatValue();
            float glow = 1.7F * glowStrength.getFloatValue();
            float tickDelta = context.tickCounter().getTickProgress(true);
            int baseRGB = this.themeColor();
            this.cr = (baseRGB >> 16 & 0xFF) / 255.0F;
            this.cg = (baseRGB >> 8 & 0xFF) / 255.0F;
            this.cb = (baseRGB & 0xFF) / 255.0F;
            this.visibleCubes.clear();
            float yaw = (float)Math.toRadians(camera.getYaw());
            float pitch = (float)Math.toRadians(camera.getPitch());
            double lookX = -MathHelper.sin(yaw) * MathHelper.cos(pitch);
            double lookY = -MathHelper.sin(pitch);
            double lookZ = MathHelper.cos(yaw) * MathHelper.cos(pitch);
            float maxRenderDistance = Math.max(30.0F, distance.getFloatValue() * 1.55F);
            float maxRenderDistanceSq = maxRenderDistance * maxRenderDistance;
            int sz = this.cubes.size();

            for (int i = 0; i < sz; i++) {
                Cubes$CubeParticle p = this.cubes.get(i);
                p.renderX = p.prevX + (p.x - p.prevX) * tickDelta;
                p.renderY = p.prevY + (p.y - p.prevY) * tickDelta;
                p.renderZ = p.prevZ + (p.z - p.prevZ) * tickDelta;
                p.renderRotX = p.prevRotX + (p.rotX - p.prevRotX) * tickDelta;
                p.renderRotY = p.prevRotY + (p.rotY - p.prevRotY) * tickDelta;
                p.renderRotZ = p.prevRotZ + (p.rotZ - p.prevRotZ) * tickDelta;
                double dx = p.renderX - cam.x;
                double dy = p.renderY - cam.y;
                double dz = p.renderZ - cam.z;
                double distSq = dx * dx + dy * dy + dz * dz;
                if (!(distSq > maxRenderDistanceSq) && !(dx * lookX + dy * lookY + dz * lookZ < -1.0)) {
                    p.renderAlpha = this.getAlpha(p);
                    if (!(p.renderAlpha < 0.01F)) {
                        this.visibleCubes.add(p);
                    }
                }
            }

            if (!this.visibleCubes.isEmpty()) {
                if (glow > 0.001F) {
                    this.drawGlowBatch(ms, camera, cam, s, glow);
                }

                boolean isCubes = shape.is("Кубы");
                boolean isTriangles = shape.is("Треугольники");
                if (isCubes) {
                    this.drawCubeFacesBatch(ms, cam, s);
                    this.drawCubeDashedEdgesBatch(ms, cam, s);
                } else if (isTriangles) {
                    this.drawTriangleFacesBatch(ms, cam, s);
                    this.drawTriangleDashedEdgesBatch(ms, cam, s);
                }
            }
        }
    }

    void drawGlowBatch(MatrixStack ms, Camera camera, Vec3d cam, float s, float glow) {
        BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int sz = this.visibleCubes.size();

        for (int particleIndex = 0; particleIndex < sz; particleIndex++) {
            Cubes$CubeParticle p = this.visibleCubes.get(particleIndex);
            float alpha = p.renderAlpha;
            if (!(alpha < 0.01F)) {
                ms.push();
                ms.translate(p.renderX - cam.x, p.renderY - cam.y, p.renderZ - cam.z);
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                Matrix4f matrix = ms.peek().getPositionMatrix();
                float scale = s * 5.0F * glow;
                float a = MathHelper.clamp(alpha * 0.45F * glow, 0.0F, 1.0F);
                float hs = scale * 0.5F;
                builder.vertex(matrix, -hs, hs, 0.0F).texture(0.0F, 1.0F).color(this.cr, this.cg, this.cb, a);
                builder.vertex(matrix, hs, hs, 0.0F).texture(1.0F, 1.0F).color(this.cr, this.cg, this.cb, a);
                builder.vertex(matrix, hs, -hs, 0.0F).texture(1.0F, 0.0F).color(this.cr, this.cg, this.cb, a);
                builder.vertex(matrix, -hs, -hs, 0.0F).texture(0.0F, 0.0F).color(this.cr, this.cg, this.cb, a);
                ms.pop();
            }
        }

        try {
            AuraRender.drawTextured(builder, AuraRender.POS_TEX_COLOR_QUADS_ADD, GLOW_TEX);
        } catch (Exception var15) {
        }
    }

    float getAlpha(Cubes$CubeParticle p) {
        float lifePct = MathHelper.clamp((float)p.life / p.maxLife, 0.0F, 1.0F);
        float fadeIn = Math.min(1.0F, (p.maxLife - p.life) / 10.0F);
        return lifePct * fadeIn;
    }

    void drawCubeFacesBatch(MatrixStack ms, Vec3d cam, float s) {
        if (this.hasFaceRenderableParticles()) {
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int sz = this.visibleCubes.size();

            for (int i = 0; i < sz; i++) {
                Cubes$CubeParticle p = this.visibleCubes.get(i);
                float alpha = p.renderAlpha * 0.4F;
                if (!(alpha < 0.01F)) {
                    ms.push();
                    ms.translate(p.renderX - cam.x, p.renderY - cam.y, p.renderZ - cam.z);
                    ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(p.renderRotX));
                    ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(p.renderRotY));
                    ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p.renderRotZ));
                    this.appendCubeFaces(buffer, ms.peek().getPositionMatrix(), s, alpha);
                    ms.pop();
                }
            }

            try {
                AuraRender.drawColor(buffer, AuraRender.POS_COLOR_QUADS);
            } catch (Exception var9) {
            }
        }
    }

    void drawTriangleFacesBatch(MatrixStack ms, Vec3d cam, float s) {
        if (this.hasFaceRenderableParticles()) {
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            int sz = this.visibleCubes.size();

            for (int i = 0; i < sz; i++) {
                Cubes$CubeParticle p = this.visibleCubes.get(i);
                float alpha = p.renderAlpha * 0.4F;
                if (!(alpha < 0.01F)) {
                    ms.push();
                    ms.translate(p.renderX - cam.x, p.renderY - cam.y, p.renderZ - cam.z);
                    ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(p.renderRotX));
                    ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(p.renderRotY));
                    ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p.renderRotZ));
                    this.appendTriangleFaces(buffer, ms.peek().getPositionMatrix(), s, alpha);
                    ms.pop();
                }
            }

            try {
                AuraRender.drawColor(buffer, AuraRender.POS_COLOR_TRIS);
            } catch (Exception var9) {
            }
        }
    }

    boolean hasFaceRenderableParticles() {
        int sz = this.visibleCubes.size();

        for (int i = 0; i < sz; i++) {
            if (this.visibleCubes.get(i).renderAlpha >= 0.025F) {
                return true;
            }
        }

        return false;
    }

    void appendCubeFaces(BufferBuilder buffer, Matrix4f m, float s, float a) {
        buffer.vertex(m, -s, -s, s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, s, -s, s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, s, s, s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, -s, s, s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, s, -s, -s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, -s, -s, -s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, -s, s, -s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, s, s, -s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, -s, s, s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, s, s, s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, s, s, -s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, -s, s, -s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, -s, -s, -s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, s, -s, -s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, s, -s, s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, -s, -s, s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, s, -s, s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, s, -s, -s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, s, s, -s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, s, s, s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, -s, -s, -s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, -s, -s, s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, -s, s, s).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, -s, s, -s).color(this.cr, this.cg, this.cb, a);
    }

    void appendTriangleFaces(BufferBuilder buffer, Matrix4f m, float s, float a) {
        float top = s;
        float bottom = -s;
        float halfBase = s * 0.866F;
        buffer.vertex(m, 0.0F, top, 0.0F).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, -halfBase, bottom, halfBase).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, halfBase, bottom, halfBase).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, 0.0F, top, 0.0F).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, halfBase, bottom, halfBase).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, halfBase, bottom, -halfBase).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, 0.0F, top, 0.0F).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, halfBase, bottom, -halfBase).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, -halfBase, bottom, -halfBase).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, 0.0F, top, 0.0F).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, -halfBase, bottom, -halfBase).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, -halfBase, bottom, halfBase).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, -halfBase, bottom, halfBase).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, halfBase, bottom, halfBase).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, halfBase, bottom, -halfBase).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, -halfBase, bottom, halfBase).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, halfBase, bottom, -halfBase).color(this.cr, this.cg, this.cb, a);
        buffer.vertex(m, -halfBase, bottom, -halfBase).color(this.cr, this.cg, this.cb, a);
    }

    void drawCubeDashedEdgesBatch(MatrixStack ms, Vec3d cam, float s) {
        BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        int lineCount = 0;
        int sz = this.visibleCubes.size();

        for (int i = 0; i < sz; i++) {
            Cubes$CubeParticle p = this.visibleCubes.get(i);
            float alpha = p.renderAlpha;
            ms.push();
            ms.translate(p.renderX - cam.x, p.renderY - cam.y, p.renderZ - cam.z);
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(p.renderRotX));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(p.renderRotY));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p.renderRotZ));
            lineCount += this.appendCubeDashedEdges(buf, ms.peek().getPositionMatrix(), s, alpha);
            ms.pop();
        }

        if (lineCount > 0) {
            try {
                AuraRender.drawColorAuto(buf, true, true, 1.5F);
            } catch (Exception var10) {
            }
        }
    }

    void drawTriangleDashedEdgesBatch(MatrixStack ms, Vec3d cam, float s) {
        BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        int lineCount = 0;
        int sz = this.visibleCubes.size();

        for (int i = 0; i < sz; i++) {
            Cubes$CubeParticle p = this.visibleCubes.get(i);
            float alpha = p.renderAlpha;
            ms.push();
            ms.translate(p.renderX - cam.x, p.renderY - cam.y, p.renderZ - cam.z);
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(p.renderRotX));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(p.renderRotY));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p.renderRotZ));
            lineCount += this.appendTriangleDashedEdges(buf, ms.peek().getPositionMatrix(), s, alpha);
            ms.pop();
        }

        if (lineCount > 0) {
            try {
                AuraRender.drawColorAuto(buf, true, true, 1.5F);
            } catch (Exception var10) {
            }
        }
    }

    int appendCubeDashedEdges(BufferBuilder buf, Matrix4f mat, float s, float alpha) {
        int colorInt = colorToInt(Math.min(1.0F, this.cr * 1.5F), Math.min(1.0F, this.cg * 1.5F), Math.min(1.0F, this.cb * 1.5F), alpha);
        float dashLen = s * 0.3F;
        float gapLen = s * 0.25F;
        int lineCount = 0;

        for (byte[] edge : CUBE_EDGES) {
            float x2 = edge[3] * s;
            float x1 = edge[0] * s;
            float dx = x2 - x1;
            float y2 = edge[4] * s;
            float y1 = edge[1] * s;
            float dy = y2 - y1;
            float z2 = edge[5] * s;
            float z1 = edge[2] * s;
            float dz = z2 - z1;
            float len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
            if (!(len < 0.001F)) {
                float nx = dx / len;
                float ny = dy / len;
                float nz = dz / len;
                float pos = 0.0F;

                for (boolean drawing = true; pos < len; drawing = !drawing) {
                    float segLen = drawing ? dashLen : gapLen;
                    float end = Math.min(pos + segLen, len);
                    if (drawing) {
                        buf.vertex(mat, x1 + nx * pos, y1 + ny * pos, z1 + nz * pos).color(colorInt);
                        buf.vertex(mat, x1 + nx * end, y1 + ny * end, z1 + nz * end).color(colorInt);
                        lineCount++;
                    }

                    pos = end;
                }
            }
        }

        return lineCount;
    }

    int appendTriangleDashedEdges(BufferBuilder buf, Matrix4f mat, float s, float alpha) {
        int colorInt = colorToInt(Math.min(1.0F, this.cr * 1.5F), Math.min(1.0F, this.cg * 1.5F), Math.min(1.0F, this.cb * 1.5F), alpha);
        float dashLen = s * 0.3F;
        float gapLen = s * 0.25F;
        int lineCount = 0;
        float top = s;
        float bottom = -s;
        float halfBase = s * 0.866F;

        for (byte[] edge : TRIANGLE_EDGES) {
            float x1 = this.trianglePointX(edge[0], halfBase);
            float y1 = edge[0] == 0 ? top : bottom;
            float z1 = this.trianglePointZ(edge[0], halfBase);
            float x2 = this.trianglePointX(edge[1], halfBase);
            float dx = x2 - x1;
            float dz;
            float y2;
            float dy;
            float len = MathHelper.sqrt(dx * dx + (dy = (y2 = edge[1] == 0 ? top : bottom) - y1) * dy + (dz = this.trianglePointZ(edge[1], halfBase) - z1) * dz);
            if (!(len < 0.001F)) {
                float nx = dx / len;
                float ny = dy / len;
                float nz = dz / len;
                float pos = 0.0F;

                for (boolean drawing = true; pos < len; drawing = !drawing) {
                    float segLen = drawing ? dashLen : gapLen;
                    float end = Math.min(pos + segLen, len);
                    if (drawing) {
                        buf.vertex(mat, x1 + nx * pos, y1 + ny * pos, z1 + nz * pos).color(colorInt);
                        buf.vertex(mat, x1 + nx * end, y1 + ny * end, z1 + nz * end).color(colorInt);
                        lineCount++;
                    }

                    pos = end;
                }
            }
        }

        return lineCount;
    }

    float trianglePointX(int index, float halfBase) {
        return switch (index) {
            case 1, 4 -> -halfBase;
            case 2, 3 -> halfBase;
            default -> 0.0F;
        };
    }

    float trianglePointZ(int index, float halfBase) {
        return switch (index) {
            case 1, 2 -> halfBase;
            case 3, 4 -> -halfBase;
            default -> 0.0F;
        };
    }

    Cubes$CubeParticle spawnCube() {
        float r = distance.getFloatValue();
        boolean falling = animation.is("Падение");
        int life = falling ? 260 + this.random.nextInt(220) : 420 + this.random.nextInt(420);
        double x = this.mc.player.getX() + (this.random.nextDouble() * 2.0 - 1.0) * r;
        double y = falling
            ? this.mc.player.getY() + 4.0 + this.random.nextDouble() * (r * 0.55)
            : this.mc.player.getY() + 2.0 + this.random.nextDouble() * (r * 0.8);
        double z = this.mc.player.getZ() + (this.random.nextDouble() * 2.0 - 1.0) * r;
        float speedMult = speed.getFloatValue();
        float vz;
        float vy;
        float vx;
        if (falling) {
            vx = (this.random.nextFloat() - 0.5F) * 0.008F * speedMult;
            vy = (-0.012F - this.random.nextFloat() * 0.012F) * speedMult;
            vz = (this.random.nextFloat() - 0.5F) * 0.008F * speedMult;
        } else {
            float yaw = this.random.nextFloat() * 360.0F;
            float vel = (0.01F + this.random.nextFloat() * 0.02F) * speedMult;
            vx = -MathHelper.sin((float)Math.toRadians(yaw)) * vel;
            vz = MathHelper.cos((float)Math.toRadians(yaw)) * vel;
            vy = (this.random.nextFloat() - 0.5F) * 0.01F * speedMult;
        }

        return new Cubes$CubeParticle(
            x,
            y,
            z,
            vx,
            vy,
            vz,
            this.random.nextFloat() * 360.0F,
            this.random.nextFloat() * 360.0F,
            this.random.nextFloat() * 360.0F,
            (this.random.nextFloat() - 0.5F) * 1.5F,
            (this.random.nextFloat() - 0.5F) * 1.5F,
            (this.random.nextFloat() - 0.5F) * 1.5F,
            life,
            (float)(this.random.nextDouble() * Math.PI * 2.0),
            this.random.nextFloat() * 10.0F
        );
    }

    static int colorToInt(float r, float g, float b, float a) {
        return (int)(a * 255.0F) << 24 | (int)(r * 255.0F) << 16 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F);
    }
}
