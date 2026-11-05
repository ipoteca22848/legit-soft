package com.example.auravisuals.module.render;

import net.minecraft.util.math.Vec3d;

public final class HitBubbles$Lens {
    final Vec3d pos;
    final long spawnTime;
    final float hitboxRadius;
    final float spinSeed;

    HitBubbles$Lens(Vec3d pos, long spawnTime, float hitboxRadius, float spinSeed) {
        this.pos = pos;
        this.spawnTime = spawnTime;
        this.hitboxRadius = hitboxRadius;
        this.spinSeed = spinSeed;
    }
}
