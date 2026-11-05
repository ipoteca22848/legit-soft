package com.example.auravisuals.module.render;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Particles$CustomParticle {
    public Particles outer;
    Vec3d prevPos;
    Vec3d pos;
    Vec3d velocity;
    float maxLife;
    float life;
    float prevLife;
    float scale;
    float prevRotation;
    float rotation;
    float rotSpeed;
    int color;
    Identifier texture;
    String flightLogic;

    float getVisualLife(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.prevLife, this.life);
    }

    public Particles$CustomParticle(Particles outer, Vec3d pos, Vec3d velocity, float life, int color, Identifier texture, String flightLogic) {
        this.outer = outer;
        this.pos = pos;
        this.prevPos = pos;
        this.velocity = velocity;
        this.maxLife = life;
        this.life = life;
        this.prevLife = life;
        this.scale = (float)(0.5 + Math.random() * 0.5);
        this.color = color;
        this.texture = texture;
        this.flightLogic = flightLogic;
        this.prevRotation = this.rotation = (float)(Math.random() * 360.0);
        this.rotSpeed = (float)((Math.random() - 0.5) * 10.0);
    }

    public void update() {
        this.prevPos = this.pos;
        this.prevRotation = this.rotation;
        this.prevLife = this.life--;
        float ageTicks = this.maxLife - this.life;
        float freeFlightTicks = 4.0F;
        if ("Свободный полёт".equals(this.flightLogic)) {
            this.pos = this.pos.add(this.velocity);
            this.velocity = this.velocity.multiply(0.95);
        } else if ("Взлёт".equals(this.flightLogic)) {
            this.pos = this.pos.add(this.velocity);
            this.velocity = this.velocity.multiply(0.95);
            if (ageTicks > freeFlightTicks) {
                float phase = (ageTicks - freeFlightTicks) / 30.0F;
                float accel = 0.015F + 0.02F * MathHelper.clamp(phase, 0.0F, 1.0F);
                this.velocity = this.velocity.add(0.0, accel, 0.0);
            }
        } else if ("Падение".equals(this.flightLogic)) {
            this.pos = this.pos.add(this.velocity);
            this.velocity = this.velocity.multiply(0.95);
            if (ageTicks > freeFlightTicks) {
                this.velocity = this.velocity.subtract(0.0, 0.05, 0.0);
            }

            BlockPos below;
            if (this.outer.mc.world != null
                && this.velocity.y < 0.0
                && this.outer.isSolidBlock(below = BlockPos.ofFloored(this.pos.x, this.pos.y - 0.05, this.pos.z))) {
                double blockTop = below.getY() + 1.0;
                this.pos = new Vec3d(this.pos.x, blockTop + 0.05, this.pos.z);
                this.velocity = new Vec3d(this.velocity.x, -this.velocity.y * 0.6, this.velocity.z);
            }
        } else {
            this.pos = this.pos.add(this.velocity);
            this.velocity = this.velocity.multiply(0.95);
        }

        this.rotation = this.rotation + this.rotSpeed;
    }

    public boolean isDead() {
        return this.life < 0.0F;
    }
}
