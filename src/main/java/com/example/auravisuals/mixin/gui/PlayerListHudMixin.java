package com.example.auravisuals.mixin.gui;

import com.example.auravisuals.module.misc.Globals;
import com.example.auravisuals.module.misc.StreamerMode;
import com.example.auravisuals.util.registry.GlobalsIconText;
import com.example.auravisuals.util.registry.GlobalsRegistry;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true, require = 0)
    private void onGetPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        Text result = (Text)cir.getReturnValue();
        if (result != null) {
            result = StreamerMode.replaceInText(result);
            if (Globals.shouldShowIcon() && entry.getProfile() != null && GlobalsRegistry.isGlobalsPlayer(entry.getProfile().name())) {
                result = GlobalsIconText.prefix(result);
            }

            cir.setReturnValue(result);
        }
    }
}
