package com.anvil.client.mixin.performance;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.anvil.client.performance.PulseOptimization;

@Environment(EnvType.CLIENT)
@Mixin({LightmapTextureManager.class})
public class LightmapTextureManagerMixin {
   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pulse$throttleLightmap(CallbackInfo var1) {
      if (PulseOptimization.instance() != null && PulseOptimization.instance().p()) {
         var1.cancel();
      }
   }

}
