package com.anvil.client.mixin.performance;

import java.util.Deque;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.anvil.client.performance.PulseOptimization;

@Environment(EnvType.CLIENT)
@Mixin({ToastManager.class})
public class ToastManagerMixin {
   @Shadow
   @Final
   private List<?> visibleEntries;
   @Shadow
   @Final
   private Deque<Toast> toastQueue;

   @Inject(
      method = {"draw"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pulse$skipEmptyDraw(DrawContext var1, CallbackInfo var2) {
      if (this.visibleEntries.isEmpty() && this.toastQueue.isEmpty() && PulseOptimization.instance()!=null && PulseOptimization.instance().q()) {
         var2.cancel();
      }
   }
}
