package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ItemHighlightSetting;
import com.example.auravisuals.setting.ItemScaleSetting;
import com.example.auravisuals.setting.LabelSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.MultiBooleanSetting;
import com.example.auravisuals.setting.NumberSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class ItemHelper extends Module {
    public final MultiBooleanSetting mode;
    final NumberSetting transparency;
    final BooleanSetting pulsing;
    final ModeSetting bgShape;
    final List<ItemHighlightSetting> itemSettings = new ArrayList<>();
    final List<ItemHelper$ScaleEntry> scaleEntries = new ArrayList<>();

    public ItemHelper() {
        super("ItemHelper", "Подсветка и увеличение предметов", Category.VISUALS);
        this.mode = new MultiBooleanSetting("Режим", "Подсветка слотов", "Увеличение предметов");
        this.mode.set("Увеличение предметов", false);
        this.addSettings(this.mode);
        this.transparency = new NumberSetting("Прозрачность", 100.0, 0.0, 255.0, 1.0, () -> this.mode.isEnabled("Подсветка слотов"));
        this.pulsing = new BooleanSetting("Пульсация", false, () -> this.mode.isEnabled("Подсветка слотов"));
        this.bgShape = new ModeSetting("Фон", "Круг", () -> this.mode.isEnabled("Подсветка слотов"), "Круг", "Квадрат");
        this.addSettings(this.transparency, this.pulsing, this.bgShape);
        LabelSetting highlightLabel = new LabelSetting("Подсветка в инвентаре").centered();
        highlightLabel.setDependency(() -> this.mode.isEnabled("Подсветка слотов"));
        this.addSettings(highlightLabel);
        this.addHighlightItem("Чарка", Items.ENCHANTED_GOLDEN_APPLE, new Color(255, 100, 255).getRGB());
        this.addHighlightItem("Золотое яблоко", Items.GOLDEN_APPLE, new Color(255, 215, 0).getRGB());
        this.addHighlightItem("Эндер-жемчуг", Items.ENDER_PEARL, new Color(0, 255, 200).getRGB());
        this.addHighlightItem("Плод хоруса", Items.CHORUS_FRUIT, new Color(200, 100, 200).getRGB());
        this.addHighlightItem("Тотем", Items.TOTEM_OF_UNDYING, new Color(255, 255, 100).getRGB());
        this.addHighlightItem("Трапка", Items.NETHERITE_SCRAP, new Color(100, 80, 80).getRGB());
        this.addHighlightItem("Пласт", Items.DRIED_KELP, new Color(50, 150, 50).getRGB());
        this.addHighlightItem("Снежок", Items.SNOWBALL, new Color(255, 255, 255).getRGB());
        this.addHighlightItem("Пузырек опыта", Items.EXPERIENCE_BOTTLE, new Color(100, 255, 100).getRGB());
        LabelSetting scaleLabel = new LabelSetting("Увеличение предметов").centered();
        scaleLabel.setDependency(() -> this.mode.isEnabled("Увеличение предметов"));
        this.addSettings(scaleLabel);
        this.addScaleItem("Чарка", Items.ENCHANTED_GOLDEN_APPLE);
        this.addScaleItem("Золотое яблоко", Items.GOLDEN_APPLE);
        this.addScaleItem("Эндер-жемчуг", Items.ENDER_PEARL);
        this.addScaleItem("Плод хоруса", Items.CHORUS_FRUIT);
        this.addScaleItem("Тотем", Items.TOTEM_OF_UNDYING);
        this.addScaleItem("Трапка", Items.NETHERITE_SCRAP);
        this.addScaleItem("Пласт", Items.DRIED_KELP);
        this.addScaleItem("Снежок", Items.SNOWBALL);
        this.addScaleItem("Пузырек опыта", Items.EXPERIENCE_BOTTLE);
        this.addScaleItem("Незеритовая броня", Items.NETHERITE_HELMET);
        this.addScaleItem("Алмазная броня", Items.DIAMOND_HELMET);
    }

    void addHighlightItem(String name, Item item, int color) {
        ItemHighlightSetting setting = new ItemHighlightSetting(name, item, color);
        setting.setDependency(() -> this.mode.isEnabled("Подсветка слотов"));
        this.itemSettings.add(setting);
        this.addSettings(setting);
    }

    void addScaleItem(String name, Item item) {
        String scaleName = name + " (размер)";
        String sliderName = scaleName + " слайдер";
        ItemScaleSetting enabled = new ItemScaleSetting(scaleName, item, () -> this.mode.isEnabled("Увеличение предметов"));
        enabled.setDisplayName(name);
        NumberSetting scale = new NumberSetting(sliderName, 1.5, 0.5, 2.5, 0.1, () -> this.mode.isEnabled("Увеличение предметов") && enabled.getValue());
        scale.setDisplayName("Размер");
        this.scaleEntries.add(new ItemHelper$ScaleEntry(item, enabled, scale));
        this.addSettings(enabled, scale);
    }

    public float getScale(Item item) {
        if (!this.mode.isEnabled("Увеличение предметов")) {
            return 1.0F;
        }

        for (ItemHelper$ScaleEntry entry : this.scaleEntries) {
            if (entry.enabled.getValue() && itemMatchesScaleRepresentative(entry.item, item)) {
                return entry.scale.getValue().floatValue();
            }
        }

        return 1.0F;
    }

    static boolean itemMatchesScaleRepresentative(Item representative, Item stackItem) {
        if (representative == stackItem) {
            return true;
        } else if (representative == Items.NETHERITE_HELMET) {
            return stackItem == Items.NETHERITE_CHESTPLATE || stackItem == Items.NETHERITE_LEGGINGS || stackItem == Items.NETHERITE_BOOTS;
        } else {
            return representative != Items.DIAMOND_HELMET
                ? false
                : stackItem == Items.DIAMOND_CHESTPLATE || stackItem == Items.DIAMOND_LEGGINGS || stackItem == Items.DIAMOND_BOOTS;
        }
    }

    public int getColor(Item item) {
        if (!this.mode.isEnabled("Подсветка слотов")) {
            return -1;
        }

        for (ItemHighlightSetting setting : this.itemSettings) {
            if (setting.getItem() == item && setting.getValue()) {
                int color = setting.getColor();
                int alpha = this.transparency.getValue().intValue();
                if (this.pulsing.getValue()) {
                    long time = System.currentTimeMillis();
                    float pulse = (float)(Math.sin(time / 200.0) + 1.0) / 2.0F;
                    alpha = (int)(alpha * (0.3F + 0.7F * pulse));
                }

                return color & 16777215 | alpha << 24;
            }
        }

        return -1;
    }

    public boolean isHighlightMode() {
        return this.mode.isEnabled("Подсветка слотов");
    }

    public boolean isCircleShape() {
        return this.bgShape.is("Круг");
    }
}
