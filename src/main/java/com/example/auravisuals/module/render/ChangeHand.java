package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BindSetting;

public class ChangeHand extends Module {
    private final BindSetting swapBind = new BindSetting("Кнопка смены рук", -1);
    private boolean swapPressed;
    private boolean visuallySwapped;

    public ChangeHand() {
        super("ChangeHand", "Визуально меняет руки местами", Category.VISUALS);
        this.addSettings(this.swapBind);
    }

    @Override
    public void onDisable() {
        this.swapPressed = false;
        this.visuallySwapped = false;
    }

    @Override
    public void onTick() {
        if (this.mc.player == null) {
            this.swapPressed = false;
        } else {
            int key = this.swapBind.getValue();
            if (key != -1 && this.mc.currentScreen == null) {
                boolean pressed = BindSetting.isPressed(this.mc.getWindow().getHandle(), key);
                if (pressed && !this.swapPressed) {
                    this.visuallySwapped = !this.visuallySwapped;
                }

                this.swapPressed = pressed;
            } else {
                this.swapPressed = false;
            }
        }
    }

    public boolean isVisuallySwapped() {
        return this.isEnabled() && this.visuallySwapped;
    }
}
