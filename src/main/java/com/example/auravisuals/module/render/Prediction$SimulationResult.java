package com.example.auravisuals.module.render;

import java.util.List;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

public class Prediction$SimulationResult {
    public final List<Vec3d> path;
    public final Vec3d hitPos;
    public final HitResult hitResult;
    public final boolean hitEntity;
    public final float time;

    public Prediction$SimulationResult(List<Vec3d> path, Vec3d hitPos, HitResult hitResult, boolean hitEntity, float time) {
        this.path = path;
        this.hitPos = hitPos;
        this.hitResult = hitResult;
        this.hitEntity = hitEntity;
        this.time = time;
    }
}
