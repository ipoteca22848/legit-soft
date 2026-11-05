package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.NumberSetting;

public class TimeChanger extends Module {
    public static final NumberSetting time = new NumberSetting("Время", 12000.0, 0.0, 24000.0, 100.0);

    public TimeChanger() {
        super("TimeChanger", "Изменяет время суток", Category.VISUALS);
        this.addSettings(time);
    }
}
