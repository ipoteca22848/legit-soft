package com.example.auravisuals.module.render;

import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.render.WorldOcclusion;
import java.util.Comparator;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class ArmorDurabilityTag extends Module {
    private static final EquipmentSlot[] ARMOR_ORDER = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int TEXT_COLOR = -1;
    private static final int SHADOW_COLOR = -1342177280;
    private final NumberSetting scale = new NumberSetting("Scale", 1.0, 0.6, 1.8, 0.05);
    private final NumberSetting height = new NumberSetting("Height", 0.55, 0.2, 1.4, 0.05);
    private final NumberSetting range = new NumberSetting("Range", 8.0, 3.0, 16.0, 0.5);
    private PlayerEntity lockedTarget;

    public ArmorDurabilityTag() {
        super("ArmorDurability", "Shows armor durability above the current target", Category.VISUALS);
        this.addSettings(this.scale, this.height, this.range);
    }

    @Override
    public void onDisable() {
        this.lockedTarget = null;
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.mc.player != null && this.mc.world != null && this.mc.textRenderer != null) {
            float tickDelta = context.tickCounter().getTickProgress(true);
            PlayerEntity target = this.resolveTarget();
            if (target != null && target.isAlive()) {
                String[] values = this.armorDurability(target);
                if (values != null) {
                    Vec3d cameraPos = context.camera().getCameraPos();
                    if (WorldOcclusion.isEntityVisible(target, cameraPos, tickDelta)) {
                        Vec3d targetPos = target.getLerpedPos(tickDelta);
                        Vec3d renderPos = targetPos.add(0.0, target.getHeight() + this.height.getValue(), 0.0);
                        double x = renderPos.x - cameraPos.x;
                        double y = renderPos.y - cameraPos.y;
                        double z = renderPos.z - cameraPos.z;
                        MatrixStack matrices = context.matrixStack();
                        matrices.push();

                        try {
                            matrices.translate(x, y, z);
                            matrices.multiply(context.camera().getRotation());
                            float distance = (float)this.mc.player.getEntityPos().distanceTo(targetPos);
                            float textScale = 0.018F * this.scale.getFloatValue() * Math.max(0.75F, distance / 6.0F);
                            matrices.scale(-textScale, -textScale, textScale);
                            this.drawDurabilityRow(matrices, context.consumers(), values);
                        } finally {
                            matrices.pop();
                        }
                    }
                }
            }
        }
    }

    private PlayerEntity resolveTarget() {
        if (this.mc.player != null && this.mc.world != null) {
            Entity class_12972 = this.mc.targetedEntity;
            PlayerEntity pointed;
            if (class_12972 instanceof PlayerEntity && this.isValidTarget(pointed = (PlayerEntity)class_12972)) {
                this.lockedTarget = pointed;
                return pointed;
            }

            class_12972 = this.mc.player.getAttacking();
            PlayerEntity attacked;
            if (class_12972 instanceof PlayerEntity && this.isValidTarget(attacked = (PlayerEntity)class_12972)) {
                this.lockedTarget = attacked;
                return attacked;
            }

            if (this.lockedTarget != null && this.isValidTarget(this.lockedTarget)) {
                return this.lockedTarget;
            }

            this.lockedTarget = this.mc
                .world
                .getEntitiesByClass(PlayerEntity.class, this.mc.player.getBoundingBox().expand(this.range.getValue()), this::isValidTarget)
                .stream()
                .min(Comparator.comparingDouble(player -> player.squaredDistanceTo(this.mc.player)))
                .orElse(null);
            return this.lockedTarget;
        } else {
            return null;
        }
    }

    private boolean isValidTarget(PlayerEntity player) {
        if (player == null || player == this.mc.player || !player.isAlive() || player.isInvisible()) {
            return false;
        } else {
            return this.mc.player == null ? false : player.squaredDistanceTo(this.mc.player) <= this.range.getValue() * this.range.getValue();
        }
    }

    private String[] armorDurability(PlayerEntity player) {
        String[] values = new String[ARMOR_ORDER.length];
        boolean hasArmor = false;

        for (int i = 0; i < ARMOR_ORDER.length; i++) {
            ItemStack stack = player.getEquippedStack(ARMOR_ORDER[i]);
            if (stack != null && !stack.isEmpty() && stack.isDamageable()) {
                hasArmor = true;
                values[i] = Integer.toString(Math.max(0, stack.getMaxDamage() - stack.getDamage()));
            } else {
                values[i] = "0";
            }
        }

        return hasArmor ? values : null;
    }

    private void drawDurabilityRow(MatrixStack matrices, VertexConsumerProvider consumers, String[] values) {
        if (FontManager.customFontRenderer != null) {
            this.drawCustomFontRow(matrices, values);
        } else if (consumers != null) {
            TextRenderer renderer = this.mc.textRenderer;
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            int gap = 10;
            int totalWidth = -gap;

            for (String value : values) {
                totalWidth += renderer.getWidth(value) + gap;
            }

            float x = -totalWidth / 2.0F;

            for (String value : values) {
                renderer.draw(value, x + 1.0F, 1.0F, -1342177280, false, matrix, consumers, TextLayerType.NORMAL, 0, 15728880);
                renderer.draw(value, x, 0.0F, -1, false, matrix, consumers, TextLayerType.NORMAL, 0, 15728880);
                x += renderer.getWidth(value) + gap;
            }
        }
    }

    private void drawCustomFontRow(MatrixStack matrices, String[] values) {
        int gap = 10;
        int totalWidth = -gap;

        for (String value : values) {
            totalWidth += FontManager.getWidth(value) + gap;
        }

        float x = -totalWidth / 2.0F;

        for (String value : values) {
            FontManager.customFontRenderer.drawString(matrices, value, x + 1.0F, 1.0F, -1342177280, false);
            FontManager.customFontRenderer.drawString(matrices, value, x, 0.0F, -1, false);
            x += FontManager.getWidth(value) + gap;
        }
    }
}
