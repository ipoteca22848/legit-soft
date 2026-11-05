package com.example.auravisuals.mixin.gui;

import com.example.auravisuals.client.gui.AuraPauseScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin {
    @Shadow
    @Final
    private boolean showMenu;

    @Inject(method = "init", at = @At("RETURN"), require = 0)
    private void auravisuals$openCustomPause(CallbackInfo ci) {
        if (this.showMenu) {
            MinecraftClient client = MinecraftClient.getInstance();
            GameMenuScreenMixin self = this;
            client.execute(() -> {
                if (client.currentScreen == (Object)self) {
                    client.setScreen(new AuraPauseScreen());
                }
            });
        }
    }
}
