package com.example.auravisuals.util.color;

import com.example.auravisuals.util.graphics.WhiteTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Arm;

public class ColorChamsHelper {
    public static VertexConsumerProvider getWrappedProvider(VertexConsumerProvider original, Arm arm, boolean isItem) {
        return original;
    }

    public static VertexConsumerProvider getPatrickHandProvider(VertexConsumerProvider original) {
        int r = 255;
        int g = 139;
        int b = 134;
        int a = 255;
        return layer -> new ColorChamsHelper$ColorConsumer(original.getBuffer(RenderLayers.entityTranslucent(WhiteTexture.get())), r, g, b, a, true);
    }

    static int packArgb(int r, int g, int b, int a) {
        return a << 24 | r << 16 | g << 8 | b;
    }

    static int multiplyPackedArgb(int packed, int r, int g, int b, int a) {
        int pa = packed >>> 24 & 0xFF;
        int pr = packed >> 16 & 0xFF;
        int pg = packed >> 8 & 0xFF;
        int pb = packed & 0xFF;
        if (pa == 0) {
            pa = 255;
        }

        return packArgb(pr * r / 255, pg * g / 255, pb * b / 255, pa * a / 255);
    }
}
