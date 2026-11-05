package com.example.auravisuals.mixin.accessor;

import java.util.List;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PostEffectProcessor.class)
public interface PostEffectProcessorAccessor {
    @Accessor("passes")
    List<PostEffectPass> auravisuals$getPasses();
}
