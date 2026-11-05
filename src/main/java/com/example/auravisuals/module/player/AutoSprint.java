package com.example.auravisuals.module.player;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import net.minecraft.entity.effect.StatusEffects;

public class AutoSprint extends Module {
    private static int resetTicks = 0;

    public AutoSprint() {
        super("AutoSprint", "Автоматический спринт", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (this.mc.player != null) {
            if (resetTicks > 0) {
                this.mc.player.setSprinting(false);
                resetTicks--;
            } else if (!(this.mc.player.forwardSpeed <= 0.0F)) {
                if (this.mc.player.getHungerManager().getFoodLevel() > 6 || this.mc.player.getAbilities().allowFlying) {
                    if (!this.mc.player.isUsingItem()) {
                        if (!this.mc.player.isSneaking()) {
                            if (!this.mc.player.horizontalCollision) {
                                if (!this.mc.player.hasStatusEffect(StatusEffects.BLINDNESS)) {
                                    if (!this.mc.player.isTouchingWater() || !this.mc.player.isSubmergedInWater()) {
                                        this.mc.player.setSprinting(true);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void onAttack() {
        resetTicks = 1;
    }
}
