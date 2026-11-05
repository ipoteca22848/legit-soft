package com.example.auravisuals.module.render;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class JumpCircle$TestWave {
    public final Vec3d position;
    public final long creationTime;
    public final float lifetimeMs;
    public final float strength;
    public final float maxRadius;
    public final int color;
    public final boolean vaporGlow;

    public JumpCircle$TestWave(Vec3d position, long creationTime, float lifetimeMs, float strength, float maxRadius, int color, boolean vaporGlow) {
        this.position = position;
        this.creationTime = creationTime;
        this.lifetimeMs = Math.max(1.0F, lifetimeMs);
        this.strength = strength;
        this.maxRadius = maxRadius;
        this.color = color;
        this.vaporGlow = vaporGlow;
    }

    public float rawProgress(long now) {
        return MathHelper.clamp((float)(now - this.creationTime) / this.lifetimeMs, 0.0F, 1.0F);
    }

    public boolean finished(long now) {
        return (float)(now - this.creationTime) >= this.lifetimeMs;
    }
}
