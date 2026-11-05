package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.render.FogBlurPipeline;
import net.minecraft.client.gl.Framebuffer;

public class FogBlur extends Module {
    private final NumberSetting strength = new NumberSetting("Сила", 15.0, 5.0, 20.0, 1.0);
    private final NumberSetting distance = new NumberSetting("Дистанция", 250.0, 100.0, 250.0, 1.0);
    private final ModeSetting quality = new ModeSetting("Качество", "Сбалансированно", "Сбалансированно", "Качественно");

    public FogBlur() {
        super("Fog Blur", "Размытие тумана", Category.VISUALS);
        this.addSettings(this.strength, this.distance, this.quality);
    }

    @Override
    public void onEnable() {
        FogBlurPipeline.clear();
    }

    @Override
    public void onDisable() {
        FogBlurPipeline.clear();
    }

    public static void renderIfEnabled() {
        renderIfEnabled(null);
    }

    public static void renderIfEnabled(Framebuffer target) {
        FogBlur self = ModuleManager.INSTANCE != null ? ModuleManager.INSTANCE.getModule(FogBlur.class) : null;
        if (self != null && self.isEnabled() && !FogBlurPipeline.isDisabledAfterError()) {
            CustomWorld customWorld = ModuleManager.INSTANCE.getModule(CustomWorld.class);
            float customFogStart = CustomWorld.shouldOverrideFog(customWorld) ? CustomWorld.fogStart.getFloatValue() : -1.0F;
            FogBlurPipeline.apply(
                target,
                self.strength.getFloatValue(),
                self.distance.getFloatValue(),
                self.quality.is("Качественно") ? 0.68F : 0.52F,
                self.quality.is("Качественно") ? 3 : 2,
                customFogStart
            );
        }
    }
}
