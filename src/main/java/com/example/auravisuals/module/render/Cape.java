package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ModeSetting;
import java.io.InputStream;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class Cape extends Module {
    public static final Identifier CAPE_TEXTURE = Identifier.of("auravisuals", "textures/cape_aura.png");
    private static Identifier capeTextureWhite;
    private static Identifier capeTextureGray;
    public final ModeSetting capeColor = new ModeSetting("Цвет", "Черный", "Черный", "Белый", "Серый");
    public final BooleanSetting onlyMe = new BooleanSetting("Только я", true);
    private boolean texturesGenerated = false;

    public Cape() {
        super("Cape", "Плащ с логотипом bamber client", Category.VISUALS);
        this.addSettings(this.capeColor, this.onlyMe);
    }

    public Identifier getCapeTexture() {
        this.generateColoredTextures();

        return switch (this.capeColor.getValue()) {
            case "Белый" -> capeTextureWhite != null ? capeTextureWhite : CAPE_TEXTURE;
            case "Серый" -> capeTextureGray != null ? capeTextureGray : CAPE_TEXTURE;
            default -> CAPE_TEXTURE;
        };
    }

    private void generateColoredTextures() {
        if (!this.texturesGenerated) {
            this.texturesGenerated = true;

            try {
                MinecraftClient mc = MinecraftClient.getInstance();
                Optional resource = mc.getResourceManager().getResource(CAPE_TEXTURE);
                if (resource.isEmpty()) {
                    return;
                }

                NativeImage original;
                try (InputStream is = ((Resource)resource.get()).getInputStream()) {
                    original = NativeImage.read(is);
                }

                capeTextureWhite = this.generateColorVariant(original, "cape_aura_white", 255, 255, 255);
                capeTextureGray = this.generateColorVariant(original, "cape_aura_gray", 78, 78, 82);
                original.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Identifier generateColorVariant(NativeImage original, String name, int bgR, int bgG, int bgB) {
        int scale = 4;
        int newW = original.getWidth() * scale;
        int newH = original.getHeight() * scale;
        NativeImage upscaled = new NativeImage(newW, newH, true);

        for (int ny = 0; ny < newH; ny++) {
            for (int nx = 0; nx < newW; nx++) {
                float srcX = (nx + 0.5F) * original.getWidth() / newW - 0.5F;
                float srcY = (ny + 0.5F) * original.getHeight() / newH - 0.5F;
                int x0 = MathHelper.clamp((int)Math.floor(srcX), 0, original.getWidth() - 1);
                int y0 = MathHelper.clamp((int)Math.floor(srcY), 0, original.getHeight() - 1);
                int x1 = MathHelper.clamp(x0 + 1, 0, original.getWidth() - 1);
                int y1 = MathHelper.clamp(y0 + 1, 0, original.getHeight() - 1);
                float fx = srcX - (float)Math.floor(srcX);
                float fy = srcY - (float)Math.floor(srcY);
                int c00 = original.getColorArgb(x0, y0);
                int c10 = original.getColorArgb(x1, y0);
                int c01 = original.getColorArgb(x0, y1);
                int c11 = original.getColorArgb(x1, y1);
                int pixel = bilinearBlend(c00, c10, c01, c11, fx, fy);
                int a = pixel >> 24 & 0xFF;
                int r = pixel >> 16 & 0xFF;
                int g = pixel >> 8 & 0xFF;
                int b = pixel & 0xFF;
                float brightness = (r + g + b) / 765.0F;
                float blend = 1.0F - MathHelper.clamp(brightness / 0.3F, 0.0F, 1.0F);
                if (a > 10 && blend > 0.01F) {
                    int nr = (int)(r + (bgR - r) * blend);
                    int ng = (int)(g + (bgG - g) * blend);
                    int nb = (int)(b + (bgB - b) * blend);
                    upscaled.setColorArgb(nx, ny, a << 24 | nr << 16 | ng << 8 | nb);
                } else {
                    upscaled.setColorArgb(nx, ny, pixel);
                }
            }
        }

        NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "auravisual", upscaled);
        Identifier id = Identifier.of("auravisuals", name);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
        return id;
    }

    private static int bilinearBlend(int c00, int c10, int c01, int c11, float fx, float fy) {
        float gx = 1.0F - fx;
        float gy = 1.0F - fy;
        float w00 = gx * gy;
        float w10 = fx * gy;
        float w01 = gx * fy;
        float w11 = fx * fy;
        int a = (int)((c00 >> 24 & 0xFF) * w00 + (c10 >> 24 & 0xFF) * w10 + (c01 >> 24 & 0xFF) * w01 + (c11 >> 24 & 0xFF) * w11);
        int r = (int)((c00 >> 16 & 0xFF) * w00 + (c10 >> 16 & 0xFF) * w10 + (c01 >> 16 & 0xFF) * w01 + (c11 >> 16 & 0xFF) * w11);
        int g = (int)((c00 >> 8 & 0xFF) * w00 + (c10 >> 8 & 0xFF) * w10 + (c01 >> 8 & 0xFF) * w01 + (c11 >> 8 & 0xFF) * w11);
        int b = (int)((c00 & 0xFF) * w00 + (c10 & 0xFF) * w10 + (c01 & 0xFF) * w01 + (c11 & 0xFF) * w11);
        return MathHelper.clamp(a, 0, 255) << 24 | MathHelper.clamp(r, 0, 255) << 16 | MathHelper.clamp(g, 0, 255) << 8 | MathHelper.clamp(b, 0, 255);
    }
}
