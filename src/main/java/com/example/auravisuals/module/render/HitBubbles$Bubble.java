package com.example.auravisuals.module.render;

import net.minecraft.util.math.Vec3d;

public final class HitBubbles$Bubble {
    final Vec3d pos;
    final long spawnTime;
    final float spinSeed;
    final float sideYaw;

    HitBubbles$Bubble(Vec3d pos, long spawnTime, float spinSeed, float sideYaw) {
        this.pos = pos;
        this.spawnTime = spawnTime;
        this.spinSeed = spinSeed;
        this.sideYaw = sideYaw;
    }
}
