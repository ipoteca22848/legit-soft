package com.example.auravisuals.module.render;

import net.minecraft.util.math.Vec3d;

public final class KillEffect$LaserWave {
    public final Vec3d position;
    public final long createdAt;
    public final int color;
    public final float radius;
    public final float[] terrainVertices;

    public KillEffect$LaserWave(Vec3d position, long createdAt, int color, float radius, float[] terrainVertices) {
        this.position = position;
        this.createdAt = createdAt;
        this.color = color;
        this.radius = radius;
        this.terrainVertices = terrainVertices;
    }
}
