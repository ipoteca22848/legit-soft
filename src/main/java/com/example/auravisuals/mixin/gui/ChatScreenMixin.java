package com.example.auravisuals.mixin.gui;

import com.example.auravisuals.client.gui.hud.HudEditor;
import com.example.auravisuals.module.misc.ChatHelper;
import com.example.auravisuals.module.misc.NoCapsLock;
import com.example.auravisuals.module.render.ViewModel;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Shadow
    private TextFieldWidget chatField;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void auravisuals$viewModelClick(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (HudEditor.handleChatMouseClick(click)) {
            cir.setReturnValue(true);
        } else {
            if (ViewModel.handleEditorClick(click)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void auravisuals$viewModelScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (ViewModel.handleEditorScroll(mouseX, mouseY, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }

    @ModifyVariable(method = "sendMessage", at = @At("HEAD"), argsOnly = true, require = 0)
    private String auravisuals$noCapsChatField(String chatText) {
        return NoCapsLock.normalizeOutgoingChat(ChatHelper.correctCommandLayout(chatText));
    }

    @Inject(method = "onChatFieldUpdate", at = @At("HEAD"), cancellable = true, require = 0)
    private void auravisuals$correctCommandWhileTyping(String chatText, CallbackInfo ci) {
        String corrected = ChatHelper.correctCommandLayout(chatText);
        if (corrected != null && !corrected.equals(chatText)) {
            int cursor = this.chatField.getCursor();
            this.chatField.setText(corrected);
            this.chatField.setCursor(Math.min(cursor, corrected.length()), false);
            ci.cancel();
        }
    }
}
