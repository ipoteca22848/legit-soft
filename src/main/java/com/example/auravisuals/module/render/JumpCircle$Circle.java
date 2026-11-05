package com.example.auravisuals.module.render;

import net.minecraft.util.math.Vec3d;

public class JumpCircle$Circle {
    public final Vec3d pos;
    public final long creationTime;
    public final float size;
    public final int primaryColor;
    public final int secondaryColor;
    public final boolean gradient;
    public final boolean glow;

    public JumpCircle$Circle(Vec3d pos, long creationTime, JumpCircle$StyleSnapshot style) {
        this.pos = pos;
        this.creationTime = creationTime;
        this.size = style.circleSize;
        this.primaryColor = style.primaryColor;
        this.secondaryColor = style.secondaryColor;
        this.gradient = style.gradient;
        this.glow = style.glow;
    }
}
