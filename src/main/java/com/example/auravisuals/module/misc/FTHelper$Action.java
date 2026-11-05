package com.example.auravisuals.module.misc;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

public enum FTHelper$Action {
    DISORIENTATION(Items.ENDER_EYE),
    TRAP(Items.NETHERITE_SCRAP),
    BLATANT(Items.TNT),
    AURA(Items.DRIED_KELP),
    PLAST(Items.DRIED_KELP);

    public final Item item;

    FTHelper$Action(Item item) {
        this.item = item;
    }
}
