package com.example.auravisuals.module.render;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.render.WorldOcclusion;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class HitRange extends Module {
    private static final double RADIUS = 3.0;
    private static final int SEGMENTS = 192;
    public static final BooleanSetting showOnOthers = new BooleanSetting("Отображать на других", false);
    public static final NumberSetting height = new NumberSetting("Высота", 0.0, 0.0, 2.0, 0.05);

    public HitRange() {
        super("HitRange", "Показывает радиус удара", Category.VISUALS);
        this.addSettings(showOnOthers, height);
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.mc.player != null && this.mc.world != null) {
            Vec3d cam = context.camera().getCameraPos();
            float tickDelta = this.mc.getRenderTickCounter().getTickProgress(true);
            this.renderForPlayer(context.matrixStack(), cam, this.mc.player, tickDelta);
            if (showOnOthers.getValue()) {
                for (PlayerEntity p : this.mc.world.getPlayers()) {
                    if (p != this.mc.player && !p.isSpectator() && p.isAlive() && WorldOcclusion.isEntityVisible(p, cam, tickDelta)) {
                        this.renderForPlayer(context.matrixStack(), cam, p, tickDelta);
                    }
                }
            }
        }
    }

    private void renderForPlayer(MatrixStack matrices, Vec3d cam, PlayerEntity centerPlayer, float tickDelta) {
        Vec3d pos = getLerpedPos(centerPlayer, tickDelta);
        boolean touchesHitbox = false;

        for (PlayerEntity p : this.mc.world.getPlayers()) {
            if (p != centerPlayer && !p.isSpectator() && p.isAlive() && circleTouchesPlayerHitboxXZ(pos.x, pos.z, 3.0, p.getBoundingBox())) {
                touchesHitbox = true;
                break;
            }
        }

        Color color = touchesHitbox ? new Color(60, 255, 90, 200) : new Color(255, 60, 60, 200);
        float yOff = 0.02F + height.getValue().floatValue();
        matrices.push();

        try {
            matrices.translate(pos.x - cam.x, pos.y - cam.y + yOff, pos.z - cam.z);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float rr = color.getRed() / 255.0F;
            float gg = color.getGreen() / 255.0F;
            float bb = color.getBlue() / 255.0F;
            float aa = color.getAlpha() / 255.0F;
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
            double step = 0.032724923474893676;

            for (int i = 0; i <= 192; i++) {
                double a = i * step;
                float x = (float)(Math.cos(a) * 3.0);
                float z = (float)(Math.sin(a) * 3.0);
                buf.vertex(matrix, x, 0.0F, z).color(rr, gg, bb, aa);
            }

            AuraRender.drawColorLines(buf, AuraRender.POS_COLOR_LINE_STRIP, 2.0F);
        } finally {
            matrices.pop();
        }
    }

    private static Vec3d getLerpedPos(PlayerEntity e, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, e.lastRenderX, e.getX());
        double y = MathHelper.lerp(tickDelta, e.lastRenderY, e.getY());
        double z = MathHelper.lerp(tickDelta, e.lastRenderZ, e.getZ());
        return new Vec3d(x, y, z);
    }

    private static boolean circleTouchesPlayerHitboxXZ(double cx, double cz, double r, Box box) {
        double dxMin = 0.0;
        if (cx < box.minX) {
            dxMin = box.minX - cx;
        } else if (cx > box.maxX) {
            dxMin = cx - box.maxX;
        }

        double dzMin = 0.0;
        if (cz < box.minZ) {
            dzMin = box.minZ - cz;
        } else if (cz > box.maxZ) {
            dzMin = cz - box.maxZ;
        }

        double dMin2 = dxMin * dxMin + dzMin * dzMin;
        double r2 = r * r;
        return dMin2 <= r2;
    }
}
