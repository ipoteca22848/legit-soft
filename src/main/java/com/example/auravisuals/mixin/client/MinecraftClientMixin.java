package com.example.auravisuals.mixin.client;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.misc.PvPSafe;
import com.example.auravisuals.util.server.AutoReconnectTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V", at = @At("HEAD"), require = 0)
    private void auravisuals$markManualDisconnect(Screen screen, boolean transferring, CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient)(Object)this;
        if (client.currentScreen instanceof GameMenuScreen && client.getCurrentServerEntry() != null) {
            AutoReconnectTracker.markManualDisconnect();
        }
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void onDisconnect(Screen screen, boolean transferring, CallbackInfo ci) {
        PvPSafe pvpSafe = ModuleManager.INSTANCE.getModule(PvPSafe.class);
        if (pvpSafe != null && pvpSafe.isEnabled() && pvpSafe.isPvpActive() && !pvpSafe.isBypassing()) {
            MinecraftClient client = (MinecraftClient)(Object)this;
            client.setScreen(new ConfirmScreen(result -> {
                if (result) {
                    pvpSafe.setBypass(true);
                    client.disconnect(screen, false);
                } else {
                    client.setScreen(screen);
                    pvpSafe.setBypass(false);
                }
            }, Text.literal("Внимание: Активно PVP!"), Text.literal("Вы уверены, что хотите выйти с сервера?"), Text.literal("Выйти"), Text.literal("Отмена")));
            ci.cancel();
        }
    }
}
