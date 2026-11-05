package com.example.auravisuals.module.render;

import net.minecraft.util.math.Vec3d;

public class Trails$VaporParticle {
    public Vec3d origin;
    public Vec3d motion;
    public long createdAt;
    public float rotation;
    public float sizeSeed;

    public Vec3d origin() {
        return this.origin;
    }

    public Vec3d motion() {
        return this.motion;
    }

    public long createdAt() {
        return this.createdAt;
    }

    public float rotation() {
        return this.rotation;
    }

    public float sizeSeed() {
        return this.sizeSeed;
    }

    public Trails$VaporParticle(Vec3d origin, Vec3d motion, long createdAt, float rotation, float sizeSeed) {
        this.origin = origin;
        this.motion = motion;
        this.createdAt = createdAt;
        this.rotation = rotation;
        this.sizeSeed = sizeSeed;
    }
}
