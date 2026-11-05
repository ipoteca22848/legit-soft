package com.example.auravisuals.module.render;

import net.minecraft.entity.effect.StatusEffectInstance;

public class EffectRemind$ExpiringEntry {
    final StatusEffectInstance instance;
    final float remainingSeconds;

    EffectRemind$ExpiringEntry(StatusEffectInstance instance, float remainingSeconds) {
        this.instance = instance;
        this.remainingSeconds = remainingSeconds;
    }
}
