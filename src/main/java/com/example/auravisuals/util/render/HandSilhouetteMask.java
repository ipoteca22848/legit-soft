package com.example.auravisuals.util.render;

import net.minecraft.client.render.VertexConsumerProvider;

public final class HandSilhouetteMask {
    HandSilhouetteMask() {
    }

    public static VertexConsumerProvider wrap(VertexConsumerProvider original) {
        return layer -> new HandSilhouetteMask$WhiteConsumer(original.getBuffer(layer));
    }
}
