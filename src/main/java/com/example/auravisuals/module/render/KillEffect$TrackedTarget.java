package com.example.auravisuals.module.render;

import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class KillEffect$TrackedTarget {
    public final UUID uuid;
    public final LivingEntity entity;
    public final boolean player;
    public final long lastHitAt;
    public Vec3d lastPosition;

    public KillEffect$TrackedTarget(LivingEntity entity, long lastHitAt) {
        this.uuid = entity.getUuid();
        this.entity = entity;
        this.player = entity instanceof PlayerEntity;
        this.lastHitAt = lastHitAt;
        this.lastPosition = entity.getEntityPos();
    }
}
