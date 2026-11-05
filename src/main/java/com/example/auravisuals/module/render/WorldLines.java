package com.example.auravisuals.module.render;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.theme.ThemeManager;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class WorldLines extends Module {
    static final int RING_SEGS = 24;
    static final int GLOW_SEGS = 16;
    static final int SPARK_SEGS = 12;
    public static final int[][] AXES = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    static final float[][] OFFSETS = new float[][]{
        {0.055F, 0.0F, 0.0F}, {-0.055F, 0.0F, 0.0F}, {0.0F, 0.055F, 0.0F}, {0.0F, -0.055F, 0.0F}, {0.0F, 0.0F, 0.055F}, {0.0F, 0.0F, -0.055F}
    };
    static final float[] LAYER_A = new float[]{0.04F, 0.12F, 0.28F, 0.6F, 1.0F};
    static final float[] SIN_RING = new float[25];
    static final float[] COS_RING = new float[25];
    static final float[] SIN_GLOW = new float[17];
    static final float[] COS_GLOW = new float[17];
    static final float[] SIN_SPARK = new float[13];
    static final float[] COS_SPARK = new float[13];
    static final float[] RAY_COS = new float[8];
    static final float[] RAY_SIN = new float[8];
    static final float[] GLOW_POW = new float[9];
    static final float[] SPARK_POW = new float[6];
    static final float[] RING_POW = new float[5];
    final NumberSetting quantity = new NumberSetting("Количество", 45.0, 20.0, 80.0, 1.0);
    public final NumberSetting speed = new NumberSetting("Скорость", 1.0, 0.5, 2.0, 0.1);
    final BooleanSetting customColor = new BooleanSetting("Свой цвет", false);
    final ColorSetting color = new ColorSetting("Цвет", new Color(255, 255, 255), () -> this.customColor.getValue());
    final BooleanSetting glow = new BooleanSetting("Свечение", true);
    public final Random random = new Random(93882L);
    final List<WorldLines$GlyphVectorGenerator> glyphs = new ArrayList<>();
    final double[] ptsX = new double[32];
    final double[] ptsY = new double[32];
    final double[] ptsZ = new double[32];

    public WorldLines() {
        super("WorldLines", "Добавляет в мир бегущие линии", Category.VISUALS);
        this.addSettings(this.quantity, this.speed, this.glow, this.customColor, this.color);
    }

    @Override
    public void onDisable() {
        this.glyphs.clear();
    }

    @Override
    public void onTick() {
        if (this.mc.player != null) {
            for (int i = this.glyphs.size() - 1; i >= 0; i--) {
                WorldLines$GlyphVectorGenerator g = this.glyphs.get(i);
                g.update();
                if (g.isExpired()) {
                    this.glyphs.set(i, this.glyphs.get(this.glyphs.size() - 1));
                    this.glyphs.remove(this.glyphs.size() - 1);
                }
            }

            int targetQuantity = Math.min(this.quantity.getIntValue(), 56);

            while (this.glyphs.size() > targetQuantity) {
                this.glyphs.remove(this.glyphs.size() - 1);
            }

            while (this.glyphs.size() < targetQuantity) {
                this.tryAddGlyph();
            }
        }
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (!this.glyphs.isEmpty() && this.mc.player != null) {
            MatrixStack stack = context.matrixStack();
            float tickDelta = context.tickCounter().getTickProgress(true);
            Vec3d camVec = context.camera().getCameraPos();
            double camX = camVec.x;
            double camY = camVec.y;
            double camZ = camVec.z;
            Matrix4f mat = stack.peek().getPositionMatrix();
            int rgb = this.customColor.getValue() ? this.color.getValue() : ThemeManager.INSTANCE.getVisualModulesColor();
            int tr = rgb >> 16 & 0xFF;
            int tg = rgb >> 8 & 0xFF;
            int tb = rgb & 0xFF;
            long now = System.currentTimeMillis();
            Tessellator tess = Tessellator.getInstance();
            if (this.glow.getValue()) {
                for (int glowLayer = 0; glowLayer < 2; glowLayer++) {
                    int verts = 0;
                    BufferBuilder glow = tess.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
                    float layerMul = 0.15F - glowLayer * 0.05F;

                    for (WorldLines$GlyphVectorGenerator glyph : this.glyphs) {
                        float alpha = (float)glyph.getFadeOut();
                        int total;
                        if (!(alpha < 0.01F) && (total = glyph.fillPoints(this.ptsX, this.ptsY, this.ptsZ, tickDelta)) >= 2) {
                            float gp2 = this.smoothPulse(now, glyph.seed, 3000L);
                            float fp = this.smoothPulse(now, glyph.seed + 1.5, 800L);
                            float pulse2 = 0.7F + gp2 * 0.22F + fp * 0.08F;
                            int tm1 = total - 1;

                            for (int i = 0; i < tm1; i++) {
                                float tA = (float)i / tm1;
                                float tB = (float)(i + 1) / tm1;
                                float aA = alpha * pulse2 * (0.22F + tA * tA * 0.78F) * layerMul;
                                float aB = alpha * pulse2 * (0.22F + tB * tB * 0.78F) * layerMul;
                                glow.vertex(mat, (float)(this.ptsX[i] - camX), (float)(this.ptsY[i] - camY), (float)(this.ptsZ[i] - camZ))
                                    .color(tr, tg, tb, this.clamp(aA));
                                glow.vertex(mat, (float)(this.ptsX[i + 1] - camX), (float)(this.ptsY[i + 1] - camY), (float)(this.ptsZ[i + 1] - camZ))
                                    .color(tr, tg, tb, this.clamp(aB));
                                verts += 2;
                            }
                        }
                    }

                    if (verts > 0) {
                        try {
                            AuraRender.drawColorLines(glow, AuraRender.POS_COLOR_LINES_ADD, 5.0F - glowLayer * 1.55F);
                        } catch (Exception var43) {
                        }
                    }
                }
            }

            int verts = 0;
            BufferBuilder buf = tess.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            for (WorldLines$GlyphVectorGenerator glyph : this.glyphs) {
                float alpha = (float)glyph.getFadeOut();
                int total;
                if (!(alpha < 0.01F) && (total = glyph.fillPoints(this.ptsX, this.ptsY, this.ptsZ, tickDelta)) >= 2) {
                    float gp = this.smoothPulse(now, glyph.seed, 3000L);
                    float fp = this.smoothPulse(now, glyph.seed + 1.5, 800L);
                    float pulse = 0.65F + gp * 0.25F + fp * 0.1F;
                    int tm1 = total - 1;

                    for (float[] offset : OFFSETS) {
                        float ox = offset[0];
                        float oy = offset[1];
                        float oz = offset[2];

                        for (int i = 0; i < tm1; i++) {
                            float tA = (float)i / tm1;
                            float tB = (float)(i + 1) / tm1;
                            float aA = alpha * pulse * tA * tA * 0.035F;
                            float aB = alpha * pulse * tB * tB * 0.035F;
                            buf.vertex(mat, (float)(this.ptsX[i] - camX) + ox, (float)(this.ptsY[i] - camY) + oy, (float)(this.ptsZ[i] - camZ) + oz)
                                .color(tr, tg, tb, this.clamp(aA));
                            buf.vertex(mat, (float)(this.ptsX[i + 1] - camX) + ox, (float)(this.ptsY[i + 1] - camY) + oy, (float)(this.ptsZ[i + 1] - camZ) + oz)
                                .color(tr, tg, tb, this.clamp(aB));
                            verts += 2;
                        }
                    }
                }
            }

            if (verts > 0) {
                try {
                    AuraRender.drawColorLines(buf, AuraRender.POS_COLOR_LINES, 1.25F);
                } catch (Exception var42) {
                }
            }

            int var47 = 0;
            buf = tess.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            for (WorldLines$GlyphVectorGenerator glyph : this.glyphs) {
                float alpha = (float)glyph.getFadeOut();
                int total;
                if (!(alpha < 0.01F) && (total = glyph.fillPoints(this.ptsX, this.ptsY, this.ptsZ, tickDelta)) >= 2) {
                    float gp = this.smoothPulse(now, glyph.seed, 3000L);
                    float fp = this.smoothPulse(now, glyph.seed + 1.5, 800L);
                    float pulse = 0.6F + gp * 0.28F + fp * 0.12F;
                    int tm1 = total - 1;

                    for (float layer : LAYER_A) {
                        for (int i = 0; i < tm1; i++) {
                            float tA = (float)i / tm1;
                            float tB = (float)(i + 1) / tm1;
                            float shimA = 0.85F + this.smoothPulse(now, glyph.seed + tA * 4.0F, 600L) * 0.15F;
                            float shimB = 0.85F + this.smoothPulse(now, glyph.seed + tB * 4.0F, 600L) * 0.15F;
                            float aA = alpha * pulse * tA * tA * shimA * layer;
                            float aB = alpha * pulse * tB * tB * shimB * layer;
                            buf.vertex(mat, (float)(this.ptsX[i] - camX), (float)(this.ptsY[i] - camY), (float)(this.ptsZ[i] - camZ))
                                .color(tr, tg, tb, this.clamp(aA));
                            buf.vertex(mat, (float)(this.ptsX[i + 1] - camX), (float)(this.ptsY[i + 1] - camY), (float)(this.ptsZ[i + 1] - camZ))
                                .color(tr, tg, tb, this.clamp(aB));
                            var47 += 2;
                        }
                    }
                }
            }

            if (var47 > 0) {
                try {
                    AuraRender.drawColorLines(buf, AuraRender.POS_COLOR_LINES_ADD, 2.15F);
                } catch (Exception var41) {
                }
            }

            for (WorldLines$GlyphVectorGenerator glyph : this.glyphs) {
                float alpha = (float)glyph.getFadeOut();
                int total;
                if (!(alpha < 0.01F) && (total = glyph.fillPoints(this.ptsX, this.ptsY, this.ptsZ, tickDelta)) >= 2) {
                    float runT = (float)((now % 2500L + glyph.seed * 400.0) / 2500.0 % 1.0);
                    float runPulse = this.smoothPulse(now, glyph.seed + 3.0, 500L);
                    float runBright = 0.75F + runPulse * 0.25F;
                    int runI = (int)(runT * (total - 1));
                    float runF = runT * (total - 1) - runI;
                    if (this.glow.getValue() && runI < total - 1) {
                        double rx = this.ptsX[runI] + (this.ptsX[runI + 1] - this.ptsX[runI]) * runF;
                        double ry = this.ptsY[runI] + (this.ptsY[runI + 1] - this.ptsY[runI]) * runF;
                        double rz = this.ptsZ[runI] + (this.ptsZ[runI + 1] - this.ptsZ[runI]) * runF;
                        this.renderSparkFlare(tess, mat, (float)(rx - camX), (float)(ry - camY), (float)(rz - camZ), tr, tg, tb, alpha * runBright, 0.12F);
                    }

                    float hx = (float)(this.ptsX[total - 1] - camX);
                    float hy = (float)(this.ptsY[total - 1] - camY);
                    float hz = (float)(this.ptsZ[total - 1] - camZ);
                    float hP1 = this.smoothPulse(now, glyph.seed * 2.1, 900L);
                    float hP2 = this.smoothPulse(now, glyph.seed * 1.3 + 0.7, 1400L);
                    float hPulse = 0.5F + hP1 * 0.3F + hP2 * 0.2F;
                    if (this.glow.getValue()) {
                        this.renderHeadFlare(tess, mat, hx, hy, hz, tr, tg, tb, alpha, hPulse, now, glyph.seed);
                    }
                }
            }
        }
    }

    void renderHeadFlare(Tessellator tess, Matrix4f mat, float hx, float hy, float hz, int tr, int tg, int tb, float alpha, float hPulse, long now, double seed) {
        float glowMaxR = 0.11F + hPulse * 0.06F;
        float coreR = 0.011F + hPulse * 0.007F;
        int quadVerts = 0;
        BufferBuilder quads = tess.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (int g = 0; g < 9; g++) {
            float gf = g / 9.0F;
            float iR = glowMaxR * gf;
            float oR = glowMaxR * (gf + 0.11111111F);
            float ga = alpha * hPulse * GLOW_POW[g] * 0.42F;
            int gi = this.clamp(ga);
            if (gi >= 1) {
                int go = Math.max(0, gi / 18);

                for (int s = 0; s < 16; s++) {
                    float caA = COS_GLOW[s];
                    float saA = SIN_GLOW[s];
                    float caB = COS_GLOW[s + 1];
                    float saB = SIN_GLOW[s + 1];
                    quads.vertex(mat, hx + caA * oR, hy + saA * oR, hz).color(tr, tg, tb, go);
                    quads.vertex(mat, hx + caA * iR, hy + saA * iR, hz).color(tr, tg, tb, gi);
                    quads.vertex(mat, hx + caB * iR, hy + saB * iR, hz).color(tr, tg, tb, gi);
                    quads.vertex(mat, hx + caB * oR, hy + saB * oR, hz).color(tr, tg, tb, go);
                    quadVerts += 4;
                }
            }
        }

        int ci = this.clamp(alpha * hPulse);
        int ce = this.clamp(alpha * hPulse * 0.12F);
        if (ci > 0) {
            for (int s = 0; s < 16; s++) {
                float caA = COS_GLOW[s];
                float saA = SIN_GLOW[s];
                float caB = COS_GLOW[s + 1];
                float saB = SIN_GLOW[s + 1];
                quads.vertex(mat, hx, hy, hz).color(tr, tg, tb, ci);
                quads.vertex(mat, hx + caA * coreR, hy + saA * coreR, hz).color(tr, tg, tb, ce);
                quads.vertex(mat, hx + caB * coreR, hy + saB * coreR, hz).color(tr, tg, tb, ce);
                quads.vertex(mat, hx, hy, hz).color(tr, tg, tb, ci);
                quadVerts += 4;
            }
        }

        if (quadVerts > 0) {
            try {
                AuraRender.drawColor(quads, AuraRender.POS_COLOR_QUADS_ADD);
            } catch (Exception var37) {
            }
        }

        float maxR = 0.048F + hPulse * 0.038F;
        int lineVerts = 0;
        BufferBuilder lines = tess.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (int ring = 0; ring < 5; ring++) {
            float rf = ring / 5.0F;
            float ringR = maxR * (0.15F + rf * 0.85F);
            float ringA = alpha * hPulse * RING_POW[ring] * 0.88F;
            int ri = this.clamp(ringA);
            int ri2 = this.clamp(ringA * 0.58F);
            int ri3 = this.clamp(ringA * 0.38F);
            if (ri >= 2) {
                for (int s = 0; s < 24; s++) {
                    float caA = COS_RING[s];
                    float saA = SIN_RING[s];
                    float caB = COS_RING[s + 1];
                    float saB = SIN_RING[s + 1];
                    lines.vertex(mat, hx + caA * ringR, hy + saA * ringR, hz).color(tr, tg, tb, ri);
                    lines.vertex(mat, hx + caB * ringR, hy + saB * ringR, hz).color(tr, tg, tb, ri);
                    lines.vertex(mat, hx + caA * ringR, hy, hz + saA * ringR).color(tr, tg, tb, ri2);
                    lines.vertex(mat, hx + caB * ringR, hy, hz + saB * ringR).color(tr, tg, tb, ri2);
                    lines.vertex(mat, hx, hy + caA * ringR, hz + saA * ringR).color(tr, tg, tb, ri3);
                    lines.vertex(mat, hx, hy + caB * ringR, hz + saB * ringR).color(tr, tg, tb, ri3);
                    lineVerts += 6;
                }
            }
        }

        for (int i = 0; i < 8; i++) {
            float rayPulse = this.smoothPulse(now, seed + i * 0.8, 480L + i * 65L);
            float rayLen = 0.048F + rayPulse * 0.075F;
            float rayA = alpha * (0.32F + rayPulse * 0.68F);
            float startR = coreR * 1.3F;
            float ca = RAY_COS[i];
            float sa = RAY_SIN[i];
            lines.vertex(mat, hx + ca * startR, hy + sa * startR, hz).color(tr, tg, tb, this.clamp(rayA));
            lines.vertex(mat, hx + ca * (startR + rayLen), hy + sa * (startR + rayLen), hz).color(tr, tg, tb, 0);
            lineVerts += 2;
        }

        if (lineVerts > 0) {
            try {
                AuraRender.drawColorLines(lines, AuraRender.POS_COLOR_LINES_ADD, 1.0F);
            } catch (Exception var36) {
            }
        }
    }

    void renderSparkFlare(Tessellator tess, Matrix4f mat, float sx, float sy, float sz, int tr, int tg, int tb, float alpha, float maxR) {
        int verts = 0;
        BufferBuilder glow = tess.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (int g = 0; g < 6; g++) {
            float gf = g / 6.0F;
            float iR = maxR * gf;
            float oR = maxR * (gf + 0.16666667F);
            float ga = alpha * SPARK_POW[g] * 0.88F;
            int gi = this.clamp(ga);
            if (gi >= 1) {
                int go = Math.max(0, gi / 20);

                for (int s = 0; s < 12; s++) {
                    float caA = COS_SPARK[s];
                    float saA = SIN_SPARK[s];
                    float caB = COS_SPARK[s + 1];
                    float saB = SIN_SPARK[s + 1];
                    glow.vertex(mat, sx + caA * oR, sy + saA * oR, sz).color(tr, tg, tb, go);
                    glow.vertex(mat, sx + caA * iR, sy + saA * iR, sz).color(tr, tg, tb, gi);
                    glow.vertex(mat, sx + caB * iR, sy + saB * iR, sz).color(tr, tg, tb, gi);
                    glow.vertex(mat, sx + caB * oR, sy + saB * oR, sz).color(tr, tg, tb, go);
                    verts += 4;
                }
            }
        }

        if (verts > 0) {
            try {
                AuraRender.drawColor(glow, AuraRender.POS_COLOR_QUADS_ADD);
            } catch (Exception var25) {
            }
        }
    }

    float smoothPulse(long now, double seed, long periodMs) {
        float t = (float)((now + (long)(seed * 1000.0)) % periodMs) / (float)periodMs;
        float s = (float)(Math.sin(t * Math.PI * 2.0) * 0.5 + 0.5);
        return s * s * (3.0F - 2.0F * s);
    }

    int clamp(float a) {
        return Math.max(0, Math.min(255, (int)(a * 255.0F)));
    }

    void tryAddGlyph() {
        if (this.mc.player != null) {
            float fov = this.mc.options.getFov().getValue().floatValue();
            float baseYaw = this.mc.player.getYaw();
            int dist = this.random.nextInt(19) + 6;
            int yaw = this.random.nextInt((int)(fov * 1.5F) + 1) + (int)(baseYaw - fov * 0.75F);
            double rad = Math.toRadians(yaw);
            Vec3d eye = this.mc.player.getEyePos();
            double sx = eye.x - Math.sin(rad) * dist;
            double sy = eye.y + this.random.nextInt(25) - 12.0;
            double sz = eye.z + Math.cos(rad) * dist;
            this.glyphs.add(new WorldLines$GlyphVectorGenerator(this, sx, sy, sz, this.random.nextInt(6) + 7, this.random.nextDouble() * Math.PI * 2.0));
        }
    }

    static {
        for (int i = 0; i <= 24; i++) {
            double a = (Math.PI / 12) * i;
            SIN_RING[i] = (float)Math.sin(a);
            COS_RING[i] = (float)Math.cos(a);
        }

        for (int var10 = 0; var10 <= 16; var10++) {
            double a = (Math.PI / 8) * var10;
            SIN_GLOW[var10] = (float)Math.sin(a);
            COS_GLOW[var10] = (float)Math.cos(a);
        }

        for (int var11 = 0; var11 <= 12; var11++) {
            double a = (Math.PI / 6) * var11;
            SIN_SPARK[var11] = (float)Math.sin(a);
            COS_SPARK[var11] = (float)Math.cos(a);
        }

        for (int var12 = 0; var12 < 8; var12++) {
            double a = (Math.PI / 4) * var12;
            RAY_COS[var12] = (float)Math.cos(a);
            RAY_SIN[var12] = (float)Math.sin(a);
        }

        for (int g = 0; g < 9; g++) {
            float f = 1.0F - g / 9.0F;
            GLOW_POW[g] = (float)Math.pow(f, 2.8);
        }

        for (int var6 = 0; var6 < 6; var6++) {
            float f = 1.0F - var6 / 6.0F;
            SPARK_POW[var6] = (float)Math.pow(f, 2.2);
        }

        for (int r = 0; r < 5; r++) {
            float f = 1.0F - r / 5.0F;
            RING_POW[r] = (float)Math.pow(f, 1.6);
        }
    }
}
