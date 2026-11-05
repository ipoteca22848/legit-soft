package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import net.minecraft.client.option.SimpleOption;

public class Gamma extends Module {
    public static final float FULL_BRIGHTNESS_FACTOR = 1000.0F;
    private Double previousGamma = null;
    private static final double MAX_SAVED_GAMMA_VALUE = 1.0;

    public Gamma() {
        super("Gamma", "Максимальная яркость", Category.VISUALS);
    }

    @Override
    public void onEnable() {
        if (this.mc.options != null) {
            try {
                if (this.previousGamma == null) {
                    this.previousGamma = this.clampGamma(this.mc.options.getGamma().getValue());
                }

                this.ensureSavedGammaValid();
                this.forceGamma(1.0);
                this.markLightmapDirty();
            } catch (Throwable var2) {
            }
        }
    }

    @Override
    public void onTick() {
        if (this.mc.options != null) {
            try {
                this.ensureSavedGammaValid();
                this.forceGamma(1.0);
                this.markLightmapDirty();
            } catch (Throwable var2) {
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.mc.options != null) {
            try {
                if (this.previousGamma != null) {
                    this.forceGamma(this.previousGamma);
                }

                this.markLightmapDirty();
            } catch (Throwable var5) {
            } finally {
                this.previousGamma = null;
            }
        }
    }

    private void ensureSavedGammaValid() {
        SimpleOption gamma = this.mc.options.getGamma();
        double value = (Double)gamma.getValue();
        if (Double.isNaN(value) || value < 0.0 || value > 1.0) {
            this.forceGamma(value);
        }
    }

    private void forceGamma(double value) {
        this.mc.options.getGamma().setValue(this.clampGamma(value));
    }

    private double clampGamma(double value) {
        return Double.isNaN(value) ? 1.0 : Math.max(0.0, Math.min(1.0, value));
    }

    private void markLightmapDirty() {
        if (this.mc.gameRenderer != null) {
            this.mc.gameRenderer.getLightmapTextureManager().tick();
        }
    }
}
