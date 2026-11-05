package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.ModeSetting;

public final class Cosmetics extends Module {
    public final ModeSetting petPosition = new ModeSetting("Позиция питомца", "Пол", "Пол", "Плечо");

    public Cosmetics() {
        super("Cosmetics", "Настройки косметики (позиция питомца)", Category.VISUALS);
        this.addSettings(this.petPosition);
    }

    public static boolean petOnShoulder() {
        Cosmetics module = ModuleManager.INSTANCE == null ? null : ModuleManager.INSTANCE.getModule(Cosmetics.class);
        return module != null && module.petPosition.is("Плечо");
    }
}
