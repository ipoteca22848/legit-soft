package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.render.RenderUtil;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.item.TippedArrowItem;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.MathHelper;

public class PotionHighlight extends Module {
    public static final ModeSetting mode = new ModeSetting("Режим цвета", "Бафф / Дебафф", "Бафф / Дебафф", "Цвет эффекта");
    public static final ColorSetting buffColor = new ColorSetting("Цвет баффов", new Color(1146486664, true));
    public static final ColorSetting debuffColor = new ColorSetting("Цвет дебаффов", new Color(1442788181, true));
    public static final NumberSetting alpha = new NumberSetting("Прозрачность", 110.0, 0.0, 255.0, 5.0);
    public static final ModeSetting shape = new ModeSetting("Форма", "Квадрат", "Квадрат", "Круг");
    public static final BooleanSetting outline = new BooleanSetting("Обводка", true);
    public static final BooleanSetting tippedArrows = new BooleanSetting("Стрелы с эффектами", true);

    public PotionHighlight() {
        super("PotionHighlight", "Подсвечивает баффы и дебаффы зелий в инвентаре", Category.VISUALS);
        this.addSettings(mode, buffColor, debuffColor, alpha, shape, outline, tippedArrows);
        this.setEnabledSilently(true);
    }

    public static int getHighlightColor(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            boolean isPotion = stack.getItem() instanceof PotionItem;
            boolean isTippedArrow = stack.getItem() instanceof TippedArrowItem || stack.isOf(Items.TIPPED_ARROW);
            if (isPotion || isTippedArrow && tippedArrows.getValue()) {
                PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
                if (contents == null) {
                    return 0;
                }

                int a = MathHelper.clamp(alpha.getValue().intValue(), 0, 255);
                if (mode.is("Цвет эффекта")) {
                    int rawRgb = contents.getColor();
                    return a << 24 | rawRgb & 16777215;
                }

                boolean hasBeneficial = false;
                boolean hasHarmful = false;

                for (StatusEffectInstance effect : contents.getEffects()) {
                    StatusEffectCategory cat = effect.getEffectType().value().getCategory();
                    if (cat == StatusEffectCategory.HARMFUL) {
                        hasHarmful = true;
                    } else if (cat == StatusEffectCategory.BENEFICIAL) {
                        hasBeneficial = true;
                    }
                }

                if (!hasBeneficial && !hasHarmful && contents.potion().isPresent()) {
                    for (StatusEffectInstance effect : contents.potion().get().value().getEffects()) {
                        StatusEffectCategory cat = effect.getEffectType().value().getCategory();
                        if (cat == StatusEffectCategory.HARMFUL) {
                            hasHarmful = true;
                        } else if (cat == StatusEffectCategory.BENEFICIAL) {
                            hasBeneficial = true;
                        }
                    }
                }

                int targetRgb = hasHarmful ? debuffColor.getValue() : (hasBeneficial ? buffColor.getValue() : contents.getColor());
                return a << 24 | targetRgb & 16777215;
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public static void renderSlotHighlight(DrawContext context, Slot slot) {
        PotionHighlight module = ModuleManager.INSTANCE.getModule(PotionHighlight.class);
        if (module != null && module.isEnabled()) {
            ItemStack stack = slot.getStack();
            int color = getHighlightColor(stack);
            if (color != 0) {
                float radius = shape.is("Круг") ? 8.0F : 2.0F;
                RenderUtil.drawRoundedRect(context, slot.x, slot.y, 16.0F, 16.0F, radius, color);
                if (outline.getValue()) {
                    int outlineColor = Math.min(255, (color >>> 24) + 60) << 24 | color & 16777215;
                    RenderUtil.drawRoundedOutline(context, slot.x, slot.y, 16.0F, 16.0F, radius, 1.0F, outlineColor);
                }
            }
        }
    }
}
