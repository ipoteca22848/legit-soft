package com.example.auravisuals.module.render;

import com.example.auravisuals.setting.ColorSetting;
import java.awt.Color;
import java.util.function.Supplier;

public class JumpCircle$1 extends ColorSetting {
    JumpCircle$1(String name, Color defaultColor, Supplier visibility) {
        super(name, defaultColor, visibility);
    }

    @Override
    public String getDisplayName() {
        return JumpCircle.mode.is("Пятиугольник") ? "Цвет 1" : super.getDisplayName();
    }
}
