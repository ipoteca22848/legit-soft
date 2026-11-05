package com.example.auravisuals.util.render.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class Overlay {
    public static final int W = 176;
    public static final int H = 166;
    public static final int TARGET_W = 352;
    public static final int TARGET_H = 332;

    private Overlay() {
    }

    public static BufferedImage composeImage(BufferedImage src, int targetW, int targetH) {
        BufferedImage out = new BufferedImage(targetW, targetH, 2);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, targetW, targetH);
        int sw = src.getWidth();
        int sh = src.getHeight();
        double scale = Math.max((double)targetW / sw, (double)targetH / sh);
        double cropW = targetW / scale;
        double cropH = targetH / scale;
        int sx = (int)Math.round((sw - cropW) / 2.0);
        int sy = (int)Math.round((sh - cropH) / 2.0);
        g.drawImage(src, 0, 0, targetW, targetH, sx, sy, sx + (int)Math.round(cropW), sy + (int)Math.round(cropH), null);
        g.dispose();
        return out;
    }
}
