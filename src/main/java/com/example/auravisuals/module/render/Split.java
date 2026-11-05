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
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Split extends Module {
    static final String MODE_TIME = "Time";
    static final String MODE_ON_HIT = "Attack";
    final ModeSetting mode = new ModeSetting("Режим", "Time", "Time", "Attack");
    final NumberSetting delay = new NumberSetting("Интервал (мс)", 200.0, 50.0, 2000.0, 50.0, () -> this.mode.is("Time"));
    final NumberSetting lifetime = new NumberSetting("Время жизни", 1000.0, 100.0, 5000.0, 100.0);
    final BooleanSetting useCustomColor = new BooleanSetting("Свой цвет", false);
    final ColorSetting color = new ColorSetting("Цвет", new Color(0, 255, 255, 100), () -> this.useCustomColor.getValue());
    final BooleanSetting firstPerson = new BooleanSetting("Первое лицо", true);
    final List<Split$Ghost> ghosts = new ArrayList<>();
    long lastSplitTime = 0L;

    public Split() {
        super("Split", "Оставляет красивую копию игрока", Category.VISUALS);
        this.addSettings(this.mode, this.delay, this.lifetime, this.useCustomColor, this.color, this.firstPerson);
    }

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.world != null) {
            if (this.mode.is("Time")) {
                boolean isMoving = this.mc.player.getVelocity().horizontalLengthSquared() > 1.0E-4;
                if (isMoving && System.currentTimeMillis() - this.lastSplitTime > this.delay.getValue()) {
                    this.createGhost(this.mc.player);
                    this.lastSplitTime = System.currentTimeMillis();
                }
            }

            this.ghosts.removeIf(ghost -> System.currentTimeMillis() - ghost.creationTime > this.lifetime.getValue());
        }
    }

    public void onAttack() {
        if ((this.mode.is("Attack") || this.mode.is("Attack")) && this.mc.player != null) {
            this.createGhost(this.mc.player);
        }
    }

    void createGhost(PlayerEntity player) {
        while (this.ghosts.size() >= 8) {
            this.ghosts.remove(0);
        }

        EntityRenderManager dispatcher = this.mc.getEntityRenderDispatcher();
        EntityRenderer class_8972 = dispatcher.getRenderer(player);
        LivingEntityRenderer renderer;
        EntityModel class_5832;
        if (class_8972 instanceof LivingEntityRenderer && (class_5832 = (renderer = (LivingEntityRenderer)class_8972).getModel()) instanceof PlayerEntityModel) {
            PlayerEntityModel model = (PlayerEntityModel)class_5832;
            this.ghosts.add(new Split$Ghost(this, player, model));
        }
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.mc.player != null && !this.ghosts.isEmpty()) {
            if (this.firstPerson.getValue() || !this.mc.options.getPerspective().isFirstPerson()) {
                MatrixStack matrices = context.matrixStack();
                Vec3d cameraPos = context.camera().getCameraPos();
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

                for (Split$Ghost ghost : this.ghosts) {
                    this.renderGhost(ghost, matrices, buffer, cameraPos);
                }

                try {
                    AuraRender.drawColor(buffer, AuraRender.POS_COLOR_QUADS);
                } catch (Exception var8) {
                }
            }
        }
    }

    void renderGhost(Split$Ghost ghost, MatrixStack matrices, BufferBuilder buffer, Vec3d cameraPos) {
        if (!this.isCameraInsideGhost(ghost, cameraPos)) {
            double dx = ghost.x - cameraPos.x;
            double dy = ghost.y - cameraPos.y;
            double dz = ghost.z - cameraPos.z;
            if (!(dx * dx + dy * dy + dz * dz > 9216.0)) {
                long age = System.currentTimeMillis() - ghost.creationTime;
                float alphaFactor = 1.0F - (float)(age / this.lifetime.getValue());
                if (!(alphaFactor <= 0.0F)) {
                    int c = this.getActiveColor();
                    float r = (c >> 16 & 0xFF) / 255.0F;
                    float g = (c >> 8 & 0xFF) / 255.0F;
                    float b = (c & 0xFF) / 255.0F;
                    float a = (c >> 24 & 0xFF) / 255.0F * alphaFactor;
                    matrices.push();

                    try {
                        matrices.translate(ghost.x - cameraPos.x, ghost.y - cameraPos.y, ghost.z - cameraPos.z);
                        float s = 0.9375F;
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(ghost.bodyYaw));
                        if (ghost.swimming) {
                            matrices.translate(0.0, 0.5, 0.0);
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(ghost.pitch - 90.0F));
                            matrices.translate(0.0, -0.5, 0.0);
                            matrices.translate(0.0, -0.8, 0.2);
                        } else if (ghost.sneaking) {
                            matrices.translate(0.0, -0.15, 0.0);
                        }

                        matrices.scale(s, -s, -s);
                        if (!ghost.swimming) {
                            matrices.translate(0.0, -1.501, 0.0);
                        } else {
                            matrices.translate(0.0, -0.2, 0.0);
                        }

                        Matrix4f matrix = matrices.peek().getPositionMatrix();

                        for (Split$TransformedQuad quad : ghost.quads) {
                            for (Vector3f v : quad.vertices) {
                                Vector3f pos = new Vector3f(v);
                                matrix.transformPosition(pos);
                                buffer.vertex(pos.x(), pos.y(), pos.z()).color(r, g, b, a);
                            }
                        }
                    } finally {
                        matrices.pop();
                    }
                }
            }
        }
    }

    boolean isCameraInsideGhost(Split$Ghost ghost, Vec3d cam) {
        double width = 0.4;
        double height = ghost.swimming ? 0.6 : (ghost.sneaking ? 1.5 : 1.8);
        return cam.x >= ghost.x - width
            && cam.x <= ghost.x + width
            && cam.y >= ghost.y
            && cam.y <= ghost.y + height
            && cam.z >= ghost.z - width
            && cam.z <= ghost.z + width;
    }

    int getActiveColor() {
        return this.useCustomColor.getValue() ? this.color.getValue() : ThemeManager.INSTANCE.getVisualModulesColor();
    }
}
