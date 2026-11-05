package com.example.auravisuals.util.render;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.CustomHand;
import com.example.auravisuals.module.render.ViewModel;
import com.example.auravisuals.module.render.ViewModel$EditorSelection;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuBuffer.MappedView;
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
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

public final class HandFxPipeline {
    private static final int BUFFER_SIZE = 128;
    private static final int HIT_SAMPLE_SIDE = 7;
    private static final long HIT_SAMPLE_MAX_AGE_NANOS = 250000000L;
    private static final double HIT_SAMPLE_MOUSE_TOLERANCE = 9.0;
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
        .withLocation(Identifier.of("auravisuals", "pipeline/hand_fx"))
        .withVertexShader(Identifier.of("auravisuals", "core/hand_fx"))
        .withFragmentShader(Identifier.of("auravisuals", "core/hand_fx"))
        .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
        .withUniform("HandFxData", UniformType.UNIFORM_BUFFER)
        .withSampler("Sampler0")
        .withSampler("Sampler1")
        .withSampler("HandDepthSampler")
        .withSampler("BeforeHandDepthSampler")
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .withCull(false)
        .build();
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3f MODEL_OFFSET = new Vector3f(0.0F, 0.0F, 0.0F);
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer dummyVertexBuffer;
    private static GpuBuffer beforeHitBuffer;
    private static GpuBuffer mainHitBuffer;
    private static GpuBuffer afterHitBuffer;
    private static ByteBuffer dataBuffer;
    private static GpuTexture beforeTex;
    private static GpuTexture mainHandTex;
    private static GpuTexture afterTex;
    private static GpuTextureView beforeView;
    private static GpuTextureView mainHandView;
    private static GpuTextureView afterView;
    private static GpuTexture beforeDepthTex;
    private static GpuTexture mainHandDepthTex;
    private static GpuTexture afterDepthTex;
    private static GpuTextureView beforeDepthView;
    private static GpuTextureView mainHandDepthView;
    private static GpuTextureView afterDepthView;
    private static int lastW;
    private static int lastH;
    private static TextureFormat lastFormat;
    private static boolean initialized;
    private static boolean hasBefore;
    private static boolean hasMainHandSplit;
    private static boolean pendingPair;
    private static boolean hitReadbackPending;
    private static boolean beforeHitReady;
    private static boolean mainHitReady;
    private static boolean afterHitReady;
    private static long textureGeneration;
    private static long hitReadbackGeneration;
    private static CommandEncoder hitMapEncoder;
    private static double pendingHitMouseX;
    private static double pendingHitMouseY;
    private static volatile double sampledHitMouseX;
    private static volatile double sampledHitMouseY;
    private static volatile long sampledHitAtNanos;
    private static volatile boolean sampledHandPixel;
    private static volatile double sampledMainHandDistanceSq = Double.POSITIVE_INFINITY;
    private static volatile double sampledOffHandDistanceSq = Double.POSITIVE_INFINITY;

    private HandFxPipeline() {
    }

    private static void init() {
        if (!initialized) {
            dataBuffer = MemoryUtil.memAlloc(128);
            ByteBuffer dummy = MemoryUtil.memAlloc(4);
            dummy.putInt(0).flip();
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "bamber client:hand_fx_dummy", 32, dummy);
            MemoryUtil.memFree(dummy);
            initialized = true;
        }
    }

    private static boolean ensureTextures(int w, int h, TextureFormat format) {
        if (beforeTex != null && w == lastW && h == lastH && format == lastFormat) {
            return true;
        }

        closeTextures();
        beforeTex = RenderSystem.getDevice().createTexture(() -> "bamber client:hand_fx_before", 14, format, w, h, 1, 1);
        afterTex = RenderSystem.getDevice().createTexture(() -> "bamber client:hand_fx_after", 14, format, w, h, 1, 1);
        mainHandTex = RenderSystem.getDevice().createTexture(() -> "bamber client:hand_fx_main_hand", 14, format, w, h, 1, 1);
        int depthUsage = 5;
        beforeDepthTex = RenderSystem.getDevice().createTexture(() -> "bamber client:hand_fx_depth_before", depthUsage, TextureFormat.DEPTH32, w, h, 1, 1);
        mainHandDepthTex = RenderSystem.getDevice().createTexture(() -> "bamber client:hand_fx_depth_main", depthUsage, TextureFormat.DEPTH32, w, h, 1, 1);
        afterDepthTex = RenderSystem.getDevice().createTexture(() -> "bamber client:hand_fx_depth_after", depthUsage, TextureFormat.DEPTH32, w, h, 1, 1);
        beforeView = RenderSystem.getDevice().createTextureView(beforeTex);
        mainHandView = RenderSystem.getDevice().createTextureView(mainHandTex);
        afterView = RenderSystem.getDevice().createTextureView(afterTex);
        beforeDepthView = RenderSystem.getDevice().createTextureView(beforeDepthTex);
        mainHandDepthView = RenderSystem.getDevice().createTextureView(mainHandDepthTex);
        afterDepthView = RenderSystem.getDevice().createTextureView(afterDepthTex);
        lastW = w;
        lastH = h;
        lastFormat = format;
        textureGeneration++;
        return beforeTex != null && afterTex != null;
    }

    public static void captureBefore() {
        pendingPair = false;
        hasMainHandSplit = false;
        if (RenderSystem.tryGetDevice() != null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            Framebuffer fb = mc.getFramebuffer();
            if (fb != null && fb.getColorAttachment() != null && fb.getColorAttachmentView() != null && fb.getDepthAttachment() != null) {
                init();
                if (ensureTextures(fb.textureWidth, fb.textureHeight, fb.getColorAttachment().getFormat())) {
                    CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                    FramebufferBlitter.blit(encoder, fb.getColorAttachmentView(), beforeView);
                    copyDepth(encoder, fb.getDepthAttachment(), beforeDepthTex);
                    hasBefore = true;
                }
            }
        }
    }

    public static void captureMainHandSplit() {
        if (hasBefore && RenderSystem.tryGetDevice() != null && mainHandView != null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            Framebuffer fb = mc.getFramebuffer();
            if (fb != null && fb.getColorAttachmentView() != null && fb.getDepthAttachment() != null) {
                CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                FramebufferBlitter.blit(encoder, fb.getColorAttachmentView(), mainHandView);
                copyDepth(encoder, fb.getDepthAttachment(), mainHandDepthTex);
                hasMainHandSplit = true;
            }
        }
    }

    public static boolean isCaptureArmed() {
        return hasBefore;
    }

    public static void captureAfterAndComposite(int mode, float intensity, float timeSec, float alpha, int rgb) {
        if (captureAfter()) {
            composite(mode, intensity, timeSec, alpha, rgb);
        }
    }

    public static boolean captureAfter() {
        if (hasBefore && RenderSystem.tryGetDevice() != null) {
            hasBefore = false;
            MinecraftClient mc = MinecraftClient.getInstance();
            Framebuffer fb = mc.getFramebuffer();
            if (fb != null && fb.getColorAttachment() != null && fb.getColorAttachmentView() != null && fb.getDepthAttachment() != null) {
                init();
                if (!ensureTextures(fb.textureWidth, fb.textureHeight, fb.getColorAttachment().getFormat())) {
                    return false;
                }

                CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                FramebufferBlitter.blit(encoder, fb.getColorAttachmentView(), afterView);
                copyDepth(encoder, fb.getDepthAttachment(), afterDepthTex);
                pendingPair = true;
                if (ViewModel.isEditorSessionActive()) {
                    queueEditorHitSample();
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static void copyDepth(CommandEncoder encoder, GpuTexture source, GpuTexture target) {
        if (encoder != null && source != null && target != null) {
            encoder.copyTextureToTexture(source, target, 0, 0, 0, 0, 0, lastW, lastH);
        }
    }

    public static boolean isEditorHandPixelAt(double mouseX, double mouseY) {
        long age = System.nanoTime() - sampledHitAtNanos;
        if (sampledHitAtNanos != 0L && age >= 0L && age <= 250000000L) {
            double dx = mouseX - sampledHitMouseX;
            double dy = mouseY - sampledHitMouseY;
            return dx * dx + dy * dy <= 81.0 && sampledHandPixel;
        } else {
            return false;
        }
    }

    public static double editorHandHitDistance(boolean mainHand, double mouseX, double mouseY) {
        long age = System.nanoTime() - sampledHitAtNanos;
        if (sampledHitAtNanos != 0L && age >= 0L && age <= 250000000L) {
            double dx = mouseX - sampledHitMouseX;
            double dy = mouseY - sampledHitMouseY;
            if (dx * dx + dy * dy > 81.0) {
                return Double.POSITIVE_INFINITY;
            } else {
                return mainHand ? sampledMainHandDistanceSq : sampledOffHandDistanceSq;
            }
        } else {
            return Double.POSITIVE_INFINITY;
        }
    }

    private static void queueEditorHitSample() {
        if (!hitReadbackPending
            && beforeTex != null
            && mainHandTex != null
            && afterTex != null
            && hasMainHandSplit
            && lastFormat != null
            && lastW >= 7
            && lastH >= 7) {
            long requiredBufferSize = (long)lastW * lastH * lastFormat.pixelSize();
            if (beforeHitBuffer == null || beforeHitBuffer.size() < requiredBufferSize) {
                if (beforeHitBuffer != null) {
                    beforeHitBuffer.close();
                }

                if (mainHitBuffer != null) {
                    mainHitBuffer.close();
                }

                if (afterHitBuffer != null) {
                    afterHitBuffer.close();
                }

                beforeHitBuffer = RenderSystem.getDevice().createBuffer(() -> "bamber client:hand_fx_hit_before", 9, requiredBufferSize);
                mainHitBuffer = RenderSystem.getDevice().createBuffer(() -> "bamber client:hand_fx_hit_main", 9, requiredBufferSize);
                afterHitBuffer = RenderSystem.getDevice().createBuffer(() -> "bamber client:hand_fx_hit_after", 9, requiredBufferSize);
            }

            MinecraftClient client = MinecraftClient.getInstance();
            int scaledWidth = Math.max(1, client.getWindow().getScaledWidth());
            int scaledHeight = Math.max(1, client.getWindow().getScaledHeight());
            double mouseX = client.mouse.getX() * scaledWidth / client.getWindow().getWidth();
            double mouseY = client.mouse.getY() * scaledHeight / client.getWindow().getHeight();
            int centerX = (int)Math.floor(mouseX * lastW / scaledWidth);
            int centerY = (int)Math.floor((scaledHeight - mouseY) * lastH / scaledHeight);
            int sourceX = Math.max(0, Math.min(lastW - 7, centerX - 3));
            int sourceY = Math.max(0, Math.min(lastH - 7, centerY - 3));
            hitReadbackPending = true;
            beforeHitReady = false;
            mainHitReady = false;
            afterHitReady = false;
            hitReadbackGeneration = textureGeneration;
            pendingHitMouseX = mouseX;
            pendingHitMouseY = mouseY;
            hitMapEncoder = RenderSystem.getDevice().createCommandEncoder();
            CommandEncoder copyEncoder = RenderSystem.getDevice().createCommandEncoder();
            long generation = hitReadbackGeneration;
            copyEncoder.copyTextureToBuffer(beforeTex, beforeHitBuffer, 0L, () -> onHitReadbackReady(0, generation), 0, sourceX, sourceY, 7, 7);
            copyEncoder.copyTextureToBuffer(mainHandTex, mainHitBuffer, 0L, () -> onHitReadbackReady(1, generation), 0, sourceX, sourceY, 7, 7);
            copyEncoder.copyTextureToBuffer(afterTex, afterHitBuffer, 0L, () -> onHitReadbackReady(2, generation), 0, sourceX, sourceY, 7, 7);
        }
    }

    private static synchronized void onHitReadbackReady(int stage, long generation) {
        if (hitReadbackPending && generation == hitReadbackGeneration && generation == textureGeneration) {
            if (stage == 0) {
                beforeHitReady = true;
            } else if (stage == 1) {
                mainHitReady = true;
            } else {
                afterHitReady = true;
            }

            if (beforeHitReady && mainHitReady && afterHitReady) {
                double mainDistanceSq = Double.POSITIVE_INFINITY;
                double offDistanceSq = Double.POSITIVE_INFINITY;

                try {
                    try (
                        MappedView beforeMap = hitMapEncoder.mapBuffer(beforeHitBuffer, true, false);
                        MappedView mainMap = hitMapEncoder.mapBuffer(mainHitBuffer, true, false);
                        MappedView afterMap = hitMapEncoder.mapBuffer(afterHitBuffer, true, false);
                    ) {
                        ByteBuffer beforeData = beforeMap.data();
                        ByteBuffer mainData = mainMap.data();
                        ByteBuffer afterData = afterMap.data();
                        int pixelSize = Math.max(1, lastFormat.pixelSize());
                        int pixels = Math.min(49, Math.min(beforeData.remaining(), Math.min(mainData.remaining(), afterData.remaining())) / pixelSize);
                        int center = 3;

                        for (int pixel = 0; pixel < pixels; pixel++) {
                            int offset = pixel * pixelSize;
                            int channels = Math.min(3, pixelSize);
                            int mainDifferenceSquared = 0;
                            int offDifferenceSquared = 0;

                            for (int channel = 0; channel < channels; channel++) {
                                int mainDelta = (mainData.get(offset + channel) & 255) - (beforeData.get(offset + channel) & 255);
                                int offDelta = (afterData.get(offset + channel) & 255) - (mainData.get(offset + channel) & 255);
                                mainDifferenceSquared += mainDelta * mainDelta;
                                offDifferenceSquared += offDelta * offDelta;
                            }

                            int px = pixel % 7 - center;
                            int py = pixel / 7 - center;
                            double distanceSq = px * px + py * py;
                            if (mainDifferenceSquared >= 25) {
                                mainDistanceSq = Math.min(mainDistanceSq, distanceSq);
                            }

                            if (offDifferenceSquared >= 25) {
                                offDistanceSq = Math.min(offDistanceSq, distanceSq);
                            }
                        }
                    }

                    sampledHitMouseX = pendingHitMouseX;
                    sampledHitMouseY = pendingHitMouseY;
                    sampledMainHandDistanceSq = mainDistanceSq;
                    sampledOffHandDistanceSq = offDistanceSq;
                } catch (Throwable ignored) {
                    try {
                        mainDistanceSq = Double.POSITIVE_INFINITY;
                        offDistanceSq = Double.POSITIVE_INFINITY;
                        sampledHitMouseX = pendingHitMouseX;
                        sampledHitMouseY = pendingHitMouseY;
                        sampledMainHandDistanceSq = mainDistanceSq;
                        sampledOffHandDistanceSq = offDistanceSq;
                    } catch (Throwable throwable) {
                        sampledHitMouseX = pendingHitMouseX;
                        sampledHitMouseY = pendingHitMouseY;
                        sampledMainHandDistanceSq = mainDistanceSq;
                        sampledOffHandDistanceSq = offDistanceSq;
                        sampledHandPixel = Double.isFinite(mainDistanceSq) || Double.isFinite(offDistanceSq);
                        sampledHitAtNanos = System.nanoTime();
                        hitReadbackPending = false;
                        beforeHitReady = false;
                        mainHitReady = false;
                        afterHitReady = false;
                        hitMapEncoder = null;
                        throw throwable;
                    }

                    sampledHandPixel = Double.isFinite(mainDistanceSq) || Double.isFinite(offDistanceSq);
                    sampledHitAtNanos = System.nanoTime();
                    hitReadbackPending = false;
                    beforeHitReady = false;
                    mainHitReady = false;
                    afterHitReady = false;
                    hitMapEncoder = null;
                }

                sampledHandPixel = Double.isFinite(mainDistanceSq) || Double.isFinite(offDistanceSq);
                sampledHitAtNanos = System.nanoTime();
                hitReadbackPending = false;
                beforeHitReady = false;
                mainHitReady = false;
                afterHitReady = false;
                hitMapEncoder = null;
            }
        }
    }

    public static void renderPendingCustomHandEffects() {
        if (pendingPair) {
            pendingPair = false;
            CustomHand module = ModuleManager.INSTANCE.getModule(CustomHand.class);
            if (module != null && module.isEnabled()) {
                if (CustomHand.blur.getValue()) {
                    float opacity = Math.max(0.05F, Math.min(1.0F, CustomHand.blurOpacity.getFloatValue() / 100.0F));
                    Color tint = CustomHand.blurTint.getColor();
                    composite(1, CustomHand.blurStrength.getFloatValue(), 0.0F, opacity, tint.getRGB());
                }

                if (CustomHand.itemOutline.getValue()) {
                    Color color = CustomHand.itemOutlineColor.getColor();
                    composite(3, 1.0F, 0.0F, color.getAlpha() / 255.0F, color.getRGB());
                }
            }

            ViewModel$EditorSelection editorSelection;
            if ((editorSelection = ViewModel.editorOutlineSelection()) != null) {
                compositeEditor(editorSelection, editorSelection.opacity(), ViewModel.editorOutlineColor());
            }
        }
    }

    private static void compositeEditor(ViewModel$EditorSelection selection, float alpha, int rgb) {
        GpuTextureView selectedAfter = afterView;
        GpuTextureView selectedBefore = beforeView;
        GpuTextureView selectedDepthAfter = afterDepthView;
        GpuTextureView selectedDepthBefore = beforeDepthView;
        float otherX = selection.otherCenterX();
        float otherY = selection.otherCenterY();
        if (hasMainHandSplit && mainHandView != null) {
            if (selection.mainHand()) {
                selectedAfter = mainHandView;
                selectedDepthAfter = mainHandDepthView;
            } else {
                selectedBefore = mainHandView;
                selectedDepthBefore = mainHandDepthView;
            }

            otherX = -10.0F;
            otherY = -10.0F;
        }

        composite(
            5,
            selection.expansion(),
            0.0F,
            alpha,
            rgb,
            selection.centerX(),
            selection.centerY(),
            otherX,
            otherY,
            selectedAfter,
            selectedBefore,
            selectedDepthAfter,
            selectedDepthBefore
        );
    }

    public static void composite(int mode, float intensity, float timeSec, float alpha, int rgb) {
        composite(mode, intensity, timeSec, alpha, rgb, 0.5F, 0.5F, 0.5F, 0.5F);
    }

    private static void composite(
        int mode, float intensity, float timeSec, float alpha, int rgb, float regionCenterX, float regionCenterY, float regionHalfWidth, float regionHalfHeight
    ) {
        composite(
            mode,
            intensity,
            timeSec,
            alpha,
            rgb,
            regionCenterX,
            regionCenterY,
            regionHalfWidth,
            regionHalfHeight,
            afterView,
            beforeView,
            afterDepthView,
            beforeDepthView
        );
    }

    private static void composite(
        int mode,
        float intensity,
        float timeSec,
        float alpha,
        int rgb,
        float regionCenterX,
        float regionCenterY,
        float regionHalfWidth,
        float regionHalfHeight,
        GpuTextureView sourceAfter,
        GpuTextureView sourceBefore,
        GpuTextureView depthAfter,
        GpuTextureView depthBefore
    ) {
        if (RenderSystem.tryGetDevice() != null && afterView != null && beforeView != null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            Framebuffer fb = mc.getFramebuffer();
            if (fb != null && fb.getColorAttachmentView() != null && depthAfter != null && depthBefore != null) {
                CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                prepareData(
                    mode, intensity, timeSec, alpha, rgb, fb.textureWidth, fb.textureHeight, regionCenterX, regionCenterY, regionHalfWidth, regionHalfHeight
                );
                encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);
                GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
                GpuSampler depthSampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
                GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                    .write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

                try (RenderPass pass = encoder.createRenderPass(
                        () -> "bamber client:hand_fx", fb.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                    )) {
                    pass.setPipeline(PIPELINE);
                    pass.setVertexBuffer(0, dummyVertexBuffer);
                    pass.bindTexture("Sampler0", sourceAfter, sampler);
                    pass.bindTexture("Sampler1", sourceBefore, sampler);
                    pass.bindTexture("HandDepthSampler", depthAfter, depthSampler);
                    pass.bindTexture("BeforeHandDepthSampler", depthBefore, depthSampler);
                    RenderSystem.bindDefaultUniforms(pass);
                    pass.setUniform("DynamicTransforms", dynamicTransforms);
                    pass.setUniform("HandFxData", uniformBuffer);
                    pass.draw(0, 6);
                }
            }
        }
    }

    private static void prepareData(
        int mode,
        float intensity,
        float timeSec,
        float alpha,
        int rgb,
        int w,
        int h,
        float regionCenterX,
        float regionCenterY,
        float regionHalfWidth,
        float regionHalfHeight
    ) {
        float r = (rgb >> 16 & 0xFF) / 255.0F;
        float g = (rgb >> 8 & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        float a = (rgb >>> 24 & 0xFF) / 255.0F;
        int scaledWidth = Math.max(1, MinecraftClient.getInstance().getWindow().getScaledWidth());
        int scaledHeight = Math.max(1, MinecraftClient.getInstance().getWindow().getScaledHeight());
        Matrix4f ortho = new Matrix4f().setOrtho(0.0F, scaledWidth, scaledHeight, 0.0F, -1000.0F, 1000.0F);
        dataBuffer.clear();
        dataBuffer.putFloat(ortho.m00()).putFloat(ortho.m01()).putFloat(ortho.m02()).putFloat(ortho.m03());
        dataBuffer.putFloat(ortho.m10()).putFloat(ortho.m11()).putFloat(ortho.m12()).putFloat(ortho.m13());
        dataBuffer.putFloat(ortho.m20()).putFloat(ortho.m21()).putFloat(ortho.m22()).putFloat(ortho.m23());
        dataBuffer.putFloat(ortho.m30()).putFloat(ortho.m31()).putFloat(ortho.m32()).putFloat(ortho.m33());
        dataBuffer.putFloat(mode).putFloat(intensity).putFloat(timeSec).putFloat(alpha);
        dataBuffer.putFloat(r).putFloat(g).putFloat(b).putFloat(a);
        dataBuffer.putFloat(scaledWidth).putFloat(scaledHeight).putFloat(w).putFloat(h);
        dataBuffer.putFloat(regionCenterX).putFloat(regionCenterY).putFloat(regionHalfWidth).putFloat(regionHalfHeight);
        dataBuffer.flip();
        ensureBuffer();
    }

    private static void ensureBuffer() {
        int size = dataBuffer.remaining();
        if (uniformBuffer == null || uniformBuffer.size() < size) {
            if (uniformBuffer != null) {
                uniformBuffer.close();
            }

            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "bamber client:hand_fx_uniform", 136, size);
        }
    }

    private static void closeTextures() {
        hitReadbackPending = false;
        beforeHitReady = false;
        mainHitReady = false;
        afterHitReady = false;
        hitMapEncoder = null;
        sampledHitAtNanos = 0L;
        sampledMainHandDistanceSq = Double.POSITIVE_INFINITY;
        sampledOffHandDistanceSq = Double.POSITIVE_INFINITY;
        if (beforeView != null) {
            beforeView.close();
            beforeView = null;
        }

        if (mainHandView != null) {
            mainHandView.close();
            mainHandView = null;
        }

        if (afterView != null) {
            afterView.close();
            afterView = null;
        }

        if (beforeDepthView != null) {
            beforeDepthView.close();
            beforeDepthView = null;
        }

        if (mainHandDepthView != null) {
            mainHandDepthView.close();
            mainHandDepthView = null;
        }

        if (afterDepthView != null) {
            afterDepthView.close();
            afterDepthView = null;
        }

        if (beforeTex != null) {
            beforeTex.close();
            beforeTex = null;
        }

        if (mainHandTex != null) {
            mainHandTex.close();
            mainHandTex = null;
        }

        if (afterTex != null) {
            afterTex.close();
            afterTex = null;
        }

        if (beforeDepthTex != null) {
            beforeDepthTex.close();
            beforeDepthTex = null;
        }

        if (mainHandDepthTex != null) {
            mainHandDepthTex.close();
            mainHandDepthTex = null;
        }

        if (afterDepthTex != null) {
            afterDepthTex.close();
            afterDepthTex = null;
        }

        if (beforeHitBuffer != null) {
            beforeHitBuffer.close();
            beforeHitBuffer = null;
        }

        if (mainHitBuffer != null) {
            mainHitBuffer.close();
            mainHitBuffer = null;
        }

        if (afterHitBuffer != null) {
            afterHitBuffer.close();
            afterHitBuffer = null;
        }

        lastW = 0;
        lastH = 0;
        lastFormat = null;
    }

    public static void invalidate() {
        hasBefore = false;
        hasMainHandSplit = false;
        pendingPair = false;
        sampledHitAtNanos = 0L;
        sampledMainHandDistanceSq = Double.POSITIVE_INFINITY;
        sampledOffHandDistanceSq = Double.POSITIVE_INFINITY;
    }
}
