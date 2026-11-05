package com.example.auravisuals.module.render;

import com.example.auravisuals.setting.ColorSetting;
import java.awt.Color;
import java.util.function.Supplier;

public class SpeechBubbles$1 extends ColorSetting {
    SpeechBubbles$1(SpeechBubbles this$0, String name, Color defaultColor, Supplier visibility) {
        super(name, defaultColor, visibility);
    }

    @Override
    public String getDisplayName() {
        return "Цвет фона";
    }
}
