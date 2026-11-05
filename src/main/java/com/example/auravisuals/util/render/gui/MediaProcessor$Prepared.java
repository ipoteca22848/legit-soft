package com.example.auravisuals.util.render.gui;

import java.awt.image.BufferedImage;
import java.util.List;

public final class MediaProcessor$Prepared {
    public final List<BufferedImage> frames;
    public final int[] durationsMs;
    public final int frameW;
    public final int frameH;
    public final String name;

    MediaProcessor$Prepared(List<BufferedImage> f, int[] d, int w, int h, String n) {
        this.frames = f;
        this.durationsMs = d;
        this.frameW = w;
        this.frameH = h;
        this.name = n;
    }
}
