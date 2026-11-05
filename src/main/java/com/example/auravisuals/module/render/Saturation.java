package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.render.SaturationPipeline;

public class Saturation extends Module {
    public static final NumberSetting saturation = new NumberSetting("Насыщенность", 1.3, 0.0, 3.0, 0.05);
    public static final NumberSetting vibrance = new NumberSetting("Сочность", 0.2, 0.0, 1.0, 0.05);
    public static final NumberSetting contrast = new NumberSetting("Контраст", 1.05, 0.5, 2.0, 0.05);
    public static final NumberSetting brightness = new NumberSetting("Яркость", 1.0, 0.5, 1.5, 0.05);

    public Saturation() {
        super("Saturation", "Насыщенность и цвет экрана", Category.VISUALS);
        this.addSettings(saturation, vibrance, contrast, brightness);
    }

    public static void renderIfEnabled() {
        Saturation self = ModuleManager.INSTANCE != null ? ModuleManager.INSTANCE.getModule(Saturation.class) : null;
        if (self != null && self.isEnabled()) {
            try {
                SaturationPipeline.apply(Saturation.saturation.getFloatValue(), vibrance.getFloatValue(), contrast.getFloatValue(), brightness.getFloatValue());
            } catch (Throwable var3) {
            }
        }
    }
}
