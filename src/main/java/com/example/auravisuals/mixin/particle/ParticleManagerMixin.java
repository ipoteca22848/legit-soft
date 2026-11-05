package com.example.auravisuals.mixin.particle;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.NoRender;
import com.example.auravisuals.module.render.Particles;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true, require = 0)
    private void onAddParticle(
        ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir
    ) {
        Particles particlesModule = ModuleManager.INSTANCE.getModule(Particles.class);
        if (particlesModule != null
            && particlesModule.isEnabled()
            && Particles.triggers.isEnabled("Тотем")
            && parameters.getType() == ParticleTypes.TOTEM_OF_UNDYING) {
            cir.setReturnValue(null);
        } else {
            if (NoRender.shouldBlockParticle(parameters)) {
                cir.setReturnValue(null);
            }
        }
    }

    @Inject(method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void aura$noRenderEmitter(Entity entity, ParticleEffect parameters, CallbackInfo ci) {
        if (NoRender.shouldBlockParticle(parameters)) {
            ci.cancel();
        }
    }

    @Inject(method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;I)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void aura$noRenderEmitterCount(Entity entity, ParticleEffect parameters, int maxAge, CallbackInfo ci) {
        if (NoRender.shouldBlockParticle(parameters)) {
            ci.cancel();
        }
    }
}
