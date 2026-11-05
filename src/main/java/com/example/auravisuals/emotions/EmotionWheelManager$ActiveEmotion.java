package com.example.auravisuals.emotions;

import net.minecraft.util.math.MathHelper;

public final class EmotionWheelManager$ActiveEmotion {
    final EmotionType type;
    final long startedAt;
    final boolean local;

    EmotionWheelManager$ActiveEmotion(EmotionType type, long startedAt, boolean local) {
        this.type = type;
        this.startedAt = startedAt;
        this.local = local;
    }

    boolean expired() {
        return System.currentTimeMillis() - this.startedAt >= this.type.durationMs();
    }

    float blend() {
        long elapsed = System.currentTimeMillis() - this.startedAt;
        long left = Math.max(0L, this.type.durationMs() - elapsed);
        float in = MathHelper.clamp((float)elapsed / 280.0F, 0.0F, 1.0F);
        float out = MathHelper.clamp((float)left / 320.0F, 0.0F, 1.0F);
        float v = Math.min(in, out);
        return v * v * (3.0F - 2.0F * v);
    }
}
