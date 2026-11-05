package com.example.auravisuals.util.render.gui;

import net.minecraft.util.Identifier;

public final class GifOption {
    private static final int UNIFORM_MS = 50;
    public final String id;
    public final String name;
    public final int frameCount;
    public final Identifier[] ids;
    public final int[] durationsMs;
    public final int frameW;
    public final int frameH;
    private final long totalMs;
    private volatile boolean ready;
    private volatile boolean loading;
    private volatile int loadedFrames;
    private Runnable activator;

    public GifOption(String id, String name, Identifier[] ids, int[] durationsMs, int frameW, int frameH, boolean ready) {
        this.id = id;
        this.name = name;
        this.ids = ids;
        this.frameCount = ids.length;
        this.durationsMs = durationsMs;
        this.frameW = frameW;
        this.frameH = frameH;
        this.ready = ready;
        this.loadedFrames = ready ? ids.length : 0;
        long t = 0L;
        if (durationsMs != null) {
            for (int d : durationsMs) {
                t += Math.max(1, d);
            }
        } else {
            t = this.frameCount * 50L;
        }

        this.totalMs = Math.max(1L, t);
    }

    public void setActivator(Runnable activator) {
        this.activator = activator;
    }

    public boolean isReady() {
        return this.ready;
    }

    public boolean isLoading() {
        return this.loading;
    }

    public int loadedFrames() {
        return this.loadedFrames;
    }

    public int progressPercent() {
        if (this.ready) {
            return 100;
        } else {
            return this.frameCount <= 0 ? 0 : Math.min(99, this.loadedFrames * 100 / this.frameCount);
        }
    }

    public void setLoading(boolean v) {
        this.loading = v;
    }

    public void setLoadedFrames(int v) {
        this.loadedFrames = v;
    }

    public void setReady(boolean v) {
        this.ready = v;
        if (v) {
            this.loading = false;
        }
    }

    public void reset() {
        this.ready = false;
        this.loading = false;
        this.loadedFrames = 0;
    }

    public void ensureLoading() {
        if (!this.ready && !this.loading) {
            Runnable a = this.activator;
            if (this.activator != null) {
                try {
                    a.run();
                } catch (Throwable var3) {
                }
            }
        }
    }

    public Identifier frameAt(long nowMs) {
        if (!this.ready) {
            this.ensureLoading();
            return null;
        }

        if (this.frameCount <= 0) {
            return null;
        }

        if (this.durationsMs != null) {
            long t = nowMs % this.totalMs;

            for (int i = 0; i < this.frameCount; i++) {
                if ((t -= Math.max(1, this.durationsMs[i])) < 0L) {
                    return this.ids[i];
                }
            }

            return this.ids[this.frameCount - 1];
        } else {
            int idx = (int)(nowMs % (this.frameCount * 50L) / 50L);
            if (idx < 0 || idx >= this.frameCount) {
                idx = 0;
            }

            return this.ids[idx];
        }
    }
}
