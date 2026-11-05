package com.example.auravisuals.mixin.client;

import com.example.auravisuals.util.language.LanguageManager;
import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextRenderer.class)
public abstract class TextRendererMixin {
    @ModifyVariable(
        method = {
                "draw(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V",
                "getWidth(Ljava/lang/String;)I"
        },
        at = @At("HEAD"),
        argsOnly = true,
        require = 0
    )
    private String auravisuals$repairString(String text) {
        return LanguageManager.repairIrcMojibake(text);
    }
}
