package com.example.auravisuals.module.render;

public class Cubes$CubeParticle {
    double x;
    double y;
    double z;
    double prevX;
    double prevY;
    double prevZ;
    double renderX;
    double renderY;
    double renderZ;
    float vx;
    float vy;
    float vz;
    float rotX;
    float rotY;
    float rotZ;
    float rotSpeedX;
    float rotSpeedY;
    float rotSpeedZ;
    float wobblePhase;
    float wobbleOffset;
    float prevRotX;
    float prevRotY;
    float prevRotZ;
    float renderRotX;
    float renderRotY;
    float renderRotZ;
    float renderAlpha;
    int life;
    int maxLife;

    Cubes$CubeParticle(
        double x,
        double y,
        double z,
        float vx,
        float vy,
        float vz,
        float rotX,
        float rotY,
        float rotZ,
        float rotSpeedX,
        float rotSpeedY,
        float rotSpeedZ,
        int life,
        float wobblePhase,
        float wobbleOffset
    ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.prevX = this.renderX = x;
        this.prevY = this.renderY = y;
        this.prevZ = this.renderZ = z;
        this.rotX = rotX;
        this.rotY = rotY;
        this.rotZ = rotZ;
        this.prevRotX = this.renderRotX = rotX;
        this.prevRotY = this.renderRotY = rotY;
        this.prevRotZ = this.renderRotZ = rotZ;
        this.rotSpeedX = rotSpeedX;
        this.rotSpeedY = rotSpeedY;
        this.rotSpeedZ = rotSpeedZ;
        this.life = this.maxLife = life;
        this.wobblePhase = wobblePhase;
        this.wobbleOffset = wobbleOffset;
    }
}
