package com.example.auravisuals.module.render;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.theme.ThemeManager;
import com.example.auravisuals.util.render.WorldOcclusion;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;

public class Hats extends Module {
    static final String MODE_CHINA = "China";
    static final String MODE_NIMB = "Nimb";
    static final String MODE_SPIRITS = "Духи";
    final ModeSetting mode = new ModeSetting("Режим", "China", "China", "Nimb", "Духи");
    final BooleanSetting showOnOthers = new BooleanSetting("Отображать на других", true, () -> this.mode.getValue().equals("China"));
    final BooleanSetting glow = new BooleanSetting("Свечение", true);
    final BooleanSetting customColor = new BooleanSetting("Свой цвет", false, () -> this.mode.getValue().equals("China"));
    final ColorSetting color = new ColorSetting("Цвет", new Color(255, 50, 50), () -> this.mode.getValue().equals("China") && this.customColor.getValue());
    final NumberSetting chinaAlpha = new NumberSetting("Прозрачность", 0.3, 0.1, 1.0, 0.05, () -> this.mode.getValue().equals("China"));
    final BooleanSetting chinaGradient = new BooleanSetting("Градиент", false, () -> this.mode.getValue().equals("China"));
    final ColorSetting chinaGradientColor = new ColorSetting(
        "Цвет 2", new Color(255, 255, 255), () -> this.mode.getValue().equals("China") && this.chinaGradient.getValue()
    );
    final NumberSetting chinaGradientSpeed = new NumberSetting(
        "Скорость вращения", 1.0, 0.1, 5.0, 0.1, () -> this.mode.getValue().equals("China") && this.chinaGradient.getValue()
    );
    final BooleanSetting nimbCustomColor = new BooleanSetting("Свой цвет", false, () -> this.mode.getValue().equals("Nimb"));
    final ColorSetting nimbColor = new ColorSetting(
        "Цвет", new Color(255, 225, 140), () -> this.mode.getValue().equals("Nimb") && this.nimbCustomColor.getValue()
    );
    final BooleanSetting nimbGradient = new BooleanSetting("Градиент", false, () -> this.mode.getValue().equals("Nimb"));
    final ColorSetting nimbGradientColor = new ColorSetting(
        "Цвет 2", new Color(255, 255, 255), () -> this.mode.getValue().equals("Nimb") && this.nimbGradient.getValue()
    );
    final BooleanSetting nimbRotate = new BooleanSetting("Вращать", true, () -> this.mode.getValue().equals("Nimb") && this.nimbGradient.getValue());
    final NumberSetting nimbRotateSpeed = new NumberSetting(
        "Скорость вращения", 1.0, 0.5, 2.5, 0.1, () -> this.mode.getValue().equals("Nimb") && this.nimbGradient.getValue() && this.nimbRotate.getValue()
    );
    final BooleanSetting crownCustomColor = new BooleanSetting("Свой цвет", false, () -> false);
    final ColorSetting crownColor = new ColorSetting("Цвет", new Color(255, 255, 255), () -> false);
    final NumberSetting spiritsSize = new NumberSetting("Размер", 1.0, 0.5, 1.5, 0.1, () -> this.mode.getValue().equals("Духи"));
    final NumberSetting spiritsSpeed = new NumberSetting("Скорость", 1.0, 0.5, 2.0, 0.1, () -> this.mode.getValue().equals("Духи"));
    final BooleanSetting spiritsSwitch = new BooleanSetting("Переходить к другим", true, () -> this.mode.getValue().equals("Духи"));
    final BooleanSetting spiritsCustomColor = new BooleanSetting("Свой цвет", false, () -> this.mode.getValue().equals("Духи"));
    final ColorSetting spiritsColor = new ColorSetting(
        "Цвет духов", new Color(255, 255, 255), () -> this.mode.getValue().equals("Духи") && this.spiritsCustomColor.getValue()
    );
    Vec3d currentSpiritsCenter = null;
    Entity lockedSpiritsTarget = null;
    Entity currentSpiritsTarget = null;
    float spiritsTransitionBoost = 1.0F;
    float ghostAngle = 0.0F;
    long lastSpiritsTimeNs = 0L;
    float spiritsDelta = 0.016666668F;
    static final Identifier GLOW_TEXTURE = Identifier.of("auravisuals", "textures/glow.png");
    final List<Hats$Ghost> ghosts = new ArrayList<>();
    static final double SPIRITS_DISTANCE = 0.4;
    static final double SPIRITS_HEIGHT = 0.3;
    static final double SPIRITS_TRANSITION_SPEED = 0.08;
    static final float BASE_PLAYER_WIDTH = 0.6F;
    static final float BASE_PLAYER_HEIGHT = 1.8F;

    public Hats() {
        super("Hats", "Красивые шляпы", Category.VISUALS);
        this.addSettings(
            this.mode,
            this.showOnOthers,
            this.glow,
            this.customColor,
            this.color,
            this.chinaAlpha,
            this.chinaGradient,
            this.chinaGradientColor,
            this.chinaGradientSpeed,
            this.nimbCustomColor,
            this.nimbColor,
            this.nimbGradient,
            this.nimbGradientColor,
            this.nimbRotate,
            this.nimbRotateSpeed,
            this.spiritsSize,
            this.spiritsSpeed,
            this.spiritsSwitch,
            this.spiritsCustomColor,
            this.spiritsColor
        );
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.mc.player != null && this.mc.world != null) {
            String currentMode = this.mode.getValue();
            if (currentMode.equals("Духи")) {
                this.updateAndRenderSpirits(context, context.matrixStack());
            } else {
                for (PlayerEntity entity : this.mc.world.getPlayers()) {
                    if ((entity == this.mc.player || !this.mode.getValue().equals("Nimb") && this.showOnOthers.getValue())
                        && (entity == this.mc.player || !entity.isInvisible())
                        && (entity == this.mc.player || !(entity.squaredDistanceTo(this.mc.player) > 4096.0))
                        && (entity != this.mc.player || !this.mc.options.getPerspective().isFirstPerson())
                        && (entity == this.mc.player || this.isVisibleToPlayer(entity))) {
                        MatrixStack matrices = context.matrixStack();
                        Vec3d cameraPos = context.camera().getCameraPos();
                        float tickDelta = context.tickCounter().getTickProgress(true);
                        double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX()) - cameraPos.x;
                        double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()) - cameraPos.y;
                        double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ()) - cameraPos.z;
                        matrices.push();

                        try {
                            matrices.translate(x, y, z);
                            if (currentMode.equals("China")) {
                                this.renderChinaHat(matrices, entity, tickDelta);
                            } else if (currentMode.equals("Nimb")) {
                                this.renderNimb(matrices, entity);
                            }
                        } finally {
                            matrices.pop();
                        }
                    }
                }
            }
        }
    }

    void updateAndRenderSpirits(WorldRenderContext context, MatrixStack matrices) {
        Entity target = null;
        if (this.spiritsSwitch.getValue()) {
            if (this.lockedSpiritsTarget != null
                && this.lockedSpiritsTarget.isAlive()
                && this.lockedSpiritsTarget != this.mc.player
                && this.lockedSpiritsTarget.distanceTo(this.mc.player) <= 6.0) {
                target = this.lockedSpiritsTarget;
            }

            if (target == null) {
                target = this.mc
                    .world
                    .getPlayers()
                    .stream()
                    .filter(p -> p != this.mc.player)
                    .filter(p -> !p.isInvisible())
                    .filter(p -> p.distanceTo(this.mc.player) <= 4.0)
                    .min(Comparator.comparingDouble(p -> p.distanceTo(this.mc.player)))
                    .orElse(null);
            }
        }

        this.updateSpiritsDelta();
        if (target == null) {
            target = this.mc.player;
        }

        if (!(
            target instanceof LivingEntity livingTarget
                && target != this.mc.player
                && !WorldOcclusion.isEntityVisible(livingTarget, context.camera().getCameraPos(), context.tickCounter().getTickProgress(true))
        )) {
            if (target == this.mc.player && this.mc.options.getPerspective().isFirstPerson()) {
                this.currentSpiritsTarget = null;
                this.currentSpiritsCenter = null;
                this.lockedSpiritsTarget = null;
            } else {
                if (this.currentSpiritsTarget == null || this.currentSpiritsTarget != target) {
                    this.currentSpiritsTarget = target;
                    this.spiritsTransitionBoost = 1.0F;
                }

                double height = target.getHeight() + 0.24;
                float tickDelta = context.tickCounter().getTickProgress(true);
                Vec3d lerpedPos = target.getLerpedPos(tickDelta);
                Vec3d targetPos = lerpedPos.add(0.0, height, 0.0);
                if (this.currentSpiritsCenter == null) {
                    this.currentSpiritsCenter = targetPos;
                    this.lockedSpiritsTarget = target;
                } else {
                    double dist = this.currentSpiritsCenter.distanceTo(targetPos);
                    if (dist < 0.5) {
                        this.currentSpiritsCenter = targetPos;
                        this.lockedSpiritsTarget = target;
                        this.spiritsTransitionBoost = 1.0F;
                    } else {
                        this.spiritsTransitionBoost = MathHelper.clamp(this.spiritsTransitionBoost + 0.02F * this.spiritsDelta * 60.0F, 1.0F, 2.0F);
                        double speedFactor = 0.14600399999999997 * this.spiritsTransitionBoost;
                        double moveDist = dist * speedFactor * this.spiritsDelta * 60.0;
                        if (moveDist > dist) {
                            moveDist = dist;
                        }

                        Vec3d dir = targetPos.subtract(this.currentSpiritsCenter).normalize();
                        this.currentSpiritsCenter = this.currentSpiritsCenter.add(dir.multiply(moveDist));
                        if (this.currentSpiritsCenter.distanceTo(targetPos) < 0.5) {
                            this.lockedSpiritsTarget = target;
                        }
                    }
                }

                this.renderGhosts(context, this.currentSpiritsCenter);
            }
        }
    }

    boolean isVisibleToPlayer(LivingEntity entity) {
        if (this.mc.player != null && this.mc.world != null) {
            Vec3d start = this.mc.player.getEyePos();
            BlockHitResult hit = this.mc.world.raycast(new RaycastContext(start, entity.getEyePos(), ShapeType.COLLIDER, FluidHandling.NONE, this.mc.player));
            return hit.getType() == Type.MISS;
        } else {
            return true;
        }
    }

    void renderChinaHat(MatrixStack matrices, LivingEntity entity, float tickDelta) {
        Color baseColor = this.customColor.getValue()
            ? new Color(this.color.getColor().getRed(), this.color.getColor().getGreen(), this.color.getColor().getBlue())
            : new Color(ThemeManager.INSTANCE.getVisualModulesColor());
        Color altRaw = this.chinaGradientColor.getColor();
        Color altColor = this.chinaGradient.getValue() ? new Color(altRaw.getRed(), altRaw.getGreen(), altRaw.getBlue()) : baseColor;
        float alpha = this.chinaAlpha.getValue().floatValue();
        PlayerEntity player;
        PlayerEntity customPlayer = entity instanceof PlayerEntity && CustomModels.isReplacing(player = (PlayerEntity)entity) ? player : null;
        float visualScale = customPlayer != null ? CustomModels.getVisualScale(customPlayer) : 1.0F;
        float visualWidth = this.getHatVisualWidth(entity, customPlayer);
        float visualHeight = this.getHatVisualHeight(entity, customPlayer);
        float radius = Math.max(0.55F, visualWidth * 0.95F);
        float height = Math.max(0.12F, visualHeight * 0.18F);
        float headYaw = customPlayer != null ? 0.0F : MathHelper.lerp(tickDelta, entity.lastHeadYaw, entity.headYaw);
        float headPitch = customPlayer != null ? 0.0F : MathHelper.lerp(tickDelta, entity.lastPitch, entity.getPitch());
        float headPivot = customPlayer != null ? 1.8F - 0.42F * visualScale : entity.getStandingEyeHeight() - Math.min(0.18F, entity.getHeight() * 0.12F);
        float headTopOffset = customPlayer != null ? 0.14F * visualScale : Math.max(0.05F, entity.getHeight() - headPivot - 0.02F);
        boolean sneaking = entity.isSneaking();
        float sneakModelOffset = sneaking ? -0.08F : 0.0F;
        float hatGap = sneaking ? 0.1F : 0.12F;
        if (this.hasHelmet(entity)) {
            hatGap += 0.07F;
        }

        float hatDrop = customPlayer != null ? -0.095F : -0.07F;
        matrices.push();

        try {
            matrices.translate(0.0F, headPivot + sneakModelOffset, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-headYaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(headPitch));
            matrices.translate(0.0F, headTopOffset + hatGap + hatDrop, 0.0F);
            this.drawConeHat(matrices, baseColor, altColor, this.chinaGradient.getValue(), radius, height, alpha);
        } finally {
            matrices.pop();
        }
    }

    boolean hasHelmet(LivingEntity entity) {
        return entity != null && !entity.getEquippedStack(EquipmentSlot.HEAD).isEmpty();
    }

    void renderNimb(MatrixStack matrices, LivingEntity entity) {
        PlayerEntity player;
        PlayerEntity customPlayer = entity instanceof PlayerEntity && CustomModels.isReplacing(player = (PlayerEntity)entity) ? player : null;
        float visualWidth = this.getHatVisualWidth(entity, customPlayer);
        float visualHeight = this.getHatVisualHeight(entity, customPlayer);
        float radius = Math.max(0.34F, visualWidth * 0.5F);
        float yOffset = visualHeight + (entity.isSneaking() ? 0.175F : 0.235F) + (this.hasHelmet(entity) ? 0.1F : 0.0F) - (customPlayer != null ? 0.05F : 0.0F);
        float baseScale = Math.max(0.125F, visualWidth * 0.17F);
        float verticalThickness = Math.max(0.018F, visualHeight * 0.012F);
        boolean rotateEnabled = this.nimbGradient.getValue() && this.nimbRotate.getValue();
        float rotationPhase = rotateEnabled ? (float)Math.toRadians(System.currentTimeMillis() * 0.144 * this.nimbRotateSpeed.getValue() % 360.0) : 0.0F;
        int segments = 72;
        Color baseColor = this.nimbCustomColor.getValue() ? this.nimbColor.getColor() : new Color(ThemeManager.INSTANCE.getVisualModulesColor());
        Color gradientColor = this.nimbGradient.getValue() ? this.nimbGradientColor.getColor() : baseColor;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder coreBuffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (int layer = 0; layer < 2; layer++) {
            float layerYOffset = yOffset + (layer == 0 ? -verticalThickness : verticalThickness);
            float layerScale = layer == 0 ? 0.98F : 0.9F;

            for (int i = 0; i < segments; i++) {
                float trailT = (float)i / segments;
                float angle = -trailT * (float) (Math.PI * 2);
                float colorAngle = angle + (float) (Math.PI / 2) + rotationPhase;
                float mix = this.nimbGradient.getValue() ? this.smoothGradientT(colorAngle, 0.0F) : 0.0F;
                Color segmentColor = this.nimbGradient.getValue() ? this.blendColors(baseColor, gradientColor, mix) : baseColor;
                int outerAlpha = layer == 0 ? 30 : 22;
                int midAlpha = layer == 0 ? 58 : 42;
                int innerAlpha = layer == 0 ? 78 : 56;
                matrices.push();

                try {
                    matrices.translate(MathHelper.cos(angle) * radius, layerYOffset, MathHelper.sin(angle) * radius);
                    matrices.multiply(this.mc.gameRenderer.getCamera().getRotation());
                    Matrix4f matrix = matrices.peek().getPositionMatrix();
                    this.appendGlowQuad(coreBuffer, matrix, segmentColor, outerAlpha, baseScale * layerScale);
                    this.appendGlowQuad(coreBuffer, matrix, this.blendColors(segmentColor, Color.WHITE, 0.28F), midAlpha, baseScale * 0.58F * layerScale);
                    this.appendGlowQuad(coreBuffer, matrix, this.blendColors(segmentColor, Color.WHITE, 0.44F), innerAlpha, baseScale * 0.24F * layerScale);
                } finally {
                    matrices.pop();
                }
            }
        }

        try {
            AuraRender.drawTextured(coreBuffer, AuraRender.POS_TEX_COLOR_QUADS, GLOW_TEXTURE);
        } catch (Exception var43) {
        }

        if (this.glow.getValue()) {
            BufferBuilder glowBuffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (int i = 0; i < segments; i += 2) {
                float trailT = (float)i / segments;
                float angle = -trailT * (float) (Math.PI * 2);
                float colorAngle = angle + (float) (Math.PI / 2) + rotationPhase;
                float mix = this.nimbGradient.getValue() ? this.smoothGradientT(colorAngle, 0.0F) : 0.0F;
                Color segmentColor = this.nimbGradient.getValue() ? this.blendColors(baseColor, gradientColor, mix) : baseColor;
                int glowAlpha = 12;
                matrices.push();

                try {
                    matrices.translate(MathHelper.cos(angle) * radius, yOffset, MathHelper.sin(angle) * radius);
                    matrices.multiply(this.mc.gameRenderer.getCamera().getRotation());
                    Matrix4f matrix = matrices.peek().getPositionMatrix();
                    this.appendGlowQuad(glowBuffer, matrix, this.blendColors(segmentColor, Color.WHITE, 0.1F), glowAlpha, baseScale * 1.3F);
                } finally {
                    matrices.pop();
                }
            }

            try {
                AuraRender.drawTextured(glowBuffer, AuraRender.POS_TEX_COLOR_QUADS_ADD, GLOW_TEXTURE);
            } catch (Exception var41) {
            }
        }
    }

    float getHatVisualWidth(LivingEntity entity, PlayerEntity customPlayer) {
        return customPlayer != null ? 0.6F : entity.getWidth();
    }

    float getHatVisualHeight(LivingEntity entity, PlayerEntity customPlayer) {
        if (customPlayer != null) {
            return entity.isSneaking() ? 1.584F : 1.8F;
        } else {
            return entity.getHeight();
        }
    }

    void drawConeHat(MatrixStack matrices, Color baseColor, Color altColor, boolean gradientEnabled, float radius, float coneHeight, float alpha) {
        float phaseAngle = (float)(System.currentTimeMillis() * 3.0E-4 * this.chinaGradientSpeed.getValue() * Math.PI * 2.0 % (Math.PI * 2));
        float apexA = MathHelper.clamp(alpha, 0.0F, 1.0F);
        int segments = 28;
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (int i2 = 0; i2 < segments; i2++) {
            float angle1 = (float)((Math.PI * 2) * i2 / segments);
            float angle2 = (float)((Math.PI * 2) * (i2 + 1) / segments);
            float midAngle = (angle1 + angle2) * 0.5F;
            float t1 = gradientEnabled ? this.smoothGradientT(angle1, phaseAngle) : 0.0F;
            float t2 = gradientEnabled ? this.smoothGradientT(angle2, phaseAngle) : 0.0F;
            float tApex = gradientEnabled ? this.smoothGradientT(midAngle, phaseAngle) : 0.0F;
            Color c1 = gradientEnabled ? this.blendColors(baseColor, altColor, t1) : baseColor;
            Color c2 = gradientEnabled ? this.blendColors(baseColor, altColor, t2) : baseColor;
            Color cApex = gradientEnabled ? this.blendColors(baseColor, altColor, tApex) : baseColor;
            float x1 = radius * (float)Math.cos(angle1);
            float z1 = radius * (float)Math.sin(angle1);
            float x2 = radius * (float)Math.cos(angle2);
            float z2 = radius * (float)Math.sin(angle2);
            buffer.vertex(matrix, x1, 0.0F, z1).color(c1.getRed() / 255.0F, c1.getGreen() / 255.0F, c1.getBlue() / 255.0F, apexA);
            buffer.vertex(matrix, x2, 0.0F, z2).color(c2.getRed() / 255.0F, c2.getGreen() / 255.0F, c2.getBlue() / 255.0F, apexA);
            buffer.vertex(matrix, 0.0F, coneHeight, 0.0F).color(cApex.getRed() / 255.0F, cApex.getGreen() / 255.0F, cApex.getBlue() / 255.0F, apexA);
            buffer.vertex(matrix, x2, 0.0F, z2).color(c2.getRed() / 255.0F, c2.getGreen() / 255.0F, c2.getBlue() / 255.0F, apexA);
            buffer.vertex(matrix, x1, 0.0F, z1).color(c1.getRed() / 255.0F, c1.getGreen() / 255.0F, c1.getBlue() / 255.0F, apexA);
            buffer.vertex(matrix, 0.0F, coneHeight, 0.0F).color(cApex.getRed() / 255.0F, cApex.getGreen() / 255.0F, cApex.getBlue() / 255.0F, apexA);
        }

        AuraRender.drawColor(buffer, AuraRender.POS_COLOR_TRIS);
        buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Color bottomCenter = gradientEnabled ? this.blendColors(baseColor, altColor, 0.5F) : baseColor;
        buffer.vertex(matrix, 0.0F, 0.0F, 0.0F).color(bottomCenter.getRed() / 255.0F, bottomCenter.getGreen() / 255.0F, bottomCenter.getBlue() / 255.0F, apexA);

        for (int i = 0; i <= segments; i++) {
            float angle = (float)((Math.PI * 2) * i / segments);
            float x = radius * (float)Math.cos(angle);
            float z = radius * (float)Math.sin(angle);
            float t = gradientEnabled ? this.smoothGradientT(angle, phaseAngle) : 0.0F;
            Color c = gradientEnabled ? this.blendColors(baseColor, altColor, t) : baseColor;
            buffer.vertex(matrix, x, 0.0F, z).color(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, apexA);
        }

        AuraRender.drawColor(buffer, AuraRender.POS_COLOR_TRI_FAN);
        buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        for (int var36 = 0; var36 <= segments; var36++) {
            float angle = (float)((Math.PI * 2) * var36 / segments);
            float x = radius * (float)Math.cos(angle);
            float z = radius * (float)Math.sin(angle);
            float t = gradientEnabled ? this.smoothGradientT(angle, phaseAngle) : 0.0F;
            Color c = gradientEnabled ? this.blendColors(baseColor, altColor, t) : baseColor;
            buffer.vertex(matrix, x, 0.0F, z).color(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, 1.0F);
        }

        AuraRender.drawColorLines(buffer, AuraRender.POS_COLOR_LINE_STRIP, 1.0F);
    }

    float smoothGradientT(float angle, float phaseAngle) {
        return (float)((1.0 + Math.cos(angle + phaseAngle)) * 0.5);
    }

    Color blendColors(Color a, Color b, float t) {
        float clamped = MathHelper.clamp(t, 0.0F, 1.0F);
        int r = (int)(a.getRed() + (b.getRed() - a.getRed()) * clamped);
        int g = (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * clamped);
        int bl = (int)(a.getBlue() + (b.getBlue() - a.getBlue()) * clamped);
        int al = (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * clamped);
        return new Color(r, g, bl, al);
    }

    void appendGlowQuad(BufferBuilder buffer, Matrix4f matrix, Color color, int alpha, float halfSize) {
        int clampedAlpha = MathHelper.clamp(alpha, 0, 255);
        buffer.vertex(matrix, -halfSize, halfSize, 0.0F).texture(0.0F, 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), clampedAlpha);
        buffer.vertex(matrix, -halfSize, -halfSize, 0.0F).texture(0.0F, 1.0F).color(color.getRed(), color.getGreen(), color.getBlue(), clampedAlpha);
        buffer.vertex(matrix, halfSize, -halfSize, 0.0F).texture(1.0F, 1.0F).color(color.getRed(), color.getGreen(), color.getBlue(), clampedAlpha);
        buffer.vertex(matrix, halfSize, halfSize, 0.0F).texture(1.0F, 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), clampedAlpha);
    }

    void renderGhosts(WorldRenderContext context, Vec3d centerPos) {
        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getCameraPos();
        float speedValue = this.spiritsSpeed.getValue().floatValue() * 4.0F;
        this.ghostAngle = this.ghostAngle + speedValue * this.spiritsDelta * 60.0F;
        if (this.ghostAngle > 360.0F) {
            this.ghostAngle -= 360.0F;
        }

        int ghostCount = 3;

        while (this.ghosts.size() < ghostCount) {
            this.ghosts.add(new Hats$Ghost());
        }

        while (this.ghosts.size() > ghostCount) {
            this.ghosts.remove(this.ghosts.size() - 1);
        }

        double radius = 0.4;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int c = this.getSpiritsColor();
        int r = c >> 16 & 0xFF;
        int g = c >> 8 & 0xFF;
        int b = c & 0xFF;
        boolean hasVertices = false;
        ArrayList<float[]> glowPoints = new ArrayList<>();

        for (int i = 0; i < ghostCount; i++) {
            Hats$Ghost ghost = this.ghosts.get(i);
            double angleOffset = 360.0 / ghostCount * i;
            double angleRad = Math.toRadians(this.ghostAngle + angleOffset);
            double x = centerPos.x + Math.cos(angleRad) * radius;
            double z = centerPos.z + Math.sin(angleRad) * radius;
            double y = centerPos.y;
            Vec3d currentGhostPos = new Vec3d(x, y, z);
            ghost.update(currentGhostPos);
            List history = ghost.history;
            int historySize = history.size();

            for (int j = 0; j < historySize; j++) {
                float pointAlpha = (float)j / historySize;
                if (!(pointAlpha < 0.05F)) {
                    int alphaVal = (int)(255.0F * pointAlpha);
                    double anglePerTick = speedValue;
                    double historyAngle = angleRad - Math.toRadians(anglePerTick * (historySize - 1 - j));
                    double tx = centerPos.x + Math.cos(historyAngle) * radius;
                    double tz = centerPos.z + Math.sin(historyAngle) * radius;
                    double ty = centerPos.y;
                    Vec3d trailPos = new Vec3d(tx, ty, tz);
                    Vec3d pos = trailPos.subtract(cameraPos);
                    matrices.push();
                    matrices.translate(pos.x, pos.y, pos.z);
                    matrices.multiply(context.camera().getRotation());
                    float scale = 0.45F * this.spiritsSize.getValue().floatValue() * (0.5F + 0.5F * pointAlpha);
                    matrices.scale(scale, scale, scale);
                    Matrix4f matrix = matrices.peek().getPositionMatrix();
                    float w = 0.5F;
                    float h = 0.5F;
                    buffer.vertex(matrix, -w, h, 0.0F).texture(0.0F, 0.0F).color(r, g, b, alphaVal);
                    buffer.vertex(matrix, -w, -h, 0.0F).texture(0.0F, 1.0F).color(r, g, b, alphaVal);
                    buffer.vertex(matrix, w, -h, 0.0F).texture(1.0F, 1.0F).color(r, g, b, alphaVal);
                    buffer.vertex(matrix, w, h, 0.0F).texture(1.0F, 0.0F).color(r, g, b, alphaVal);
                    hasVertices = true;
                    matrices.pop();
                    if (this.glow.getValue() && j % 4 == 0) {
                        glowPoints.add(new float[]{(float)pos.x, (float)pos.y, (float)pos.z, pointAlpha});
                    }
                }
            }
        }

        if (hasVertices) {
            AuraRender.drawTextured(buffer, AuraRender.POS_TEX_COLOR_QUADS, GLOW_TEXTURE);
        }

        if (!glowPoints.isEmpty()) {
            BufferBuilder glowBuf = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            float glowScale = this.spiritsSize.getValue().floatValue() * 0.5F;

            for (float[] gp : glowPoints) {
                matrices.push();
                matrices.translate(gp[0], gp[1], gp[2]);
                matrices.multiply(context.camera().getRotation());
                matrices.scale(glowScale, glowScale, glowScale);
                Matrix4f gm = matrices.peek().getPositionMatrix();
                int ga = (int)(255.0F * gp[3] * 0.18F);
                if (ga < 1) {
                    ga = 1;
                }

                glowBuf.vertex(gm, -1.0F, 1.0F, 0.0F).texture(0.0F, 0.0F).color(r, g, b, ga);
                glowBuf.vertex(gm, -1.0F, -1.0F, 0.0F).texture(0.0F, 1.0F).color(r, g, b, ga);
                glowBuf.vertex(gm, 1.0F, -1.0F, 0.0F).texture(1.0F, 1.0F).color(r, g, b, ga);
                glowBuf.vertex(gm, 1.0F, 1.0F, 0.0F).texture(1.0F, 0.0F).color(r, g, b, ga);
                matrices.pop();
            }

            try {
                AuraRender.drawTextured(glowBuf, AuraRender.POS_TEX_COLOR_QUADS_ADD, GLOW_TEXTURE);
            } catch (Exception var51) {
            }
        }
    }

    public static void renderInGui(DrawContext context, LivingEntity entity) {
        Hats module = ModuleManager.INSTANCE.getModule(Hats.class);
        if (module != null && module.isEnabled()) {
            MatrixStack matrices = AuraRender.guiStack(context);
            Immediate consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
            matrices.push();
            String currentMode = module.mode.getValue();
            matrices.pop();
        }
    }

    void renderCrown(MatrixStack matrices, VertexConsumerProvider consumers, float entityHeight) {
        VertexConsumer buffer = consumers.getBuffer(RenderLayers.lightning());
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float time = (float)(System.currentTimeMillis() % 2000L) / 1000.0F;
        float yOffset = entityHeight + 0.12F + MathHelper.sin(time * (float) Math.PI) * 0.02F;
        int c = this.crownCustomColor.getValue() ? this.crownColor.getValue() : ThemeManager.INSTANCE.getVisualModulesColor();
        float a = 1.0F;
        float r = (c >> 16 & 0xFF) / 255.0F;
        float g = (c >> 8 & 0xFF) / 255.0F;
        float b = (c & 0xFF) / 255.0F;
        int segments = 8;
        float radius = 0.25F;
        float height = 0.25F;
        matrices.push();

        try {
            long time2 = System.currentTimeMillis() % 3600L;
            float rotation = (float)time2 / 10.0F;
            matrices.translate(0.0F, yOffset, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
            matrix = matrices.peek().getPositionMatrix();

            for (int i = 0; i < segments; i++) {
                float angle1 = (float)(i * 2 * Math.PI / segments);
                float angle2 = (float)((i + 1) * 2 * Math.PI / segments);
                float x1 = MathHelper.cos(angle1) * radius;
                float z1 = MathHelper.sin(angle1) * radius;
                float x2 = MathHelper.cos(angle2) * radius;
                float z2 = MathHelper.sin(angle2) * radius;
                float bandHeight = 0.12F;
                buffer.vertex(matrix, x1, 0.0F, z1).color(r, g, b, a).normal(0.0F, 0.0F, 1.0F);
                buffer.vertex(matrix, x1, bandHeight, z1).color(r, g, b, a).normal(0.0F, 0.0F, 1.0F);
                buffer.vertex(matrix, x2, bandHeight, z2).color(r, g, b, a).normal(0.0F, 0.0F, 1.0F);
                buffer.vertex(matrix, x2, 0.0F, z2).color(r, g, b, a).normal(0.0F, 0.0F, 1.0F);
                buffer.vertex(matrix, x2, 0.0F, z2).color(r, g, b, a).normal(0.0F, 0.0F, -1.0F);
                buffer.vertex(matrix, x2, bandHeight, z2).color(r, g, b, a).normal(0.0F, 0.0F, -1.0F);
                buffer.vertex(matrix, x1, bandHeight, z1).color(r, g, b, a).normal(0.0F, 0.0F, -1.0F);
                buffer.vertex(matrix, x1, 0.0F, z1).color(r, g, b, a).normal(0.0F, 0.0F, -1.0F);
                float midAngle = (angle1 + angle2) / 2.0F;
                float spikeX = MathHelper.cos(midAngle) * radius;
                float spikeZ = MathHelper.sin(midAngle) * radius;
                buffer.vertex(matrix, x1, bandHeight, z1).color(r, g, b, a).normal(0.0F, 0.0F, 1.0F);
                buffer.vertex(matrix, spikeX, bandHeight + height, spikeZ).color(r, g, b, a).normal(0.0F, 0.0F, 1.0F);
                buffer.vertex(matrix, x2, bandHeight, z2).color(r, g, b, a).normal(0.0F, 0.0F, 1.0F);
                buffer.vertex(matrix, x2, bandHeight, z2).color(r, g, b, a).normal(0.0F, 0.0F, 1.0F);
                buffer.vertex(matrix, x2, bandHeight, z2).color(r, g, b, a).normal(0.0F, 0.0F, -1.0F);
                buffer.vertex(matrix, spikeX, bandHeight + height, spikeZ).color(r, g, b, a).normal(0.0F, 0.0F, -1.0F);
                buffer.vertex(matrix, x1, bandHeight, z1).color(r, g, b, a).normal(0.0F, 0.0F, -1.0F);
                buffer.vertex(matrix, x1, bandHeight, z1).color(r, g, b, a).normal(0.0F, 0.0F, -1.0F);
            }
        } finally {
            matrices.pop();
        }
    }

    int getSpiritsColor() {
        return this.spiritsCustomColor.getValue() ? this.spiritsColor.getValue() : ThemeManager.INSTANCE.getVisualModulesColor();
    }

    void updateSpiritsDelta() {
        long now = System.nanoTime();
        if (this.lastSpiritsTimeNs == 0L) {
            this.lastSpiritsTimeNs = now;
            this.spiritsDelta = 0.016666668F;
        } else {
            float delta = (float)(now - this.lastSpiritsTimeNs) / 1.0E9F;
            this.lastSpiritsTimeNs = now;
            this.spiritsDelta = MathHelper.clamp(delta, 0.0F, 0.1F);
        }
    }
}
