package com.example.auravisuals.module.player;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.MultiBooleanSetting;
import com.example.auravisuals.setting.NumberSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.potion.Potions;
import net.minecraft.util.Hand;

public class AutoEat extends Module {
    private final MultiBooleanSetting modes = new MultiBooleanSetting("Режимы", "Еда", "Инвиз");
    private final NumberSetting hungerThreshold = new NumberSetting("Голод", 14.0, 0.0, 19.0, 1.0);
    private int lastSlot = -1;
    private boolean eating = false;
    private boolean drinking = false;

    public AutoEat() {
        super("AutoEat", "Автоматически ест", Category.PLAYER);
        this.hungerThreshold.setDependency(() -> this.modes.isEnabled("Еда"));
        this.addSettings(this.modes, this.hungerThreshold);
    }

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.world != null) {
            if (this.modes.isEnabled("Еда") && !this.eating && !this.drinking) {
                this.handleFood();
            }

            if (this.modes.isEnabled("Инвиз") && !this.eating && !this.drinking) {
                this.handleInvis();
            }

            if ((this.eating || this.drinking) && !this.mc.player.isUsingItem()) {
                this.stopUsage();
            }
        }
    }

    private void handleFood() {
        int slot;
        if (this.mc.player.getHungerManager().getFoodLevel() <= this.hungerThreshold.getValue() && (slot = this.findFoodSlot()) != -1) {
            this.startUsage(slot, true);
        }
    }

    private void handleInvis() {
        boolean hasInvis = this.mc.player.hasStatusEffect(StatusEffects.INVISIBILITY);
        int duration = hasInvis ? this.mc.player.getStatusEffect(StatusEffects.INVISIBILITY).getDuration() : 0;
        int slot;
        if ((!hasInvis || duration <= 100) && (slot = this.findInvisPotionSlot()) != -1) {
            this.startUsage(slot, false);
        }
    }

    private void startUsage(int slot, boolean isFood) {
        this.lastSlot = this.mc.player.getInventory().getSelectedSlot();
        if (slot != this.lastSlot) {
            this.mc.player.getInventory().setSelectedSlot(slot);
            this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }

        this.mc.options.useKey.setPressed(true);
        this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
        if (isFood) {
            this.eating = true;
        } else {
            this.drinking = true;
        }
    }

    private void stopUsage() {
        this.mc.options.useKey.setPressed(false);
        if (this.lastSlot != -1 && this.lastSlot != this.mc.player.getInventory().getSelectedSlot()) {
            this.mc.player.getInventory().setSelectedSlot(this.lastSlot);
            this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.lastSlot));
        }

        this.lastSlot = -1;
        this.eating = false;
        this.drinking = false;
    }

    private int findFoodSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = this.mc.player.getInventory().getStack(i);
            if (stack.contains(DataComponentTypes.FOOD)) {
                return i;
            }
        }

        return -1;
    }

    private int findInvisPotionSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = this.mc.player.getInventory().getStack(i);
            PotionContentsComponent contents;
            if (stack.getItem() instanceof PotionItem && (contents = stack.get(DataComponentTypes.POTION_CONTENTS)) != null) {
                if (contents.matches(Potions.INVISIBILITY) || contents.matches(Potions.LONG_INVISIBILITY)) {
                    return i;
                }

                for (StatusEffectInstance effect : contents.getEffects()) {
                    if (effect.getEffectType() == StatusEffects.INVISIBILITY) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }
}
