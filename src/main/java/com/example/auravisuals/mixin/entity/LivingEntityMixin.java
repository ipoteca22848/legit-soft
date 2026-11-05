package com.example.auravisuals.mixin.entity;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.JumpCircle;
import com.example.auravisuals.module.render.KillEffect;
import com.example.auravisuals.module.render.Particles;
import com.example.auravisuals.module.render.SwingAnimation;
import com.example.auravisuals.module.render.Wave;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true, require = 0)
    private void onGetHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
        SwingAnimation swingAnimation;
        if ((Object)this == MinecraftClient.getInstance().player
            && (swingAnimation = ModuleManager.INSTANCE.getModule(SwingAnimation.class)) != null
            && swingAnimation.isEnabled()) {
            cir.setReturnValue(SwingAnimation.getSwingDurationOverride());
        }
    }

    @Inject(method = "jump()V", at = @At("HEAD"), require = 0)
    private void onJump(CallbackInfo ci) {
        if ((Object)this == MinecraftClient.getInstance().player) {
            JumpCircle jumpCircle = ModuleManager.INSTANCE.getModule(JumpCircle.class);
            if (jumpCircle != null && jumpCircle.isEnabled()) {
                jumpCircle.onJump();
            }

            Wave wave;
            if ((wave = ModuleManager.INSTANCE.getModule(Wave.class)) != null && wave.isEnabled()) {
                wave.onJump();
            }

            Particles particles;
            if ((particles = ModuleManager.INSTANCE.getModule(Particles.class)) != null && particles.isEnabled()) {
                LivingEntity self = (LivingEntity)(Object)this;
                particles.onEvent("Прыжок", self.getEntityPos().add(0.0, 0.1, 0.0));
            }
        }
    }

    @Inject(method = "handleStatus", at = @At("HEAD"), require = 0)
    private void auravisuals$onHandleStatus(byte status, CallbackInfo ci) {
        KillEffect killEffect;
        if (status == 3 && (killEffect = ModuleManager.INSTANCE.getModule(KillEffect.class)) != null && killEffect.isEnabled()) {
            killEffect.onEntityDeath((Entity)(Object)this);
        }
    }
}
