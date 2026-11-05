package com.example.auravisuals.module.render;

import net.minecraft.util.math.Vec3d;

public class PingMarker$Ping {
    public Vec3d labelPos;
    public Vec3d groundPos;
    public String label;
    public String sender;
    public long createdAt;

    public Vec3d labelPos() {
        return this.labelPos;
    }

    public Vec3d groundPos() {
        return this.groundPos;
    }

    public String label() {
        return this.label;
    }

    public String sender() {
        return this.sender;
    }

    public long createdAt() {
        return this.createdAt;
    }

    public PingMarker$Ping(Vec3d labelPos, Vec3d groundPos, String label, String sender, long createdAt) {
        this.labelPos = labelPos;
        this.groundPos = groundPos;
        this.label = label;
        this.sender = sender;
        this.createdAt = createdAt;
    }
}
