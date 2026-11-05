package com.example.auravisuals.client.gui.font.icon;

import com.example.auravisuals.util.render.RenderUtil;
import net.minecraft.client.util.math.MatrixStack;

public final class TtfWorldIcon {
    private TtfWorldIcon() {
    }

    public static void drawIconChar(MatrixStack matrices, char ch, int r, int g, int b, int a, float targetWidth) {
        if (TtfIconFonts.hasIcons()) {
            HelsingStyleTtfFont font = TtfIconFonts.icons(72);
            if (font != null) {
                float fullW = font.getStringWidth(String.valueOf(ch)) * 2.0F;
                if (!(fullW <= 0.0F)) {
                    float scale = targetWidth / fullW;
                    int argb = a << 24 | r << 16 | g << 8 | b;
                    matrices.push();

                    try {
                        matrices.scale(scale, scale, 1.0F);
                        RenderUtil.shaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        font.drawVerticallyCenteredString(matrices, String.valueOf(ch), 0.0, 0.0, argb);
                    } finally {
                        matrices.pop();
                    }
                }
            }
        }
    }
}
