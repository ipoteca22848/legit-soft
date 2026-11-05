package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;

public final class FreeFPS extends Module {
    private static final int SAMPLE_COUNT = 100;
    private static FreeFPS instance;
    private static final ThreadLocal<Boolean> READING_ACTUAL_FPS = ThreadLocal.withInitial(() -> false);
    private final int[] samples = new int[100];
    private long sampleSum;
    private int sampleSize;
    private int sampleIndex;

    public FreeFPS() {
        super("FreeFPS", "Умножает средний FPS на 10 чисто ради рофла", Category.MISC);
        instance = this;
    }

    @Override
    public void onEnable() {
        this.resetSamples();
        this.sampleCurrentFps();
    }

    @Override
    public void onTick() {
        this.sampleCurrentFps();
    }

    public static int displayFps(int actualFps) {
        if (READING_ACTUAL_FPS.get()) {
            return Math.max(0, actualFps);
        } else {
            FreeFPS module = instance;
            if (module != null && module.isEnabled()) {
                double average = module.sampleSize == 0 ? Math.max(0, actualFps) : (double)module.sampleSum / module.sampleSize;
                return (int)Math.min(2147483647L, Math.round(average * 10.0));
            } else {
                return Math.max(0, actualFps);
            }
        }
    }

    private void sampleCurrentFps() {
        if (this.mc != null) {
            READING_ACTUAL_FPS.set(true);

            int fps;
            try {
                fps = Math.max(0, this.mc.getCurrentFps());
            } finally {
                READING_ACTUAL_FPS.set(false);
            }

            if (this.sampleSize == 100) {
                this.sampleSum = this.sampleSum - this.samples[this.sampleIndex];
            } else {
                this.sampleSize++;
            }

            this.samples[this.sampleIndex] = fps;
            this.sampleSum += fps;
            this.sampleIndex = (this.sampleIndex + 1) % 100;
        }
    }

    private void resetSamples() {
        this.sampleSum = 0L;
        this.sampleSize = 0;
        this.sampleIndex = 0;
    }
}
