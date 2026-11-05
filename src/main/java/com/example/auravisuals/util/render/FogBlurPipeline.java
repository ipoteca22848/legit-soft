package com.example.auravisuals.util.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
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
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

public final class FogBlurPipeline {
    private static final int MAX_PASSES = 4;
    private static final int UNIFORM_SIZE = 48;
    private static final int FAILURE_LIMIT = 3;
    private static final RenderPipeline DOWN_PIPELINE = RenderPipeline.builder()
        .withLocation(Identifier.of("auravisuals", "pipeline/post/fogblur/kawase_down"))
        .withVertexShader(Identifier.of("auravisuals", "post/fogblur/kawase"))
        .withFragmentShader(Identifier.of("auravisuals", "post/fogblur/blur"))
        .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
        .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
        .withSampler("Sampler0")
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .withCull(false)
        .build();
    private static final RenderPipeline UP_PIPELINE = RenderPipeline.builder()
        .withLocation(Identifier.of("auravisuals", "pipeline/post/fogblur/kawase_up"))
        .withVertexShader(Identifier.of("auravisuals", "post/fogblur/kawase"))
        .withFragmentShader(Identifier.of("auravisuals", "post/fogblur/blur"))
        .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
        .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
        .withSampler("Sampler0")
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .withCull(false)
        .build();
    private static final RenderPipeline COMPOSITE_PIPELINE = RenderPipeline.builder()
        .withLocation(Identifier.of("auravisuals", "pipeline/post/fogblur/composite"))
        .withVertexShader(Identifier.of("auravisuals", "post/fogblur/fogblur"))
        .withFragmentShader(Identifier.of("auravisuals", "post/fogblur/composite"))
        .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
        .withUniform("FogBlurData", UniformType.UNIFORM_BUFFER)
        .withSampler("BlurSampler")
        .withSampler("DepthSampler")
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .withCull(false)
        .build();
    private static ByteBuffer dataBuffer;
    private static GpuBuffer dummyVertexBuffer;
    private static GpuBuffer kawaseUniform;
    private static GpuBuffer compositeUniform;
    private static final GpuTexture[] downTextures = new GpuTexture[4];
    private static final GpuTextureView[] downViews = new GpuTextureView[4];
    private static final GpuTexture[] upTextures = new GpuTexture[4];
    private static final GpuTextureView[] upViews = new GpuTextureView[4];
    private static final int[] downW = new int[4];
    private static final int[] downH = new int[4];
    private static final int[] upW = new int[4];
    private static final int[] upH = new int[4];
    private static int sceneW = -1;
    private static int sceneH = -1;
    private static int blurW = -1;
    private static int blurH = -1;
    private static TextureFormat colorFormat;
    private static int allocatedPasses;
    private static boolean initialized;
    private static boolean disabledAfterError;
    private static int failures;

    private FogBlurPipeline() {
    }

    public static void clear() {
        closeTargets();
        disabledAfterError = false;
        failures = 0;
    }

    public static boolean isDisabledAfterError() {
        return disabledAfterError;
    }

    public static void apply(Framebuffer target, float strength, float distanceBlocks, float renderScale, int maxPasses, float customFogStartRatio) {
        if (!disabledAfterError && RenderSystem.tryGetDevice() != null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            Framebuffer fb = target != null ? target : mc.getFramebuffer();
            if (mc.player != null && mc.world != null && usable(fb)) {
                int width = fb.textureWidth;
                int height = fb.textureHeight;
                int passes = computePasses(strength, maxPasses);
                GpuTextureView stableColor = StableWorldFrameCapture.colorView();
                GpuTextureView liveDepth = fb.getDepthAttachmentView();
                if (stableColor != null && liveDepth != null && StableWorldFrameCapture.width() == width && StableWorldFrameCapture.height() == height) {
                    try {
                        init();
                        TextureFormat format = fb.getColorAttachment().getFormat();
                        ensureTargets(width, height, renderScale, passes, format);
                        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                        float offset = clamp(0.85F + strength * 0.06F, 0.85F, 2.0F);
                        GpuTextureView blurred = blurChain(encoder, stableColor, width, height, passes, offset);
                        float farPlane = viewDistance();
                        boolean customFog = customFogStartRatio >= 0.0F;
                        float start = customFog
                            ? farPlane * clamp(customFogStartRatio, 0.0F, 0.95F)
                            : clamp(Math.min(distanceBlocks, farPlane * 0.64F), 1.0F, farPlane * 0.82F);
                        float fade = customFog ? Math.max(20.0F, farPlane * 0.16F) : Math.max(52.0F, farPlane * 0.3F);
                        float minThreshold = clamp(start / farPlane, 0.0F, 0.995F);
                        float maxThreshold = clamp((start + fade) / farPlane, minThreshold + (customFog ? 0.025F : 0.04F), customFog ? 0.997F : 0.975F);
                        composite(encoder, fb.getColorAttachmentView(), blurred, liveDepth, 1.0F, farPlane, minThreshold, maxThreshold);
                        failures = 0;
                    } catch (Throwable var23) {
                        var23.printStackTrace();
                        fail();
                    }
                }
            }
        }
    }

    private static void init() {
        if (!initialized) {
            dataBuffer = MemoryUtil.memAlloc(48);
            ByteBuffer dummy = MemoryUtil.memAlloc(4);
            dummy.putInt(0).flip();
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "bamber client:fogblur_dummy", 40, dummy);
            MemoryUtil.memFree(dummy);
            kawaseUniform = RenderSystem.getDevice().createBuffer(() -> "bamber client:fogblur_kawase_uniform", 136, 48L);
            compositeUniform = RenderSystem.getDevice().createBuffer(() -> "bamber client:fogblur_composite_uniform", 136, 48L);
            initialized = true;
        }
    }

    private static void ensureTargets(int width, int height, float renderScale, int passes, TextureFormat format) {
        int targetW = Math.max(1, Math.round(width * clamp(renderScale, 0.25F, 0.75F)));
        int targetH = Math.max(1, Math.round(height * clamp(renderScale, 0.25F, 0.75F)));
        if (downTextures[0] == null
            || sceneW != width
            || sceneH != height
            || blurW != targetW
            || blurH != targetH
            || allocatedPasses < passes
            || format != colorFormat) {
            closeTargets();
            int cw = targetW;
            int ch = targetH;

            for (int i = 0; i < passes; i++) {
                downTextures[i] = create("bamber client:fogblur_down_" + i, cw, ch, 12, format);
                downViews[i] = RenderSystem.getDevice().createTextureView(downTextures[i]);
                downW[i] = cw;
                downH[i] = ch;
                if (i < passes - 1) {
                    upTextures[i] = create("bamber client:fogblur_up_" + i, cw, ch, 12, format);
                    upViews[i] = RenderSystem.getDevice().createTextureView(upTextures[i]);
                    upW[i] = cw;
                    upH[i] = ch;
                }

                cw = Math.max(1, cw / 2);
                ch = Math.max(1, ch / 2);
            }

            sceneW = width;
            sceneH = height;
            blurW = targetW;
            blurH = targetH;
            allocatedPasses = passes;
            colorFormat = format;
        }
    }

    private static GpuTexture create(String label, int width, int height, int usage, TextureFormat format) {
        return RenderSystem.getDevice().createTexture(() -> label, usage, format, width, height, 1, 1);
    }

    private static GpuTextureView blurChain(CommandEncoder encoder, GpuTextureView source, int sourceW, int sourceH, int passes, float offsetScale) {
        GpuTextureView current = source;
        int currentW = sourceW;
        int currentH = sourceH;

        for (int i = 0; i < passes; i++) {
            kawase(encoder, DOWN_PIPELINE, current, currentW, currentH, downViews[i], offsetScale);
            current = downViews[i];
            currentW = downW[i];
            currentH = downH[i];
        }

        for (int i = passes - 2; i >= 0; i--) {
            kawase(encoder, UP_PIPELINE, current, currentW, currentH, upViews[i], offsetScale);
            current = upViews[i];
            currentW = upW[i];
            currentH = upH[i];
        }

        return current;
    }

    private static void kawase(
        CommandEncoder encoder, RenderPipeline pipeline, GpuTextureView source, int sourceW, int sourceH, GpuTextureView target, float offsetScale
    ) {
        if (source != null && target != null) {
            dataBuffer.clear();
            dataBuffer.putFloat(0.0F).putFloat(0.0F).putFloat(1.0F).putFloat(1.0F);
            dataBuffer.putFloat(offsetScale / Math.max(1, sourceW)).putFloat(offsetScale / Math.max(1, sourceH)).putFloat(0.0F).putFloat(0.0F);
            dataBuffer.putFloat(0.55F).putFloat(0.65F).putFloat(0.78F).putFloat(1.0F);
            dataBuffer.flip();
            encoder.writeToBuffer(kawaseUniform.slice(), dataBuffer);
            GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

            try (RenderPass pass = encoder.createRenderPass(() -> "bamber client:fogblur_kawase", target, OptionalInt.of(0), null, OptionalDouble.empty())) {
                pass.setPipeline(pipeline);
                pass.setVertexBuffer(0, dummyVertexBuffer);
                pass.bindTexture("Sampler0", source, sampler);
                pass.setUniform("KawaseParams", kawaseUniform);
                pass.draw(0, 6);
            }
        }
    }

    private static void composite(
        CommandEncoder encoder,
        GpuTextureView target,
        GpuTextureView blur,
        GpuTextureView depth,
        float opacity,
        float farPlane,
        float minThreshold,
        float maxThreshold
    ) {
        if (target != null && blur != null && depth != null) {
            dataBuffer.clear();
            dataBuffer.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F).putFloat(opacity);
            dataBuffer.putFloat(0.05F).putFloat(farPlane).putFloat(minThreshold).putFloat(maxThreshold);
            dataBuffer.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
            dataBuffer.flip();
            encoder.writeToBuffer(compositeUniform.slice(), dataBuffer);
            GpuSampler colorSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            GpuSampler depthSampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);

            try (RenderPass pass = encoder.createRenderPass(() -> "bamber client:fogblur_composite", target, OptionalInt.empty(), null, OptionalDouble.empty())) {
                pass.setPipeline(COMPOSITE_PIPELINE);
                pass.setVertexBuffer(0, dummyVertexBuffer);
                pass.bindTexture("BlurSampler", blur, colorSampler);
                pass.bindTexture("DepthSampler", depth, depthSampler);
                pass.setUniform("FogBlurData", compositeUniform);
                pass.draw(0, 6);
            }
        }
    }

    private static boolean usable(Framebuffer fb) {
        return fb != null
            && fb.getColorAttachment() != null
            && fb.getColorAttachmentView() != null
            && fb.getDepthAttachment() != null
            && fb.getDepthAttachmentView() != null
            && fb.textureWidth > 0
            && fb.textureHeight > 0;
    }

    private static int computePasses(float strength, int maxPasses) {
        int requested = strength >= 14.0F ? 4 : (strength >= 8.0F ? 3 : (strength >= 3.0F ? 2 : 1));
        return Math.max(1, Math.min(Math.max(1, maxPasses), requested));
    }

    private static float viewDistance() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc != null && mc.options != null ? Math.max(96.0F, (mc.options.getViewDistance().getValue() + 1) * 16.0F) : 192.0F;
    }

    private static void closeTargets() {
        for (int i = 0; i < 4; i++) {
            if (downViews[i] != null) {
                downViews[i].close();
                downViews[i] = null;
            }

            if (upViews[i] != null) {
                upViews[i].close();
                upViews[i] = null;
            }

            if (downTextures[i] != null) {
                downTextures[i].close();
                downTextures[i] = null;
            }

            if (upTextures[i] != null) {
                upTextures[i].close();
                upTextures[i] = null;
            }
        }

        sceneW = -1;
        sceneH = -1;
        blurW = -1;
        blurH = -1;
        allocatedPasses = 0;
        colorFormat = null;
    }

    private static void fail() {
        failures++;
        if (failures >= 3) {
            disabledAfterError = true;
            closeTargets();
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
