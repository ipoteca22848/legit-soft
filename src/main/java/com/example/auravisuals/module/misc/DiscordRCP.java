package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.util.integration.DiscordRPC;

public class DiscordRCP extends Module {
    public DiscordRCP() {
        super("DiscordRPC", "Показывает статус игры в Discord (версия, сервер, кнопки)", Category.MISC);
    }

    @Override
    public void onEnable() {
        DiscordRPC.start();
    }

    @Override
    public void onDisable() {
        DiscordRPC.shutdown();
    }
}
