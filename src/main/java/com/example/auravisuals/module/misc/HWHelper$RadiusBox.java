package com.example.auravisuals.module.misc;

public class HWHelper$RadiusBox {
    public float minX;
    public float maxX;
    public float minY;
    public float maxY;
    public float minZ;
    public float maxZ;

    public float minX() {
        return this.minX;
    }

    public float maxX() {
        return this.maxX;
    }

    public float minY() {
        return this.minY;
    }

    public float maxY() {
        return this.maxY;
    }

    public float minZ() {
        return this.minZ;
    }

    public float maxZ() {
        return this.maxZ;
    }

    public HWHelper$RadiusBox(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }
}
