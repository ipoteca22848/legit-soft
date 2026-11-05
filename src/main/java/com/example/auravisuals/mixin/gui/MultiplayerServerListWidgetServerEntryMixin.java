package com.example.auravisuals.mixin.gui;

import com.example.auravisuals.mixin.accessor.MultiplayerScreenAccessor;
import com.example.auravisuals.util.ServerPinHelper;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget.Entry;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget.ServerEntry;
import net.minecraft.client.gui.widget.SquareWidgetEntry;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerEntry.class)
public abstract class MultiplayerServerListWidgetServerEntryMixin extends Entry implements SquareWidgetEntry {
    @Shadow
    @Final
    private MultiplayerScreen screen;
    @Shadow
    @Final
    private ServerInfo server;

    @Inject(method = "swapEntries", at = @At("HEAD"), cancellable = true)
    private void onSwapEntries(int i, int j, CallbackInfo ci) {
        if (i == 0 || j == 0) {
            ci.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        MultiplayerServerListWidget widget = ((MultiplayerScreenAccessor)(Object)this.screen).getServerListWidget();
        if (widget != null) {
            int i = widget.children().indexOf(this);
            int mouseX = (int)click.x() - this.getContentX();
            int mouseY = (int)click.y() - this.getContentY();
            if (i == 0 && ServerPinHelper.isPinned(this.server)) {
                if (this.isTopLeft(mouseX, mouseY, 32)) {
                    cir.setReturnValue(true);
                }
            } else if (i == 1 && this.isBottomLeft(mouseX, mouseY, 32)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyInput keyInput, CallbackInfoReturnable<Boolean> cir) {
        if (keyInput.hasShift()) {
            MultiplayerServerListWidget widget = ((MultiplayerScreenAccessor)(Object)this.screen).getServerListWidget();
            if (widget == null) {
                return;
            }

            int i = widget.children().indexOf(this);
            if (i == 0 && ServerPinHelper.isPinned(this.server) && keyInput.isDown()) {
                cir.setReturnValue(true);
            } else if (i == 1 && keyInput.isUp()) {
                cir.setReturnValue(true);
            }
        }
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"
        )
    )
    private void redirectDrawTexture(DrawContext context, RenderPipeline pipeline, Identifier texture, int x, int y, int width, int height) {
        if (texture != null) {
            String path = texture.getPath();
            if (path.contains("move_down") && ServerPinHelper.isPinned(this.server)) {
                return;
            }

            MultiplayerServerListWidget widget;
            if (path.contains("move_up")
                && (widget = ((MultiplayerScreenAccessor)(Object)this.screen).getServerListWidget()) != null
                && widget.children().indexOf(this) <= 1) {
                return;
            }
        }

        context.drawGuiTexture(pipeline, texture, x, y, width, height);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerServerListWidget$ServerEntry;isTopLeft(III)Z"))
    private boolean redirectIsTopLeft(ServerEntry entry, int mouseX, int mouseY, int size) {
        return ServerPinHelper.isPinned(this.server) ? false : entry.isTopLeft(mouseX, mouseY, size);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerServerListWidget$ServerEntry;isBottomLeft(III)Z"))
    private boolean redirectIsBottomLeft(ServerEntry entry, int mouseX, int mouseY, int size) {
        MultiplayerServerListWidget widget = ((MultiplayerScreenAccessor)(Object)this.screen).getServerListWidget();
        return widget != null && widget.children().indexOf(this) <= 1 ? false : entry.isBottomLeft(mouseX, mouseY, size);
    }
}
