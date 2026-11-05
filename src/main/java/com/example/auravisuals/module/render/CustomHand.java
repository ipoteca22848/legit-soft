package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.MultiBooleanSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.render.HandsFlamePipeline;
import java.awt.Color;

public class CustomHand extends Module {
    public static final MultiBooleanSetting hands = new MultiBooleanSetting("Руки", "Правая", "Левая");
    public static final BooleanSetting customColor = new BooleanSetting("Свой цвет", false, () -> false);
    public static final ColorSetting color = new ColorSetting("Цвет", new Color(255, 255, 255), () -> false);
    public static final NumberSetting textureAlpha = new NumberSetting("Прозрачность текстуры", 150.0, 0.0, 255.0, 1.0, () -> false);
    public static final BooleanSetting gradient = new BooleanSetting("Градиент", false, () -> false);
    public static final ColorSetting gradientColor = new ColorSetting("Цвет градиента", new Color(0, 150, 255), () -> false);
    public static final BooleanSetting blur = new BooleanSetting("Блюр", true);
    public static final BooleanSetting itemOutline = new BooleanSetting("Обводка контура", false);
    public static final BooleanSetting flameEffect = new BooleanSetting("Свечение", false);
    public static final ColorSetting itemOutlineColor = new ColorSetting("Цвет обводки", new Color(255, 255, 255), () -> itemOutline.getValue());
    public static final NumberSetting blurStrength = new NumberSetting("Сила блюра", 3.0, 1.0, 10.0, 1.0, () -> blur.getValue());
    public static final NumberSetting blurOpacity = new NumberSetting("Прозрачность блюра", 100.0, 5.0, 100.0, 5.0, () -> blur.getValue());
    public static final ColorSetting blurTint = new ColorSetting("Оттенок блюра", new Color(100, 150, 255, 100), () -> blur.getValue());
    public static final ModeSetting flameColorMode = new ModeSetting(
        "Режим цвета", "От предмета", () -> flameEffect.getValue(), "От предмета", "Кастом", "Разноцветный"
    );
    public static final ColorSetting flameColor = new ColorSetting(
        "Цвет", new Color(255, 255, 255, 230), () -> flameEffect.getValue() && flameColorMode.is("Кастом")
    );
    public static final NumberSetting flameStrength = new NumberSetting("Сила", 0.85, 0.0, 2.0, 0.05, () -> flameEffect.getValue());
    public static final NumberSetting flameRiseSpeed = new NumberSetting("Скорость подъёма", 0.0, 0.0, 2.0, 0.05, () -> flameEffect.getValue());
    public static final NumberSetting flameWobble = new NumberSetting("Колебание", 0.65, 0.0, 2.0, 0.05, () -> flameEffect.getValue());
    public static final NumberSetting flameLength = new NumberSetting("Длина", 0.95, 0.1, 2.5, 0.05, () -> flameEffect.getValue());
    public static final NumberSetting flameBrightness = new NumberSetting("Яркость", 0.9, 0.0, 2.0, 0.05, () -> flameEffect.getValue());

    public CustomHand() {
        super("CustomHand", "Кастомные руки", Category.VISUALS);
        hands.set("Правая", true);
        hands.set("Левая", true);
        this.addSettings(
            hands,
            customColor,
            color,
            textureAlpha,
            gradient,
            gradientColor,
            blur,
            blurStrength,
            blurOpacity,
            blurTint,
            itemOutline,
            itemOutlineColor,
            flameEffect,
            flameColorMode,
            flameColor,
            flameStrength,
            flameRiseSpeed,
            flameWobble,
            flameLength,
            flameBrightness
        );
    }

    @Override
    public void onEnable() {
        HandsFlamePipeline.clearError();
        this.syncFlame();
    }

    @Override
    public void onDisable() {
        HandsFlamePipeline.resetTrail();
    }

    @Override
    public void onTick() {
        this.syncFlame();
    }

    public boolean flameActive() {
        return flameEffect.getValue();
    }

    public void syncFlame() {
        if (this.flameActive()) {
            int colorModeId = flameColorMode.is("От предмета") ? 0 : 1;
            HandsFlamePipeline.configure(
                flameStrength.getFloatValue(),
                flameRiseSpeed.getFloatValue(),
                flameWobble.getFloatValue(),
                flameLength.getFloatValue(),
                flameBrightness.getFloatValue(),
                colorModeId,
                this.resolveFlameColor(),
                false,
                hands.isEnabled("Правая"),
                hands.isEnabled("Левая")
            );
        }
    }

    private int resolveFlameColor() {
        if (flameColorMode.is("Кастом")) {
            return flameColor.getValue();
        } else if (flameColorMode.is("Разноцветный")) {
            float hue = (float)(System.currentTimeMillis() % 4000L) / 4000.0F;
            return -436207616 | Color.HSBtoRGB(hue, 0.72F, 1.0F) & 16777215;
        } else {
            return -419430401;
        }
    }
}
