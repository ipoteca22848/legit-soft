package com.example.auravisuals.module.render;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Firefly$Particle {
    double x;
    double y;
    double z;
    double prevX;
    double prevY;
    double prevZ;
    double currentSize = 0.0;
    double angleY;
    double angleV;
    double radiusNoise;
    double speedOffset;

    public Firefly$Particle(Firefly firefly) {
        if (Firefly.access$000(firefly).player != null) {
            this.x = Firefly.access$100(firefly).player.getX();
            this.y = Firefly.access$200(firefly).player.getY() + 1.5;
            this.z = Firefly.access$300(firefly).player.getZ();
        }

        this.prevX = this.x;
        this.prevY = this.y;
        this.prevZ = this.z;
        this.angleY = firefly.random.nextDouble() * Math.PI * 2.0;
        this.angleV = firefly.random.nextDouble() * Math.PI * 2.0;
        this.radiusNoise = 0.8 + firefly.random.nextDouble() * 0.4;
        this.speedOffset = 0.8 + firefly.random.nextDouble() * 0.4;
    }

    void update(Vec3d center, double smoothFactor, double orbSpeed, double baseRadius, double targetSize) {
        this.prevX = this.x;
        this.prevY = this.y;
        this.prevZ = this.z;
        this.currentSize = MathHelper.lerp(0.15, this.currentSize, targetSize);
        this.angleY = this.angleY + 0.05 * orbSpeed * this.speedOffset;
        this.angleV = this.angleV + 0.03 * orbSpeed * this.speedOffset;
        double r = baseRadius * this.radiusNoise;
        if (targetSize < 0.01) {
            r = 0.0;
        }

        double offsetX = Math.cos(this.angleY) * r;
        double offsetZ = Math.sin(this.angleY) * r;
        double offsetY = Math.sin(this.angleV) * (r * 0.8);
        double targetX = center.x + offsetX;
        double targetY = center.y + offsetY;
        double targetZ = center.z + offsetZ;
        this.x = this.x + (targetX - this.x) * smoothFactor;
        this.y = this.y + (targetY - this.y) * smoothFactor;
        this.z = this.z + (targetZ - this.z) * smoothFactor;
    }
}
