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
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class HitBubbles extends Module {
    static final String MODE_BUBBLE = "Пузырь";
    static final String MODE_GLASS = "Жидкое стекло";
    public static final ModeSetting mode = new ModeSetting("Вид", "Пузырь", "Пузырь", "Жидкое стекло");
    public static final BooleanSetting useCustomColor = new BooleanSetting("Свой цвет", false);
    public static final ColorSetting color = new ColorSetting("Цвет", new Color(120, 200, 255), () -> useCustomColor.getValue());
    public static final NumberSetting size = new NumberSetting("Размер", 1.0, 0.3, 3.0, 0.1, () -> mode.is("Пузырь"));
    public static final NumberSetting lifeTime = new NumberSetting("Время жизни", 1.6, 0.5, 4.0, 0.1, () -> mode.is("Пузырь"));
    public static final BooleanSetting additive = new BooleanSetting("Свечение", true, () -> mode.is("Пузырь"));
    public static final NumberSetting glassLifetime = new NumberSetting("Время жизни", 900.0, 200.0, 3000.0, 50.0, () -> mode.is("Жидкое стекло"));
    public static final NumberSetting glassSize = new NumberSetting("Размер стекла", 1.25, 0.35, 3.5, 0.05, () -> mode.is("Жидкое стекло"));
    public static final NumberSetting glassStrength = new NumberSetting("Сила искажения", 1.0, 0.05, 2.5, 0.05, () -> mode.is("Жидкое стекло"));
    static final Identifier TEXTURE = Identifier.of("auravisuals", "textures/hitbubble.png");
    final CopyOnWriteArrayList<HitBubbles$Bubble> bubbles = new CopyOnWriteArrayList<>();
    final CopyOnWriteArrayList<HitBubbles$Lens> lenses = new CopyOnWriteArrayList<>();

    public HitBubbles() {
        super("HitBubbles", "Круг при ударе по игроку", Category.VISUALS);
        this.addSettings(mode, useCustomColor, color, size, lifeTime, additive, glassLifetime, glassSize, glassStrength);
    }

    @Override
    public void onDisable() {
        this.bubbles.clear();
        this.lenses.clear();
    }

    long lifeMs() {
        return (long)(lifeTime.getFloatValue() * 1000.0F);
    }

    public void onAttack(PlayerEntity attacker, LivingEntity target) {
        if (attacker != null && target != null) {
            if (!mode.is("Жидкое стекло")) {
                Vec3d sideDir = this.getHitSideDirection(target, attacker.getEntityPos());
                Vec3d pos = this.resolveHitPoint(target, sideDir);
                float sideYaw = (float)Math.toDegrees(Math.atan2(sideDir.x, sideDir.z));
                this.bubbles.add(new HitBubbles$Bubble(pos, System.currentTimeMillis(), (float)(Math.random() * 360.0), sideYaw));
            } else {
                Vec3d center = target.getBoundingBox().getCenter();
                float hitboxRadius = Math.max(target.getWidth(), target.getHeight() * 0.62F);

                while (this.lenses.size() >= 6) {
                    this.lenses.remove(0);
                }

                this.lenses.add(new HitBubbles$Lens(center, System.currentTimeMillis(), hitboxRadius, (float)(Math.random() * (float) (Math.PI * 2))));
            }
        }
    }

    Vec3d resolveHitPoint(LivingEntity target, Vec3d sideDir) {
        HitResult hr = this.mc.crosshairTarget;
        EntityHitResult ehr;
        if (hr instanceof EntityHitResult && (ehr = (EntityHitResult)hr).getEntity() == target) {
            return ehr.getPos().add(sideDir.multiply(0.1));
        }

        Vec3d center = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.5, target.getZ());
        return center.add(sideDir.multiply(0.1));
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if ((!this.bubbles.isEmpty() || !this.lenses.isEmpty()) && this.mc.player != null) {
            long now = System.currentTimeMillis();
            this.bubbles.removeIf(b -> now - b.spawnTime >= this.lifeMs());
            this.lenses.removeIf(l -> now - l.spawnTime >= Math.max(1L, glassLifetime.getValue().longValue()));
            MatrixStack stack = context.matrixStack();
            Vec3d cam = context.camera().getCameraPos();
            if (!this.lenses.isEmpty()) {
                this.renderLenses(context, now, cam);
            }

            if (!this.bubbles.isEmpty()) {
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                boolean any = false;

                for (HitBubbles$Bubble bubble : this.bubbles) {
                    any |= this.appendBubble(buffer, stack, cam, bubble, now);
                }

                if (any) {
                    try {
                        AuraRender.drawTextured(buffer, additive.getValue() ? AuraRender.POS_TEX_COLOR_QUADS_ADD : AuraRender.POS_TEX_COLOR_QUADS, TEXTURE);
                    } catch (Exception var11) {
                    }
                }
            }
        }
    }

    void renderLenses(WorldRenderContext context, long now, Vec3d cam) {
        Matrix4f modelView = context.matrixStack().peek().getPositionMatrix();
        int distortionOnlyColor = (useCustomColor.getValue() ? color.getValue() : ThemeManager.INSTANCE.getVisualModulesColor()) & 16777215;
        float life = Math.max(1.0F, glassLifetime.getFloatValue());

        for (HitBubbles$Lens lens : this.lenses) {
            float raw;
            float fade;
            if (!(lens.pos.squaredDistanceTo(cam) > 9216.0) && !((fade = liquidFade(raw = clamp01((float)(now - lens.spawnTime) / life))) <= 0.003F)) {
                float expansion = 0.72F + 0.38F * easeOutCubic(raw);
                float radius = lens.hitboxRadius * glassSize.getFloatValue() * expansion;
                ImpactLensRenderer.render(
                    modelView, context.camera(), lens.pos, radius, raw, glassStrength.getFloatValue(), fade, lens.spinSeed, distortionOnlyColor
                );
            }
        }
    }

    boolean appendBubble(BufferBuilder buffer, MatrixStack stack, Vec3d cam, HitBubbles$Bubble bubble, long now) {
        float progress = (float)(now - bubble.spawnTime) / (float)this.lifeMs();
        if (progress >= 1.0F) {
            return false;
        }

        float inPhase = clamp01(progress / 0.22F);
        float outPhase = clamp01((progress - 0.225F) / 0.4F);
        float scaleIn = inPhase * inPhase * (3.0F - 2.0F * inPhase);
        float scaleOut = 1.0F - outPhase * outPhase;
        float scale = (0.02F + 1.55F * scaleIn * scaleOut) * size.getFloatValue();
        float alpha = 1.0F - outPhase * outPhase * outPhase;
        if (alpha <= 0.0F) {
            return false;
        }

        float rotation = (float)(now - bubble.spawnTime) / 1.5F + bubble.spinSeed;
        int rgb = (useCustomColor.getValue() ? color.getValue() : ThemeManager.INSTANCE.getVisualModulesColor()) & 16777215;
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        int a = (int)(alpha * 255.0F);
        Vec3d rel = bubble.pos.subtract(cam);
        stack.push();

        try {
            stack.translate(rel.x, rel.y, rel.z);
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(bubble.sideYaw));
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-210.0F));
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
            Matrix4f mat = stack.peek().getPositionMatrix();
            float h = scale * 0.5F;
            buffer.vertex(mat, -h, -h, 0.0F).texture(0.0F, 0.0F).color(r, g, b, a);
            buffer.vertex(mat, -h, h, 0.0F).texture(0.0F, 1.0F).color(r, g, b, a);
            buffer.vertex(mat, h, h, 0.0F).texture(1.0F, 1.0F).color(r, g, b, a);
            buffer.vertex(mat, h, -h, 0.0F).texture(1.0F, 0.0F).color(r, g, b, a);
        } finally {
            stack.pop();
        }

        return true;
    }

    static float clamp01(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }

    static float liquidFade(float raw) {
        float appear = smooth(raw / 0.12F);
        float disappear = smooth((1.0F - raw) / 0.76F);
        return clamp01(appear * disappear);
    }

    static float smooth(float value) {
        float t = clamp01(value);
        return t * t * (3.0F - 2.0F * t);
    }

    static float easeOutCubic(float value) {
        float inverse = 1.0F - clamp01(value);
        return 1.0F - inverse * inverse * inverse;
    }

    Vec3d getHitSideDirection(LivingEntity target, Vec3d attackerPos) {
        Vec3d dir = attackerPos.subtract(target.getEntityPos());
        dir = new Vec3d(dir.x, 0.0, dir.z);
        if (dir.lengthSquared() < 1.0E-4) {
            Vec3d fallback = target.getRotationVector();
            dir = new Vec3d(fallback.x, 0.0, fallback.z);
        }

        if (dir.lengthSquared() < 1.0E-4) {
            dir = new Vec3d(0.0, 0.0, 1.0);
        }

        return dir.normalize();
    }
}
