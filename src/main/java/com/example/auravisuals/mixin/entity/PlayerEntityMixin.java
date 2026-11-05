package com.example.auravisuals.mixin.entity;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.misc.TotemTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "handleStatus", at = @At("HEAD"), require = 0)
    private void auravisuals$onHandleStatus(byte status, CallbackInfo ci) {
        if (status == 35) {
            TotemTracker tracker = ModuleManager.INSTANCE.getModule(TotemTracker.class);
            if (tracker != null && tracker.isEnabled()) {
                tracker.onTotemPop((Entity)(Object)this);
            }
        }
    }
}
