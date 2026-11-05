package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BindSetting;

public class Zoom extends Module {
    public static final BindSetting bind = new BindSetting("Приблизить: ", 67);
    private static final double DEFAULT_ZOOM_AMOUNT = 50.0;
    private static final double MIN_ZOOM_AMOUNT = 5.0;
    private static final double MAX_ZOOM_AMOUNT = 90.0;
    private static final double SCROLL_STEP = 2.0;
    private double currentAmount = 50.0;
    private boolean wasZoomingOnTick = false;
    private boolean wasZoomingForSmoothing = false;
    private float smoothedFov = -1.0F;
    private long lastSmoothNanos = 0L;

    public Zoom() {
        super("Zoom", "Приближение камеры", Category.VISUALS);
        this.addSettings(bind);
        this.setKey(0);
    }

    public double getAmount() {
        return this.currentAmount;
    }

    public float getSmoothedOrInstantFov(float vanillaFov, float tickDelta, boolean smoothMotion) {
        if (!smoothMotion) {
            this.smoothedFov = -1.0F;
            this.lastSmoothNanos = 0L;
            return this.isZooming() ? (float)this.currentAmount : vanillaFov;
        } else {
            return this.getSmoothedFov(vanillaFov);
        }
    }

    public float getSmoothedFov(float vanillaFov) {
        boolean zooming = this.isZooming();
        if (!zooming) {
            this.wasZoomingForSmoothing = false;
            this.smoothedFov = -1.0F;
            this.lastSmoothNanos = 0L;
            return vanillaFov;
        }

        if (this.smoothedFov < 0.0F || Float.isNaN(this.smoothedFov) || !this.wasZoomingForSmoothing) {
            this.smoothedFov = vanillaFov;
            this.lastSmoothNanos = 0L;
        }

        this.wasZoomingForSmoothing = true;
        float target = (float)this.currentAmount;
        long now = System.nanoTime();
        float dt = this.lastSmoothNanos == 0L ? 0.016666668F : Math.min(0.08F, Math.max(0.004166667F, (float)(now - this.lastSmoothNanos) / 1.0E9F));
        this.lastSmoothNanos = now;
        float speed = 8.0F;
        float factor = 1.0F - (float)Math.exp(-speed * dt);
        this.smoothedFov = this.smoothedFov + (target - this.smoothedFov) * factor;
        return this.smoothedFov;
    }

    public void adjustZoomByScroll(double vertical) {
        if (vertical != 0.0) {
            double next = this.currentAmount - Math.signum(vertical) * 2.0;
            this.currentAmount = Math.max(5.0, Math.min(90.0, next));
        }
    }

    public boolean isZooming() {
        if (!this.isEnabled()) {
            return false;
        }

        if (this.mc.getWindow() == null) {
            return false;
        }

        if (BindSetting.shouldBlockKeyInput()) {
            return false;
        }

        int key = bind.getValue();
        return key <= 0 ? false : BindSetting.isPressed(this.mc.getWindow().getHandle(), key);
    }

    @Override
    public void onTick() {
        if (this.getKey() != 0) {
            this.setKey(0);
        }

        boolean zooming = this.isZooming();
        if (this.wasZoomingOnTick && !zooming) {
            this.currentAmount = 50.0;
        }

        this.wasZoomingOnTick = zooming;
    }

    @Override
    public void onDisable() {
        this.currentAmount = 50.0;
        this.wasZoomingOnTick = false;
        this.wasZoomingForSmoothing = false;
        this.smoothedFov = -1.0F;
        this.lastSmoothNanos = 0L;
    }
}
