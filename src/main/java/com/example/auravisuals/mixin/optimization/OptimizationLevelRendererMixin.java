package com.example.auravisuals.mixin.optimization;

import com.example.auravisuals.module.render.Optimization;
import com.example.auravisuals.util.optimization.OcclusionCuller;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class OptimizationLevelRendererMixin {
    @Inject(method = "fillEntityRenderStates", at = @At("HEAD"), require = 0)
    private void aura$beginCullFrame(CallbackInfo ci) {
        OcclusionCuller.beginFrame();
    }

    @ModifyArg(method = "fillEntityRenderStates", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setRenderDistanceMultiplier(D)V"), index = 0, require = 0)
    private double aura$scaleEntityDistance(double original) {
        return Optimization.scaleEntityViewScale(original);
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true, require = 0)
    private void aura$skipClouds(CallbackInfo ci) {
        if (Optimization.cullClouds()) {
            ci.cancel();
        }
    }
}
