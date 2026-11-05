package com.example.auravisuals.cosmetics;

public class CosmeticsManager$Placement {
    public float x;
    public float y;
    public float z;
    public float scale;
    public float autoYaw;

    public float x() {
        return this.x;
    }

    public float y() {
        return this.y;
    }

    public float z() {
        return this.z;
    }

    public float scale() {
        return this.scale;
    }

    public float autoYaw() {
        return this.autoYaw;
    }

    public CosmeticsManager$Placement(float x, float y, float z, float scale, float autoYaw) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.scale = scale;
        this.autoYaw = autoYaw;
    }
}
