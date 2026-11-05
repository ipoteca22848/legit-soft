package com.example.auravisuals.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

public final class StableWorldFrameCapture {
    private static GpuTexture colorTexture;
    private static GpuTextureView colorView;
    private static int width = -1;
    private static int height = -1;
    private static TextureFormat colorFormat;
    private static boolean populated;

    private StableWorldFrameCapture() {
    }

    public static void captureCleanWorld() {
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        if (usable(framebuffer) && RenderSystem.tryGetDevice() != null) {
            try {
                ensureTexture(framebuffer);
                FramebufferBlitter.blit(framebuffer.getColorAttachmentView(), colorView);
                populated = true;
            } catch (Throwable var2) {
                populated = false;
            }
        } else {
            populated = false;
        }
    }

    public static GpuTextureView colorView() {
        return populated ? colorView : null;
    }

    public static int width() {
        return width;
    }

    public static int height() {
        return height;
    }

    private static void ensureTexture(Framebuffer framebuffer) {
        TextureFormat nextFormat = framebuffer.getColorAttachment().getFormat();
        if (colorTexture == null || width != framebuffer.textureWidth || height != framebuffer.textureHeight || colorFormat != nextFormat) {
            closeTexture();
            width = framebuffer.textureWidth;
            height = framebuffer.textureHeight;
            colorFormat = nextFormat;
            colorTexture = RenderSystem.getDevice().createTexture(() -> "bamber client:stable_world_color", 12, colorFormat, width, height, 1, 1);
            colorView = RenderSystem.getDevice().createTextureView(colorTexture);
        }
    }

    private static boolean usable(Framebuffer framebuffer) {
        return framebuffer != null
            && framebuffer.getColorAttachment() != null
            && framebuffer.getColorAttachmentView() != null
            && framebuffer.textureWidth > 0
            && framebuffer.textureHeight > 0;
    }

    private static void closeTexture() {
        if (colorView != null) {
            colorView.close();
            colorView = null;
        }

        if (colorTexture != null) {
            colorTexture.close();
            colorTexture = null;
        }

        populated = false;
        width = -1;
        height = -1;
        colorFormat = null;
    }
}
