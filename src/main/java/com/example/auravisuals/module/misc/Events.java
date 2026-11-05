package com.example.auravisuals.module.misc;

import com.example.auravisuals.client.gui.FunTimeEventsScreen;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BindSetting;
import net.minecraft.client.MinecraftClient;

public class Events extends Module {
    public final BindSetting openPanelBind = new BindSetting("Панель ивентов", -1);
    private boolean openPanelBindWasPressed;

    public Events() {
        super("Events", "Ивенты и шахты FunTime", Category.MISC);
        this.addSettings(this.openPanelBind);
    }

    @Override
    public void onTick() {
        if (this.isEnabled()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.player != null) {
                Integer code = this.openPanelBind.getValue();
                if (code != null && code != -1) {
                    boolean pressed = BindSetting.isPressed(mc.getWindow().getHandle(), code);
                    if (pressed && !this.openPanelBindWasPressed && !BindSetting.shouldBlockKeyInput()) {
                        mc.execute(() -> mc.setScreen(new FunTimeEventsScreen(mc.currentScreen)));
                    }

                    this.openPanelBindWasPressed = pressed;
                }
            }
        }
    }
}
