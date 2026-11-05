package com.example.auravisuals.mixin.gui;

import com.example.auravisuals.client.chat.ClientDotSuggestions;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatInputSuggestor.class)
public abstract class ChatInputSuggestorMixin {
    @Shadow
    @Final
    private TextFieldWidget textField;
    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Invoker("showCommandSuggestions")
    protected abstract void auravisuals$showCommandSuggestions();

    @Inject(method = "refresh", at = @At("TAIL"), require = 0)
    private void auravisuals$clientDotSuggestions(CallbackInfo ci) {
        String s = this.textField.getText();
        if (s.startsWith(".") && !s.startsWith("../")) {
            int cursor = this.textField.getCursor();
            CompletableFuture fut = ClientDotSuggestions.buildFuture(s, cursor);
            fut.thenRun(() -> {
                if (fut.isDone()) {
                    Suggestions sugs = (Suggestions)fut.join();
                    if (!sugs.getList().isEmpty()) {
                        this.pendingSuggestions = fut;
                        this.auravisuals$showCommandSuggestions();
                    }
                }
            });
        }
    }
}
