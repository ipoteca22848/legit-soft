package com.example.auravisuals.mixin.client;

import com.example.auravisuals.module.misc.StreamerMode;
import com.example.auravisuals.util.language.LanguageManager;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Text.class)
public interface TextMixin {
    @ModifyVariable(method = "of(Ljava/lang/String;)Lnet/minecraft/text/Text;", at = @At("HEAD"), argsOnly = true, require = 0)
    private static String onOf(String string) {
        return LanguageManager.repairIrcMojibake(StreamerMode.getReplacedName(string));
    }

    @ModifyVariable(method = "literal(Ljava/lang/String;)Lnet/minecraft/text/MutableText;", at = @At("HEAD"), argsOnly = true, require = 0)
    private static String onLiteral(String string) {
        return LanguageManager.repairIrcMojibake(StreamerMode.getReplacedName(string));
    }
}
