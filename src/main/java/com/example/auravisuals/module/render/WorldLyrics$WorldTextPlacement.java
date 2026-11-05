package com.example.auravisuals.module.render;

import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

public class WorldLyrics$WorldTextPlacement {
    public Vec3d anchor;
    public Quaternionf rotation;
    public float tiltX;
    public float tiltY;
    public float roll;
    public float physicalScale;

    public Vec3d anchor() {
        return this.anchor;
    }

    public Quaternionf rotation() {
        return this.rotation;
    }

    public float tiltX() {
        return this.tiltX;
    }

    public float tiltY() {
        return this.tiltY;
    }

    public float roll() {
        return this.roll;
    }

    public float physicalScale() {
        return this.physicalScale;
    }

    public WorldLyrics$WorldTextPlacement(Vec3d anchor, Quaternionf rotation, float tiltX, float tiltY, float roll, float physicalScale) {
        this.anchor = anchor;
        this.rotation = rotation;
        this.tiltX = tiltX;
        this.tiltY = tiltY;
        this.roll = roll;
        this.physicalScale = physicalScale;
    }
}
