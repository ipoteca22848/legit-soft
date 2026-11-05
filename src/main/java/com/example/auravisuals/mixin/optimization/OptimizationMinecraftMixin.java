package com.example.auravisuals.mixin.optimization;

import com.example.auravisuals.module.render.Optimization;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftClient.class)
public abstract class OptimizationMinecraftMixin {
    @ModifyReturnValue(method = "usesImprovedTransparency", at = @At("RETURN"), require = 0)
    private static boolean aura$forceFastTransparency(boolean original) {
        return Optimization.shaderTransparency(original);
    }
}
