package com.example.auravisuals.mixin.particle;

import net.minecraft.client.particle.FireworksSparkParticle.Explosion;
import net.minecraft.client.particle.FireworksSparkParticle.FireworkParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FireworkParticle.class)
public class FireworksSparkFireworkParticleMixin {
    @Redirect(method = "addExplosionParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/FireworksSparkParticle$Explosion;setTrail(Z)V"))
    private void auravisuals$nullSafeSetTrail(Explosion explosion, boolean trail) {
        if (explosion != null) {
            explosion.setTrail(trail);
        }
    }

    @Redirect(method = "addExplosionParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/FireworksSparkParticle$Explosion;setFlicker(Z)V"))
    private void auravisuals$nullSafeSetFlicker(Explosion explosion, boolean flicker) {
        if (explosion != null) {
            explosion.setFlicker(flicker);
        }
    }
}
