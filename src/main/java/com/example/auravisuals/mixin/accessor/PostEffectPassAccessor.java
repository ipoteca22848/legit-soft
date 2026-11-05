package com.example.auravisuals.mixin.accessor;

import com.mojang.blaze3d.buffers.GpuBuffer;
import java.util.Map;
import net.minecraft.client.gl.PostEffectPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PostEffectPass.class)
public interface PostEffectPassAccessor {
    @Accessor("uniformBuffers")
    Map<String, GpuBuffer> auravisuals$getUniformBuffers();
}
