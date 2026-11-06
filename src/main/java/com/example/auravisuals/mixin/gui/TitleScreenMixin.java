package com.example.auravisuals.mixin.gui;

import com.example.auravisuals.client.gui.AuraMainMenuScreen;
import com.example.auravisuals.module.render.MainMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Inject(method = "init", at = @At("RETURN"), require = 0)
    private void aura$openCustomMainMenu(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        TitleScreenMixin self = this;
        client.execute(() -> {
            if (client.currentScreen == (Object)self && com.anvil.client.AnvilRuntime.isActive() && MainMenu.shouldUseAuraMenu()) {
                client.setScreen(new rich.screens.menu.MainMenuScreen());
            }
        });
    }
}
