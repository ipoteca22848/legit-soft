package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.theme.ThemeManager;
import java.awt.Color;

public class HitColor extends Module {
    public static final int FULL_PAINT_ALPHA = 255;
    public static final BooleanSetting useCustomColor = new BooleanSetting("Свой цвет", false);
    public static final ColorSetting color = new ColorSetting("Цвет удара", new Color(255, 0, 0, 100), () -> useCustomColor.getValue());
    public static final ModeSetting paintMode = new ModeSetting("Режим покраски", "Оттенок", "Оттенок", "Полная");
    public static final NumberSetting transparency = new NumberSetting("Прозрачность", 100.0, 0.0, 255.0, 1.0, () -> !paintMode.is("Полная"));
    public static final BooleanSetting colorArmor = new BooleanSetting("Красить броню", true);
    public static volatile boolean armorTintActive = false;
    public static volatile int armorTintColorValue = -1;

    public HitColor() {
        super("HitColor", "Изменяет цвет сущности при получении урона", Category.VISUALS);
        this.addSettings(useCustomColor, color, paintMode, transparency, colorArmor);
    }

    public static boolean isFullPaintMode() {
        return paintMode.is("Полная");
    }

    public int getColorValue() {
        Color target = useCustomColor.getValue() ? color.getColor() : new Color(ThemeManager.INSTANCE.getVisualModulesColor(), true);
        int alpha = isFullPaintMode() ? 255 : transparency.getIntValue();
        return new Color(target.getRed(), target.getGreen(), target.getBlue(), alpha).getRGB();
    }
}
