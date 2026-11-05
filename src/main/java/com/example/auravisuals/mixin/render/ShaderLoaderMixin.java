package com.example.auravisuals.mixin.render;

import com.example.auravisuals.util.render.FrameEspGlow;
import java.util.Set;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShaderLoader.class)
public class ShaderLoaderMixin {
    @Inject(method = "loadPostEffect(Lnet/minecraft/util/Identifier;Ljava/util/Set;)Lnet/minecraft/client/gl/PostEffectProcessor;", at = @At("RETURN"), require = 0)
    private void auravisuals$captureEntityOutline(Identifier id, Set<Identifier> targets, CallbackInfoReturnable<PostEffectProcessor> cir) {
        if (id != null && "minecraft".equals(id.getNamespace()) && "entity_outline".equals(id.getPath())) {
            FrameEspGlow.setProcessor((PostEffectProcessor)cir.getReturnValue());
        }
    }
}
