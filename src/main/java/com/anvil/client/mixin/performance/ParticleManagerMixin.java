package com.anvil.client.mixin.performance;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.anvil.client.performance.PulseOptimization;


@Environment(EnvType.CLIENT)
@Mixin({ParticleManager.class})
public class ParticleManagerMixin {
   @Inject(
      method = {"addParticle(Lnet/minecraft/client/particle/Particle;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pulse$limitParticle(Particle var1, CallbackInfo var2) {
      if (var1 != null) {
         PulseOptimization var3 = PulseOptimization.instance();
         if (var3 != null && var3.n()) {
            Vec3d var4 = var1.getBoundingBox().getCenter();
            if (var3.a(var4.x, var4.y, var4.z)) {
               var2.cancel();
            }
         }
      }
   }

}
