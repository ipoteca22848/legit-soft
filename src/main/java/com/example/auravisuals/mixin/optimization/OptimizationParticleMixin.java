package com.example.auravisuals.mixin.optimization;

import com.example.auravisuals.module.render.Optimization;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public abstract class OptimizationParticleMixin {
    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void aura$cullDistantParticles(Particle particle, CallbackInfo ci) {
        Box box = particle.getBoundingBox();
        double x = (box.minX + box.maxX) * 0.5;
        double y = (box.minY + box.maxY) * 0.5;
        double z = (box.minZ + box.maxZ) * 0.5;
        if (!Optimization.allowParticle(x, y, z)) {
            ci.cancel();
        }
    }
}
