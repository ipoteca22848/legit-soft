package com.example.auravisuals.mixin.entity;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.player.LockSlot;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void onDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        LockSlot lockSlot = ModuleManager.INSTANCE.getModule(LockSlot.class);
        ClientPlayerEntity player;
        if (lockSlot != null && lockSlot.isEnabled() && lockSlot.isSlotLocked((player = (ClientPlayerEntity)(Object)this).getInventory().getSelectedSlot())) {
            cir.setReturnValue(false);
        }
    }
}
