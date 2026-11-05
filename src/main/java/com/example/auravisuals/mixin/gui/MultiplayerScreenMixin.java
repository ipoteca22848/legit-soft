package com.example.auravisuals.mixin.gui;

import com.example.auravisuals.util.ServerPinHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget.Entry;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget.ServerEntry;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
    @Shadow
    private ButtonWidget buttonEdit;
    @Shadow
    private ButtonWidget buttonDelete;
    @Shadow
    protected MultiplayerServerListWidget serverListWidget;

    protected MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "updateButtonActivationStates", at = @At("RETURN"))
    private void onUpdateButtonActivationStates(CallbackInfo ci) {
        ServerEntry serverEntry;
        Entry entry;
        if (this.serverListWidget != null
            && (entry = this.serverListWidget.getSelectedOrNull()) instanceof ServerEntry
            && ServerPinHelper.isPinned((serverEntry = (ServerEntry)entry).getServer())) {
            if (this.buttonEdit != null) {
                this.buttonEdit.active = false;
            }

            if (this.buttonDelete != null) {
                this.buttonDelete.active = false;
            }
        }
    }

    @Inject(method = "editEntry", at = @At("HEAD"), cancellable = true)
    private void onEditEntry(boolean confirmed, CallbackInfo ci) {
        ServerEntry serverEntry;
        Entry entry;
        if (this.serverListWidget != null
            && (entry = this.serverListWidget.getSelectedOrNull()) instanceof ServerEntry
            && ServerPinHelper.isPinned((serverEntry = (ServerEntry)entry).getServer())) {
            ci.cancel();
        }
    }

    @Inject(method = "removeEntry", at = @At("HEAD"), cancellable = true)
    private void onRemoveEntry(boolean confirmed, CallbackInfo ci) {
        ServerEntry serverEntry;
        Entry entry;
        if (this.serverListWidget != null
            && (entry = this.serverListWidget.getSelectedOrNull()) instanceof ServerEntry
            && ServerPinHelper.isPinned((serverEntry = (ServerEntry)entry).getServer())) {
            ci.cancel();
        }
    }
}
