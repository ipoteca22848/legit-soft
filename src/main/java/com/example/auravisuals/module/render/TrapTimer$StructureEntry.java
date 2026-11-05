package com.example.auravisuals.module.render;

import net.minecraft.util.math.Vec3d;

public class TrapTimer$StructureEntry {
    public final Vec3d position;
    public final long createdAt;
    public TrapTimer$StructureType type;
    public long duration;
    public boolean typeDetermined;
    public boolean needsNetheriteCheck;

    public TrapTimer$StructureEntry(
        Vec3d position, long createdAt, TrapTimer$StructureType type, long duration, boolean typeDetermined, boolean needsNetheriteCheck
    ) {
        this.position = position;
        this.createdAt = createdAt;
        this.type = type;
        this.duration = duration;
        this.typeDetermined = typeDetermined;
        this.needsNetheriteCheck = needsNetheriteCheck;
    }
}
