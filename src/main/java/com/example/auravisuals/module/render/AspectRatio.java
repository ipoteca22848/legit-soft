package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;

public class AspectRatio extends Module {
    public static final ModeSetting mode = new ModeSetting("Mode", "16:9", "4:3", "16:9", "16:10", "Custom");
    public static final NumberSetting ratio = new NumberSetting("Ratio", 1.77, 0.1, 5.0, 0.01, () -> mode.is("Custom"));

    public AspectRatio() {
        super("AspectRatio", "Изменяет соотношение сторон экрана", Category.VISUALS);
        this.addSettings(mode, ratio);
    }

    public static float getAspectRatio() {
        if (ModuleManager.INSTANCE.getModule(AspectRatio.class) != null && ModuleManager.INSTANCE.getModule(AspectRatio.class).isEnabled()) {
            switch (mode.getValue()) {
                case "4:3":
                    return 1.3333334F;
                case "16:9":
                    return 1.7777778F;
                case "16:10":
                    return 1.6F;
                case "Custom":
                    return ratio.getValue().floatValue();
                default:
                    return -1.0F;
            }
        } else {
            return -1.0F;
        }
    }
}
