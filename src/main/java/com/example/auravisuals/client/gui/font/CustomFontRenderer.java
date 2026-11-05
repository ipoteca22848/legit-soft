package com.example.auravisuals.client.gui.font;

import com.example.auravisuals.AuraVisuals;
import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.Gui;
import com.example.auravisuals.compat.Gui$Tex;
import com.example.auravisuals.util.render.RenderUtil;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public class CustomFontRenderer {
    Identifier textureId;
    final Map<Character, CustomFontRenderer$Glyph> glyphs = new HashMap<>();
    int fontHeight = 9;
    final int textureSize = 2048;
    boolean initialized = false;
    boolean available = false;
    boolean failed = false;
    final float rasterSize;
    final float baseScale;
    final String texturePath;

    public CustomFontRenderer() {
        this(36.0F, 0.25F, "font_atlas");
    }

    public CustomFontRenderer(float rasterSize, float baseScale, String texturePath) {
        this.rasterSize = rasterSize;
        this.baseScale = baseScale;
        this.texturePath = texturePath;
    }

    public boolean isAvailable() {
        if (!this.initialized && !this.failed) {
            this.ensureInitialized();
        }

        return this.available;
    }

    void ensureInitialized() {
        if (!this.initialized && !this.failed) {
            try {
                Font font = this.loadFont();
                if (font != null) {
                    this.generateAtlas(font);
                    this.initialized = true;
                    this.available = true;
                    AuraVisuals.LOGGER.info("CustomFontRenderer: successfully loaded and generated custom font atlas.");
                } else {
                    AuraVisuals.LOGGER.warn("CustomFontRenderer: could not load embedded TTF font, custom font disabled.");
                    this.failed = true;
                    this.available = false;
                }
            } catch (Throwable t) {
                AuraVisuals.LOGGER.warn("CustomFontRenderer: AWT font error ({}), falling back to vanilla font renderer", t.toString());
                this.failed = true;
                this.available = false;
            }
        }
    }

    Font loadFont() {
        String lookupPath = "assets/auravisuals/fonts/wixmadefortext-medium.ttf";
        Font font2 = this.loadViaClassloader(this.getClass().getClassLoader(), lookupPath, "Class classloader");
        if (font2 != null) {
            return font2;
        }

        ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();
        if (ctxLoader != null && (font2 = this.loadViaClassloader(ctxLoader, lookupPath, "Thread context classloader")) != null) {
            return font2;
        }

        font2 = this.loadViaClassloader(ClassLoader.getSystemClassLoader(), lookupPath, "System classloader");
        if (font2 != null) {
            return font2;
        }

        label69: {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getResourceManager() != null) {
                Identifier id = Identifier.of("auravisuals", "fonts/wixmadefortext-medium.ttf");
                Optional res = mc.getResourceManager().getResource(id);
                if (res.isPresent()) {
                    try (InputStream in = ((Resource)res.get()).getInputStream()) {
                        font2 = Font.createFont(0, in).deriveFont(this.rasterSize);
                        AuraVisuals.LOGGER.info("CustomFontRenderer: loaded font via Minecraft ResourceManager");
                        return font2;
                    } catch (Exception e) {
                    }
                }
            }

            return null;
        }
    }

    Font loadViaClassloader(ClassLoader loader, String path, String strategyName) {
        InputStream is = loader.getResourceAsStream(path);
        if (is == null) {
            return null;
        }

        InputStream inputStream = is;

        try (is) {
            Font font2 = Font.createFont(0, is).deriveFont(this.rasterSize);
            AuraVisuals.LOGGER.info("CustomFontRenderer: loaded font via {}", strategyName);
            return font2;
        } catch (Exception e) {
            return null;
        }
    }

    void generateAtlas(Font font) {
        BufferedImage image = new BufferedImage(2048, 2048, 2);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setFont(font);
        FontMetrics metrics = g2d.getFontMetrics();
        this.fontHeight = metrics.getHeight();
        int x = 2;
        int y = 2;
        int maxHeight = 0;
        String characters = this.getAllSupportedChars();

        for (char c : characters.toCharArray()) {
            int charWidth = metrics.charWidth(c);
            if (charWidth <= 0) {
                charWidth = 7;
            }

            if (x + charWidth + 2 >= 2048) {
                x = 2;
                y += maxHeight + 2;
                maxHeight = 0;
            }

            if (metrics.getHeight() > maxHeight) {
                maxHeight = metrics.getHeight();
            }

            g2d.setColor(Color.WHITE);
            g2d.drawString(String.valueOf(c), x, y + metrics.getAscent());
            this.glyphs.put(c, new CustomFontRenderer$Glyph(x, y, charWidth, metrics.getHeight()));
            x += charWidth + 2;
        }

        g2d.dispose();

        try {
            NativeImage nativeImage = this.convertToNative(image);
            this.textureId = Identifier.of("auravisuals", this.texturePath);
            MinecraftClient.getInstance().getTextureManager().registerTexture(this.textureId, new NativeImageBackedTexture(() -> "auravisual", nativeImage));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    NativeImage convertToNative(BufferedImage image) throws IOException {
        NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), false);

        for (int v = 0; v < image.getHeight(); v++) {
            for (int u = 0; u < image.getWidth(); u++) {
                int argb = image.getRGB(u, v);
                nativeImage.setColorArgb(u, v, argb & -16711936 | (argb & 0xFF0000) >> 16 | (argb & 0xFF) << 16);
            }
        }

        return nativeImage;
    }

    String getAllSupportedChars() {
        StringBuilder sb = new StringBuilder();

        for (int i = 32; i < 256; i++) {
            sb.append((char)i);
        }

        for (int var3 = 1024; var3 < 1327; var3++) {
            sb.append((char)var3);
        }

        sb.append("РЃС‘");
        return sb.toString();
    }

    public void drawString(DrawContext context, String text, float x, float y, int color, boolean shadow) {
        this.drawString(context, text, x, y, color, shadow, this.baseScale);
    }

    public void drawString(DrawContext context, String text, float x, float y, int color, boolean shadow, float scale) {
        this.ensureInitialized();
        if (this.initialized && text != null && !text.isEmpty()) {
            float a = (color >> 24 & 0xFF) / 255.0F;
            if (a == 0.0F) {
                a = 1.0F;
            }

            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            if (shadow) {
                this.renderTextGui(context, text, x + 0.5F, y + 0.5F, 0.1F, 0.1F, 0.1F, a * 0.8F, scale);
            }

            this.renderTextGui(context, text, x, y, r, g, b, a, scale);
        }
    }

    public void drawStringGradient(DrawContext context, String text, float x, float y, int topColor, int bottomColor, boolean shadow, float scale) {
        this.ensureInitialized();
        if (this.initialized && text != null && !text.isEmpty()) {
            float topA = this.colorAlpha(topColor);
            float bottomA = this.colorAlpha(bottomColor);
            if (!(topA <= 0.0F) || !(bottomA <= 0.0F)) {
                if (shadow) {
                    float shadowA = Math.max(topA, bottomA) * 0.8F;
                    this.renderTextGui(context, text, x + 0.5F, y + 0.5F, 0.1F, 0.1F, 0.1F, shadowA, scale);
                }

                this.renderTextGradientGui(context, text, x, y, topColor, bottomColor, scale);
            }
        }
    }

    public void drawString(MatrixStack matrices, String text, float x, float y, int color, boolean shadow) {
        this.drawString(matrices, text, x, y, color, shadow, this.baseScale);
    }

    public void drawString(MatrixStack matrices, String text, float x, float y, int color, boolean shadow, float scale) {
        this.drawStringInternal(matrices, text, x, y, color, shadow, scale, false);
    }

    public void drawStringExplicitAlpha(MatrixStack matrices, String text, float x, float y, int color, boolean shadow) {
        this.drawStringInternal(matrices, text, x, y, color, shadow, this.baseScale, true);
    }

    void drawStringInternal(MatrixStack matrices, String text, float x, float y, int color, boolean shadow, float scale, boolean explicitAlpha) {
        this.ensureInitialized();
        if (this.initialized && text != null && !text.isEmpty()) {
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float a = (color >> 24 & 0xFF) / 255.0F;
            if (!explicitAlpha && a == 0.0F) {
                a = 1.0F;
            }

            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            if (shadow) {
                this.renderText(text, x + 0.5F, y + 0.5F, 0.1F, 0.1F, 0.1F, a * 0.8F, scale, matrix);
            }

            this.renderText(text, x, y, r, g, b, a, scale, matrix);
        }
    }

    void renderTextGui(DrawContext context, String text, float x, float y, float r, float g, float b, float a, float scale) {
        Gui$Tex tex = Gui.tex(context, this.textureId);
        int col = Gui.argb(r, g, b, a);
        float currentX = x;
        boolean hasVertices = false;

        for (char c : text.toCharArray()) {
            CustomFontRenderer$Glyph glyph = this.glyphs.get(c);
            if (glyph != null) {
                float u1 = glyph.x / 2048.0F;
                float v1 = glyph.y / 2048.0F;
                float u2 = (glyph.x + glyph.width) / 2048.0F;
                float v2 = (glyph.y + glyph.height) / 2048.0F;
                float w = glyph.width * scale;
                float h = glyph.height * scale;
                tex.vertex(currentX, y + h, u1, v2, col)
                    .vertex(currentX + w, y + h, u2, v2, col)
                    .vertex(currentX + w, y, u2, v1, col)
                    .vertex(currentX, y, u1, v1, col);
                currentX += w;
                hasVertices = true;
            }
        }

        if (hasVertices) {
            tex.submit(context);
        }
    }

    void renderTextGradientGui(DrawContext context, String text, float x, float y, int topColor, int bottomColor, float scale) {
        Gui$Tex tex = Gui.tex(context, this.textureId);
        float topR = (topColor >> 16 & 0xFF) / 255.0F;
        float topG = (topColor >> 8 & 0xFF) / 255.0F;
        float topB = (topColor & 0xFF) / 255.0F;
        float topA = this.colorAlpha(topColor);
        float bottomR = (bottomColor >> 16 & 0xFF) / 255.0F;
        float bottomG = (bottomColor >> 8 & 0xFF) / 255.0F;
        float bottomB = (bottomColor & 0xFF) / 255.0F;
        float bottomA = this.colorAlpha(bottomColor);
        int topCol = Gui.argb(topR, topG, topB, topA);
        int bottomCol = Gui.argb(bottomR, bottomG, bottomB, bottomA);
        float currentX = x;
        boolean hasVertices = false;

        for (char c : text.toCharArray()) {
            CustomFontRenderer$Glyph glyph = this.glyphs.get(c);
            if (glyph != null) {
                float u1 = glyph.x / 2048.0F;
                float v1 = glyph.y / 2048.0F;
                float u2 = (glyph.x + glyph.width) / 2048.0F;
                float v2 = (glyph.y + glyph.height) / 2048.0F;
                float w = glyph.width * scale;
                float h = glyph.height * scale;
                tex.vertex(currentX, y + h, u1, v2, bottomCol)
                    .vertex(currentX + w, y + h, u2, v2, bottomCol)
                    .vertex(currentX + w, y, u2, v1, topCol)
                    .vertex(currentX, y, u1, v1, topCol);
                currentX += w;
                hasVertices = true;
            }
        }

        if (hasVertices) {
            tex.submit(context);
        }
    }

    void renderText(String text, float x, float y, float r, float g, float b, float a, float scale, Matrix4f matrix) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        float currentX = x;
        boolean hasVertices = false;

        for (char c : text.toCharArray()) {
            CustomFontRenderer$Glyph glyph = this.glyphs.get(c);
            if (glyph != null) {
                float u1 = glyph.x / 2048.0F;
                float v1 = glyph.y / 2048.0F;
                float u2 = (glyph.x + glyph.width) / 2048.0F;
                float v2 = (glyph.y + glyph.height) / 2048.0F;
                float w = glyph.width * scale;
                float h = glyph.height * scale;
                buffer.vertex(matrix, currentX, y + h, 0.0F).texture(u1, v2).color(r, g, b, a);
                buffer.vertex(matrix, currentX + w, y + h, 0.0F).texture(u2, v2).color(r, g, b, a);
                buffer.vertex(matrix, currentX + w, y, 0.0F).texture(u2, v1).color(r, g, b, a);
                buffer.vertex(matrix, currentX, y, 0.0F).texture(u1, v1).color(r, g, b, a);
                currentX += w;
                hasVertices = true;
            }
        }

        if (hasVertices) {
            AuraRender.drawTexAuto(buffer, this.textureId, false, false);
        }
    }

    void renderTextGradient(String text, float x, float y, int topColor, int bottomColor, float scale, Matrix4f matrix) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        float topR = (topColor >> 16 & 0xFF) / 255.0F;
        float topG = (topColor >> 8 & 0xFF) / 255.0F;
        float topB = (topColor & 0xFF) / 255.0F;
        float topA = this.colorAlpha(topColor);
        float bottomR = (bottomColor >> 16 & 0xFF) / 255.0F;
        float bottomG = (bottomColor >> 8 & 0xFF) / 255.0F;
        float bottomB = (bottomColor & 0xFF) / 255.0F;
        float bottomA = this.colorAlpha(bottomColor);
        float currentX = x;
        boolean hasVertices = false;

        for (char c : text.toCharArray()) {
            CustomFontRenderer$Glyph glyph = this.glyphs.get(c);
            if (glyph != null) {
                float u1 = glyph.x / 2048.0F;
                float v1 = glyph.y / 2048.0F;
                float u2 = (glyph.x + glyph.width) / 2048.0F;
                float v2 = (glyph.y + glyph.height) / 2048.0F;
                float w = glyph.width * scale;
                float h = glyph.height * scale;
                buffer.vertex(matrix, currentX, y + h, 0.0F).texture(u1, v2).color(bottomR, bottomG, bottomB, bottomA);
                buffer.vertex(matrix, currentX + w, y + h, 0.0F).texture(u2, v2).color(bottomR, bottomG, bottomB, bottomA);
                buffer.vertex(matrix, currentX + w, y, 0.0F).texture(u2, v1).color(topR, topG, topB, topA);
                buffer.vertex(matrix, currentX, y, 0.0F).texture(u1, v1).color(topR, topG, topB, topA);
                currentX += w;
                hasVertices = true;
            }
        }

        if (hasVertices) {
            AuraRender.drawTexAuto(buffer, this.textureId, false, false);
        } else {
            RenderUtil.closeBuffer(buffer);
        }
    }

    float colorAlpha(int color) {
        int alpha = color >>> 24 & 0xFF;
        if (alpha == 0 && (color & 16777215) != 0) {
            alpha = 255;
        }

        return alpha / 255.0F;
    }

    public int getWidth(String text) {
        this.ensureInitialized();
        float width = 0.0F;

        for (char c : text.toCharArray()) {
            CustomFontRenderer$Glyph glyph = this.glyphs.get(c);
            if (glyph != null) {
                width += glyph.width * this.baseScale;
            }
        }

        return (int)width;
    }

    public int getHeight() {
        this.ensureInitialized();
        return (int)(this.fontHeight * this.baseScale);
    }
}
