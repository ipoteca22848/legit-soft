package com.example.auravisuals.util.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class WorldOcclusion {
    private static final double[] HEIGHT_SAMPLES = new double[]{0.18, 0.52, 0.9};

    private WorldOcclusion() {
    }

    public static boolean isEntityVisible(LivingEntity entity, Vec3d cameraPos, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (entity == null || mc.world == null || mc.player == null) {
            return false;
        }

        if (entity == mc.player) {
            return true;
        }

        Vec3d base = entity.getLerpedPos(tickDelta);
        double height = Math.max(0.2, entity.getHeight());

        for (double sample : HEIGHT_SAMPLES) {
            Vec3d target = base.add(0.0, height * sample, 0.0);
            HitResult hit = mc.world.raycast(new RaycastContext(cameraPos, target, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
            if (hit == null || hit.getType() == Type.MISS) {
                return true;
            }
        }

        return false;
    }
}
