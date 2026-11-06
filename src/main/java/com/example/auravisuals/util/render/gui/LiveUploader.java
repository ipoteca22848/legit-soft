package com.example.auravisuals.util.render.gui;

import java.awt.image.BufferedImage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.TextureManager;

public final class LiveUploader {
    static final int BATCH = 4;
    static volatile LiveUploader$Job current;

    LiveUploader() {
    }

    public static void requestFromImages(GifOption opt, int slot, BufferedImage[] imgs) {
        if (!opt.isReady() && !opt.isLoading()) {
            opt.setLoading(true);
            opt.setLoadedFrames(0);
            cancelCurrentIfDifferent(opt);
            current = new LiveUploader$Job(opt, slot, imgs);
        }
    }

    public static void requestFromDisk(GifOption opt, int slot) {
        if (!opt.isReady() && !opt.isLoading()) {
            opt.setLoading(true);
            opt.setLoadedFrames(0);
            Thread reader = new Thread(() -> {
                try {
                    BufferedImage[] imgs = MediaProcessor.readSlotFrames(slot, opt.frameCount);
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc != null) {
                        mc.execute(() -> {
                            if (opt.isLoading()) {
                                cancelCurrentIfDifferent(opt);
                                current = new LiveUploader$Job(opt, slot, imgs);
                            }
                        });
                    }
                } catch (Throwable var4) {
                    opt.setLoading(false);
                }
            }, "anvil visual-read-" + slot);
            reader.setDaemon(true);
            reader.start();
        }
    }

    static void cancelCurrentIfDifferent(GifOption opt) {
        LiveUploader$Job c = current;
        if (c != null && c.opt != opt) {
            c.cancelled = true;
            c.opt.setLoading(false);
            current = null;
        }
    }

    public static void tick() {
        LiveUploader$Job j = current;
        if (j != null) {
            if (j.cancelled) {
                current = null;
            } else {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) {
                    TextureManager tm = mc.getTextureManager();
                    if (tm != null) {
                        if (j.index == 0) {
                            MediaProcessor.destroySlotTextures(tm, j.slot);
                        }

                        int end = Math.min(j.index + 4, j.imgs.length);

                        for (int i = j.index; i < end; i++) {
                            if (j.imgs[i] != null) {
                                MediaProcessor.registerTexture(tm, MediaProcessor.frameId(j.slot, i), j.imgs[i], j.slot, i);
                            }
                        }

                        j.index = end;
                        j.opt.setLoadedFrames(end);
                        if (end >= j.imgs.length) {
                            j.opt.setReady(true);
                            current = null;
                        }
                    }
                }
            }
        }
    }
}
