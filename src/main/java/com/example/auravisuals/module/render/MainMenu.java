package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;

public class MainMenu extends Module {
    public MainMenu() {
        super("MainMenu", "Кастомное главное меню", Category.VISUALS);
        this.setEnabledSilently(true);
    }

    public static boolean shouldUseAuraMenu() {
        MainMenu module = ModuleManager.INSTANCE != null ? ModuleManager.INSTANCE.getModule(MainMenu.class) : null;
        return module == null || module.isEnabled();
    }
}
