package com.example.auravisuals.mixin.gui;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.player.QuickRespawn;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public class DeathScreenMixin {
    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void onTick(CallbackInfo ci) {
        if (ModuleManager.INSTANCE.getModule(QuickRespawn.class).isEnabled() && MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.requestRespawn();
            MinecraftClient.getInstance().setScreen(null);
        }
    }
}
