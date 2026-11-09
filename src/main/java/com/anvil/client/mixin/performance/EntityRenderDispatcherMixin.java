package com.anvil.client.mixin.performance;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.anvil.client.performance.PulseOptimization;

@Environment(EnvType.CLIENT)
@Mixin({EntityRenderManager.class})
public class EntityRenderDispatcherMixin {
   @Inject(
      method = {"shouldRender"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private <E extends Entity> void pulse$cullHiddenEntities(E var1, Frustum var2, double var3, double var5, double var7, CallbackInfoReturnable<Boolean> var9) {
      if (var9.getReturnValueZ() && PulseOptimization.instance()!=null && PulseOptimization.instance().a(var1)) {
         var9.setReturnValue(false);
      }
   }
}
