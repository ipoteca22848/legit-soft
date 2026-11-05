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
import com.example.auravisuals.util.render.ImpactLensRenderer;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class Trails extends Module {
    static final String MODE_GLASS = "Жидкое стекло";
    static final String MODE_BUBBLES = "Пузыри";
    final ModeSetting mode = new ModeSetting("Режим", "Линия", "Линия", "Жидкое стекло", "Пузыри");
    final NumberSetting length = new NumberSetting("Длина", 500.0, 200.0, 1500.0, 100.0, () -> !this.mode.is("Пузыри"));
    final NumberSetting size = new NumberSetting("Размер", 2.0, 1.0, 3.0, 0.1);
    final BooleanSetting customColor = new BooleanSetting("Свой цвет", false);
    final ColorSetting colorSetting = new ColorSetting("Цвет", new Color(255, 255, 255), () -> this.customColor.getValue());
    final NumberSetting lineAlpha = new NumberSetting("Прозрачность", 1.0, 0.1, 1.0, 0.05, () -> this.mode.is("Линия"));
    final BooleanSetting lineGradient = new BooleanSetting("Градиент", false, () -> this.mode.is("Линия"));
    final ColorSetting lineGradientColor = new ColorSetting("Цвет 2", new Color(50, 50, 255), () -> this.mode.is("Линия") && this.lineGradient.getValue());
    final BooleanSetting hideIn1stPerson = new BooleanSetting("Скрывать от 1-го лица", true);
    final BooleanSetting glow = new BooleanSetting("Свечение", true, () -> !this.mode.is("Жидкое стекло"));
    final NumberSetting glassLifetime = new NumberSetting("Время жизни", 1200.0, 300.0, 4000.0, 50.0, () -> this.mode.is("Жидкое стекло"));
    final NumberSetting glassSize = new NumberSetting("Размер", 1.15, 0.4, 3.0, 0.05, () -> this.mode.is("Жидкое стекло"));
    final NumberSetting glassIntensity = new NumberSetting("Интенсивность", 1.0, 0.1, 2.0, 0.05, () -> this.mode.is("Жидкое стекло"));
    final NumberSetting bubbleAmount = new NumberSetting("Количество", 3.0, 1.0, 10.0, 1.0, () -> this.mode.is("Пузыри"));
    final NumberSetting bubbleLifetime = new NumberSetting("Время жизни", 1400.0, 300.0, 5000.0, 50.0, () -> this.mode.is("Пузыри"));
    final NumberSetting bubbleSize = new NumberSetting("Размер", 0.34, 0.08, 1.2, 0.02, () -> this.mode.is("Пузыри"));
    final NumberSetting bubbleRise = new NumberSetting("Подъём", 0.7, 0.05, 2.5, 0.05, () -> this.mode.is("Пузыри"));
    final NumberSetting bubbleSpread = new NumberSetting("Разброс", 0.38, 0.05, 1.5, 0.05, () -> this.mode.is("Пузыри"));
    static final long BUBBLE_DELAY_MS = 100L;
    static final Identifier GLOW_TEXTURE = Identifier.of("auravisuals", "textures/glow.png");
    static final Identifier TAIL_INNER_GLOW_TEXTURE = Identifier.of("auravisuals", "textures/particles/ghost-glow.png");
    static final Identifier GLASS_TEXTURE = Identifier.of("auravisuals", "textures/jump2.png");
    static final Identifier GLASS_RING_TEXTURE = Identifier.of("auravisuals", "textures/jump_circle_ring.png");
    final Map<Integer, List<Trails$Point>> trailsMap = new HashMap<>();
    final List<Trails$GlassStep> glassSteps = new ArrayList<>();
    final List<Trails$VaporParticle> vaporParticles = new ArrayList<>();
    final List<Trails$Point> renderPointsScratch = new ArrayList<>();
    final List<Trails$Point> densifiedPointsScratch = new ArrayList<>();
    static final long FADE_OUT_MS = 400L;
    static final int MAX_RECORDED_POINTS = 128;
    static final int MAX_DENSIFIED_POINTS = 320;
    Vec3d lastGlassStepPos;
    int glassFootSide = 1;
    long lastVaporSpawnAt;

    public Trails() {
        super("Trails", "Оставляет красивый след за собой", Category.VISUALS);
        this.addSettings(
            this.mode,
            this.length,
            this.customColor,
            this.colorSetting,
            this.lineAlpha,
            this.lineGradient,
            this.lineGradientColor,
            this.glassLifetime,
            this.glassSize,
            this.glassIntensity,
            this.bubbleAmount,
            this.bubbleLifetime,
            this.bubbleSize,
            this.bubbleRise,
            this.bubbleSpread,
            this.glow,
            this.hideIn1stPerson
        );
    }

    int getActiveColor() {
        return this.customColor.getValue() ? this.colorSetting.getValue() : ThemeManager.INSTANCE.getVisualModulesColor();
    }

    @Override
    public void onEnable() {
        this.trailsMap.clear();
        this.glassSteps.clear();
        this.vaporParticles.clear();
        this.lastGlassStepPos = null;
        this.lastVaporSpawnAt = 0L;
    }

    @Override
    public void onDisable() {
        this.trailsMap.clear();
        this.glassSteps.clear();
        this.vaporParticles.clear();
        this.lastGlassStepPos = null;
    }

    @Override
    public void onTick() {
        if (this.mc.world != null && this.mc.player != null) {
            if (this.mode.is("Пузыри")) {
                this.trailsMap.clear();
                this.glassSteps.clear();
                this.lastGlassStepPos = null;
                this.updateVaporParticles();
            } else {
                this.vaporParticles.clear();
                if (this.mode.is("Жидкое стекло")) {
                    this.trailsMap.clear();
                    this.updateGlassSteps();
                } else {
                    this.glassSteps.clear();
                    this.lastGlassStepPos = null;
                    int selfId = this.mc.player.getId();
                    this.trailsMap.keySet().removeIf(id -> id != selfId || this.mc.world.getEntityById(id) == null);
                    List<Trails$Point> points = this.trailsMap.computeIfAbsent(selfId, k -> new ArrayList<>());
                    Vec3d pos = this.mc.player.getEntityPos();
                    if (points.isEmpty() || points.get(points.size() - 1).pos.squaredDistanceTo(pos) > 0.0036) {
                        points.add(new Trails$Point(pos, this.mc.player.getHeight()));
                        int pointLimit = Optimization.trailPointLimit(128);
                        if (points.size() > pointLimit) {
                            points.subList(0, points.size() - pointLimit).clear();
                        }
                    }

                    long maxTime = this.length.getValue().longValue();
                    points.removeIf(p -> System.currentTimeMillis() - p.time > maxTime + 400L);
                }
            }
        }
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getCameraPos();
        float tickDelta = context.tickCounter().getTickProgress(true);
        Tessellator tessellator = Tessellator.getInstance();
        ClientPlayerEntity player = this.mc.player;
        if (player != null) {
            boolean isFirstPerson = this.mc.options.getPerspective().isFirstPerson();
            if (!isFirstPerson || !this.hideIn1stPerson.getValue()) {
                if (this.mode.is("Пузыри")) {
                    this.renderVaporParticles(context, matrices, cameraPos);
                } else if (this.mode.is("Жидкое стекло")) {
                    this.renderGlassSteps(context, matrices, cameraPos);
                } else {
                    List<Trails$Point> recordedPoints = this.trailsMap.get(player.getId());
                    if (recordedPoints != null && !recordedPoints.isEmpty()) {
                        double x = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX());
                        List<Trails$Point> points = this.buildRenderPoints(
                            recordedPoints,
                            new Vec3d(
                                x, MathHelper.lerp(tickDelta, player.lastRenderY, player.getY()), MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ())
                            ),
                            player.getHeight()
                        );
                        if (points.size() >= 2) {
                            if (this.mode.is("Линия")) {
                                BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
                                Matrix4f matrix = matrices.peek().getPositionMatrix();
                                List<Trails$Point> linePoints = this.densifyPoints(points, 0.18F);
                                long now = System.currentTimeMillis();
                                long maxTime = this.length.getValue().longValue();
                                float maxAlpha = this.lineAlpha.getValue().floatValue();
                                boolean useGradient = this.lineGradient.getValue();
                                int gradColor = useGradient ? this.lineGradientColor.getValue() : 0;
                                int gr2 = gradColor >> 16 & 0xFF;
                                int gg2 = gradColor >> 8 & 0xFF;
                                int gb2 = gradColor & 0xFF;

                                for (int i = 0; i < linePoints.size(); i++) {
                                    Trails$Point point = linePoints.get(i);
                                    double px = point.pos.x - cameraPos.x;
                                    double py = point.pos.y - cameraPos.y;
                                    double pz = point.pos.z - cameraPos.z;
                                    float age = (float)(now - point.time);
                                    float progress = 1.0F - MathHelper.clamp(age / (float)maxTime, 0.0F, 1.0F);
                                    int color = this.getActiveColor();
                                    float fade = 1.0F - MathHelper.clamp((age - (float)maxTime) / 400.0F, 0.0F, 1.0F);
                                    float sp = progress * progress * progress * (progress * (progress * 6.0F - 15.0F) + 10.0F);
                                    float alpha = sp * fade * maxAlpha;
                                    int r = color >> 16 & 0xFF;
                                    int g = color >> 8 & 0xFF;
                                    int b = color & 0xFF;
                                    int a = (int)(alpha * 255.0F);
                                    buffer.vertex(matrix, (float)px, (float)py, (float)pz).color(r, g, b, a);
                                    if (useGradient) {
                                        buffer.vertex(matrix, (float)px, (float)py + point.height, (float)pz).color(gr2, gg2, gb2, a);
                                    } else {
                                        buffer.vertex(matrix, (float)px, (float)py + point.height, (float)pz).color(r, g, b, a);
                                    }
                                }

                                AuraRender.drawColor(buffer, AuraRender.POS_COLOR_TRI_STRIP);
                            } else if (this.mode.is("Хвост")) {
                                BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                                Matrix4f matrix = matrices.peek().getPositionMatrix();
                                float baseSize = this.size.getValue().floatValue() * 0.5F;
                                List<Trails$Point> smoothPoints = this.densifyPoints(points, this.getTailSpacing());
                                if (smoothPoints.size() < 2) {
                                    AuraRender.drawTextured(buffer, AuraRender.POS_TEX_COLOR_QUADS, GLOW_TEXTURE);
                                    return;
                                }

                                long now = System.currentTimeMillis();
                                long maxTime = this.length.getValue().longValue();
                                Vec3d look = player.getRotationVec(tickDelta);
                                Vec3d backOffset = new Vec3d(look.x, 0.0, look.z);
                                if (backOffset.lengthSquared() > 1.0E-4) {
                                    backOffset = backOffset.normalize().multiply(-0.35);
                                }

                                for (int i = 0; i < smoothPoints.size(); i++) {
                                    Trails$Point point = smoothPoints.get(i);
                                    double px = point.pos.x - cameraPos.x + backOffset.x;
                                    double py = point.pos.y - cameraPos.y + backOffset.y;
                                    double pz = point.pos.z - cameraPos.z + backOffset.z;
                                    float progress = (float)i / (smoothPoints.size() - 1);
                                    int color = this.getActiveColor();
                                    float age = (float)(now - point.time) / (float)maxTime;
                                    float fade = MathHelper.clamp(1.0F - age, 0.0F, 1.0F);
                                    float tailFade = 1.0F - MathHelper.clamp((float)(now - point.time - maxTime) / 400.0F, 0.0F, 1.0F);
                                    float smoothFade = fade * fade * (3.0F - 2.0F * fade);
                                    float alpha = progress * smoothFade * tailFade;
                                    float scale = baseSize * (this.glow.getValue() ? 0.5F + 0.5F * alpha : 0.55F);
                                    int r = color >> 16 & 0xFF;
                                    int g = color >> 8 & 0xFF;
                                    int b = color & 0xFF;
                                    int a = (int)(alpha * 255.0F);
                                    matrices.push();

                                    try {
                                        matrices.translate(px, py + point.height * 0.35 + 0.15, pz);
                                        matrices.multiply(context.camera().getRotation());
                                        matrices.scale(scale, scale, scale);
                                        Matrix4f m = matrices.peek().getPositionMatrix();
                                        buffer.vertex(m, -0.5F, 0.5F, 0.0F).texture(0.0F, 0.0F).color(r, g, b, a);
                                        buffer.vertex(m, -0.5F, -0.5F, 0.0F).texture(0.0F, 1.0F).color(r, g, b, a);
                                        buffer.vertex(m, 0.5F, -0.5F, 0.0F).texture(1.0F, 1.0F).color(r, g, b, a);
                                        buffer.vertex(m, 0.5F, 0.5F, 0.0F).texture(1.0F, 0.0F).color(r, g, b, a);
                                    } finally {
                                        matrices.pop();
                                    }
                                }

                                AuraRender.drawTextured(buffer, AuraRender.POS_TEX_COLOR_QUADS, GLOW_TEXTURE);
                                if (this.glow.getValue()) {
                                    BufferBuilder innerGlow = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                                    this.appendTailInnerGlow(innerGlow, context, matrices, cameraPos, smoothPoints, baseSize, now, maxTime, backOffset);
                                    AuraRender.drawTextured(innerGlow, AuraRender.POS_TEX_COLOR_QUADS_ADD, TAIL_INNER_GLOW_TEXTURE);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    void appendTailInnerGlow(
        BufferBuilder buffer,
        WorldRenderContext context,
        MatrixStack matrices,
        Vec3d cameraPos,
        List<Trails$Point> points,
        float baseSize,
        long now,
        long maxTime,
        Vec3d backOffset
    ) {
        for (int i = 0; i < points.size(); i++) {
            Trails$Point point = points.get(i);
            float progress = (float)i / (points.size() - 1);
            float age = (float)(now - point.time) / (float)maxTime;
            float fade = MathHelper.clamp(1.0F - age, 0.0F, 1.0F);
            float tailFade = 1.0F - MathHelper.clamp((float)(now - point.time - maxTime) / 400.0F, 0.0F, 1.0F);
            float smoothFade = fade * fade * (3.0F - 2.0F * fade);
            float alpha = progress * smoothFade * tailFade;
            float scale = baseSize * (0.5F + 0.5F * alpha);
            int color = this.getActiveColor();
            int red = color >> 16 & 0xFF;
            int green = color >> 8 & 0xFF;
            int blue = color & 0xFF;
            int outAlpha = MathHelper.clamp((int)(alpha * 235.0F), 0, 255);
            matrices.push();

            try {
                matrices.translate(
                    point.pos.x - cameraPos.x + backOffset.x,
                    point.pos.y - cameraPos.y + backOffset.y + point.height * 0.35 + 0.15,
                    point.pos.z - cameraPos.z + backOffset.z
                );
                matrices.multiply(context.camera().getRotation());
                matrices.scale(scale, scale, scale);
                Matrix4f matrix = matrices.peek().getPositionMatrix();
                float core = 0.31F;
                buffer.vertex(matrix, -core, core, 0.006F).texture(0.0F, 0.0F).color(red, green, blue, outAlpha);
                buffer.vertex(matrix, -core, -core, 0.006F).texture(0.0F, 1.0F).color(red, green, blue, outAlpha);
                buffer.vertex(matrix, core, -core, 0.006F).texture(1.0F, 1.0F).color(red, green, blue, outAlpha);
                buffer.vertex(matrix, core, core, 0.006F).texture(1.0F, 0.0F).color(red, green, blue, outAlpha);
            } finally {
                matrices.pop();
            }
        }
    }

    void updateGlassSteps() {
        long now = System.currentTimeMillis();
        long lifetime = Math.max(1L, this.glassLifetime.getValue().longValue());
        this.glassSteps.removeIf(step -> now - step.createdAt >= lifetime);
        Vec3d playerPos = this.mc.player.getEntityPos();
        Vec3d horizontal = new Vec3d(playerPos.x, 0.0, playerPos.z);
        double spacing = Math.max(0.14, this.glassSize.getValue() * 0.22);
        if (this.mc.player.isOnGround()) {
            if (this.lastGlassStepPos == null || !(this.lastGlassStepPos.squaredDistanceTo(horizontal) < spacing * spacing)) {
                float yaw = this.mc.player.getYaw();
                double yawRad = Math.toRadians(yaw);
                Vec3d right = new Vec3d(Math.cos(yawRad), 0.0, Math.sin(yawRad));
                Vec3d stepPos = playerPos.add(right.multiply(this.glassFootSide * 0.14));
                this.glassFootSide = -this.glassFootSide;
                this.glassSteps.add(new Trails$GlassStep(stepPos, now, -yaw));
                int stepLimit = Optimization.transientEffectLimit(96);
                if (this.glassSteps.size() > stepLimit) {
                    this.glassSteps.subList(0, this.glassSteps.size() - stepLimit).clear();
                }

                this.lastGlassStepPos = horizontal;
            }
        }
    }

    void renderGlassSteps(WorldRenderContext context, MatrixStack matrices, Vec3d cameraPos) {
        if (!this.glassSteps.isEmpty()) {
            long now = System.currentTimeMillis();
            float lifetime = Math.max(1.0F, this.glassLifetime.getFloatValue());
            int color = this.getActiveColor();
            float intensity = this.glassIntensity.getFloatValue();
            Matrix4f modelView = matrices.peek().getPositionMatrix();

            for (Trails$GlassStep step : this.glassSteps) {
                float progress = MathHelper.clamp((float)(now - step.createdAt) / lifetime, 0.0F, 1.0F);
                float fade = 1.0F - progress;
                fade *= fade * (3.0F - 2.0F * fade);
                float scale = this.glassSize.getFloatValue() * (0.82F + progress * 0.28F);
                ImpactLensRenderer.renderHorizontal(
                    modelView,
                    context.camera(),
                    step.pos.add(0.0, 0.035, 0.0),
                    scale * 0.5F,
                    progress,
                    intensity,
                    fade,
                    step.rotation / 360.0F,
                    color & 16777215
                );
            }
        }
    }

    void updateVaporParticles() {
        long now = System.currentTimeMillis();
        long lifeMs = Math.max(1L, this.bubbleLifetime.getValue().longValue());
        this.vaporParticles.removeIf(particle -> now - particle.createdAt >= lifeMs);
        Vec3d velocity = this.mc.player.getVelocity();
        boolean moving = velocity.x * velocity.x + velocity.z * velocity.z > 9.0E-4;
        if (moving) {
            if (now - this.lastVaporSpawnAt >= 100L) {
                this.lastVaporSpawnAt = now;
                ThreadLocalRandom random = ThreadLocalRandom.current();
                double spread = this.bubbleSpread.getValue();

                for (int i = 0; i < this.bubbleAmount.getIntValue(); i++) {
                    double angle = random.nextDouble(Math.PI * 2);
                    double distance = Math.sqrt(random.nextDouble()) * spread;
                    Vec3d origin = this.mc.player.getEntityPos().add(Math.cos(angle) * distance, 0.04 + random.nextDouble(0.08), Math.sin(angle) * distance);
                    double drift = spread * 0.18;
                    Vec3d motion = new Vec3d(
                        random.nextDouble(-drift, drift), this.bubbleRise.getValue() * random.nextDouble(0.72, 1.18), random.nextDouble(-drift, drift)
                    );
                    this.vaporParticles.add(new Trails$VaporParticle(origin, motion, now, random.nextFloat() * 360.0F, random.nextFloat() * 0.45F + 0.78F));
                }

                int particleLimit = Optimization.transientEffectLimit(256);
                if (this.vaporParticles.size() > particleLimit) {
                    this.vaporParticles.subList(0, this.vaporParticles.size() - particleLimit).clear();
                }
            }
        }
    }

    void renderVaporParticles(WorldRenderContext context, MatrixStack matrices, Vec3d cameraPos) {
        if (!this.vaporParticles.isEmpty()) {
            long now = System.currentTimeMillis();
            float lifeMs = Math.max(1.0F, this.bubbleLifetime.getFloatValue());
            int activeColor = this.getActiveColor();
            int red = activeColor >> 16 & 0xFF;
            int green = activeColor >> 8 & 0xFF;
            int blue = activeColor & 0xFF;
            BufferBuilder glass = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (Trails$VaporParticle particle : this.vaporParticles) {
                this.appendVaporParticle(glass, context, matrices, cameraPos, particle, now, lifeMs, red, green, blue, 1.0F, false);
            }

            AuraRender.drawTextured(glass, AuraRender.POS_TEX_COLOR_QUADS, GLASS_TEXTURE);
            if (this.glow.getValue()) {
                BufferBuilder halo = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                for (Trails$VaporParticle particle : this.vaporParticles) {
                    this.appendVaporParticle(halo, context, matrices, cameraPos, particle, now, lifeMs, red, green, blue, 1.28F, true);
                }

                AuraRender.drawTextured(halo, AuraRender.POS_TEX_COLOR_QUADS_ADD, GLASS_RING_TEXTURE);
            }
        }
    }

    void appendVaporParticle(
        BufferBuilder buffer,
        WorldRenderContext context,
        MatrixStack matrices,
        Vec3d cameraPos,
        Trails$VaporParticle particle,
        long now,
        float lifeMs,
        int red,
        int green,
        int blue,
        float sizeMultiplier,
        boolean halo
    ) {
        float progress = MathHelper.clamp((float)(now - particle.createdAt) / lifeMs, 0.0F, 1.0F);
        float appear = smoothVapor(Math.min(1.0F, progress / 0.14F));
        float disappear = smoothVapor(Math.min(1.0F, (1.0F - progress) / 0.58F));
        float alpha = appear * disappear * (halo ? 0.38F : 0.82F);
        float seconds = (float)(now - particle.createdAt) / 1000.0F;
        Vec3d worldPos = particle.origin
            .add(particle.motion.multiply(seconds))
            .add(Math.sin(seconds * 3.1F + particle.rotation) * 0.025, 0.0, Math.cos(seconds * 2.7F + particle.rotation) * 0.025);
        float particleSize = this.bubbleSize.getFloatValue() * particle.sizeSeed * sizeMultiplier * (0.72F + progress * 0.55F);
        int outAlpha = (int)(MathHelper.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        matrices.push();

        try {
            matrices.translate(worldPos.x - cameraPos.x, worldPos.y - cameraPos.y, worldPos.z - cameraPos.z);
            matrices.multiply(context.camera().getRotation());
            matrices.scale(particleSize, particleSize, particleSize);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            buffer.vertex(matrix, -0.5F, 0.5F, 0.0F).texture(0.0F, 0.0F).color(red, green, blue, outAlpha);
            buffer.vertex(matrix, -0.5F, -0.5F, 0.0F).texture(0.0F, 1.0F).color(red, green, blue, outAlpha);
            buffer.vertex(matrix, 0.5F, -0.5F, 0.0F).texture(1.0F, 1.0F).color(red, green, blue, outAlpha);
            buffer.vertex(matrix, 0.5F, 0.5F, 0.0F).texture(1.0F, 0.0F).color(red, green, blue, outAlpha);
        } finally {
            matrices.pop();
        }
    }

    static float smoothVapor(float value) {
        float t = MathHelper.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    List<Trails$Point> buildRenderPoints(List<Trails$Point> recordedPoints, Vec3d currentPos, float height) {
        this.renderPointsScratch.clear();
        this.renderPointsScratch.addAll(recordedPoints);
        if (this.renderPointsScratch.isEmpty()) {
            return this.renderPointsScratch;
        }

        this.renderPointsScratch.set(this.renderPointsScratch.size() - 1, new Trails$Point(currentPos, height, System.currentTimeMillis()));
        return this.renderPointsScratch;
    }

    List<Trails$Point> densifyPoints(List<Trails$Point> points, float spacing) {
        if (points.size() < 2) {
            return points;
        }

        this.densifiedPointsScratch.clear();
        int pointLimit = Optimization.densifiedTrailPointLimit(320);
        Trails$Point prev = points.get(0);
        this.densifiedPointsScratch.add(prev);

        for (int i = 1; i < points.size(); i++) {
            Trails$Point next = points.get(i);
            double dist = prev.pos.distanceTo(next.pos);
            int steps = Math.min(8, (int)(dist / spacing));

            for (int s = 1; s <= steps; s++) {
                double t = (double)s / (steps + 1);
                double ix = MathHelper.lerp(t, prev.pos.x, next.pos.x);
                double iy = MathHelper.lerp(t, prev.pos.y, next.pos.y);
                double iz = MathHelper.lerp(t, prev.pos.z, next.pos.z);
                float ih = (float)MathHelper.lerp(t, prev.height, next.height);
                long it = (long)(prev.time + (next.time - prev.time) * t);
                this.densifiedPointsScratch.add(new Trails$Point(new Vec3d(ix, iy, iz), ih, it));
                if (this.densifiedPointsScratch.size() >= pointLimit) {
                    return this.densifiedPointsScratch;
                }
            }

            this.densifiedPointsScratch.add(next);
            prev = next;
            if (this.densifiedPointsScratch.size() >= pointLimit) {
                return this.densifiedPointsScratch;
            }
        }

        return this.densifiedPointsScratch;
    }

    float getTailSpacing() {
        return MathHelper.clamp(this.size.getValue().floatValue() * 0.12F, 0.06F, 0.18F);
    }
}
