package com.example.auravisuals.module.render;

import net.minecraft.util.math.Vec3d;

public final class JumpCircle$GlassWave {
    public final Vec3d position;
    public final long createdAt = System.currentTimeMillis();
    public final float lifeMs;
    public final float maxRadius;
    public final float strength;
    public final int color;
    public final boolean ambience;
    public final String effect;

    public JumpCircle$GlassWave(Vec3d position, float lifeMs, float maxRadius, float strength, int color) {
        this(position, lifeMs, maxRadius, strength, color, false, "Волны");
    }

    public JumpCircle$GlassWave(Vec3d position, float lifeMs, float maxRadius, float strength, int color, boolean ambience, String effect) {
        this.position = position;
        this.lifeMs = Math.max(1.0F, lifeMs);
        this.maxRadius = maxRadius;
        this.strength = strength;
        this.color = color;
        this.ambience = ambience;
        this.effect = effect;
    }
}
