package com.example.auravisuals.module.render;

import com.example.auravisuals.setting.ColorSetting;
import java.awt.Color;

public class TargetESP$1 extends ColorSetting {
    TargetESP$1(String name, Color defaultColor) {
        super(name, defaultColor);
    }

    @Override
    public String getDisplayName() {
        return TargetESP.usesTwoColors() ? "Цвет 1" : "Цвет";
    }
}
