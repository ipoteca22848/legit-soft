package com.example.auravisuals.mixin.render;

import com.example.auravisuals.module.misc.CustomSword;
import com.example.auravisuals.module.misc.HWHelper;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemModelManager.class)
public abstract class ItemModelResolverMixin {
    @ModifyVariable(method = "updateForLivingEntity", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private ItemStack auravisuals$customSwordLiving(ItemStack stack) {
        return applyVisuals(stack);
    }

    @ModifyVariable(method = "updateForNonLivingEntity", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private ItemStack auravisuals$customSwordNonLiving(ItemStack stack) {
        return applyVisuals(stack);
    }

    @ModifyVariable(method = "clearAndUpdate", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private ItemStack auravisuals$customSwordClearAndUpdate(ItemStack stack) {
        return applyVisuals(stack);
    }

    @ModifyVariable(method = "update", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private ItemStack auravisuals$customSwordUpdate(ItemStack stack) {
        return applyVisuals(stack);
    }

    private static ItemStack applyVisuals(ItemStack stack) {
        ItemStack result = CustomSword.INSTANCE == null ? stack : CustomSword.INSTANCE.getRenderStack(stack);
        return HWHelper.INSTANCE == null ? result : HWHelper.INSTANCE.applyTalismanVisual(result);
    }
}
