package com.example.auravisuals.mixin.world;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.BlockOverlay;
import com.example.auravisuals.module.render.CustomWorld;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true, require = 0)
    private void aura$hideVanillaBlockOutline(CallbackInfo ci) {
        if (BlockOverlay.shouldHideVanillaOutline()) {
            ci.cancel();
        }
    }

    @Inject(method = "checkEmpty", at = @At("HEAD"), cancellable = true, require = 0)
    private void aura$checkEmpty(MatrixStack matrices, CallbackInfo ci) {
        try {
            while (true) {
                if (!matrices.isEmpty()) {
                    matrices.pop();
                } else {
                    ci.cancel();
                    break;
                }
            }
        } catch (Throwable var4) {
        }
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true, require = 0)
    private void aura$hideClouds(CallbackInfo ci) {
        CustomWorld module = ModuleManager.INSTANCE.getModule(CustomWorld.class);
        if (CustomWorld.shouldHideClouds(module)) {
            ci.cancel();
        }
    }
}
