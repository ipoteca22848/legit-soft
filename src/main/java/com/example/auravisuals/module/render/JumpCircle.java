package com.example.auravisuals.module.render;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.cosmetics.CosmeticsNetworkClient;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.theme.ThemeManager;
import com.example.auravisuals.util.render.JumpWaveRenderer;
import com.example.auravisuals.util.render.RenderUtil;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class JumpCircle extends Module {
    static final String MODE_PENTAGON = "Пятиугольник";
    static final String MODE_GLASS = "Жидкое стекло";
    public static final ModeSetting mode = new ModeSetting(
        "Режим", "JumpCircle$Circle", "JumpCircle$Circle", "New JumpCircle$Circle", "Пятиугольник", "Жидкое стекло"
    );
    public static final NumberSetting circleSize = new NumberSetting("Размер круга", 1.0, 0.5, 2.0, 0.1, JumpCircle::isClassicCircleMode);
    public static final BooleanSetting useCustomColor = new BooleanSetting("Свой цвет", false, () -> !mode.is("Пятиугольник"));
    public static final ColorSetting color = new JumpCircle$1("Цвет", new Color(255, 255, 255), () -> mode.is("Пятиугольник") || useCustomColor.getValue());
    public static final BooleanSetting gradient = new BooleanSetting("Градиент", false, () -> false);
    public static final ColorSetting gradientColor = new ColorSetting("Цвет 2", new Color(255, 255, 255), () -> mode.is("Пятиугольник"));
    public static final BooleanSetting glow = new BooleanSetting("Свечение", true, () -> false);
    public static final NumberSetting newCircleRadius = new NumberSetting("Радиус", 2.0, 0.5, 2.0, 0.1, () -> mode.is("New JumpCircle$Circle"));
    public static final NumberSetting glassStrength = new NumberSetting("Сила волны", 0.8, 0.2, 2.0, 0.05, () -> mode.is("Жидкое стекло"));
    public static final NumberSetting glassRadius = new NumberSetting("Радиус волны", 1.0, 0.5, 3.0, 0.1, JumpCircle::isWaveMode);
    public static final NumberSetting glassTime = new NumberSetting("Время волны", 1250.0, 500.0, 2500.0, 10.0, JumpCircle::isWaveMode);
    static final Identifier TEXTURE = Identifier.of("auravisuals", "textures/jump2.png");
    static final Identifier RING_TEXTURE = Identifier.of("auravisuals", "textures/jump_circle_ring.png");
    static final Identifier GLOW_TEXTURE = Identifier.of("auravisuals", "textures/glow.png");
    static final float DEFAULT_LIFE_TIME = 20.0F;
    static final float DEFAULT_SCALE = 1.0F;
    final List<JumpCircle$Circle> circles = new ArrayList<>();
    final List<JumpCircle$TestWave> testWaves = new ArrayList<>();
    final List<JumpCircle$GlassWave> glassWaves = new ArrayList<>();

    public JumpCircle() {
        super("JumpCircle", "Круг при прыжке", Category.VISUALS);
        this.addSettings(mode, circleSize, useCustomColor, color, gradient, gradientColor, glow, newCircleRadius, glassStrength, glassRadius, glassTime);
    }

    public static boolean isWaveMode() {
        return mode.is("JumpCircle$Circle") || mode.is("Жидкое стекло");
    }

    public void onJump() {
        if (this.mc.player != null) {
            Vec3d position = this.mc.player.getEntityPos();
            JumpCircle$StyleSnapshot style = JumpCircle$StyleSnapshot.capture();
            this.spawn(position, style);
            CosmeticsNetworkClient.INSTANCE.sendJumpCircle(position, style.toJson());
        }
    }

    public void onRemoteJump(String playerName, Vec3d position, JsonObject styleJson) {
        if (this.isEnabled() && this.mc.player != null && position != null && styleJson != null) {
            if (!(position.squaredDistanceTo(this.mc.player.getEntityPos()) > 16384.0)) {
                this.spawn(position, JumpCircle$StyleSnapshot.fromJson(styleJson));
            }
        }
    }

    void spawn(Vec3d position, JumpCircle$StyleSnapshot style) {
        if (style.isMode("JumpCircle$Circle") || style.isMode("Жидкое стекло")) {
            while (this.glassWaves.size() >= 8) {
                this.glassWaves.remove(0);
            }

            float strength = style.isMode("JumpCircle$Circle") ? 0.0F : style.glassStrength;
            this.glassWaves.add(new JumpCircle$GlassWave(position, style.glassTime, style.glassRadius, strength, style.primaryColor));
        } else if (style.isMode("New JumpCircle$Circle")) {
            long now = System.currentTimeMillis();

            while (this.testWaves.size() >= 8) {
                this.testWaves.remove(0);
            }

            this.testWaves.add(new JumpCircle$TestWave(position, now, 1850.0F, 6.0F, style.newCircleRadius, style.primaryColor, style.vaporGlow));
        } else {
            while (this.circles.size() >= 8) {
                this.circles.remove(0);
            }

            this.circles.add(new JumpCircle$Circle(position, System.currentTimeMillis() - 120L, style));
        }
    }

    @Override
    public void onDisable() {
        this.circles.clear();
        this.testWaves.clear();
        this.glassWaves.clear();
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        this.renderGlassWaves(context.matrixStack(), context.camera().getCameraPos());
        this.renderTestMode(context);
        this.renderClassicCircles(context);
    }

    void renderClassicCircles(WorldRenderContext context) {
        if (!this.circles.isEmpty()) {
            MatrixStack matrices = context.matrixStack();
            Tessellator tessellator = Tessellator.getInstance();
            Vec3d cam = context.camera().getCameraPos();
            this.cleanupCircles();
            if (!this.circles.isEmpty()) {
                BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

                for (JumpCircle$Circle c : this.circles) {
                    this.renderDefaultCircle(c, buffer, matrices, cam);
                }

                try {
                    AuraRender.drawTextured(buffer, AuraRender.POS_TEX_COLOR_TRIS_NODEPTH, RING_TEXTURE);
                } catch (Exception var11) {
                }

                boolean hasGlow = this.circles.stream().anyMatch(circle -> circle.glow);
                if (hasGlow) {
                    BufferBuilder glowBuf = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                    for (JumpCircle$Circle c : this.circles) {
                        this.renderCircleGlow(c, glowBuf, matrices, cam);
                    }

                    try {
                        AuraRender.drawTextured(glowBuf, AuraRender.POS_TEX_COLOR_QUADS_ADD_NODEPTH, GLOW_TEXTURE);
                    } catch (Exception var10) {
                    }
                }
            }
        }
    }

    void renderGlassWaves(MatrixStack matrices, Vec3d camera) {
        if (!this.glassWaves.isEmpty()) {
            long now = System.currentTimeMillis();
            Matrix4f modelView = matrices.peek().getPositionMatrix();
            Iterator<JumpCircle$GlassWave> iterator = this.glassWaves.iterator();

            while (iterator.hasNext()) {
                JumpCircle$GlassWave wave = iterator.next();
                float raw = MathHelper.clamp((float)(now - wave.createdAt) / wave.lifeMs, 0.0F, 1.0F);
                if (raw >= 1.0F) {
                    iterator.remove();
                } else {
                    float inverse = 1.0F - raw;
                    float ring = 1.0F - inverse * inverse * inverse;
                    float fade = glassFade(raw);

                    try {
                        if (wave.ambience) {
                            JumpWaveRenderer.renderAmbience(
                                modelView, camera, wave.position, wave.maxRadius, ring, fade, wave.strength, wave.effect, wave.color
                            );
                        } else {
                            JumpWaveRenderer.render(modelView, camera, wave.position, wave.maxRadius, ring, wave.strength, fade, wave.color);
                        }
                    } catch (Exception var13) {
                    }
                }
            }

            this.renderShaderCircleCrests(matrices, camera, now);
        }
    }

    void renderShaderCircleCrests(MatrixStack matrices, Vec3d camera, long now) {
        if (!this.glassWaves.stream().noneMatch(wave -> wave.ambience)) {
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            boolean rendered = false;
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            for (JumpCircle$GlassWave wave2 : this.glassWaves) {
                float raw;
                if (wave2.ambience && !((raw = MathHelper.clamp((float)(now - wave2.createdAt) / wave2.lifeMs, 0.0F, 1.0F)) >= 1.0F)) {
                    float inverse = 1.0F - raw;
                    float radius = wave2.maxRadius * (1.0F - inverse * inverse * inverse);
                    float fade = glassFade(raw);
                    if (!(radius <= 0.01F) && !(fade <= 0.003F)) {
                        Color crest = themeWhite(wave2.color);
                        float width = Math.max(0.022F, radius * 0.035F);
                        this.addCrestBand(buffer, matrix, wave2.position, camera, radius - width * 2.2F, radius + width * 2.2F, crest, fade * 0.18F);
                        this.addCrestBand(buffer, matrix, wave2.position, camera, radius - width, radius + width, crest, fade * 0.94F);
                        rendered = true;
                    }
                }
            }

            if (rendered) {
                try {
                    AuraRender.drawColor(buffer, AuraRender.POS_COLOR_TRIS_ADD);
                } catch (Exception var16) {
                }
            } else {
                RenderUtil.closeBuffer(buffer);
            }
        }
    }

    void addCrestBand(BufferBuilder buffer, Matrix4f matrix, Vec3d center, Vec3d camera, float innerRadius, float outerRadius, Color color, float alpha) {
        if (!(innerRadius < 0.0F) && !(outerRadius <= innerRadius) && !(alpha <= 0.002F)) {
            int segments = 64;
            float y = (float)(center.y - camera.y + 0.047);
            float cx = (float)(center.x - camera.x);
            float cz = (float)(center.z - camera.z);
            int a = this.alphaByte(alpha);

            for (int i = 0; i < 64; i++) {
                float angle0 = (float)((Math.PI * 2) * i / 64.0);
                float angle1 = (float)((Math.PI * 2) * (i + 1) / 64.0);
                float cos0 = MathHelper.cos(angle0);
                float sin0 = MathHelper.sin(angle0);
                float cos1 = MathHelper.cos(angle1);
                float sin1 = MathHelper.sin(angle1);
                this.addCrestVertex(buffer, matrix, cx + cos0 * innerRadius, y, cz + sin0 * innerRadius, color, a);
                this.addCrestVertex(buffer, matrix, cx + cos0 * outerRadius, y, cz + sin0 * outerRadius, color, a);
                this.addCrestVertex(buffer, matrix, cx + cos1 * outerRadius, y, cz + sin1 * outerRadius, color, a);
                this.addCrestVertex(buffer, matrix, cx + cos0 * innerRadius, y, cz + sin0 * innerRadius, color, a);
                this.addCrestVertex(buffer, matrix, cx + cos1 * outerRadius, y, cz + sin1 * outerRadius, color, a);
                this.addCrestVertex(buffer, matrix, cx + cos1 * innerRadius, y, cz + sin1 * innerRadius, color, a);
            }
        }
    }

    void addCrestVertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, Color color, int alpha) {
        buffer.vertex(matrix, x, y, z).color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    static Color themeWhite(int themeColor) {
        Color theme = new Color(themeColor, true);
        return new Color(
            Math.round(89.25F + theme.getRed() * 0.65F), Math.round(89.25F + theme.getGreen() * 0.65F), Math.round(89.25F + theme.getBlue() * 0.65F)
        );
    }

    static float glassFade(float raw) {
        float appear = 1.0F;
        float disappear = smoothStepStatic((1.0F - raw) / 0.82F);
        return MathHelper.clamp(appear * disappear, 0.0F, 1.0F);
    }

    void renderDefaultCircle(JumpCircle$Circle c, BufferBuilder buffer, MatrixStack matrices, Vec3d cam) {
        if (!(c.pos.squaredDistanceTo(cam) > 16384.0)) {
            long age = System.currentTimeMillis() - c.creationTime;
            float sizeFactor = c.size;
            double maxLife = this.getDefaultMaxLife() * 50.0F * sizeFactor;
            if (!(age > maxLife)) {
                float progress = (float)age / (float)maxLife;
                float scaleProgress = this.getDefaultScaleProgress(progress);
                float currentScale = 1.0F * scaleProgress * sizeFactor;
                float alpha = progress > 0.5F ? 1.0F - (progress - 0.5F) * 2.0F : 1.0F;
                if (alpha < 0.0F) {
                    alpha = 0.0F;
                }

                Color baseColor = new Color(c.primaryColor, true);
                Color altColor = new Color(c.secondaryColor, true);
                int a = (int)(alpha * 255.0F);
                matrices.push();

                try {
                    matrices.translate(c.pos.x - cam.x, c.pos.y - cam.y + 0.05, c.pos.z - cam.z);
                    matrices.scale(currentScale, 1.0F, currentScale);
                    Matrix4f matrix = matrices.peek().getPositionMatrix();
                    float s = 1.0F;
                    if (c.gradient) {
                        float angleOffset = (float)age * 0.01F;
                        float step = (float) (Math.PI / 8);
                        Color centerColor = this.blendColors(baseColor, altColor, (MathHelper.sin(angleOffset) + 1.0F) * 0.5F);

                        for (int i = 0; i < 16; i++) {
                            float angle1 = step * i;
                            float angle2 = step * (i + 1);
                            float x1 = MathHelper.cos(angle1) * s;
                            float z1 = MathHelper.sin(angle1) * s;
                            float x2 = MathHelper.cos(angle2) * s;
                            float z2 = MathHelper.sin(angle2) * s;
                            float u1 = 0.5F + MathHelper.cos(angle1) * 0.5F;
                            float v1 = 0.5F + MathHelper.sin(angle1) * 0.5F;
                            float u2 = 0.5F + MathHelper.cos(angle2) * 0.5F;
                            float v2 = 0.5F + MathHelper.sin(angle2) * 0.5F;
                            Color c1 = this.blendColors(baseColor, altColor, (MathHelper.sin(angle1 + angleOffset) + 1.0F) * 0.5F);
                            Color c2 = this.blendColors(baseColor, altColor, (MathHelper.sin(angle2 + angleOffset) + 1.0F) * 0.5F);
                            this.addTriangle(buffer, matrix, 0.0F, 0.0F, 0.5F, 0.5F, centerColor, a, x1, z1, u1, v1, c1, a, x2, z2, u2, v2, c2, a);
                        }
                    } else {
                        this.addTriangle(buffer, matrix, -s, -s, 0.0F, 0.0F, baseColor, a, -s, s, 0.0F, 1.0F, baseColor, a, s, s, 1.0F, 1.0F, baseColor, a);
                        this.addTriangle(buffer, matrix, -s, -s, 0.0F, 0.0F, baseColor, a, s, s, 1.0F, 1.0F, baseColor, a, s, -s, 1.0F, 0.0F, baseColor, a);
                    }
                } finally {
                    matrices.pop();
                }
            }
        }
    }

    boolean renderCircleGlow(JumpCircle$Circle c, BufferBuilder buffer, MatrixStack matrices, Vec3d cam) {
        if (!c.glow) {
            return false;
        }

        if (c.pos.squaredDistanceTo(cam) > 16384.0) {
            return false;
        }

        long age = System.currentTimeMillis() - c.creationTime;
        float sizeFactor = c.size;
        double maxLife = this.getDefaultMaxLife() * 50.0F * sizeFactor;
        if (age > maxLife) {
            return false;
        }

        float progress = (float)age / (float)maxLife;
        float scaleProgress = this.getDefaultScaleProgress(progress);
        float currentScale = 1.0F * scaleProgress * sizeFactor;
        float alpha = progress > 0.5F ? 1.0F - (progress - 0.5F) * 2.0F : 1.0F;
        if (alpha < 0.0F) {
            return false;
        }

        Color baseColor = new Color(c.primaryColor, true);
        float r = baseColor.getRed() / 255.0F;
        float g = baseColor.getGreen() / 255.0F;
        float b = baseColor.getBlue() / 255.0F;
        float glowAlpha = alpha * 0.25F;
        float glowScale = currentScale * 1.4F;
        matrices.push();

        try {
            matrices.translate(c.pos.x - cam.x, c.pos.y - cam.y + 0.06, c.pos.z - cam.z);
            matrices.scale(glowScale, 1.0F, glowScale);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            buffer.vertex(matrix, -1.0F, 0.0F, -1.0F).texture(0.0F, 0.0F).color(r, g, b, glowAlpha);
            buffer.vertex(matrix, -1.0F, 0.0F, 1.0F).texture(0.0F, 1.0F).color(r, g, b, glowAlpha);
            buffer.vertex(matrix, 1.0F, 0.0F, 1.0F).texture(1.0F, 1.0F).color(r, g, b, glowAlpha);
            buffer.vertex(matrix, 1.0F, 0.0F, -1.0F).texture(1.0F, 0.0F).color(r, g, b, glowAlpha);
        } finally {
            matrices.pop();
        }

        return true;
    }

    void renderTestMode(WorldRenderContext context) {
        long now = System.currentTimeMillis();
        this.cleanupTestEffects(now);
        if (!this.testWaves.isEmpty()) {
            MatrixStack matrices = context.matrixStack();
            Vec3d cam = context.camera().getCameraPos();
            float time = (float)(now % 1000000L) / 1000.0F;
            BufferBuilder surface = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            boolean hasSurface = false;

            for (JumpCircle$TestWave wave2 : this.testWaves) {
                hasSurface |= this.renderTestWave(wave2, now, time, surface, matrices, cam);
            }

            if (hasSurface) {
                try {
                    AuraRender.drawColor(surface, AuraRender.POS_COLOR_TRIS_NODEPTH);
                } catch (Exception var14) {
                }
            } else {
                RenderUtil.closeBuffer(surface);
            }

            if (this.testWaves.stream().anyMatch(wave -> wave.vaporGlow)) {
                BufferBuilder vapor = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
                boolean hasVapor = false;

                for (JumpCircle$TestWave wave3 : this.testWaves) {
                    if (wave3.vaporGlow) {
                        hasVapor |= this.renderTestVapor(wave3, now, time, vapor, matrices, cam);
                    }
                }

                if (hasVapor) {
                    try {
                        AuraRender.drawColor(vapor, AuraRender.POS_COLOR_TRIS_ADD_NODEPTH);
                    } catch (Exception var13) {
                    }
                } else {
                    RenderUtil.closeBuffer(vapor);
                }
            }
        }
    }

    boolean renderTestWave(JumpCircle$TestWave wave, long now, float time, BufferBuilder buffer, MatrixStack matrices, Vec3d cam) {
        if (wave.position.squaredDistanceTo(cam) > 16384.0) {
            return false;
        }

        float raw = wave.rawProgress(now);
        if (raw >= 1.0F) {
            return false;
        }

        float progress = easeOutCubic(raw);
        float fade = fadeEnvelope(raw);
        float radius = Math.max(0.001F, wave.maxRadius * progress);
        float strength = MathHelper.clamp(wave.strength, 0.0F, 6.0F);
        if (!(fade <= 0.003F) && !(strength <= 0.001F)) {
            matrices.push();

            try {
                matrices.translate(wave.position.x - cam.x, wave.position.y - cam.y + 0.035, wave.position.z - cam.z);
                Matrix4f matrix = matrices.peek().getPositionMatrix();
                Color base = new Color(wave.color, true);
                float bandWidth = Math.max(0.08F, wave.maxRadius * 0.11F);
                this.addTestRingBand(
                    buffer, matrix, Math.max(0.0F, radius - bandWidth), radius, 0.0F, fade * Math.min(0.94F, 0.42F + strength * 0.24F), time, true, base
                );
                this.addTestRingBand(buffer, matrix, radius, radius + bandWidth, fade * Math.min(0.94F, 0.42F + strength * 0.24F), 0.0F, time, true, base);
            } finally {
                matrices.pop();
            }

            return true;
        } else {
            return false;
        }
    }

    boolean renderTestVapor(JumpCircle$TestWave wave, long now, float time, BufferBuilder buffer, MatrixStack matrices, Vec3d cam) {
        float raw = wave.rawProgress(now);
        if (raw >= 1.0F) {
            return false;
        }

        float progress = easeOutCubic(raw);
        float fade = fadeEnvelope(raw);
        float radius = Math.max(0.001F, wave.maxRadius * progress);
        if (!(fade <= 0.003F) && !(radius <= 0.002F)) {
            int segments = 48;
            int heightSlices = 7;
            float height = 1.0F;
            float tailLength = this.lerp(0.35F, 1.0F, progress);
            matrices.push();

            try {
                matrices.translate(wave.position.x - cam.x, wave.position.y - cam.y + 0.02, wave.position.z - cam.z);
                Matrix4f matrix = matrices.peek().getPositionMatrix();

                for (int i = 0; i < 48; i++) {
                    float a0 = (float)((Math.PI * 2) * i / 48.0);
                    float a1 = (float)((Math.PI * 2) * (i + 1) / 48.0);

                    for (int j = 0; j < 7; j++) {
                        float v0 = j / 7.0F;
                        float v1 = (j + 1) / 7.0F;
                        float alpha0 = this.vaporAlpha(a0, v0, time, tailLength, fade);
                        float alpha1 = this.vaporAlpha(a1, v0, time, tailLength, fade);
                        float alpha2 = this.vaporAlpha(a1, v1, time, tailLength, fade);
                        float alpha3 = this.vaporAlpha(a0, v1, time, tailLength, fade);
                        if (!(Math.max(Math.max(alpha0, alpha1), Math.max(alpha2, alpha3)) <= 0.002F)) {
                            float x0 = MathHelper.cos(a0) * radius;
                            float z0 = MathHelper.sin(a0) * radius;
                            float x1 = MathHelper.cos(a1) * radius;
                            float z1 = MathHelper.sin(a1) * radius;
                            Color c0;
                            Color c1 = c0 = new Color(wave.color, true);
                            this.addTestVertex(buffer, matrix, x0, v0 * 1.0F, z0, c0, this.alphaByte(alpha0));
                            this.addTestVertex(buffer, matrix, x1, v0 * 1.0F, z1, c1, this.alphaByte(alpha1));
                            this.addTestVertex(buffer, matrix, x1, v1 * 1.0F, z1, c1, this.alphaByte(alpha2));
                            this.addTestVertex(buffer, matrix, x0, v0 * 1.0F, z0, c0, this.alphaByte(alpha0));
                            this.addTestVertex(buffer, matrix, x1, v1 * 1.0F, z1, c1, this.alphaByte(alpha2));
                            this.addTestVertex(buffer, matrix, x0, v1 * 1.0F, z0, c0, this.alphaByte(alpha3));
                        }
                    }
                }
            } finally {
                matrices.pop();
            }

            return true;
        } else {
            return false;
        }
    }

    void addTestRingBand(
        BufferBuilder buffer, Matrix4f matrix, float innerRadius, float outerRadius, float innerAlpha, float outerAlpha, float time, boolean crest, Color base
    ) {
        if (!(outerRadius <= innerRadius) && !(outerRadius <= 0.0F)) {
            int segments = 64;

            for (int i = 0; i < 64; i++) {
                float a0 = (float)((Math.PI * 2) * i / 64.0);
                float a1 = (float)((Math.PI * 2) * (i + 1) / 64.0);
                Color c0 = base;
                Color c1 = base;
                if (crest) {
                    c0 = this.blendColors(c0, Color.WHITE, 0.42F);
                    c1 = this.blendColors(c1, Color.WHITE, 0.42F);
                }

                float cos0 = MathHelper.cos(a0);
                float sin0 = MathHelper.sin(a0);
                float cos1 = MathHelper.cos(a1);
                float sin1 = MathHelper.sin(a1);
                int ia = this.alphaByte(innerAlpha * this.colorAlpha(c0));
                int oa = this.alphaByte(outerAlpha * this.colorAlpha(c0));
                int ia1 = this.alphaByte(innerAlpha * this.colorAlpha(c1));
                int oa1 = this.alphaByte(outerAlpha * this.colorAlpha(c1));
                this.addTestVertex(buffer, matrix, cos0 * innerRadius, 0.0F, sin0 * innerRadius, c0, ia);
                this.addTestVertex(buffer, matrix, cos0 * outerRadius, 0.0F, sin0 * outerRadius, c0, oa);
                this.addTestVertex(buffer, matrix, cos1 * outerRadius, 0.0F, sin1 * outerRadius, c1, oa1);
                this.addTestVertex(buffer, matrix, cos0 * innerRadius, 0.0F, sin0 * innerRadius, c0, ia);
                this.addTestVertex(buffer, matrix, cos1 * outerRadius, 0.0F, sin1 * outerRadius, c1, oa1);
                this.addTestVertex(buffer, matrix, cos1 * innerRadius, 0.0F, sin1 * innerRadius, c1, ia1);
            }
        }
    }

    void addTestDisc(BufferBuilder buffer, Matrix4f matrix, float radius, Color base, int alpha, float time) {
        if (!(radius <= 0.0F) && alpha > 0) {
            int segments = 64;
            int centerAlpha = Math.max(0, Math.min(255, Math.round(alpha * 0.36F)));

            for (int i = 0; i < 64; i++) {
                float a0 = (float)((Math.PI * 2) * i / 64.0);
                float a1 = (float)((Math.PI * 2) * (i + 1) / 64.0);
                this.addTestVertex(buffer, matrix, 0.0F, -0.001F, 0.0F, base, centerAlpha);
                this.addTestVertex(buffer, matrix, MathHelper.cos(a0) * radius, -0.001F, MathHelper.sin(a0) * radius, base, alpha);
                this.addTestVertex(buffer, matrix, MathHelper.cos(a1) * radius, -0.001F, MathHelper.sin(a1) * radius, base, alpha);
            }
        }
    }

    void addTestVertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, Color value, int alpha) {
        buffer.vertex(matrix, x, y, z).color(value.getRed(), value.getGreen(), value.getBlue(), alpha);
    }

    float vaporAlpha(float angle, float v, float time, float tailLength, float fade) {
        float bottomFade = this.smoothStep(v / 0.18F);
        float trail = 1.0F - this.smoothStep(v / Math.max(0.001F, tailLength));
        float steam = 0.62F + 0.38F * this.ringNoise(angle, v, time);
        return MathHelper.clamp(bottomFade * trail * steam * fade * 0.82F, 0.0F, 1.0F);
    }

    float ringNoise(float angle, float v, float time) {
        double x = Math.cos(angle) * 3.0 + v * 4.0;
        double z = Math.sin(angle) * 3.0 - time * 2.0;
        double n1 = Math.sin(x * 2.31 + z * 1.73);
        double n2 = Math.sin(x * 4.17 - z * 2.43 + 1.7);
        return MathHelper.clamp((float)(0.5 + n1 * 0.24 + n2 * 0.16), 0.0F, 1.0F);
    }

    int alphaByte(float alpha) {
        return Math.max(0, Math.min(255, Math.round(MathHelper.clamp(alpha, 0.0F, 1.0F) * 255.0F)));
    }

    float colorAlpha(Color value) {
        return value.getAlpha() / 255.0F;
    }

    static float fadeEnvelope(float raw) {
        float clamped = MathHelper.clamp(raw, 0.0F, 1.0F);
        float appear = smoothStepStatic(clamped / 0.18F);
        float disappear = smoothStepStatic((1.0F - clamped) / 0.82F);
        return MathHelper.clamp(appear * disappear, 0.0F, 1.0F);
    }

    static float easeOutCubic(float value) {
        float clamped = MathHelper.clamp(value, 0.0F, 1.0F);
        float inverse = 1.0F - clamped;
        return 1.0F - inverse * inverse * inverse;
    }

    static float smoothStepStatic(float value) {
        float t = MathHelper.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    void cleanupTestEffects(long now) {
        this.testWaves.removeIf(wave -> wave.finished(now));
    }

    Color getActiveColor() {
        return useCustomColor.getValue() ? color.getColor() : new Color(ThemeManager.INSTANCE.getVisualModulesColor(), true);
    }

    Identifier getCircleTexture() {
        return mode.is("Пятиугольник") ? RING_TEXTURE : TEXTURE;
    }

    static boolean isClassicCircleMode() {
        return mode.is("Пятиугольник");
    }

    float getDefaultScaleProgress(float progress) {
        return MathHelper.clamp(progress, 0.0F, 1.0F);
    }

    float smoothStep(float t) {
        float clamped = MathHelper.clamp(t, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    float getDefaultMaxLife() {
        return 23.0F;
    }

    float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    void addTriangle(
        BufferBuilder buffer,
        Matrix4f matrix,
        float x1,
        float z1,
        float u1,
        float v1,
        Color c1,
        int a1,
        float x2,
        float z2,
        float u2,
        float v2,
        Color c2,
        int a2,
        float x3,
        float z3,
        float u3,
        float v3,
        Color c3,
        int a3
    ) {
        this.addVertex(buffer, matrix, x1, z1, u1, v1, c1, a1);
        this.addVertex(buffer, matrix, x2, z2, u2, v2, c2, a2);
        this.addVertex(buffer, matrix, x3, z3, u3, v3, c3, a3);
    }

    void addVertex(BufferBuilder buffer, Matrix4f matrix, float x, float z, float u, float v, Color c, int a) {
        buffer.vertex(matrix, x, 0.0F, z).texture(u, v).color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    Color blendColors(Color a, Color b, float t) {
        float clamped = MathHelper.clamp(t, 0.0F, 1.0F);
        int r = (int)(a.getRed() + (b.getRed() - a.getRed()) * clamped);
        int g = (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * clamped);
        int bl = (int)(a.getBlue() + (b.getBlue() - a.getBlue()) * clamped);
        return new Color(r, g, bl);
    }

    void cleanupCircles() {
        this.circles.removeIf(c -> (float)(System.currentTimeMillis() - c.creationTime) > this.getDefaultMaxLife() * 50.0F * c.size);
    }
}
