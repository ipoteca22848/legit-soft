package com.example.auravisuals.live;

import com.example.auravisuals.compat.WorldRenderContext;
import net.minecraft.client.gui.DrawContext;

public interface LiveVisual {
    default void onInit() {
    }

    default void onTick() {
    }

    default void onRender2D(DrawContext context, float tickDelta) {
    }

    default void onRender3D(WorldRenderContext context) {
    }

    default void onCleanup() {
    }
}
