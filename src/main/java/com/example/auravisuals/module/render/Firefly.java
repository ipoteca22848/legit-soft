package com.example.auravisuals.module.render;

import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.theme.ThemeManager;
import com.example.auravisuals.util.color.ColorUtil;
import com.example.auravisuals.util.render.GlowRenderer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Firefly extends Module {
    static final double DEFAULT_FLIGHT_SMOOTHNESS = 0.285;
    final NumberSetting count = new NumberSetting("Количество", 20.0, 10.0, 50.0, 1.0);
    final NumberSetting speed = new NumberSetting("Плавность полета", 0.05, 0.01, 0.3, 0.01);
    final NumberSetting orbitSpeed = new NumberSetting("Скор. вращения", 1.0, 0.1, 5.0, 0.1);
    final NumberSetting size = new NumberSetting("Размер", 0.05, 0.01, 0.15, 0.01);
    final BooleanSetting shaderGlow = new BooleanSetting("Свечение", false);
    final BooleanSetting customColor = new BooleanSetting("Свой цвет", false);
    final ColorSetting color = new ColorSetting("Цвет", new Color(255, 200, 50), () -> this.customColor.getValue());
    final List<Firefly$Particle> particles = new ArrayList<>();
    public final Random random = new Random();
    Entity currentTarget = null;
    long startFlyOutTime = 0L;
    long startReturnTime = 0L;
    static final long TIMEOUT = 15000L;

    public Firefly() {
        super("Firefly", "Красивые светлячки при ударе", Category.VISUALS);
        this.addSettings(this.count, this.orbitSpeed, this.size, this.shaderGlow, this.customColor, this.color);
    }

    public void onAttack(Entity target) {
        if (this.isEnabled() && target != null) {
            long now = System.currentTimeMillis();
            if (this.currentTarget != target) {
                this.currentTarget = target;
                this.startFlyOutTime = now;
            }

            this.startReturnTime = now + 15000L;
        }
    }

    @Override
    public void onDisable() {
        this.particles.clear();
        this.currentTarget = null;
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (this.isEnabled() && this.mc.player != null) {
            long now = System.currentTimeMillis();
            boolean shouldReturn = false;
            if (this.currentTarget != null) {
                if (!this.currentTarget.isAlive() || this.mc.player.distanceTo(this.currentTarget) > 20.0F) {
                    if (this.startReturnTime > now) {
                        this.startReturnTime = now;
                    }

                    shouldReturn = true;
                }
            } else {
                shouldReturn = true;
            }

            if (now > this.startReturnTime) {
                shouldReturn = true;
            }

            if (this.currentTarget != null) {
                Entity class_12972 = this.currentTarget;
                LivingEntity living;
                if (this.currentTarget instanceof LivingEntity && (living = (LivingEntity)class_12972).isInvisible()) {
                    this.currentTarget = null;
                    shouldReturn = true;
                }
            }

            if (shouldReturn && this.currentTarget != null && now > this.startReturnTime + this.particles.size() * 100) {
                this.currentTarget = null;
            }

            int targetCount = Math.min(this.count.getValue().intValue(), 40);

            while (this.particles.size() < targetCount) {
                this.particles.add(new Firefly$Particle(this));
            }

            while (this.particles.size() > targetCount) {
                this.particles.remove(0);
            }

            for (int i = 0; i < this.particles.size(); i++) {
                Firefly$Particle p = this.particles.get(i);
                long delay = i * 50L;
                boolean isFlyingToTarget = false;
                if (this.currentTarget != null && !shouldReturn) {
                    if (now > this.startFlyOutTime + delay) {
                        isFlyingToTarget = true;
                    }
                } else if (this.currentTarget != null && shouldReturn && now < this.startReturnTime + delay) {
                    isFlyingToTarget = true;
                }

                double targetSizeVal;
                double targetRadius;
                Entity destEntity;
                if (isFlyingToTarget) {
                    destEntity = this.currentTarget;
                    targetRadius = destEntity.getWidth() + 0.5;
                    targetSizeVal = this.size.getValue().floatValue();
                } else {
                    destEntity = this.mc.player;
                    targetRadius = 0.1;
                    targetSizeVal = 0.0;
                }

                double yOffset = destEntity == this.mc.player ? destEntity.getHeight() * 0.7 : destEntity.getHeight() * 0.5;
                Vec3d center = destEntity.getEntityPos().add(0.0, yOffset, 0.0);
                p.update(center, 0.285, this.orbitSpeed.getValue(), targetRadius, targetSizeVal);
            }
        }
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.isEnabled() && !this.particles.isEmpty()) {
            MatrixStack matrices = context.matrixStack();
            Vec3d camera = context.camera().getCameraPos();
            int col = this.getActiveColor();
            float tickDelta = this.mc.getRenderTickCounter().getTickProgress(true);

            for (Firefly$Particle p : this.particles) {
                double dz;
                double dy;
                double dx;
                if (!(p.currentSize < 0.005F)
                    && !(
                        (dx = MathHelper.lerp(tickDelta, p.prevX, p.x) - camera.x) * dx
                                + (dy = MathHelper.lerp(tickDelta, p.prevY, p.y) - camera.y) * dy
                                + (dz = MathHelper.lerp(tickDelta, p.prevZ, p.z) - camera.z) * dz
                            > 4096.0
                    )) {
                    matrices.push();

                    try {
                        matrices.translate(dx, dy, dz);
                        matrices.multiply(context.camera().getRotation());
                        float pr = (float)p.currentSize;
                        GlowRenderer.drawGlowingCircle(matrices, pr, col);
                        if (this.shaderGlow.getValue() && pr >= 0.005F) {
                            int baseA = ColorUtil.getAlpha(col);
                            int haloA = Math.max(8, (int)(baseA * 0.22F));
                            GlowRenderer.drawGlowingCircle(matrices, pr * 2.35F, ColorUtil.withAlpha(col, haloA));
                        }
                    } finally {
                        matrices.pop();
                    }
                }
            }
        }
    }

    int getActiveColor() {
        return this.customColor.getValue() ? this.color.getValue() : ThemeManager.INSTANCE.getVisualModulesColor();
    }

    static MinecraftClient access$000(Firefly x0) {
        return x0.mc;
    }

    static MinecraftClient access$100(Firefly x0) {
        return x0.mc;
    }

    static MinecraftClient access$200(Firefly x0) {
        return x0.mc;
    }

    static MinecraftClient access$300(Firefly x0) {
        return x0.mc;
    }
}
