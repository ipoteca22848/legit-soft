package com.example.auravisuals.mixin.entity;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.misc.TotemTracker;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class AbstractClientPlayerEntityMixin {
    @Inject(method = "handleStatus(B)V", at = @At("HEAD"), require = 0)
    private void onHandleStatus(byte status, CallbackInfo ci) {
        if (status == 35) {
            if ((Object)this instanceof AbstractClientPlayerEntity) {
                TotemTracker tracker = ModuleManager.INSTANCE.getModule(TotemTracker.class);
                if (tracker != null && tracker.isEnabled()) {
                    tracker.onTotemPop((Entity)(Object)this);
                }
            }
        }
    }
}
