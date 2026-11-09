package com.anvil.client.mixin.performance;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.InactivityFpsLimiter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.anvil.client.performance.PulseOptimization;

@Environment(EnvType.CLIENT)
@Mixin({InactivityFpsLimiter.class})
public class InactivityFpsLimiterMixin {
   @Inject(
      method = {"update"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void pulse$dynamicFps(CallbackInfoReturnable<Integer> var1) {
      if(PulseOptimization.instance()!=null)var1.setReturnValue(PulseOptimization.instance().b(var1.getReturnValueI()));
   }
}
