package com.example.auravisuals.mixin.entity;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.misc.FakePlayer;
import com.example.auravisuals.module.render.NoRender;
import com.example.auravisuals.module.render.Particles;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    private static Entity auravisuals$getActiveFakePlayerEntity() {
        FakePlayer mod = ModuleManager.INSTANCE.getModule(FakePlayer.class);
        return mod != null && mod.isEnabled() ? mod.getFakePlayer() : null;
    }

    @Inject(method = "collidesWith", at = @At("HEAD"), cancellable = true, require = 0)
    private void auravisuals$fakePlayerNoCollideBox(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (other != null) {
            Entity fake = auravisuals$getActiveFakePlayerEntity();
            if (fake != null) {
                Entity self = (Entity)(Object)this;
                if (self == fake || other == fake) {
                    cir.setReturnValue(false);
                }
            }
        }
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true, require = 0)
    private void auravisuals$fakePlayerNoPush(Entity other, CallbackInfo ci) {
        if (other != null) {
            Entity fake = auravisuals$getActiveFakePlayerEntity();
            if (fake != null) {
                Entity self = (Entity)(Object)this;
                if (self == fake || other == fake) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "handleStatus", at = @At("HEAD"), require = 0)
    private void onHandleStatus(byte status, CallbackInfo ci) {
        if (status == 35) {
            Particles particles = ModuleManager.INSTANCE.getModule(Particles.class);
            if (particles != null && particles.isEnabled()) {
                particles.spawnTotemParticles((Entity)(Object)this);
            }

            if (NoRender.isActive(NoRender.totemAnimation)) {
                return;
            }
        }
    }
}
