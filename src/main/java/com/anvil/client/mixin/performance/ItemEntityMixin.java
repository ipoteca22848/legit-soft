package com.anvil.client.mixin.performance;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.anvil.client.performance.PulseOptimization;

@Environment(EnvType.CLIENT)
@Mixin({ItemEntity.class})
public class ItemEntityMixin {
   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pulse$skipExcessItemTick(CallbackInfo var1) {
      ItemEntity var2 = (ItemEntity)(Object)this;
      if (var2.getEntityWorld().isClient() && PulseOptimization.instance()!=null && PulseOptimization.instance().a(var2)) {
         var1.cancel();
      }
   }
}
