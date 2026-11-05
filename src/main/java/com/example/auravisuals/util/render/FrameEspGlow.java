package com.example.auravisuals.util.render;

import com.example.auravisuals.mixin.accessor.PostEffectPassAccessor;
import com.example.auravisuals.mixin.accessor.PostEffectProcessorAccessor;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.FrameESP;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import org.lwjgl.system.MemoryUtil;

public final class FrameEspGlow {
    private static volatile PostEffectProcessor processor;
    private static ByteBuffer scratch;
    private static float lastSize = Float.NaN;

    private FrameEspGlow() {
    }

    public static void setProcessor(PostEffectProcessor value) {
        processor = value;
        lastSize = Float.NaN;
    }

    public static void update() {
        try {
            PostEffectProcessor proc = processor;
            if (proc == null) {
                return;
            }

            FrameESP module = ModuleManager.INSTANCE == null ? null : ModuleManager.INSTANCE.getModule(FrameESP.class);
            if (module == null || !module.isEnabled()) {
                return;
            }

            float size = FrameESP.glowSize.getFloatValue();
            if (size == lastSize) {
                return;
            }

            List<PostEffectPass> passes = ((PostEffectProcessorAccessor)proc).auravisuals$getPasses();
            if (passes == null) {
                return;
            }

            boolean written = false;

            for (PostEffectPass pass : passes) {
                Map buffers = ((PostEffectPassAccessor)pass).auravisuals$getUniformBuffers();
                GpuBuffer buffer;
                if (buffers != null && (buffer = (GpuBuffer)buffers.get("GlowParams")) != null) {
                    if (scratch == null) {
                        scratch = MemoryUtil.memAlloc(16);
                    }

                    scratch.clear();
                    scratch.putFloat(size).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
                    scratch.flip();
                    RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), scratch);
                    written = true;
                }
            }

            if (written) {
                lastSize = size;
            }
        } catch (Throwable var10) {
        }
    }
}
