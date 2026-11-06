package com.example.auravisuals.util.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

public final class SaturationPipeline {
    private static final int BUFFER_SIZE = 128;
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
        .withLocation(Identifier.of("auravisuals", "pipeline/saturation"))
        .withVertexShader(Identifier.of("auravisuals", "core/saturation"))
        .withFragmentShader(Identifier.of("auravisuals", "core/saturation"))
        .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
        .withUniform("SaturationData", UniformType.UNIFORM_BUFFER)
        .withSampler("Sampler0")
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .withCull(false)
        .build();
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3f MODEL_OFFSET = new Vector3f(0.0F, 0.0F, 0.0F);
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer dummyVertexBuffer;
    private static ByteBuffer dataBuffer;
    private static GpuTexture sourceTexture;
    private static GpuTextureView sourceTextureView;
    private static int lastWidth;
    private static int lastHeight;
    private static TextureFormat lastFormat;
    private static boolean initialized;

    private SaturationPipeline() {
    }

    private static void init() {
        if (!initialized) {
            dataBuffer = MemoryUtil.memAlloc(128);
            ByteBuffer dummyData = MemoryUtil.memAlloc(4);
            dummyData.putInt(0);
            dummyData.flip();
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "anvil visual:saturation_dummy_vertex", 32, dummyData);
            MemoryUtil.memFree(dummyData);
            initialized = true;
        }
    }

    public static void apply(float saturation, float vibrance, float contrast, float brightness) {
        if (RenderSystem.tryGetDevice() != null) {
            saturation = Math.max(0.0F, Math.min(3.0F, saturation));
            vibrance = Math.max(0.0F, Math.min(1.0F, vibrance));
            contrast = Math.max(0.5F, Math.min(2.0F, contrast));
            brightness = Math.max(0.5F, Math.min(1.5F, brightness));
            if (!(Math.abs(saturation - 1.0F) < 0.001F)
                || !(Math.abs(vibrance) < 0.001F)
                || !(Math.abs(contrast - 1.0F) < 0.001F)
                || !(Math.abs(brightness - 1.0F) < 0.001F)) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.getFramebuffer() != null && client.getFramebuffer().getColorAttachment() != null) {
                    init();
                    int fbWidth = client.getFramebuffer().textureWidth;
                    int fbHeight = client.getFramebuffer().textureHeight;
                    ensureTexture(fbWidth, fbHeight, client.getFramebuffer().getColorAttachment().getFormat());
                    if (sourceTexture != null) {
                        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                        FramebufferBlitter.blit(encoder, client.getFramebuffer().getColorAttachmentView(), sourceTextureView);
                        int screenW = client.getWindow().getScaledWidth();
                        int screenH = client.getWindow().getScaledHeight();
                        prepareData(screenW, screenH, saturation, vibrance, contrast, brightness);
                        encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);
                        GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
                        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                            .write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

                        try (RenderPass pass = encoder.createRenderPass(
                                () -> "anvil visual:saturation",
                                client.getFramebuffer().getColorAttachmentView(),
                                OptionalInt.empty(),
                                null,
                                OptionalDouble.empty()
                            )) {
                            pass.setPipeline(PIPELINE);
                            pass.setVertexBuffer(0, dummyVertexBuffer);
                            pass.bindTexture("Sampler0", sourceTextureView, sampler);
                            RenderSystem.bindDefaultUniforms(pass);
                            pass.setUniform("DynamicTransforms", dynamicTransforms);
                            pass.setUniform("SaturationData", uniformBuffer);
                            pass.draw(0, 6);
                        }
                    }
                }
            }
        }
    }

    private static void prepareData(int screenW, int screenH, float saturation, float vibrance, float contrast, float brightness) {
        Matrix4f ortho = new Matrix4f().setOrtho(0.0F, screenW, screenH, 0.0F, -1000.0F, 1000.0F);
        dataBuffer.clear();
        dataBuffer.putFloat(ortho.m00()).putFloat(ortho.m01()).putFloat(ortho.m02()).putFloat(ortho.m03());
        dataBuffer.putFloat(ortho.m10()).putFloat(ortho.m11()).putFloat(ortho.m12()).putFloat(ortho.m13());
        dataBuffer.putFloat(ortho.m20()).putFloat(ortho.m21()).putFloat(ortho.m22()).putFloat(ortho.m23());
        dataBuffer.putFloat(ortho.m30()).putFloat(ortho.m31()).putFloat(ortho.m32()).putFloat(ortho.m33());
        dataBuffer.putFloat(screenW).putFloat(screenH).putFloat(0.0F).putFloat(0.0F);
        dataBuffer.putFloat(saturation).putFloat(vibrance).putFloat(contrast).putFloat(brightness);
        dataBuffer.flip();
        ensureBuffer();
    }

    private static void ensureTexture(int fbWidth, int fbHeight, TextureFormat format) {
        if (sourceTexture == null || fbWidth != lastWidth || fbHeight != lastHeight || format != lastFormat) {
            if (sourceTextureView != null) {
                sourceTextureView.close();
                sourceTextureView = null;
            }

            if (sourceTexture != null) {
                sourceTexture.close();
                sourceTexture = null;
            }

            sourceTexture = RenderSystem.getDevice().createTexture(() -> "anvil visual:saturation_source", 12, format, fbWidth, fbHeight, 1, 1);
            sourceTextureView = RenderSystem.getDevice().createTextureView(sourceTexture);
            lastWidth = fbWidth;
            lastHeight = fbHeight;
            lastFormat = format;
        }
    }

    private static void ensureBuffer() {
        int size = dataBuffer.remaining();
        if (uniformBuffer == null || uniformBuffer.size() < size) {
            if (uniformBuffer != null) {
                uniformBuffer.close();
            }

            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "anvil visual:saturation_uniform", 136, size);
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

        if (sourceTextureView != null) {
            sourceTextureView.close();
            sourceTextureView = null;
        }

        if (sourceTexture != null) {
            sourceTexture.close();
            sourceTexture = null;
        }

        initialized = false;
        lastWidth = 0;
        lastHeight = 0;
        lastFormat = null;
    }
}
