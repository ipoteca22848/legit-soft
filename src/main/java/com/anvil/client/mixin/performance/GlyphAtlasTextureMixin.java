package com.anvil.client.mixin.performance;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.GlyphAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import com.anvil.client.performance.PulseOptimization;

@Environment(EnvType.CLIENT)
@Mixin({GlyphAtlasTexture.class})
public abstract class GlyphAtlasTextureMixin {
   @Unique
   private static final int PULSE$BIG_SIZE = 1024;
   @Unique
   private Boolean pulse$bigAtlas;

   @Unique
   private boolean pulse$useBigAtlas() {
      if (this.pulse$bigAtlas == null) {
         this.pulse$bigAtlas = PulseOptimization.instance()!=null && PulseOptimization.instance().s();
      }

      return this.pulse$bigAtlas;
   }

   @ModifyConstant(
      method = {"*"},
      constant = {@Constant(
         intValue = 256
      )}
   )
   private int pulse$atlasSize(int var1) {
      return this.pulse$useBigAtlas() ? 1024 : var1;
   }

   @ModifyConstant(
      method = {"*"},
      constant = {@Constant(
         floatValue = 256.0F
      )}
   )
   private float pulse$atlasSizeF(float var1) {
      return this.pulse$useBigAtlas() ? 1024.0F : var1;
   }
}
