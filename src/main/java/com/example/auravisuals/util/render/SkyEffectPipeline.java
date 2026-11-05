package com.example.auravisuals.util.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

public final class SkyEffectPipeline {
    private static final int BUFFER_SIZE = 128;
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
        .withLocation(Identifier.of("auravisuals", "pipeline/sky_effect"))
        .withVertexShader(Identifier.of("auravisuals", "core/sky_effect"))
        .withFragmentShader(Identifier.of("auravisuals", "core/sky_effect"))
        .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
        .withUniform("SkyData", UniformType.UNIFORM_BUFFER)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        .withDepthWrite(false)
        .withCull(false)
        .build();
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3f MODEL_OFFSET = new Vector3f(0.0F, 0.0F, 0.0F);
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer dummyVertexBuffer;
    private static ByteBuffer dataBuffer;
    private static boolean initialized;

    private SkyEffectPipeline() {
    }

    private static void init() {
        if (!initialized) {
            dataBuffer = MemoryUtil.memAlloc(128);
            ByteBuffer dummy = MemoryUtil.memAlloc(4);
            dummy.putInt(0);
            dummy.flip();
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "bamber client:sky_effect_dummy_vertex", 32, dummy);
            MemoryUtil.memFree(dummy);
            initialized = true;
        }
    }

    public static void render(
        Vector3f forward, Vector3f up, Vector3f left, float fovScaleX, float fovScaleY, float r, float g, float b, float a, float time, float scale, int mode
    ) {
        if (!(a <= 0.003F)) {
            MinecraftClient client = MinecraftClient.getInstance();
            Framebuffer main = client.getFramebuffer();
            if (main != null && main.getColorAttachmentView() != null && RenderSystem.tryGetDevice() != null) {
                init();
                prepareData(forward, up, left, fovScaleX, fovScaleY, r, g, b, a, time, scale, mode);
                CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);
                GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                    .write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
                GpuTextureView colorView = main.getColorAttachmentView();
                GpuTextureView depthView = main.useDepthAttachment ? main.getDepthAttachmentView() : null;

                try (RenderPass pass = encoder.createRenderPass(
                        () -> "bamber client:sky_effect", colorView, OptionalInt.empty(), depthView, OptionalDouble.empty()
                    )) {
                    pass.setPipeline(PIPELINE);
                    pass.setVertexBuffer(0, dummyVertexBuffer);
                    RenderSystem.bindDefaultUniforms(pass);
                    pass.setUniform("DynamicTransforms", dynamicTransforms);
                    pass.setUniform("SkyData", uniformBuffer);
                    pass.draw(0, 6);
                }
            }
        }
    }

    private static void prepareData(
        Vector3f fwd, Vector3f up, Vector3f left, float fovX, float fovY, float r, float g, float b, float a, float time, float scale, int mode
    ) {
        dataBuffer.clear();
        dataBuffer.putFloat(fwd.x).putFloat(fwd.y).putFloat(fwd.z).putFloat(0.0F);
        dataBuffer.putFloat(up.x).putFloat(up.y).putFloat(up.z).putFloat(0.0F);
        dataBuffer.putFloat(left.x).putFloat(left.y).putFloat(left.z).putFloat(0.0F);
        dataBuffer.putFloat(fovX).putFloat(fovY).putFloat(0.0F).putFloat(0.0F);
        dataBuffer.putFloat(r).putFloat(g).putFloat(b).putFloat(a);
        dataBuffer.putFloat(time).putInt(mode).putFloat(Math.max(0.1F, scale)).putFloat(0.0F);
        dataBuffer.flip();
        ensureBuffer();
    }

    private static void ensureBuffer() {
        int size = dataBuffer.remaining();
        if (uniformBuffer == null || uniformBuffer.size() < size) {
            if (uniformBuffer != null) {
                uniformBuffer.close();
            }

            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "bamber client:sky_effect_uniform", 136, size);
        }
    }

    public static void shutdown() {
        if (uniformBuffer != null) {
            uniformBuffer.close();
            uniformBuffer = null;
        }

        if (dummyVertexBuffer != null) {
            dummyVertexBuffer.close();
            dummyVertexBuffer = null;
        }

        if (dataBuffer != null) {
            MemoryUtil.memFree(dataBuffer);
            dataBuffer = null;
        }

        initialized = false;
    }
}
