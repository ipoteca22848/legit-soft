package com.example.auravisuals.module.render;

import java.util.UUID;

public final class TntTimer$TimerEntry {
    public final UUID uuid;
    public final long order;
    public int entityId;
    public int fuseTicks;
    public String name;
    public float currentY = Float.NaN;
    public float alpha;
    public boolean leaving;

    public TntTimer$TimerEntry(UUID uuid, int entityId, long order, String name) {
        this.uuid = uuid;
        this.entityId = entityId;
        this.order = order;
        this.name = name;
    }
}
