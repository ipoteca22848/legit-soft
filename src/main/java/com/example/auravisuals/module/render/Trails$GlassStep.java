package com.example.auravisuals.module.render;

import net.minecraft.util.math.Vec3d;

public class Trails$GlassStep {
    public Vec3d pos;
    public long createdAt;
    public float rotation;

    public Vec3d pos() {
        return this.pos;
    }

    public long createdAt() {
        return this.createdAt;
    }

    public float rotation() {
        return this.rotation;
    }

    public Trails$GlassStep(Vec3d pos, long createdAt, float rotation) {
        this.pos = pos;
        this.createdAt = createdAt;
        this.rotation = rotation;
    }
}
