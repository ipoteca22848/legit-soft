package com.example.auravisuals.mixin.optimization;

import com.example.auravisuals.module.render.Optimization;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameOptions.class)
public abstract class OptimizationOptionsMixin {
    @ModifyReturnValue(method = "getClampedViewDistance", at = @At("RETURN"), require = 0)
    private int aura$capRenderDistance(int original) {
        return Optimization.capEffectiveRenderDistance(original);
    }
}
