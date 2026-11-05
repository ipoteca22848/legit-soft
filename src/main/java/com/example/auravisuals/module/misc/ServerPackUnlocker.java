package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;

public class ServerPackUnlocker extends Module {
    public ServerPackUnlocker() {
        super("ServerPackUnlocker", "Allows local resource packs to stay above server packs", Category.MISC);
    }

    @Override
    public void onEnable() {
        clearServerPacks();
    }

    public static boolean shouldBlockServerPacks() {
        ServerPackUnlocker module = ModuleManager.INSTANCE.getModule(ServerPackUnlocker.class);
        return module != null && module.isEnabled();
    }

    public static void clearServerPacks() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getServerResourcePackProvider() != null) {
                client.getServerResourcePackProvider().removeAll();
            }
        } catch (Throwable var1) {
        }
    }
}
