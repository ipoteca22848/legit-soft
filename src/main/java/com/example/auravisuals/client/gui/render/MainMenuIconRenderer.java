package com.example.auravisuals.client.gui.render;

import com.example.auravisuals.util.render.RenderUtil;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.NativeImage.Format;
import net.minecraft.util.Identifier;

public final class MainMenuIconRenderer {
    private static final Map<String, Identifier> CACHE = new HashMap<>();

    private MainMenuIconRenderer() {
    }

    public static void draw(DrawContext context, Identifier source, float x, float y, float size) {
        float rx = Math.round(x);
        float ry = Math.round(y);
        int textureSize = Math.max(16, Math.round(size));
        Identifier texture = CACHE.computeIfAbsent(source.toString() + ":" + textureSize, key -> createTexture(source, textureSize));
        RenderUtil.drawTexture(context, texture, rx, ry, textureSize, textureSize, 0.0F, 0.0F, textureSize, textureSize, textureSize, textureSize, false);
    }

    private static Identifier createTexture(Identifier source, int size) {
        Identifier id = Identifier.of("auravisuals", "generated/main_menu_icon/" + source.getPath().replace('/', '_').replace('.', '_') + "_" + size);

        try (InputStream stream = MinecraftClient.getInstance().getResourceManager().open(source)) {
            BufferedImage input = ImageIO.read(stream);
            BufferedImage output = new BufferedImage(size, size, 2);
            Graphics2D graphics = output.createGraphics();

            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                graphics.drawImage(input, 0, 0, size, size, null);
            } finally {
                graphics.dispose();
            }

            NativeImage image = new NativeImage(Format.RGBA, size, size, false);

            for (int texture = 0; texture < size; texture++) {
                for (int px = 0; px < size; px++) {
                    image.setColorArgb(px, texture, output.getRGB(px, texture));
                }
            }

            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "auravisual", image);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
            texture.upload();
            return id;
        } catch (Exception var17) {
            return source;
        }
    }
}
