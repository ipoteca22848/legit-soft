package com.example.auravisuals.module.player;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;

public class ShiftTap extends Module {
    private boolean isSneaking = false;
    private static int hitPlayerTicks = 0;

    public ShiftTap() {
        super("ShiftTap", "Автоматический Shift при ударе", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (this.mc.player != null) {
            if (hitPlayerTicks > 0) {
                if (!this.isSneaking) {
                    this.isSneaking = true;
                    this.mc.options.sneakKey.setPressed(true);
                }

                hitPlayerTicks--;
            } else {
                if (this.isSneaking) {
                    this.isSneaking = false;
                    this.mc.options.sneakKey.setPressed(false);
                }

                if (hitPlayerTicks > 0) {
                    hitPlayerTicks--;
                }
            }
        }
    }

    public static void onHitPlayer() {
        hitPlayerTicks = 2;
    }

    @Override
    public void onDisable() {
        if (this.mc.player != null) {
            this.mc.options.sneakKey.setPressed(false);
        }

        this.isSneaking = false;
        hitPlayerTicks = 0;
    }
}
