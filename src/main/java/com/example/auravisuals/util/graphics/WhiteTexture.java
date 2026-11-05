package com.example.auravisuals.util.graphics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public class WhiteTexture {
    private static final Identifier TEXTURE = Identifier.of("auravisuals", "white_blank");
    private static boolean initialized = false;

    public static Identifier get() {
        if (!initialized) {
            NativeImage image = new NativeImage(1, 1, false);
            image.setColorArgb(0, 0, -1);
            MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURE, new NativeImageBackedTexture(() -> "auravisual", image));
            initialized = true;
        }

        return TEXTURE;
    }
}
