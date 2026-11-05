package com.example.auravisuals.module.player;

import com.example.auravisuals.mixin.accessor.MinecraftClientAccessor;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.NumberSetting;
import net.minecraft.item.Items;

public class FastExp extends Module {
    private final NumberSetting delay = new NumberSetting("Задержка", 1.0, 0.0, 4.0, 1.0);

    public FastExp() {
        super("Fast Exp", "Быстрое использование пузырьков опыта", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (this.mc.player != null) {
            if ((
                    this.mc.player.getMainHandStack().getItem() == Items.EXPERIENCE_BOTTLE
                        || this.mc.player.getOffHandStack().getItem() == Items.EXPERIENCE_BOTTLE
                )
                && this.mc.options.useKey.isPressed()
                && this.mc instanceof MinecraftClientAccessor) {
                int d = this.delay.getValue().intValue();
                if (((MinecraftClientAccessor)this.mc).getItemUseCooldown() > d) {
                    ((MinecraftClientAccessor)this.mc).setItemUseCooldown(d);
                }
            }
        }
    }
}
