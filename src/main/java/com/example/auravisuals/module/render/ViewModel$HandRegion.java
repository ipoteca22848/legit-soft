package com.example.auravisuals.module.render;

public class ViewModel$HandRegion {
    public float x;
    public float y;
    public float width;
    public float height;

    public float x() {
        return this.x;
    }

    public float y() {
        return this.y;
    }

    public float width() {
        return this.width;
    }

    public float height() {
        return this.height;
    }

    public ViewModel$HandRegion(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float centerX() {
        return this.x + this.width / 2.0F;
    }

    public float centerY() {
        return this.y + this.height / 2.0F;
    }

    public double distanceSquared(double mouseX, double mouseY) {
        double dx = mouseX - this.centerX();
        double dy = mouseY - this.centerY();
        return dx * dx + dy * dy;
    }
}
