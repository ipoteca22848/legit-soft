package com.example.auravisuals.mixin.optimization;

import com.example.auravisuals.module.render.Optimization;
import net.minecraft.client.render.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class OptimizationEntityShadowMixin {
    @Inject(method = "updateShadow", at = @At("HEAD"), cancellable = true, require = 0)
    private void aura$skipShadow(CallbackInfo ci) {
        if (Optimization.hideEntityShadows()) {
            ci.cancel();
        }
    }
}
