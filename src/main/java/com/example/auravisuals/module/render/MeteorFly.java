package com.example.auravisuals.module.render;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.theme.ThemeManager;
import com.example.auravisuals.util.color.ColorUtil;
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
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class MeteorFly extends Module {
    static final Identifier GLOW_TEXTURE = Identifier.of("auravisuals", "textures/glow.png");
    static final float SPEED = 0.35F;
    static final float SPAWN_RADIUS = 35.0F;
    static final int TRAIL_LENGTH = 42;
    static final long MIN_LIFETIME_MS = 4000L;
    static final long MAX_LIFETIME_MS = 8000L;
    static final long FADE_IN_MS = 900L;
    static final long FADE_OUT_MS = 1800L;
    final NumberSetting count = new NumberSetting("Количество", 20.0, 10.0, 50.0, 1.0);
    final BooleanSetting glow = new BooleanSetting("Свечение", true);
    final BooleanSetting customColor = new BooleanSetting("Свой цвет", false);
    final ColorSetting color = new ColorSetting("Цвет", new Color(255, 110, 80), () -> this.customColor.getValue());
    final List<MeteorFly$MeteorParticle> particles = new ArrayList<>();
    final Random random = new Random();

    public MeteorFly() {
        super("MeteorFly", "Красивые метеориты в мире", Category.VISUALS);
        this.addSettings(this.count, this.glow, this.customColor, this.color);
    }

    @Override
    public void onEnable() {
        this.particles.clear();
    }

    @Override
    public void onDisable() {
        this.particles.clear();
    }

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.world != null) {
            Vec3d playerPos = this.mc.player.getEntityPos();
            float maxSpeed = 0.525F;
            long now = System.currentTimeMillis();

            for (MeteorFly$MeteorParticle particle : this.particles) {
                particle.update(0.35F, maxSpeed, playerPos);
            }

            this.particles.removeIf(p -> p.isExpired(now) || p.isDead(playerPos.x, playerPos.y, playerPos.z));
            int targetCount = this.count.getIntValue();

            while (this.particles.size() > targetCount) {
                this.particles.remove(this.particles.size() - 1);
            }

            while (this.particles.size() < targetCount) {
                this.spawnParticle(playerPos);
            }
        }
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.mc.player != null && this.mc.world != null && !this.particles.isEmpty()) {
            this.renderMeteors(context.matrixStack(), context.camera(), context.tickCounter().getTickProgress(true));
        }
    }

    void spawnParticle(Vec3d playerPos) {
        double distance = this.random.nextDouble() * 30.0 + 5.0;
        double yawRad = Math.toRadians(this.random.nextDouble() * 360.0);
        double xOffset = -Math.sin(yawRad) * distance;
        double zOffset = Math.cos(yawRad) * distance;
        double yOffset = (this.random.nextDouble() - 0.3) * 8.0 + 1.0;
        double velocityYaw = Math.toRadians(this.random.nextDouble() * 360.0);
        double velocityPitch = Math.toRadians((this.random.nextDouble() - 0.5) * 60.0);
        double velX = -Math.sin(velocityYaw) * Math.cos(velocityPitch) * 0.35F;
        double velY = Math.sin(velocityPitch) * 0.35F * 0.5;
        double velZ = Math.cos(velocityYaw) * Math.cos(velocityPitch) * 0.35F;
        int[] randomColors = new int[]{-10496, -256, -16711936, -16711681, -38476, -23296, -16728065};
        int randomColor = randomColors[this.random.nextInt(randomColors.length)];
        long lifetime = 4000L + (long)(this.random.nextDouble() * 4000.0);
        this.particles
            .add(new MeteorFly$MeteorParticle(playerPos.x + xOffset, playerPos.y + yOffset, playerPos.z + zOffset, velX, velY, velZ, randomColor, 42, lifetime));
    }

    void renderMeteors(MatrixStack stack, Camera camera, float tickDelta) {
        Vec3d cameraPos = camera.getCameraPos();
        int theme = ThemeManager.INSTANCE.getVisualModulesColor();
        BufferBuilder buffer = null;
        Matrix4f globalMatrix = stack.peek().getPositionMatrix();

        for (MeteorFly$MeteorParticle particle : this.particles) {
            float lifeAlpha = particle.getLifeAlpha();
            if (!(lifeAlpha <= 0.01F) && particle.trail.size() >= 2) {
                int renderColor = this.customColor.getValue() ? this.color.getValue() : theme;
                double px = particle.getInterpolatedX(tickDelta);
                double pz;
                double py;
                if (!(cameraPos.squaredDistanceTo(px, py = particle.getInterpolatedY(tickDelta), pz = particle.getInterpolatedZ(tickDelta)) > 9216.0)) {
                    int segments = particle.trail.size();
                    double currentXWorld = px;
                    double currentYWorld = py;
                    double currentZWorld = pz;

                    for (int i = 0; i < segments; i++) {
                        MeteorFly$TrailPoint point = particle.trail.get(i);
                        double nextXWorld = point.x;
                        double nextYWorld = point.y;
                        double nextZWorld = point.z;
                        float t1 = (float)i / segments;
                        float t2 = (float)(i + 1) / segments;
                        float w1 = 0.12F * (1.0F - t1);
                        float w2 = 0.12F * (1.0F - t2);
                        float alpha1 = (1.0F - t1) * (1.0F - t1) * lifeAlpha * 0.6F;
                        float alpha2 = (1.0F - t2) * (1.0F - t2) * lifeAlpha * 0.6F;
                        double dirZ;
                        double dirY;
                        double dirX;
                        Vec3d dir;
                        if ((!(alpha1 <= 0.01F) || !(alpha2 <= 0.01F))
                            && !(
                                (dir = new Vec3d(dirX = currentXWorld - nextXWorld, dirY = currentYWorld - nextYWorld, dirZ = currentZWorld - nextZWorld))
                                        .lengthSquared()
                                    < 1.0E-4
                            )) {
                            Vec3d cross1 = dir.crossProduct(new Vec3d(cameraPos.x - currentXWorld, cameraPos.y - currentYWorld, cameraPos.z - currentZWorld));
                            Vec3d cross2 = dir.crossProduct(new Vec3d(cameraPos.x - nextXWorld, cameraPos.y - nextYWorld, cameraPos.z - nextZWorld));
                            if (!(cross1.lengthSquared() < 1.0E-4) && !(cross2.lengthSquared() < 1.0E-4)) {
                                Vec3d right1 = cross1.normalize().multiply(w1);
                                Vec3d right2 = cross2.normalize().multiply(w2);
                                int c1 = ColorUtil.withAlpha(renderColor, (int)(alpha1 * 255.0F));
                                int c2 = ColorUtil.withAlpha(renderColor, (int)(alpha2 * 255.0F));
                                double currentX = currentXWorld - cameraPos.x;
                                double currentY = currentYWorld - cameraPos.y;
                                double currentZ = currentZWorld - cameraPos.z;
                                double nextX = nextXWorld - cameraPos.x;
                                double nextY = nextYWorld - cameraPos.y;
                                double nextZ = nextZWorld - cameraPos.z;
                                if (buffer == null) {
                                    buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                                }

                                buffer.vertex(globalMatrix, (float)(currentX + right1.x), (float)(currentY + right1.y), (float)(currentZ + right1.z))
                                    .texture(0.0F, t1)
                                    .color(c1);
                                buffer.vertex(globalMatrix, (float)(currentX - right1.x), (float)(currentY - right1.y), (float)(currentZ - right1.z))
                                    .texture(1.0F, t1)
                                    .color(c1);
                                buffer.vertex(globalMatrix, (float)(nextX - right2.x), (float)(nextY - right2.y), (float)(nextZ - right2.z))
                                    .texture(1.0F, t2)
                                    .color(c2);
                                buffer.vertex(globalMatrix, (float)(nextX + right2.x), (float)(nextY + right2.y), (float)(nextZ + right2.z))
                                    .texture(0.0F, t2)
                                    .color(c2);
                                currentXWorld = nextXWorld;
                                currentYWorld = nextYWorld;
                                currentZWorld = nextZWorld;
                            }
                        }
                    }
                }
            }
        }

        for (MeteorFly$MeteorParticle particle : this.particles) {
            float lifeAlpha = particle.getLifeAlpha();
            float finalAlpha;
            if (!(lifeAlpha <= 0.01F) && !((finalAlpha = particle.getPulseAlpha() / 255.0F * lifeAlpha) <= 0.01F)) {
                int renderColor = this.customColor.getValue() ? this.color.getValue() : theme;
                double px = particle.getInterpolatedX(tickDelta);
                double py = particle.getInterpolatedY(tickDelta);
                double pz = particle.getInterpolatedZ(tickDelta);
                stack.push();
                stack.translate(px - cameraPos.x, py - cameraPos.y, pz - cameraPos.z);
                stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                Matrix4f localMatrix = stack.peek().getPositionMatrix();
                if (buffer == null) {
                    buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                }

                if (this.glow.getValue()) {
                    this.drawQuad(buffer, localMatrix, 0.35F, renderColor, finalAlpha * 0.6F);
                    this.drawQuad(buffer, localMatrix, 0.22F, renderColor, finalAlpha);
                }

                this.drawQuad(buffer, localMatrix, 0.1F, -1, finalAlpha);
                stack.pop();
            }
        }

        if (buffer != null) {
            AuraRender.drawTextured(buffer, this.glow.getValue() ? AuraRender.POS_TEX_COLOR_QUADS_ADD : AuraRender.POS_TEX_COLOR_QUADS, GLOW_TEXTURE);
        }
    }

    void drawQuad(BufferBuilder buffer, Matrix4f matrix, float size, int color, float alphaMod) {
        if (!(alphaMod <= 0.01F)) {
            int finalColor = ColorUtil.withAlpha(color, (int)(alphaMod * 255.0F));
            buffer.vertex(matrix, -size, -size, 0.0F).texture(0.0F, 0.0F).color(finalColor);
            buffer.vertex(matrix, -size, size, 0.0F).texture(0.0F, 1.0F).color(finalColor);
            buffer.vertex(matrix, size, size, 0.0F).texture(1.0F, 1.0F).color(finalColor);
            buffer.vertex(matrix, size, -size, 0.0F).texture(1.0F, 0.0F).color(finalColor);
        }
    }
}
