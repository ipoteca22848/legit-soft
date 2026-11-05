package com.example.auravisuals.client.gui.font;

import com.example.auravisuals.client.gui.font.icon.TtfIconFonts;
import com.example.auravisuals.util.language.LanguageManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class FontManager {
    public static CustomFontRenderer customFontRenderer;
    private static boolean useCustomFont = true;

    public static void init() {
        customFontRenderer = new CustomFontRenderer();
        TtfIconFonts.init();
    }

    public static void setCustomFont(boolean use) {
        useCustomFont = use;
    }

    public static boolean isUsingCustomFont() {
        return useCustomFont && customFontRenderer != null && customFontRenderer.isAvailable();
    }

    public static TextRenderer getFontRenderer() {
        return MinecraftClient.getInstance().textRenderer;
    }

    private static String cleanText(String text) {
        return text == null ? "" : LanguageManager.repairIrcMojibake(text);
    }

    public static void drawCustomText(DrawContext context, String text, float x, float y, int color) {
        drawCustomText(context, text, x, y, color, false);
    }

    public static void drawCustomText(DrawContext context, String text, float x, float y, int color, boolean shadow) {
        text = cleanText(text);
        if (isUsingCustomFont()) {
            customFontRenderer.drawString(context, text, x, y, color, shadow);
        } else {
            context.drawText(getFontRenderer(), text, (int)x, (int)y, color, shadow);
        }
    }

    public static void drawCustomTextScaled(DrawContext context, String text, float x, float y, int color, boolean shadow, float scale) {
        text = cleanText(text);
        if (isUsingCustomFont()) {
            customFontRenderer.drawString(context, text, x, y, color, shadow, scale);
        } else {
            context.getMatrices().pushMatrix();

            try {
                context.getMatrices().translate(x, y);
                context.getMatrices().scale(scale / 0.25F, scale / 0.25F);
                context.drawText(getFontRenderer(), text, 0, 0, color, shadow);
            } finally {
                context.getMatrices().popMatrix();
            }
        }
    }

    public static void drawCustomTextVerticalGradient(DrawContext context, String text, float x, float y, int topColor, int bottomColor, boolean shadow) {
        drawCustomTextVerticalGradientScaled(context, text, x, y, topColor, bottomColor, shadow, 0.25F);
    }

    public static void drawCustomTextVerticalGradientScaled(
        DrawContext context, String text, float x, float y, int topColor, int bottomColor, boolean shadow, float scale
    ) {
        text = cleanText(text);
        if (isUsingCustomFont()) {
            customFontRenderer.drawStringGradient(context, text, x, y, topColor, bottomColor, shadow, scale);
        } else {
            drawCustomTextScaled(context, text, x, y, topColor, shadow, scale);
        }
    }

    public static int getWidth(String text) {
        text = cleanText(text);
        return isUsingCustomFont() ? customFontRenderer.getWidth(text) : getFontRenderer().getWidth(text);
    }

    public static int getHeight() {
        return isUsingCustomFont() ? customFontRenderer.getHeight() : 9;
    }

    public static float getScaledTextWidth(String text, float scale) {
        return getWidth(text) * (scale / 0.25F);
    }

    public static float getScaledTextHeight(float scale) {
        return getHeight() * (scale / 0.25F);
    }

    public static void drawCenteredText(DrawContext context, String text, float x, float y, int color) {
        float width = getWidth(text);
        drawCustomText(context, text, x - width / 2.0F, y, color);
    }
}
