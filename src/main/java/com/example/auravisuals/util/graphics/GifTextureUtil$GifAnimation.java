package com.example.auravisuals.util.graphics;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Identifier;

public final class GifTextureUtil$GifAnimation {
    final List<Identifier> frameIds;
    final List<Integer> frameDurationsMs;
    final int width;
    final int height;
    final long totalDurationMs;
    final Map<String, Integer> lastFrameIndices = new ConcurrentHashMap<>();

    GifTextureUtil$GifAnimation(List<Identifier> frameIds, List<Integer> frameDurationsMs, int width, int height) {
        this.frameIds = frameIds;
        this.frameDurationsMs = frameDurationsMs;
        this.width = width;
        this.height = height;
        long total = 0L;

        for (Integer duration : frameDurationsMs) {
            total += Math.max(20, duration == null ? 100 : duration);
        }

        this.totalDurationMs = Math.max(1L, total);
    }

    Identifier getCurrentFrame(String stateKey, boolean play) {
        String key = stateKey != null && !stateKey.isBlank() ? stateKey : "default";
        if (this.frameIds.size() <= 1) {
            return this.frameIds.getFirst();
        }

        if (!play) {
            int frozenIndex = this.lastFrameIndices.getOrDefault(key, 0);
            return this.frameIds.get(Math.max(0, Math.min(frozenIndex, this.frameIds.size() - 1)));
        }

        long frameTime = Math.floorMod(System.currentTimeMillis(), this.totalDurationMs);
        long passed = 0L;

        for (int i = 0; i < this.frameIds.size(); i++) {
            passed += Math.max(20, this.frameDurationsMs.get(i));
            if (frameTime < passed) {
                this.lastFrameIndices.put(key, i);
                return this.frameIds.get(i);
            }
        }

        this.lastFrameIndices.put(key, this.frameIds.size() - 1);
        return this.frameIds.getLast();
    }
}
