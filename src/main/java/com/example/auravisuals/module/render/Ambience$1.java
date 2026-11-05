package com.example.auravisuals.module.render;

import com.example.auravisuals.setting.ModeSetting;

public class Ambience$1 extends ModeSetting {
    Ambience$1(String name, String defaultMode, String... modes) {
        super(name, defaultMode, modes);
    }

    @Override
    public void setValue(String mode) {
        super.setValue("Кометасолнце".equalsIgnoreCase(mode) ? "Комета" : mode);
    }
}
