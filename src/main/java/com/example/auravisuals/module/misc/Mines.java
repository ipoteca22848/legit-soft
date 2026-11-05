package com.example.auravisuals.module.misc;

import com.example.auravisuals.client.gui.FunTimeEventsScreen;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import net.minecraft.client.MinecraftClient;

public class Mines extends Module {
    public Mines() {
        super("Mines", "Шахты FunTime", Category.MINES);
    }

    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            mc.execute(() -> mc.setScreen(new FunTimeEventsScreen(mc.currentScreen)));
        }

        this.setEnabledSilently(false);
    }
}
