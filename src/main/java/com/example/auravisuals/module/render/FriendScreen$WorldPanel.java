package com.example.auravisuals.module.render;

import net.minecraft.util.math.Vec3d;

public final class FriendScreen$WorldPanel {
    public Vec3d center;
    public Vec3d normal;
    public Vec3d targetCenter;
    public Vec3d targetNormal;
    public float width;

    public FriendScreen$WorldPanel(Vec3d center, Vec3d normal, float width) {
        this.center = center;
        this.normal = normal;
        this.targetCenter = center;
        this.targetNormal = normal;
        this.width = width;
    }

    public float height() {
        return this.width * 9.0F / 16.0F;
    }

    public Vec3d right() {
        return new Vec3d(-this.normal.z, 0.0, this.normal.x);
    }

    public void smooth(double amount) {
        this.center = this.center.lerp(this.targetCenter, amount);
        Vec3d blendedNormal = this.normal.lerp(this.targetNormal, amount);
        if (blendedNormal.lengthSquared() > 1.0E-8) {
            this.normal = blendedNormal.normalize();
        }
    }
}
