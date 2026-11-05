package com.example.auravisuals.module.render;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.render.WorldOcclusion;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class TargetESP extends Module {
    static final String MODE_CRYSTALS = "Кристаллы";
    static final String MODE_RHOMB = "Ромб";
    static final String MODE_RHOMB_2 = "Ромб 2";
    static final String MODE_GHOST = "Призраки";
    static final String MODE_CHAIN = "Цепь";
    static final String MODE_SKULL = "Череп";
    static final String MODE_VORTEX = "Вихрь";
    public static final ModeSetting mode = new ModeSetting("Режим", "Кристаллы", "Кристаллы", "Ромб", "Ромб 2", "Призраки", "Цепь", "Череп", "Вихрь");
    public static final ColorSetting color1 = new TargetESP$1("Цвет 1", new Color(127, 242, 255, 136));
    public static final ColorSetting color2 = new ColorSetting("Цвет 2", new Color(255, 50, 150, 255), TargetESP::usesTwoColors);
    public static final BooleanSetting changeColorOnHit = new BooleanSetting("Менять цвет при ударе", true);
    public static final ColorSetting hitColor = new ColorSetting("Цвет при ударе", new Color(255, 55, 55), () -> changeColorOnHit.getValue());
    public static final BooleanSetting crystalGlow = new BooleanSetting("Свечение", true, () -> mode.is("Кристаллы"));
    public static final NumberSetting ghostSize = new NumberSetting("Размер", 0.9, 0.5, 1.5, 0.05, () -> mode.is("Призраки"));
    public static final NumberSetting ghostSpeed = new NumberSetting("Скорость", 1.3, 0.75, 2.5, 0.05, () -> mode.is("Призраки"));
    static final Identifier RHOMB_TEXTURE = Identifier.of("auravisuals", "textures/world/cube.png");
    static final Identifier MARKER_TEXTURE = Identifier.of("auravisuals", "textures/features/targetesp/marker.png");
    static final Identifier CHAIN_TEXTURE = Identifier.of("auravisuals", "textures/features/targetesp/chain.png");
    static final Identifier SKULL_TEXTURE = Identifier.of("auravisuals", "textures/features/targetesp/skull.png");
    static final Identifier VORTEX_TEXTURE = Identifier.of("auravisuals", "textures/features/targetesp/vortex.png");
    static final Identifier BLOOM_TEXTURE = Identifier.of("auravisuals", "textures/bloom_polairis.png");
    static final Identifier GLOW_TEXTURE = Identifier.of("auravisuals", "textures/particles/polairis_glow.png");
    static final Identifier GHOST_TEXTURE = Identifier.of("auravisuals", "textures/particles/ghost-glow.png");
    static final Vector3f[] CRYSTAL_VERTICES = new Vector3f[]{
        new Vector3f(0.0F, 1.5F, 0.0F),
        new Vector3f(0.0F, -1.5F, 0.0F),
        new Vector3f(1.0F, 0.0F, 0.0F),
        new Vector3f(-1.0F, 0.0F, 0.0F),
        new Vector3f(0.0F, 0.0F, 1.0F),
        new Vector3f(0.0F, 0.0F, -1.0F)
    };
    static final int[][] CRYSTAL_FACES = new int[][]{{0, 2, 4}, {0, 4, 3}, {0, 3, 5}, {0, 5, 2}, {1, 4, 2}, {1, 3, 4}, {1, 5, 3}, {1, 2, 5}};
    static final float[] CRYSTAL_BRIGHTNESS = new float[]{1.0F, 0.8F, 0.6F, 0.9F, 0.7F, 0.5F, 0.4F, 0.6F};
    static final int VORTEX_LAYERS = 3;
    static final long MODULE_START_MS = System.currentTimeMillis();
    static final Random RANDOM = new Random();
    final float[] vortexRotSpeed = new float[3];
    final float[] vortexPitchSpeed = new float[3];
    final float[] vortexPitchPhase = new float[3];
    final float[] vortexYawBase = new float[3];
    LivingEntity currentTarget;
    LivingEntity lastTarget;
    LivingEntity vortexSeededFor;
    Vec3d smoothedPos;
    float alpha;
    float hurtProgress;
    float chainImpactProgress;
    long hitColorStartedMs = Long.MIN_VALUE;
    int lastHurtTime;
    float frameScale = 1.0F;
    long lastFrameMs = System.currentTimeMillis();

    public TargetESP() {
        super("TargetESP", "Красивая анимация на таргет", Category.VISUALS);
        this.addSettings(mode, color1, color2, changeColorOnHit, hitColor, crystalGlow, ghostSize, ghostSpeed);
        this.reseedVortex(null);
    }

    public void onAttack(LivingEntity attacked) {
        if (this.isEnabled() && changeColorOnHit.getValue() && attacked != null) {
            LivingEntity selected = this.resolveTarget();
            if (attacked == this.currentTarget || attacked == selected) {
                this.hitColorStartedMs = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void onDisable() {
        this.currentTarget = null;
        this.lastTarget = null;
        this.vortexSeededFor = null;
        this.smoothedPos = null;
        this.alpha = 0.0F;
        this.hurtProgress = 0.0F;
        this.chainImpactProgress = 0.0F;
        this.hitColorStartedMs = Long.MIN_VALUE;
        this.lastHurtTime = 0;
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.mc.player != null && this.mc.world != null) {
            long frameMs = System.currentTimeMillis();
            float delta = MathHelper.clamp((float)(frameMs - this.lastFrameMs) / 16.666666F, 0.0F, 3.0F);
            this.lastFrameMs = frameMs;
            this.frameScale = delta;
            float tickDelta = context.tickCounter().getTickProgress(true);
            LivingEntity target = this.resolveTarget();
            if (target != null && target.isAlive()) {
                this.currentTarget = target;
                if (this.lastTarget == target && this.smoothedPos != null) {
                    Vec3d targetPos = target.getLerpedPos(tickDelta);
                    float smoothing = Math.min(1.0F, Math.max(0.12F, tickDelta * 1.5F));
                    this.smoothedPos = new Vec3d(
                        this.smoothedPos.x + (targetPos.x - this.smoothedPos.x) * smoothing,
                        this.smoothedPos.y + (targetPos.y - this.smoothedPos.y) * smoothing,
                        this.smoothedPos.z + (targetPos.z - this.smoothedPos.z) * smoothing
                    );
                } else {
                    this.lastTarget = target;
                    this.smoothedPos = target.getLerpedPos(tickDelta);
                    this.lastHurtTime = 0;
                    this.hitColorStartedMs = Long.MIN_VALUE;
                }

                this.alpha = approach(this.alpha, 1.0F, 0.1F * delta);
            } else {
                this.alpha = approach(this.alpha, 0.0F, 0.1F * delta);
            }

            if (!(this.alpha <= 0.01F) && this.currentTarget != null && this.smoothedPos != null) {
                if (this.currentTarget.hurtTime > this.lastHurtTime) {
                    this.hitColorStartedMs = frameMs;
                }

                this.lastHurtTime = this.currentTarget.hurtTime;
                this.hurtProgress = changeColorOnHit.getValue() ? this.hitColorProgress(frameMs) : 0.0F;
                float impactTarget = this.currentTarget.hurtTime > 0 ? 1.0F : 0.0F;
                float impactStep = (impactTarget > this.chainImpactProgress ? 0.3F : 0.1F) * delta;
                this.chainImpactProgress = approach(this.chainImpactProgress, impactTarget, impactStep);
                Vec3d camera = context.camera().getCameraPos();
                if (WorldOcclusion.isEntityVisible(this.currentTarget, camera, tickDelta)) {
                    float cameraFade = this.computeCameraFade(this.currentTarget, camera, this.smoothedPos);
                    float renderAlpha = this.alpha * cameraFade;
                    if (!(renderAlpha <= 0.01F)) {
                        MatrixStack matrices = context.matrixStack();
                        matrices.push();

                        try {
                            matrices.translate(this.smoothedPos.x - camera.x, this.smoothedPos.y - camera.y, this.smoothedPos.z - camera.z);
                            String value = mode.getValue();
                            if ("Кристаллы".equals(value)) {
                                this.renderCrystals(matrices, this.currentTarget, renderAlpha, frameMs);
                            } else if ("Ромб".equals(value)) {
                                this.renderRhomb(matrices, this.currentTarget, renderAlpha, frameMs);
                            } else if ("Ромб 2".equals(value)) {
                                this.renderMarker(matrices, this.currentTarget, renderAlpha, frameMs);
                            } else if ("Призраки".equals(value)) {
                                this.renderGhost(matrices, this.currentTarget, renderAlpha, frameMs);
                            } else if ("Цепь".equals(value)) {
                                this.renderChain(matrices, this.currentTarget, renderAlpha, tickDelta);
                            } else if ("Череп".equals(value)) {
                                this.renderSkull(matrices, this.currentTarget, renderAlpha);
                            } else if ("Вихрь".equals(value)) {
                                this.renderVortex(matrices, this.currentTarget, renderAlpha, frameMs, VORTEX_TEXTURE);
                            }
                        } finally {
                            matrices.pop();
                        }
                    }
                }
            }
        }
    }

    LivingEntity resolveTarget() {
        Entity pointed = this.mc.targetedEntity;
        LivingEntity living;
        if (pointed instanceof LivingEntity && this.isValidTarget(living = (LivingEntity)pointed) && this.mc.player.distanceTo(living) <= 4.0F) {
            return living;
        }

        if (this.currentTarget != null && this.isValidTarget(this.currentTarget) && this.mc.player.distanceTo(this.currentTarget) <= 10.0F) {
            return this.currentTarget;
        }

        LivingEntity closest = null;
        double closestDistanceSq = 16.0;

        for (LivingEntity entity : this.mc.world.getEntitiesByClass(LivingEntity.class, this.mc.player.getBoundingBox().expand(4.0), this::isValidTarget)) {
            double distanceSq = entity.squaredDistanceTo(this.mc.player);
            if (distanceSq <= closestDistanceSq) {
                closestDistanceSq = distanceSq;
                closest = entity;
            }
        }

        return closest;
    }

    boolean isValidTarget(LivingEntity entity) {
        return entity != null && entity != this.mc.player && entity.isAlive() && !entity.isInvisible()
            ? entity instanceof PlayerEntity || entity instanceof MobEntity || entity instanceof AnimalEntity
            : false;
    }

    float computeCameraFade(LivingEntity target, Vec3d camera, Vec3d targetPos) {
        Box dangerZone = target.getBoundingBox().expand(0.35);
        if (dangerZone.contains(camera)) {
            return 0.0F;
        }

        double distance = camera.distanceTo(targetPos);
        float fadeStart = Math.max(1.25F, target.getWidth() * 1.6F);
        float fadeEnd = fadeStart + 0.9F;
        return MathHelper.clamp((float)((distance - fadeStart) / (fadeEnd - fadeStart)), 0.0F, 1.0F);
    }

    void renderRhomb(MatrixStack matrices, LivingEntity target, float alpha, long frameMs) {
        float timeRotation = (float)(frameMs % 6283L) / 1000.0F;
        float rotation = MathHelper.sin(timeRotation) * 360.0F;
        int c1 = withAlpha(this.animatedColor(color1.getValue()), (int)(255.0F * alpha));
        int c2 = withAlpha(this.animatedColor(color2.getValue()), (int)(255.0F * alpha));
        this.billboardGradient(matrices, RHOMB_TEXTURE, target.getHeight() / 2.0F, 0.35F, rotation, c2, c1, c2, c1);
    }

    void renderMarker(MatrixStack matrices, LivingEntity target, float alpha, long frameMs) {
        float rotation = (float)(frameMs % 3600L) / 3600.0F * 360.0F;
        float pulse = 1.0F + MathHelper.sin((float)(frameMs % 1400L) / 1400.0F * (float) (Math.PI * 2)) * 0.08F;
        int c1 = withAlpha(this.animatedColor(color1.getValue()), (int)(255.0F * alpha));
        int c2 = withAlpha(this.animatedColor(color2.getValue()), (int)(255.0F * alpha));
        int c3 = withAlpha(this.animatedColor(color1.getValue()), (int)(215.0F * alpha));
        int c4 = withAlpha(this.animatedColor(color2.getValue()), (int)(215.0F * alpha));
        this.billboardGradient(matrices, MARKER_TEXTURE, target.getHeight() / 2.0F, 0.65F * pulse, rotation, c1, c2, c3, c4);
    }

    void renderSkull(MatrixStack matrices, LivingEntity target, float alpha) {
        float impact = easeOutCubic(this.chainImpactProgress);
        float scale = 0.48F * (1.0F - 0.14F * impact);
        float strongAlpha = Math.min(1.0F, alpha * 1.85F);
        int c1 = withAlpha(this.animatedColor(color1.getValue()), (int)(255.0F * strongAlpha));
        int c2 = withAlpha(this.animatedColor(color2.getValue()), (int)(255.0F * strongAlpha));
        int c3 = withAlpha(this.animatedColor(color1.getValue()), (int)(250.0F * strongAlpha));
        int c4 = withAlpha(this.animatedColor(color2.getValue()), (int)(250.0F * strongAlpha));
        this.billboardGradient(matrices, SKULL_TEXTURE, target.getHeight() * 0.5F, scale, 180.0F, c1, c2, c3, c4);
    }

    void renderGhost(MatrixStack matrices, LivingEntity target, float alpha, long frameMs) {
        matrices.push();
        matrices.translate(0.0F, target.getHeight() * 0.6F, 0.0F);
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        this.ghostParticle(matrices, buffer, alpha, frameMs, 0);
        this.ghostParticle(matrices, buffer, alpha, frameMs, 1);
        this.ghostParticle(matrices, buffer, alpha, frameMs, 2);
        AuraRender.drawTextured(buffer, AuraRender.POS_TEX_COLOR_QUADS_ADD_NODEPTH, GHOST_TEXTURE);
        matrices.pop();
    }

    void ghostParticle(MatrixStack matrices, BufferBuilder buffer, float alpha, long frameMs, int transform) {
        double radius = 0.8 * (1.0 + (ghostSize.getFloatValue() - 0.9) * 0.18);
        double distance = 20.0;
        float particleSize = ghostSize.getFloatValue();
        float speed = ghostSpeed.getFloatValue();
        int count = Math.max(4, Math.round(10.0F * alpha));
        Quaternionf cameraRotation = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();

        for (int i = 0; i < count; i++) {
            double angle = 0.2 * (frameMs * 0.55 * speed - i * distance) / 40.0;
            double sin = Math.sin(angle) * radius;
            double cos = Math.cos(angle) * radius;

            Vec3d trans = switch (transform) {
                case 0 -> new Vec3d(sin, cos, -cos);
                case 1 -> new Vec3d(-sin, sin, -cos);
                default -> new Vec3d(-sin, -sin, cos);
            };
            matrices.push();
            matrices.translate(trans.x, trans.y, trans.z);
            matrices.multiply(cameraRotation);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)frameMs * 0.1F * speed - i * 10.0F));
            matrices.translate(particleSize / 2.0F, particleSize / 2.0F, 0.0F);
            int a = MathHelper.clamp((int)((255 - i) * alpha), 0, 255);
            int start = withAlpha(this.animatedColor(color1.getValue()), a);
            int end = withAlpha(this.animatedColor(color2.getValue()), a);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            vertex(buffer, matrix, 0.0F, -particleSize, 0.0F, 0.0F, 0.0F, end);
            vertex(buffer, matrix, -particleSize, -particleSize, 0.0F, 0.0F, 1.0F, end);
            vertex(buffer, matrix, -particleSize, 0.0F, 0.0F, 1.0F, 1.0F, start);
            vertex(buffer, matrix, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, start);
            matrices.pop();
        }
    }

    void renderChain(MatrixStack matrices, LivingEntity target, float alpha, float tickDelta) {
        float animationTime = target.age + tickDelta;
        float impact = easeOutCubic(this.chainImpactProgress);
        float baseRadius = target.getWidth() * 1.35F;
        float bandHeight = Math.max(0.9F, target.getHeight() * 0.54F);
        float compressedRadius = baseRadius * (1.0F - impact * 0.18F);
        float primaryBandHeight = bandHeight * (1.0F - impact * 0.08F);
        float secondaryBandHeight = bandHeight * 0.8F * (1.0F - impact * 0.12F);
        float roll = MathHelper.sin(animationTime * 0.22F) * 18.0F;
        float pitch = MathHelper.cos(animationTime * 0.18F) * 18.0F;
        float spin = animationTime * 5.0F;
        int start = this.animatedColor(color1.getValue());
        int end = this.animatedColor(color2.getValue());
        matrices.push();
        matrices.translate(0.0F, target.getHeight() * 0.52F, 0.0F);
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        this.renderChainBand(matrices, buffer, compressedRadius * 0.9F, primaryBandHeight, roll, pitch, spin, alpha, start, end, 0.0F);
        this.renderChainBand(matrices, buffer, compressedRadius * 0.98F, secondaryBandHeight, -roll, -pitch, -spin * 0.92F, alpha * 0.92F, end, start, 0.38F);
        AuraRender.drawTextured(buffer, AuraRender.POS_TEX_COLOR_QUADS_NODEPTH, CHAIN_TEXTURE);
        matrices.pop();
    }

    void renderChainBand(
        MatrixStack matrices,
        BufferBuilder buffer,
        float radius,
        float bandHeight,
        float roll,
        float pitch,
        float spin,
        float alpha,
        int startColor,
        int endColor,
        float uvOffset
    ) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float bottomY = -bandHeight * 0.5F;
        float topY = bandHeight * 0.5F;
        int segments = 64;
        float uvRepeats = 4.0F;

        for (int i = 0; i < segments; i++) {
            float p0 = (float)i / segments;
            float p1 = (float)(i + 1) / segments;
            float a0 = (float)Math.toRadians(p0 * 360.0F + spin);
            float a1 = (float)Math.toRadians(p1 * 360.0F + spin);
            float x0 = MathHelper.sin(a0) * radius;
            float z0 = MathHelper.cos(a0) * radius;
            float x1 = MathHelper.sin(a1) * radius;
            float z1 = MathHelper.cos(a1) * radius;
            float wobble0 = MathHelper.sin(p0 * (float) (Math.PI * 2) + spin * 0.05F) * 0.06F;
            float wobble1 = MathHelper.sin(p1 * (float) (Math.PI * 2) + spin * 0.05F) * 0.06F;
            int c0 = scaleAlpha(lerpColor(startColor, endColor, p0), alpha);
            int c1 = scaleAlpha(lerpColor(startColor, endColor, p1), alpha);
            float u0 = p0 * uvRepeats + uvOffset;
            float u1 = p1 * uvRepeats + uvOffset;
            vertex(buffer, matrix, x0, bottomY + wobble0, z0, u0, 0.0F, c0);
            vertex(buffer, matrix, x1, bottomY + wobble1, z1, u1, 0.0F, c1);
            vertex(buffer, matrix, x1, topY + wobble1, z1, u1, 1.0F, c1);
            vertex(buffer, matrix, x0, topY + wobble0, z0, u0, 1.0F, c0);
        }

        matrices.pop();
    }

    void renderVortex(MatrixStack matrices, LivingEntity target, float alpha, long frameMs, Identifier texture) {
        if (this.vortexSeededFor != target) {
            this.reseedVortex(target);
        }

        float time = (float)(frameMs - MODULE_START_MS) / 1000.0F;
        float speed = 1.0F;
        float sizeMul = 1.0F;
        float half = target.getWidth() * 2.5F * sizeMul;
        float baseY = target.getHeight() * 0.62F - 0.006F;
        int baseColor = this.animatedColor(color1.getValue());
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (int pass = 0; pass < 2; pass++) {
            float passAlpha = pass == 0 ? 1.0F : 0.85F;

            for (int i = 0; i < 3; i++) {
                float pulse = (MathHelper.sin(time * speed * 3.4F + i * (float) (Math.PI * 2.0 / 3.0)) + 1.0F) * 0.5F;
                float layerAlpha = MathHelper.lerp(pulse, 0.35F, 1.0F);
                float yaw = -time * 140.0F * speed * this.vortexRotSpeed[i] + this.vortexYawBase[i];
                float pitch = MathHelper.sin(time * this.vortexPitchSpeed[i] + this.vortexPitchPhase[i]) * 30.0F;
                int color = withAlpha(baseColor, (int)(255.0F * alpha * layerAlpha * passAlpha));
                this.drawVortexLayer(matrices, buffer, half, baseY + 0.006F * i, yaw, pitch, color);
            }
        }

        AuraRender.drawTextured(buffer, AuraRender.POS_TEX_COLOR_QUADS_NODEPTH, texture);
    }

    void drawVortexLayer(MatrixStack matrices, BufferBuilder buffer, float half, float y, float yaw, float pitch, int color) {
        matrices.push();
        matrices.translate(0.0F, y, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F + yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        vertex(buffer, matrix, -half, 0.0F, -half, 0.0F, 0.0F, color);
        vertex(buffer, matrix, -half, 0.0F, half, 0.0F, 1.0F, color);
        vertex(buffer, matrix, half, 0.0F, half, 1.0F, 1.0F, color);
        vertex(buffer, matrix, half, 0.0F, -half, 1.0F, 0.0F, color);
        matrices.pop();
    }

    void renderCrystals(MatrixStack matrices, LivingEntity target, float alpha, long frameMs) {
        float anim = MathHelper.clamp(alpha, 0.0F, 1.0F);
        float moving = (float)(frameMs % 100000L) * 0.2F;
        float width = target.getWidth() * 1.5F;
        float height = target.getHeight();
        int rgb = this.animatedColor(color1.getValue()) | 0xFF000000;
        int baseColor = withAlpha(rgb, Math.round(255.0F * anim));
        int bloomColor = withAlpha(rgb, Math.round(255.0F * anim * 0.38F));
        BufferBuilder crystal = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < 360; i += 20) {
            float val = 1.2F - 0.5F * anim;
            float angle = (float)Math.toRadians(i + moving * 0.3F);
            float sin = (float)(Math.sin(angle) * width * val);
            float cos = (float)(Math.cos(angle) * width * val);
            float y = 0.1F + height * Math.abs((float)Math.sin(i));
            matrices.push();
            matrices.translate(sin, y, cos);
            Vector3f dir = new Vector3f(-sin, height * 0.5F - 1.0F, -cos);
            if (dir.lengthSquared() < 1.0E-8F) {
                dir.set(0.0F, 1.0F, 0.0F);
            } else {
                dir.normalize();
            }

            matrices.multiply(new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), dir));
            this.renderCrystalMesh(matrices, crystal, 0.1F, baseColor);
            matrices.pop();
        }

        AuraRender.drawColor(crystal, AuraRender.POS_COLOR_TRIS_NODEPTH);
        if (crystalGlow.getValue()) {
            Quaternionf cameraRotation = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();
            BufferBuilder bloom = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (int i = 0; i < 360; i += 20) {
                float val = 1.2F - 0.5F * anim;
                float angle = (float)Math.toRadians(i + moving * 0.3F);
                float sin = (float)(Math.sin(angle) * width * val);
                float cos = (float)(Math.cos(angle) * width * val);
                float y = 0.1F + height * Math.abs((float)Math.sin(i));
                this.appendSprite(matrices, bloom, cameraRotation, sin, y, cos, 0.62F, bloomColor);
                this.appendSprite(matrices, bloom, cameraRotation, sin, y, cos, 0.34F, withAlpha(rgb, Math.round(255.0F * anim * 0.62F)));
            }

            AuraRender.drawTextured(bloom, AuraRender.POS_TEX_COLOR_QUADS_ADD_NODEPTH, GLOW_TEXTURE);
        }
    }

    void renderCrystalMesh(MatrixStack matrices, BufferBuilder buffer, float scale, int color) {
        matrices.push();
        matrices.scale(scale, scale, scale);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (int i = 0; i < CRYSTAL_FACES.length; i++) {
            int shaded = applyBrightness(color, CRYSTAL_BRIGHTNESS[i]);

            for (int index : CRYSTAL_FACES[i]) {
                Vector3f v = CRYSTAL_VERTICES[index];
                buffer.vertex(matrix, v.x, v.y, v.z).color(shaded);
            }
        }

        matrices.pop();
    }

    void billboardGradient(MatrixStack matrices, Identifier texture, float y, float scale, float rotation, int c1, int c2, int c3, int c4) {
        matrices.push();
        matrices.translate(0.0F, y, 0.0F);
        matrices.multiply(MinecraftClient.getInstance().gameRenderer.getCamera().getRotation());
        if (rotation != 0.0F) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        }

        matrices.scale(scale, scale, 1.0F);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        vertex(buffer, matrix, -1.0F, -1.0F, 0.0F, 0.0F, 0.0F, c1);
        vertex(buffer, matrix, -1.0F, 1.0F, 0.0F, 0.0F, 1.0F, c2);
        vertex(buffer, matrix, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, c3);
        vertex(buffer, matrix, 1.0F, -1.0F, 0.0F, 1.0F, 0.0F, c4);
        AuraRender.drawTextured(buffer, AuraRender.POS_TEX_COLOR_QUADS_NODEPTH, texture);
        matrices.pop();
    }

    void appendSprite(MatrixStack matrices, BufferBuilder buffer, Quaternionf cameraRotation, float x, float y, float z, float size, int color) {
        if ((color >>> 24 & 0xFF) > 0) {
            matrices.push();
            matrices.translate(x, y, z);
            matrices.multiply(cameraRotation);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float h = size * 0.5F;
            vertex(buffer, matrix, -h, -h, 0.0F, 0.0F, 0.0F, color);
            vertex(buffer, matrix, h, -h, 0.0F, 1.0F, 0.0F, color);
            vertex(buffer, matrix, h, h, 0.0F, 1.0F, 1.0F, color);
            vertex(buffer, matrix, -h, h, 0.0F, 0.0F, 1.0F, color);
            matrices.pop();
        }
    }

    void reseedVortex(LivingEntity target) {
        this.vortexSeededFor = target;

        for (int i = 0; i < 3; i++) {
            this.vortexRotSpeed[i] = 0.75F + RANDOM.nextFloat() * 0.5F;
            this.vortexPitchSpeed[i] = 0.8F + RANDOM.nextFloat() * 0.8F;
            this.vortexPitchPhase[i] = RANDOM.nextFloat() * (float) (Math.PI * 2);
        }

        for (int attempt = 0; attempt < 64; attempt++) {
            for (int i = 0; i < 3; i++) {
                this.vortexYawBase[i] = -30.0F + RANDOM.nextFloat() * 60.0F;
            }

            boolean ok = true;

            for (int a = 0; a < 3 && ok; a++) {
                for (int b = a + 1; b < 3; b++) {
                    if (Math.abs(this.vortexYawBase[a] - this.vortexYawBase[b]) < 22.0F) {
                        ok = false;
                        break;
                    }
                }
            }

            if (ok) {
                return;
            }
        }

        this.vortexYawBase[0] = -30.0F + RANDOM.nextFloat() * 4.0F;
        this.vortexYawBase[1] = -2.0F + RANDOM.nextFloat() * 4.0F;
        this.vortexYawBase[2] = 30.0F - RANDOM.nextFloat() * 4.0F;
    }

    static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float u, float v, int color) {
        buffer.vertex(matrix, x, y, z).texture(u, v).color(color);
    }

    public static boolean usesTwoColors() {
        return !mode.is("Кристаллы") && !mode.is("Вихрь");
    }

    float hitColorProgress(long nowMs) {
        if (this.hitColorStartedMs == Long.MIN_VALUE) {
            return 0.0F;
        } else {
            long elapsed = nowMs - this.hitColorStartedMs;
            if (elapsed < 0L || elapsed >= 700L) {
                return 0.0F;
            } else if (elapsed < 180L) {
                return smoothStep((float)elapsed / 180.0F);
            } else {
                return elapsed < 500L ? 1.0F : smoothStep(1.0F - (float)(elapsed - 500L) / 200.0F);
            }
        }
    }

    int animatedColor(int baseColor) {
        return changeColorOnHit.getValue() && !(this.hurtProgress <= 0.0F) ? lerpColor(baseColor, hitColor.getValue(), this.hurtProgress) : baseColor;
    }

    static float smoothStep(float value) {
        float t = MathHelper.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    static float approach(float current, float target, float delta) {
        return current < target ? Math.min(current + delta, target) : Math.max(current - delta, target);
    }

    static float easeOutCubic(float value) {
        float clamped = MathHelper.clamp(value, 0.0F, 1.0F);
        float inv = 1.0F - clamped;
        return 1.0F - inv * inv * inv;
    }

    static int withAlpha(int color, int alpha) {
        return MathHelper.clamp(alpha, 0, 255) << 24 | color & 16777215;
    }

    static int scaleAlpha(int color, float scale) {
        int alpha = color >>> 24 & 0xFF;
        return withAlpha(color, Math.round(alpha * MathHelper.clamp(scale, 0.0F, 1.0F)));
    }

    static int rgba(int r, int g, int b, int a) {
        return MathHelper.clamp(a, 0, 255) << 24 | MathHelper.clamp(r, 0, 255) << 16 | MathHelper.clamp(g, 0, 255) << 8 | MathHelper.clamp(b, 0, 255);
    }

    static int lerpColor(int start, int end, float progress) {
        float t = MathHelper.clamp(progress, 0.0F, 1.0F);
        int sa = start >>> 24 & 0xFF;
        int sr = start >>> 16 & 0xFF;
        int sg = start >>> 8 & 0xFF;
        int sb = start & 0xFF;
        int ea = end >>> 24 & 0xFF;
        int er = end >>> 16 & 0xFF;
        int eg = end >>> 8 & 0xFF;
        int eb = end & 0xFF;
        return rgba(
            Math.round(MathHelper.lerp(t, sr, er)),
            Math.round(MathHelper.lerp(t, sg, eg)),
            Math.round(MathHelper.lerp(t, sb, eb)),
            Math.round(MathHelper.lerp(t, sa, ea))
        );
    }

    static int applyBrightness(int color, float brightness) {
        int a = color >>> 24 & 0xFF;
        int r = MathHelper.clamp((int)((color >>> 16 & 0xFF) * brightness), 0, 255);
        int g = MathHelper.clamp((int)((color >>> 8 & 0xFF) * brightness), 0, 255);
        int b = MathHelper.clamp((int)((color & 0xFF) * brightness), 0, 255);
        return rgba(r, g, b, a);
    }
}
