package com.example.auravisuals.module.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class MeteorFly$MeteorParticle {
    double x;
    double y;
    double z;
    double prevX;
    double prevY;
    double prevZ;
    double velX;
    double velY;
    double velZ;
    final int baseRandomColor;
    long spawnTime;
    final long lifetime;
    final List<MeteorFly$TrailPoint> trail = new ArrayList<>();
    final int maxTrailLength;
    double targetVelX;
    double targetVelY;
    double targetVelZ;
    long lastDirectionChange;
    final Random random = new Random();

    MeteorFly$MeteorParticle(double x, double y, double z, double velX, double velY, double velZ, int baseRandomColor, int maxTrailLength, long lifetime) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.velX = velX;
        this.velY = velY;
        this.velZ = velZ;
        this.targetVelX = velX;
        this.targetVelY = velY;
        this.targetVelZ = velZ;
        this.baseRandomColor = baseRandomColor;
        this.spawnTime = System.currentTimeMillis();
        this.lastDirectionChange = System.currentTimeMillis();
        this.maxTrailLength = maxTrailLength;
        this.lifetime = lifetime;
    }

    void update(float speedMult, float maxSpeed, Vec3d playerPos) {
        this.prevX = this.x;
        this.prevY = this.y;
        this.prevZ = this.z;
        long timeSinceChange = System.currentTimeMillis() - this.lastDirectionChange;
        if (timeSinceChange > 2000 + this.random.nextInt(2000)) {
            double angle = Math.toRadians(this.random.nextDouble() * 360.0);
            double pitch = Math.toRadians((this.random.nextDouble() - 0.5) * 40.0);
            this.targetVelX = -Math.sin(angle) * Math.cos(pitch) * speedMult;
            this.targetVelY = Math.sin(pitch) * speedMult * 0.3;
            this.targetVelZ = Math.cos(angle) * Math.cos(pitch) * speedMult;
            this.lastDirectionChange = System.currentTimeMillis();
        }

        double dz;
        double dy;
        double dx;
        double distToPlayer;
        if ((distToPlayer = Math.sqrt((dx = playerPos.x - this.x) * dx + (dy = playerPos.y + 1.0 - this.y) * dy + (dz = playerPos.z - this.z) * dz)) > 35.0
            && distToPlayer > 0.001) {
            this.targetVelX += dx / distToPlayer * speedMult * 0.15;
            this.targetVelY += dy / distToPlayer * speedMult * 0.15;
            this.targetVelZ += dz / distToPlayer * speedMult * 0.15;
        }

        double lerpFactor = 0.02;
        this.velX = this.velX + (this.targetVelX - this.velX) * lerpFactor;
        this.velY = this.velY + (this.targetVelY - this.velY) * lerpFactor;
        this.velZ = this.velZ + (this.targetVelZ - this.velZ) * lerpFactor;
        double wobble = 0.03;
        this.velX = this.velX + (this.random.nextDouble() - 0.5) * wobble;
        this.velY = this.velY + (this.random.nextDouble() - 0.5) * wobble;
        this.velZ = this.velZ + (this.random.nextDouble() - 0.5) * wobble;
        this.velX = MathHelper.clamp(this.velX, -maxSpeed, maxSpeed);
        this.velY = MathHelper.clamp(this.velY, -maxSpeed, maxSpeed);
        this.velZ = MathHelper.clamp(this.velZ, -maxSpeed, maxSpeed);
        this.x = this.x + this.velX;
        this.y = this.y + this.velY;
        this.z = this.z + this.velZ;
        this.trail.add(0, new MeteorFly$TrailPoint(this.x, this.y, this.z));

        while (this.trail.size() > this.maxTrailLength) {
            this.trail.remove(this.trail.size() - 1);
        }
    }

    boolean isDead(double px, double py, double pz) {
        double dx = this.x - px;
        double dy = this.y - py;
        double dz = this.z - pz;
        return dx * dx + dy * dy + dz * dz > 6400.0;
    }

    boolean isExpired(long now) {
        return now - this.spawnTime >= this.lifetime;
    }

    double getInterpolatedX(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.prevX, this.x);
    }

    double getInterpolatedY(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.prevY, this.y);
    }

    double getInterpolatedZ(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.prevZ, this.z);
    }

    int getPulseAlpha() {
        long age = System.currentTimeMillis() - this.spawnTime;
        double pulse = 0.8 + 0.2 * Math.sin(age / 200.0);
        return (int)(pulse * 255.0);
    }

    float getLifeAlpha() {
        long age = System.currentTimeMillis() - this.spawnTime;
        float fadeIn = age < 900L ? (float)age / 900.0F : 1.0F;
        long timeLeft = this.lifetime - age;
        float fadeOut = timeLeft < 1800L ? Math.max(0.0F, (float)timeLeft / 1800.0F) : 1.0F;
        float alpha = Math.min(fadeIn, fadeOut);
        return alpha * alpha * (3.0F - 2.0F * alpha);
    }
}
