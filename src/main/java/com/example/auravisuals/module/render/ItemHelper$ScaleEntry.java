package com.example.auravisuals.module.render;

import com.example.auravisuals.setting.ItemScaleSetting;
import com.example.auravisuals.setting.NumberSetting;
import net.minecraft.item.Item;

public class ItemHelper$ScaleEntry {
    public final Item item;
    public final ItemScaleSetting enabled;
    public final NumberSetting scale;

    public ItemHelper$ScaleEntry(Item item, ItemScaleSetting enabled, NumberSetting scale) {
        this.item = item;
        this.enabled = enabled;
        this.scale = scale;
    }
}
