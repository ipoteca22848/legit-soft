package com.example.auravisuals.module.player;

import com.example.auravisuals.mixin.accessor.MinecraftClientAccessor;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import net.minecraft.util.Hand;

public class TapeMouse extends Module {
    private final ModeSetting button = new ModeSetting("Кнопка", "Левая", "Левая", "Правая");
    private final NumberSetting delay = new NumberSetting("Задержка (сек)", 1.5, 0.5, 6.0, 0.1);
    private long lastClickTime = 0L;

    public TapeMouse() {
        super("TapeMouse", "Автоматический клик", Category.PLAYER);
        this.addSettings(this.button, this.delay);
    }

    @Override
    public void onEnable() {
        this.lastClickTime = System.currentTimeMillis();
    }

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.world != null) {
            double delayMs = this.delay.getValue() * 1000.0;
            if (System.currentTimeMillis() - this.lastClickTime >= delayMs) {
                this.performClick();
                this.lastClickTime = System.currentTimeMillis();
            }
        }
    }

    private void performClick() {
        if (this.button.is("Левая")) {
            ((MinecraftClientAccessor)this.mc).invokeDoAttack();
            this.mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            ((MinecraftClientAccessor)this.mc).invokeDoItemUse();
        }
    }
}
