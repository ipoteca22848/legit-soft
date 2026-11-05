package com.example.auravisuals.util.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class TotemUtil {
    private TotemUtil() {
    }

    public static boolean isEnchantedTotem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() != Items.TOTEM_OF_UNDYING) {
            return false;
        }

        if (stack.hasEnchantments()) {
            return true;
        }

        Boolean forcedGlint = stack.get(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
        if (Boolean.TRUE.equals(forcedGlint)) {
            return true;
        }

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            return true;
        }

        AttributeModifiersComponent attributes = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        return attributes != null && !attributes.modifiers().isEmpty();
    }
}
