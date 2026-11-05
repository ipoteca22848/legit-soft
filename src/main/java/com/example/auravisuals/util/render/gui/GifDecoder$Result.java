package com.example.auravisuals.util.render.gui;

import java.awt.image.BufferedImage;
import java.util.List;

public final class GifDecoder$Result {
    public final List<BufferedImage> frames;
    public final int[] delaysMs;

    public GifDecoder$Result(List<BufferedImage> frames, int[] delaysMs) {
        this.frames = frames;
        this.delaysMs = delaysMs;
    }
}
