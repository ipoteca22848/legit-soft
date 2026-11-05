package com.example.auravisuals.util.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
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

public final class HandsFlamePipeline {
    private static final int UNIFORM_BYTES = 64;
    private static final int FAILURE_LIMIT = 3;
    private static final int TRAIL_DOWNSAMPLE = 2;
    private static final RenderPipeline TRAIL_PIPELINE = RenderPipeline.builder()
        .withLocation(Identifier.of("auravisuals", "pipeline/effects/hands_flame_trail"))
        .withVertexShader(Identifier.of("auravisuals", "effects/hands_flame/fullscreen"))
        .withFragmentShader(Identifier.of("auravisuals", "effects/hands_flame/trail"))
        .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
        .withUniform("HandsFlameData", UniformType.UNIFORM_BUFFER)
        .withSampler("BeforeSampler")
        .withSampler("AfterSampler")
        .withSampler("PrevTrailSampler")
        .withSampler("HandDepthSampler")
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .withCull(false)
        .build();
    private static final RenderPipeline COMPOSITE_PIPELINE = RenderPipeline.builder()
        .withLocation(Identifier.of("auravisuals", "pipeline/effects/hands_flame_composite"))
        .withVertexShader(Identifier.of("auravisuals", "effects/hands_flame/fullscreen"))
        .withFragmentShader(Identifier.of("auravisuals", "effects/hands_flame/composite"))
        .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
        .withUniform("HandsFlameData", UniformType.UNIFORM_BUFFER)
        .withSampler("SceneSampler")
        .withSampler("BeforeSampler")
        .withSampler("TrailSampler")
        .withSampler("HandDepthSampler")
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .withCull(false)
        .build();
    private static float strength = 0.85F;
    private static float riseSpeed;
    private static float wobble = 0.65F;
    private static float length = 0.95F;
    private static float brightness = 0.9F;
    private static int colorMode;
    private static int color = -419430401;
    private static boolean itemsOnly;
    private static boolean rightEnabled = true;
    private static boolean leftEnabled = true;
    private static ByteBuffer dataBuffer;
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer dummyVertexBuffer;
    private static GpuTexture beforeTexture;
    private static GpuTexture afterTexture;
    private static GpuTexture trailTextureA;
    private static GpuTexture trailTextureB;
    private static GpuTextureView beforeView;
    private static GpuTextureView afterView;
    private static GpuTextureView trailViewA;
    private static GpuTextureView trailViewB;
    private static int lastWidth = -1;
    private static int lastHeight = -1;
    private static TextureFormat sourceFormat;
    private static boolean initialized;
    private static boolean capturedBeforeHands;
    private static boolean awaitingAfterCapture;
    private static boolean pendingPair;
    private static boolean useTrailAAsHistory = true;
    private static boolean disabledAfterError;
    private static int failures;

    private HandsFlamePipeline() {
    }

    public static void clearError() {
        disabledAfterError = false;
        failures = 0;
        capturedBeforeHands = false;
        awaitingAfterCapture = false;
        pendingPair = false;
    }

    public static void configure(
        float strength,
        float riseSpeed,
        float wobble,
        float length,
        float brightness,
        int colorMode,
        int color,
        boolean itemsOnly,
        boolean rightEnabled,
        boolean leftEnabled
    ) {
        HandsFlamePipeline.strength = clamp(strength, 0.0F, 2.0F);
        HandsFlamePipeline.riseSpeed = clamp(riseSpeed, 0.0F, 2.0F);
        HandsFlamePipeline.wobble = clamp(wobble, 0.0F, 2.0F);
        HandsFlamePipeline.length = clamp(length, 0.1F, 2.5F);
        HandsFlamePipeline.brightness = clamp(brightness, 0.0F, 2.0F);
        HandsFlamePipeline.colorMode = Math.max(0, Math.min(2, colorMode));
        HandsFlamePipeline.color = color;
        HandsFlamePipeline.itemsOnly = itemsOnly;
        HandsFlamePipeline.rightEnabled = rightEnabled;
        HandsFlamePipeline.leftEnabled = leftEnabled;
    }

    public static void resetTrail() {
        capturedBeforeHands = false;
        awaitingAfterCapture = false;
        pendingPair = false;
        if (RenderSystem.tryGetDevice() != null && trailTextureA != null && trailTextureB != null) {
            try {
                CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                encoder.clearColorTexture(trailTextureA, 0);
                encoder.clearColorTexture(trailTextureB, 0);
            } catch (Throwable throwable) {
                fail(throwable);
            }
        }
    }

    public static void captureBeforeHandRender() {
        capturedBeforeHands = false;
        awaitingAfterCapture = false;
        pendingPair = false;
        if (shouldRender()) {
            Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
            if (usable(framebuffer) && ensureReady(framebuffer)) {
                try {
                    FramebufferBlitter.blit(framebuffer.getColorAttachmentView(), beforeView);
                    capturedBeforeHands = true;
                } catch (Throwable throwable) {
                    fail(throwable);
                }
            }
        }
    }

    public static void renderCapturedHandsFlame() {
        if (capturedBeforeHands) {
            capturedBeforeHands = false;
            awaitingAfterCapture = shouldRender();
        }
    }

    public static void captureAfterHands() {
        if (awaitingAfterCapture) {
            awaitingAfterCapture = false;
            Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
            if (usable(framebuffer) && ensureReady(framebuffer)) {
                try {
                    FramebufferBlitter.blit(framebuffer.getColorAttachmentView(), afterView);
                    pendingPair = true;
                } catch (Throwable throwable) {
                    fail(throwable);
                }
            }
        }
    }

    public static void renderReadyHandsFlame() {
        if (pendingPair) {
            pendingPair = false;
            if (shouldRender()) {
                Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
                if (usable(framebuffer) && ensureReady(framebuffer)) {
                    try {
                        writeUniforms(framebuffer.textureWidth, framebuffer.textureHeight);
                        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                        encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);
                        renderTrail(encoder, framebuffer);
                        swapTrailHistory();
                        renderComposite(encoder, framebuffer);
                        failures = 0;
                    } catch (Throwable throwable) {
                        fail(throwable);
                    }
                }
            }
        }
    }

    private static boolean shouldRender() {
        if (!disabledAfterError && RenderSystem.tryGetDevice() != null && (rightEnabled || leftEnabled)) {
            MinecraftClient client = MinecraftClient.getInstance();
            return client != null && client.player != null && client.world != null
                ? !itemsOnly || !client.player.getMainHandStack().isEmpty() || !client.player.getOffHandStack().isEmpty()
                : false;
        } else {
            return false;
        }
    }

    private static boolean ensureReady(Framebuffer framebuffer) {
        init();
        ensureTextures(framebuffer.textureWidth, framebuffer.textureHeight, framebuffer.getColorAttachment().getFormat());
        return dataBuffer != null
            && uniformBuffer != null
            && dummyVertexBuffer != null
            && beforeView != null
            && afterView != null
            && trailViewA != null
            && trailViewB != null;
    }

    private static void init() {
        if (!initialized) {
            dataBuffer = MemoryUtil.memAlloc(64);
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "bamber client:hands_flame_uniform", 136, 64L);
            ByteBuffer dummy = MemoryUtil.memAlloc(4);
            dummy.putInt(0).flip();
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "bamber client:hands_flame_dummy", 40, dummy);
            MemoryUtil.memFree(dummy);
            initialized = true;
        }
    }

    private static void ensureTextures(int width, int height, TextureFormat format) {
        if (beforeTexture == null || width != lastWidth || height != lastHeight || sourceFormat != format) {
            closeTextures();
            int snapshotUsage = 12;
            beforeTexture = createTexture("bamber client:hands_flame_before", width, height, snapshotUsage, format);
            afterTexture = createTexture("bamber client:hands_flame_after", width, height, snapshotUsage, format);
            int trailUsage = 12;
            int trailWidth = Math.max(1, (width + 2 - 1) / 2);
            int trailHeight = Math.max(1, (height + 2 - 1) / 2);
            trailTextureA = createTexture("bamber client:hands_flame_trail_a", trailWidth, trailHeight, trailUsage, TextureFormat.RGBA8);
            trailTextureB = createTexture("bamber client:hands_flame_trail_b", trailWidth, trailHeight, trailUsage, TextureFormat.RGBA8);
            beforeView = RenderSystem.getDevice().createTextureView(beforeTexture);
            afterView = RenderSystem.getDevice().createTextureView(afterTexture);
            trailViewA = RenderSystem.getDevice().createTextureView(trailTextureA);
            trailViewB = RenderSystem.getDevice().createTextureView(trailTextureB);
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.clearColorTexture(trailTextureA, 0);
            encoder.clearColorTexture(trailTextureB, 0);
            useTrailAAsHistory = true;
            lastWidth = width;
            lastHeight = height;
            sourceFormat = format;
        }
    }

    private static GpuTexture createTexture(String label, int width, int height, int usage, TextureFormat format) {
        return RenderSystem.getDevice().createTexture(() -> label, usage, format, width, height, 1, 1);
    }

    private static void writeUniforms(int width, int height) {
        int a = color >>> 24 & 0xFF;
        int r = color >>> 16 & 0xFF;
        int g = color >>> 8 & 0xFF;
        int b = color & 0xFF;
        float alpha = a <= 0 ? 1.0F : a / 255.0F;
        dataBuffer.clear();
        dataBuffer.putFloat(r / 255.0F).putFloat(g / 255.0F).putFloat(b / 255.0F).putFloat(alpha);
        dataBuffer.putFloat(strength).putFloat(riseSpeed).putFloat(wobble).putFloat(length);
        dataBuffer.putFloat(brightness).putFloat(timeSec()).putFloat(colorMode + (itemsOnly ? 10.0F : 0.0F)).putFloat(alpha);
        dataBuffer.putFloat(rightEnabled ? 1.0F : 0.0F)
            .putFloat(leftEnabled ? 1.0F : 0.0F)
            .putFloat(1.0F / Math.max(1, width))
            .putFloat(1.0F / Math.max(1, height));
        dataBuffer.flip();
    }

    private static void renderTrail(CommandEncoder encoder, Framebuffer framebuffer) {
        GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        GpuSampler depthSampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);

        try (RenderPass pass = encoder.createRenderPass(
                () -> "bamber client:hands_flame_trail", nextTrailView(), OptionalInt.empty(), null, OptionalDouble.empty()
            )) {
            pass.setPipeline(TRAIL_PIPELINE);
            pass.setVertexBuffer(0, dummyVertexBuffer);
            pass.bindTexture("BeforeSampler", beforeView, sampler);
            pass.bindTexture("AfterSampler", afterView, sampler);
            pass.bindTexture("PrevTrailSampler", historyTrailView(), sampler);
            pass.bindTexture("HandDepthSampler", framebuffer.getDepthAttachmentView(), depthSampler);
            pass.setUniform("HandsFlameData", uniformBuffer);
            pass.draw(0, 6);
        }
    }

    private static void renderComposite(CommandEncoder encoder, Framebuffer framebuffer) {
        GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        GpuSampler depthSampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);

        try (RenderPass pass = encoder.createRenderPass(
                () -> "bamber client:hands_flame_composite", framebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
            )) {
            pass.setPipeline(COMPOSITE_PIPELINE);
            pass.setVertexBuffer(0, dummyVertexBuffer);
            pass.bindTexture("SceneSampler", afterView, sampler);
            pass.bindTexture("BeforeSampler", beforeView, sampler);
            pass.bindTexture("TrailSampler", historyTrailView(), sampler);
            pass.bindTexture("HandDepthSampler", framebuffer.getDepthAttachmentView(), depthSampler);
            pass.setUniform("HandsFlameData", uniformBuffer);
            pass.draw(0, 6);
        }
    }

    private static GpuTextureView historyTrailView() {
        return useTrailAAsHistory ? trailViewA : trailViewB;
    }

    private static GpuTextureView nextTrailView() {
        return useTrailAAsHistory ? trailViewB : trailViewA;
    }

    private static void swapTrailHistory() {
        useTrailAAsHistory = !useTrailAAsHistory;
    }

    private static boolean usable(Framebuffer framebuffer) {
        return framebuffer != null
            && framebuffer.getColorAttachment() != null
            && framebuffer.getColorAttachmentView() != null
            && framebuffer.getDepthAttachmentView() != null
            && framebuffer.textureWidth > 0
            && framebuffer.textureHeight > 0;
    }

    private static void closeTextures() {
        if (beforeView != null) {
            beforeView.close();
            beforeView = null;
        }

        if (afterView != null) {
            afterView.close();
            afterView = null;
        }

        if (trailViewA != null) {
            trailViewA.close();
            trailViewA = null;
        }

        if (trailViewB != null) {
            trailViewB.close();
            trailViewB = null;
        }

        if (beforeTexture != null) {
            beforeTexture.close();
            beforeTexture = null;
        }

        if (afterTexture != null) {
            afterTexture.close();
            afterTexture = null;
        }

        if (trailTextureA != null) {
            trailTextureA.close();
            trailTextureA = null;
        }

        if (trailTextureB != null) {
            trailTextureB.close();
            trailTextureB = null;
        }

        lastWidth = -1;
        lastHeight = -1;
        sourceFormat = null;
    }

    private static void fail(Throwable throwable) {
        throwable.printStackTrace();
        capturedBeforeHands = false;
        awaitingAfterCapture = false;
        pendingPair = false;
        if (++failures >= 3) {
            disabledAfterError = true;
        }
    }

    private static float timeSec() {
        return (float)(System.nanoTime() % 180000000000L) / 1.0E9F;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
