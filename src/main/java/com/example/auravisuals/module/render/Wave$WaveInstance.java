package com.example.auravisuals.module.render;

import net.minecraft.util.math.Vec3d;

public class Wave$WaveInstance {
    public final Vec3d pos;
    public final long creationTime;
    public final String kind;

    public Wave$WaveInstance(Vec3d pos, String kind) {
        this.pos = pos;
        this.kind = kind;
        this.creationTime = System.currentTimeMillis() - 120L;
    }
}
