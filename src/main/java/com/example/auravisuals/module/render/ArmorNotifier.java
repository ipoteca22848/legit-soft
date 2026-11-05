package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.render.RenderUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EquipmentSlot.Type;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class ArmorNotifier extends Module {
    private final NumberSetting threshold = new NumberSetting("Порог (%)", 50.0, 1.0, 100.0, 1.0);
    private final BooleanSetting showPercentage = new BooleanSetting("Показывать процент", true);
    private final NumberSetting scale = new NumberSetting("Scale", 1.0, 0.5, 2.0, 0.05);
    private int posX = 20;
    private int posY = 20;
    private boolean dragging;
    private boolean wasMouseDown;
    private double dragOffsetX;
    private double dragOffsetY;

    public ArmorNotifier() {
        super("Armor Notifier", "Уведомление о низкой прочности брони", Category.VISUALS);
        this.addSettings(this.threshold, this.showPercentage);
    }

    @Override
    public void onHudRender(DrawContext context) {
        if (this.mc.player != null) {
            ArrayList<String> lines = new ArrayList<>();

            for (ItemStack stack : List.of(
                this.mc.player.getEquippedStack(EquipmentSlot.HEAD),
                this.mc.player.getEquippedStack(EquipmentSlot.CHEST),
                this.mc.player.getEquippedStack(EquipmentSlot.LEGS),
                this.mc.player.getEquippedStack(EquipmentSlot.FEET)
            )) {
                int n;
                int remaining;
                float percent;
                EquippableComponent equippable;
                if (!stack.isEmpty()
                    && (equippable = stack.get(DataComponentTypes.EQUIPPABLE)) != null
                    && equippable.slot().getType() == Type.HUMANOID_ARMOR
                    && stack.isDamageable()
                    && !((percent = (float)(remaining = (n = stack.getMaxDamage()) - stack.getDamage()) / n * 100.0F) > this.threshold.getValue())) {
                    String text = stack.getItem().getName().getString();
                    text = text + (this.showPercentage.getValue() ? " " + String.format("%.0f%%", percent) : " " + remaining);
                    lines.add(text);
                }
            }

            if (lines.isEmpty()) {
                this.dragging = false;
                this.wasMouseDown = false;
            } else {
                float drawScale = this.scale.getFloatValue();
                int maxWidth = 0;

                for (String string : lines) {
                    maxWidth = Math.max(maxWidth, this.mc.textRenderer.getWidth(string));
                }

                int lineHeight = 11;
                int n = maxWidth;
                int contentHeight = lines.size() * lineHeight;
                int boxWidth = (int)Math.ceil((n + 8) * drawScale);
                int boxHeight = (int)Math.ceil((contentHeight + 6) * drawScale);
                this.handleDragging(context, boxWidth, boxHeight);
                int bgColor = new Color(0, 0, 0, 110).getRGB();
                int outlineColor = new Color(255, 80, 80, this.mc.currentScreen instanceof ChatScreen ? 180 : 120).getRGB();
                context.getMatrices().pushMatrix();

                try {
                    context.getMatrices().translate(this.posX, this.posY);
                    context.getMatrices().scale(drawScale, drawScale);
                    context.getMatrices().translate(-this.posX, -this.posY);
                    context.fill(this.posX - 4, this.posY - 3, this.posX + n + 4, this.posY + contentHeight + 3, bgColor);
                    if (this.mc.currentScreen instanceof ChatScreen) {
                        RenderUtil.drawBorder(context, this.posX - 4, this.posY - 3, n + 8, contentHeight + 6, outlineColor);
                    }

                    int drawY = this.posY;

                    for (String string : lines) {
                        context.drawText(this.mc.textRenderer, string, this.posX, drawY, Color.RED.getRGB(), true);
                        drawY += lineHeight;
                    }
                } finally {
                    context.getMatrices().popMatrix();
                }
            }
        }
    }

    private void handleDragging(DrawContext context, int width, int height) {
        if (this.mc.currentScreen instanceof ChatScreen && this.mc.getWindow() != null) {
            double mouseX = this.mc.mouse.getX() * context.getScaledWindowWidth() / this.mc.getWindow().getWidth();
            double mouseY = this.mc.mouse.getY() * context.getScaledWindowHeight() / this.mc.getWindow().getHeight();
            boolean mouseDown = GLFW.glfwGetMouseButton(this.mc.getWindow().getHandle(), 0) == 1;
            if (mouseDown && !this.wasMouseDown) {
                if (mouseX >= this.posX && mouseX <= this.posX + width && mouseY >= this.posY && mouseY <= this.posY + height) {
                    this.dragging = true;
                    this.dragOffsetX = mouseX - this.posX;
                    this.dragOffsetY = mouseY - this.posY;
                }
            } else if (!mouseDown) {
                this.dragging = false;
            }

            if (this.dragging) {
                int maxX = Math.max(0, context.getScaledWindowWidth() - width);
                int maxY = Math.max(0, context.getScaledWindowHeight() - height);
                this.posX = (int)Math.max(0.0, Math.min(maxX, mouseX - this.dragOffsetX));
                this.posY = (int)Math.max(0.0, Math.min(maxY, mouseY - this.dragOffsetY));
            }

            this.wasMouseDown = mouseDown;
        } else {
            this.dragging = false;
            this.wasMouseDown = false;
        }
    }
}
