package com.example.auravisuals.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public final class Inputs {
    private Inputs() {
    }

    private static boolean down(int key) {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), key);
    }

    public static boolean hasShiftDown() {
        return down(340) || down(344);
    }

    public static boolean hasControlDown() {
        return down(341) || down(345) || down(343) || down(347);
    }

    public static boolean hasAltDown() {
        return down(342) || down(346);
    }
}
