package com.example.auravisuals.module.render;

import net.minecraft.util.math.Vec3d;

public class FriendLabels$Label {
    public String sender;
    public Vec3d position;
    public int color;
    public String targetUuid;
    public String targetName;
    public long createdAt;

    public FriendLabels$Label(String sender, Vec3d position, int color, String targetUuid, String targetName, long createdAt) {
        this.sender = sender;
        this.position = position;
        this.color = color;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.createdAt = createdAt;
    }
}
