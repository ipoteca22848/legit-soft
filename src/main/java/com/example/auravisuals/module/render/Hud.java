package com.example.auravisuals.module.render;

import com.example.auravisuals.client.gui.HudAiEditorScreen;
import com.example.auravisuals.config.HudPresetManager;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.ActionSetting;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.setting.StringSetting;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;

public class Hud extends Module {
    private static final String HUD_STYLE_CLASSIC = "Основной";
    private static final String HUD_STYLE_LIQUID_GLASS = "Жидкое стекло";
    private static boolean creatingPreset;
    private static String appliedPreset;
    public static final ModeSetting presetMode = new ModeSetting("Режим", "Основной", "Основной");
    public static final StringSetting presetName = new StringSetting("Название режима", "", () -> creatingPreset);
    public static final ActionSetting createPreset = new ActionSetting("Создать свой режим", Hud::handleCreatePreset);
    public static final ActionSetting editPreset = new ActionSetting(
        "Редактировать HUD", Hud::openPresetEditor, () -> !creatingPreset && !presetMode.is("Основной")
    );
    public static final ModeSetting hudStyle = new ModeSetting("Стиль", "Основной", "Основной", "Жидкое стекло");
    public static final ColorSetting hudGlassColor = new ColorSetting("Цвет стекла", new Color(255, 176, 158, 72), Hud::isLiquidGlassStyle);
    public static final BooleanSetting hudGlassGlow = new BooleanSetting("HUD Edge Glow", true, Hud::isLiquidGlassStyle);
    public static final NumberSetting hudGlowStrength = new NumberSetting(
        "HUD Glow Strength", 1.0, 0.25, 2.0, 0.05, () -> isLiquidGlassStyle() && hudGlassGlow.getValue()
    );
    public static final BooleanSetting stableEffects = new BooleanSetting("Спокойные эффекты HUD",true);
    public static final BooleanSetting customHotbar = new BooleanSetting("Хотбар", true);
    public static final BooleanSetting armorHud = new BooleanSetting("Броня", true);
    public static final BooleanSetting watermark = new BooleanSetting("Ватермарка", true);
    public static final ModeSetting language = new ModeSetting("Язык", "Русский", "Русский", "Английский");
    public static final ColorSetting accentColor = new ColorSetting("Акцентный цвет", new Color(255, 78, 90));
    public static final BooleanSetting shadow = new BooleanSetting("Тень", true);
    public static final BooleanSetting blur = new BooleanSetting("Блюр", true);
    public static final BooleanSetting notifications = new BooleanSetting("Уведомления", true);
    public static final BooleanSetting target = new BooleanSetting("Таргет", true);
    public static final BooleanSetting hotkeys = new BooleanSetting("Хоткеи", true);
    public static final BooleanSetting potions = new BooleanSetting("Зелья", true);
    public static final BooleanSetting cooldowns = new BooleanSetting("Кулдауны", true);
    public static final BooleanSetting inventory = new BooleanSetting("Инвентарь", true);
    public static final BooleanSetting mediaPlayer = new BooleanSetting("Медиаплеер", false);
    private static double gpsX = 0.0;
    private static double gpsZ = 0.0;
    private static boolean gpsActive = false;

    public Hud() {
        super("Hud", "Настройки визуального интерфейса", Category.VISUALS);
        this.addSettings(
            stableEffects, presetMode, presetName, createPreset, editPreset, hudGlassGlow, hudGlowStrength,
            hudStyle,
            hudGlassColor,
            watermark,
            customHotbar,
            armorHud,
            language,
            accentColor,
            shadow,
            blur,
            notifications,
            target,
            hotkeys,
            potions,
            cooldowns,
            inventory,
            mediaPlayer
        );
        this.setEnabledSilently(true);
    }

    public static boolean isLiquidGlassStyle() {
        return hudStyle.is("Жидкое стекло") || hudStyle.is("Liquid Glass");
    }

    public static void setHudStyle(String style) {
        if ("Жидкое стекло".equalsIgnoreCase(style) || "Liquid Glass".equalsIgnoreCase(style)) {
            hudStyle.setValue("Жидкое стекло");
        } else if ("Основной".equalsIgnoreCase(style) || "Classic".equalsIgnoreCase(style)) {
            hudStyle.setValue("Основной");
        }
    }

    public static boolean isEnglish() {
        return language.is("Английский");
    }

    public static String text(String russian, String english) {
        return isEnglish() ? english : russian;
    }

    private static void handleCreatePreset() {
        if (!creatingPreset) {
            creatingPreset = true;
            createPreset.setDisplayName("Создать");
        } else {
            String created = HudPresetManager.create(presetName.getValue());
            if (created != null) {
                presetMode.setModes(HudPresetManager.modes());
                presetMode.setValue(created);
                presetName.setValue("");
                creatingPreset = false;
                createPreset.setDisplayName("Создать свой режим");
            }
        }
    }

    private static void openPresetEditor() {
        if (!presetMode.is("Основной")) {
            MinecraftClient.getInstance().setScreen(new HudAiEditorScreen(presetMode.getValue()));
        }
    }

    public void disableGps() {
        gpsActive = false;
    }

    public void setGpsTarget(double x, double z) {
        gpsX = x;
        gpsZ = z;
        gpsActive = true;
    }

    public static boolean isGpsActive() {
        return gpsActive;
    }

    public static double getGpsX() {
        return gpsX;
    }

    public static double getGpsZ() {
        return gpsZ;
    }

    @Override
    public void onEnable() {
        com.example.auravisuals.client.gui.hud.Hud.resetAnimations();
    }

    @Override
    public void onTick() {
        String selected = presetMode.getValue();
        if (appliedPreset == null) {
            appliedPreset = selected;
            if (!selected.equalsIgnoreCase("Основной")) {
                HudPresetManager.apply(selected);
            }
        } else if (!selected.equalsIgnoreCase(appliedPreset)) {
            HudPresetManager.saveCurrent(appliedPreset);
            HudPresetManager.apply(selected);
            appliedPreset = selected;
        }
    }
}
