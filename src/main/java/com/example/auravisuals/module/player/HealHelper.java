package com.example.auravisuals.module.player;

import com.example.auravisuals.client.gui.hud.Notifications;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.NumberSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class HealHelper extends Module {
    public static final NumberSetting hungerThreshold = new NumberSetting("Сытость ниже", 17.0, 0.0, 20.0, 1.0);
    public static final NumberSetting healthThreshold = new NumberSetting("HP ниже", 12.0, 1.0, 20.0, 1.0);
    public static final BooleanSetting onlyWithFood = new BooleanSetting("Только если есть еда", true);
    public static final BooleanSetting hudWarning = new BooleanSetting("Текст на экране", true);
    public static final BooleanSetting notify = new BooleanSetting("Уведомления", true);
    private static final long NOTIFY_INTERVAL_MS = 4000L;
    private static final int REGEN_FOOD_LEVEL = 18;
    private boolean shouldEat = false;
    private long lastNotifyMs = 0L;

    public HealHelper() {
        super("HealHelper", "Напоминает поесть для хила", Category.PLAYER);
        this.addSettings(hungerThreshold, healthThreshold, onlyWithFood, hudWarning, notify);
    }

    @Override
    public void onDisable() {
        this.shouldEat = false;
    }

    @Override
    public void onTick() {
        this.shouldEat = false;
        if (this.mc.player != null && !this.mc.player.isCreative() && !this.mc.player.isSpectator()) {
            float health = this.mc.player.getHealth();
            int food = this.mc.player.getHungerManager().getFoodLevel();
            boolean hungry = food <= hungerThreshold.getIntValue();
            boolean lowHpNoRegen = health <= healthThreshold.getIntValue() && food < 18;
            boolean need = hungry || lowHpNoRegen;
            if (need) {
                if (!onlyWithFood.getValue() || this.hasFood()) {
                    this.shouldEat = true;
                    long now;
                    if (notify.getValue() && (now = System.currentTimeMillis()) - this.lastNotifyMs >= 4000L) {
                        this.lastNotifyMs = now;

                        try {
                            Notifications.postMessage(
                                lowHpNoRegen
                                    ? "HealHelper: поешь — HP " + (int)health + "/20, реген заблокирован"
                                    : "HealHelper: поешь — сытость " + food + "/20"
                            );
                        } catch (Throwable var10) {
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onHudRender(DrawContext context) {
        if (this.shouldEat && hudWarning.getValue() && this.mc.player != null) {
            String text = "⚠ Поешь чтобы отхилиться  ("
                + (int)this.mc.player.getHealth()
                + "/20 HP, "
                + this.mc.player.getHungerManager().getFoodLevel()
                + "/20)";
            int sw = this.mc.getWindow().getScaledWidth();
            int sh = this.mc.getWindow().getScaledHeight();
            int width = this.mc.textRenderer.getWidth(text);
            int x = (sw - width) / 2;
            int y = sh - 62;
            float pulse = (float)(0.6 + 0.4 * Math.sin(System.currentTimeMillis() / 250.0));
            int r = 255;
            int g = (int)(60.0F + 80.0F * pulse);
            int color = new Color(r, Math.min(255, g), 60).getRGB();
            context.drawText(this.mc.textRenderer, text, x, y, color, true);
        }
    }

    private boolean hasFood() {
        if (this.mc.player == null) {
            return false;
        }

        PlayerInventory inv = this.mc.player.getInventory();

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.contains(DataComponentTypes.FOOD)) {
                return true;
            }
        }

        return false;
    }
}
