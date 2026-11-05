package com.example.auravisuals.util.render;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.Identifier;

public final class HitColorArmorRenderFrame {
    public final Identifier texture;
    public final LivingEntityRenderState state;

    public HitColorArmorRenderFrame(Identifier texture, LivingEntityRenderState state) {
        this.texture = texture;
        this.state = state;
    }
}
