package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.BooleanSetting;

public class NoFluids extends Module {
    public static final BooleanSetting lava = new BooleanSetting("Лава", true);
    public static final BooleanSetting water = new BooleanSetting("Вода", true);

    public NoFluids() {
        super("NoFluids", "Убирает жидкости", Category.VISUALS);
        this.addSettings(lava, water);
    }

    public static boolean isActive(BooleanSetting setting) {
        NoFluids module = ModuleManager.INSTANCE != null ? ModuleManager.INSTANCE.getModule(NoFluids.class) : null;
        return module != null && module.isEnabled() && setting.getValue();
    }
}
