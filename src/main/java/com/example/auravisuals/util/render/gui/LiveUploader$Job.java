package com.example.auravisuals.util.render.gui;

import java.awt.image.BufferedImage;

public final class LiveUploader$Job {
    final GifOption opt;
    final int slot;
    final BufferedImage[] imgs;
    int index;
    volatile boolean cancelled;

    LiveUploader$Job(GifOption opt, int slot, BufferedImage[] imgs) {
        this.opt = opt;
        this.slot = slot;
        this.imgs = imgs;
    }
}
