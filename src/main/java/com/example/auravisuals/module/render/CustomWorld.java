package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import java.awt.Color;

public class CustomWorld extends Module {
    public static final BooleanSetting useCustomTime = new BooleanSetting("Кастомное время", false);
    public static final NumberSetting time = new NumberSetting("Время", 12000.0, 0.0, 24000.0, 100.0, () -> useCustomTime.getValue());
    public static final BooleanSetting useCustomWeather = new BooleanSetting("Кастомная погода", false);
    public static final ModeSetting weatherMode = new ModeSetting("Погода", "Ясно", () -> useCustomWeather.getValue(), "Ясно", "Дождь", "Гроза", "Снег");
    public static final BooleanSetting clouds = new BooleanSetting("Облака", false);
    public static final BooleanSetting useCustomFogColor = new BooleanSetting("Туман", false);
    public static final ColorSetting fogColor = new ColorSetting("Цвет тумана", new Color(255, 255, 255), () -> useCustomFogColor.getValue());
    public static final NumberSetting fogStart = new NumberSetting("Начало тумана", 0.65, 0.0, 0.95, 0.05, () -> useCustomFogColor.getValue());
    public static final BooleanSetting useCustomSkyColor = new BooleanSetting("Небо", false);
    public static final ColorSetting skyColor = new ColorSetting("Цвет неба", new Color(90, 140, 255), () -> useCustomSkyColor.getValue());
    private static Color smoothFogColor = new Color(255, 255, 255);
    private static Color smoothSkyColor = new Color(90, 140, 255);

    public CustomWorld() {
        super("CustomWorld", "Настройка визуала мира", Category.VISUALS);
        this.addSettings(useCustomTime, time, useCustomWeather, weatherMode, clouds, useCustomFogColor, fogColor, fogStart, useCustomSkyColor, skyColor);
    }

    public Color getActiveFogColor() {
        smoothFogColor = lerpColor(smoothFogColor, fogColor.getColor(), 0.12F);
        return smoothFogColor;
    }

    public Color getActiveSkyColor() {
        Color target = useCustomSkyColor.getValue() ? skyColor.getColor() : this.getActiveFogColor();
        smoothSkyColor = lerpColor(smoothSkyColor, target, 0.12F);
        return smoothSkyColor;
    }

    public static boolean shouldOverrideTime(CustomWorld module) {
        return module != null && module.isEnabled() && useCustomTime.getValue();
    }

    public static boolean shouldOverrideFog(CustomWorld module) {
        return module != null && module.isEnabled() && useCustomFogColor.getValue();
    }

    public static boolean shouldOverrideSky(CustomWorld module) {
        return module != null && module.isEnabled() && useCustomSkyColor.getValue();
    }

    public static boolean shouldOverrideWeather(CustomWorld module) {
        return module != null && module.isEnabled() && useCustomWeather.getValue();
    }

    public static boolean shouldHideClouds(CustomWorld module) {
        return module != null && module.isEnabled() && !clouds.getValue();
    }

    public static float getRainGradientOverride() {
        return useCustomWeather.getValue() && !weatherMode.is("Ясно") ? 1.0F : 0.0F;
    }

    public static float getThunderGradientOverride() {
        return weatherMode.is("Гроза") ? 1.0F : 0.0F;
    }

    private static Color lerpColor(Color from, Color to, float speed) {
        float k = Math.max(0.0F, Math.min(1.0F, speed));
        int r = (int)(from.getRed() + (to.getRed() - from.getRed()) * k);
        int g = (int)(from.getGreen() + (to.getGreen() - from.getGreen()) * k);
        int b = (int)(from.getBlue() + (to.getBlue() - from.getBlue()) * k);
        int a = (int)(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * k);
        return new Color(r, g, b, a);
    }
}
