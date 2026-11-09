package com.anvil.client.mixin.performance;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.anvil.client.performance.PulseOptimization;

@Environment(EnvType.CLIENT)
@Mixin({BlockEntityRenderManager.class})
public class BlockEntityRenderManagerMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pulse$cullBlockEntities(
      BlockEntityRenderState var1, MatrixStack var2, OrderedRenderCommandQueue var3, CameraRenderState var4, CallbackInfo var5
   ) {
      if (PulseOptimization.instance()!=null && PulseOptimization.instance().a(var1.pos, var1.type)) {
         var5.cancel();
      }
   }
}
