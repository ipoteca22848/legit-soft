package com.example.auravisuals.module.render;

public final class KillEffect$BodyShard {
    public double x;
    public double y;
    public double z;
    public double prevX;
    public double prevY;
    public double prevZ;
    public double vx;
    public double vy;
    public double vz;
    public final float size;
    public final int color;
    public int life;
    public float rotX;
    public float rotY;
    public float rotZ;
    public float prevRotX;
    public float prevRotY;
    public float prevRotZ;
    public float spinX;
    public float spinY;
    public float spinZ;
    public boolean grounded;

    public KillEffect$BodyShard(
        double x,
        double y,
        double z,
        double vx,
        double vy,
        double vz,
        float size,
        int color,
        int life,
        float rotX,
        float rotY,
        float rotZ,
        float spinX,
        float spinY,
        float spinZ
    ) {
        this.x = this.prevX = x;
        this.y = this.prevY = y;
        this.z = this.prevZ = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.size = size;
        this.color = color;
        this.life = life;
        this.rotX = this.prevRotX = rotX;
        this.rotY = this.prevRotY = rotY;
        this.rotZ = this.prevRotZ = rotZ;
        this.spinX = spinX;
        this.spinY = spinY;
        this.spinZ = spinZ;
    }
}
