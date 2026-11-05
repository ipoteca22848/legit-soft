package com.example.auravisuals.module.render;

import net.minecraft.util.math.Vec3d;

public class Trails$Point {
    public final Vec3d pos;
    public final long time;
    public final float height;

    public Trails$Point(Vec3d pos, float height) {
        this(pos, height, System.currentTimeMillis());
    }

    public Trails$Point(Vec3d pos, float height, long time) {
        this.pos = pos;
        this.time = time;
        this.height = height;
    }
}
