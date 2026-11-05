package com.example.auravisuals.mixin.client;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.misc.AutoMessage;
import com.example.auravisuals.module.misc.Xeaklt;
import com.example.auravisuals.module.player.ShiftTap;
import com.example.auravisuals.module.render.Firefly;
import com.example.auravisuals.module.render.HitBubbles;
import com.example.auravisuals.module.render.HitCounter;
import com.example.auravisuals.module.render.KillEffect;
import com.example.auravisuals.module.render.Particles;
import com.example.auravisuals.module.render.Split;
import com.example.auravisuals.module.render.TargetESP;
import com.example.auravisuals.module.render.Wave;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackEntity", at = @At("TAIL"), require = 0)
    private void auravisuals$hitCounterAttackPlayerTail(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (player == MinecraftClient.getInstance().player) {
            if (target instanceof PlayerEntity victim) {
                HitCounter hitCounter = ModuleManager.INSTANCE.getModule(HitCounter.class);
                if (hitCounter != null && hitCounter.isEnabled()) {
                    hitCounter.onLocalMeleeHitPlayer(victim);
                }
            }
        }
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), require = 0)
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (player == MinecraftClient.getInstance().player) {
            AutoMessage autoMessage = ModuleManager.INSTANCE.getModule(AutoMessage.class);
            if (autoMessage != null && autoMessage.isEnabled()) {
                autoMessage.setTarget(target);
            }

            if (target instanceof LivingEntity living) {
                TargetESP targetESP = ModuleManager.INSTANCE.getModule(TargetESP.class);
                if (targetESP != null) {
                    targetESP.onAttack(living);
                }

                HitBubbles hitBubbles;
                if ((hitBubbles = ModuleManager.INSTANCE.getModule(HitBubbles.class)) != null && hitBubbles.isEnabled()) {
                    hitBubbles.onAttack(player, living);
                }

                KillEffect killEffect;
                if ((killEffect = ModuleManager.INSTANCE.getModule(KillEffect.class)) != null && killEffect.isEnabled()) {
                    killEffect.setTarget(living);
                }
            }

            ShiftTap shiftTap;
            if (target instanceof PlayerEntity && (shiftTap = ModuleManager.INSTANCE.getModule(ShiftTap.class)) != null && shiftTap.isEnabled()) {
                ShiftTap.onHitPlayer();
            }

            Xeaklt xeaklt;
            if ((xeaklt = ModuleManager.INSTANCE.getModule(Xeaklt.class)) != null && xeaklt.isEnabled()) {
                xeaklt.onAttackEntity(player, target);
            }

            Particles particles;
            if ((particles = ModuleManager.INSTANCE.getModule(Particles.class)) != null && particles.isEnabled()) {
                Vec3d at = target.getEntityPos().add(0.0, target.getHeight() * 0.5, 0.0);
                particles.onEvent("Удар", at);
            }

            Split split;
            if ((split = ModuleManager.INSTANCE.getModule(Split.class)) != null && split.isEnabled()) {
                split.onAttack();
            }

            Wave wave;
            if ((wave = ModuleManager.INSTANCE.getModule(Wave.class)) != null && wave.isEnabled()) {
                wave.onAttack(target.getEntityPos());
            }

            Firefly firefly;
            if ((firefly = ModuleManager.INSTANCE.getModule(Firefly.class)) != null && firefly.isEnabled()) {
                firefly.onAttack(target);
            }
        }
    }
}
