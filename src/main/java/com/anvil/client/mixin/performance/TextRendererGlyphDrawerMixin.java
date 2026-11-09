package com.anvil.client.mixin.performance;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.anvil.client.performance.PulseOptimization;


@Environment(EnvType.CLIENT)
@Mixin(
   targets = {"net/minecraft/client/font/TextRenderer$GlyphDrawer$1"}
)
public abstract class TextRendererGlyphDrawerMixin {
   @Unique
   private RenderLayer pulse$lastLayer;
   @Unique
   private VertexConsumer pulse$lastBuffer;
   @Unique
   private boolean pulse$batchChecked;
   @Unique
   private boolean pulse$batchEnabled;

   @Redirect(
      method = {"draw"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/VertexConsumerProvider;getBuffer(Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;"
      )
   )
   private VertexConsumer pulse$batchGetBuffer(VertexConsumerProvider var1, RenderLayer var2) {
      if (!this.pulse$batchChecked) {
         PulseOptimization var3 = PulseOptimization.instance();
         this.pulse$batchEnabled = var3 != null && var3.r();
         this.pulse$batchChecked = true;
      }

      if (!this.pulse$batchEnabled) {
         return var1.getBuffer(var2);
      } else if (this.pulse$lastLayer == var2 && this.pulse$lastBuffer != null) {
         return this.pulse$lastBuffer;
      } else {
         this.pulse$lastLayer = var2;
         this.pulse$lastBuffer = var1.getBuffer(var2);
         return this.pulse$lastBuffer;
      }
   }
}
