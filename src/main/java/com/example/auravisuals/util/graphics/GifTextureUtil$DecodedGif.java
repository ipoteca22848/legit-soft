package com.example.auravisuals.util.graphics;

import java.util.List;
import net.minecraft.client.texture.NativeImage;

public final class GifTextureUtil$DecodedGif {
    final List<NativeImage> frames;
    final List<Integer> delays;
    final int width;
    final int height;

    GifTextureUtil$DecodedGif(List<NativeImage> frames, List<Integer> delays, int width, int height) {
        this.frames = frames;
        this.delays = delays;
        this.width = width;
        this.height = height;
    }
}
