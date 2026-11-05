package com.example.auravisuals.util.render;

public final class HandBlurHelper {
    private static boolean active;
    private static int blurPasses;
    private static boolean blurRequested;

    private HandBlurHelper() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void beginBlurHand(int passes, boolean doBlur) {
        blurPasses = Math.max(1, passes);
        blurRequested = doBlur;
        HandFxPipeline.captureBefore();
        active = true;
    }

    public static void bindHelperFramebuffer() {
    }

    public static void beginOutlineSilhouettePass() {
    }

    public static void endOutlineSilhouettePass() {
    }

    public static void captureOutlineMaskFromStencil() {
    }

    public static void endBlurHand(
        float tintR,
        float tintG,
        float tintB,
        float tintA,
        float itemLayerOpacity,
        boolean silhouetteOutline,
        float outlineR,
        float outlineG,
        float outlineB,
        boolean doBlur
    ) {
        endBlurHand(tintR, tintG, tintB, tintA, itemLayerOpacity, silhouetteOutline, outlineR, outlineG, outlineB, 1.0F, doBlur);
    }

    public static void endBlurHand(
        float tintR,
        float tintG,
        float tintB,
        float tintA,
        float itemLayerOpacity,
        boolean silhouetteOutline,
        float outlineR,
        float outlineG,
        float outlineB,
        float outlineAlpha,
        boolean doBlur
    ) {
        if (active) {
            active = false;
            if (HandFxPipeline.captureAfter()) {
                float now = (float)(System.currentTimeMillis() % 100000L) / 1000.0F;
                if (doBlur && blurRequested) {
                    int tint = packColor(tintR, tintG, tintB, tintA);
                    HandFxPipeline.composite(1, blurPasses / 3.0F, now, Math.max(0.0F, Math.min(1.0F, itemLayerOpacity)), tint);
                }

                if (silhouetteOutline) {
                    HandFxPipeline.composite(3, 1.0F, now, Math.max(0.0F, Math.min(1.0F, outlineAlpha)), packColor(outlineR, outlineG, outlineB, outlineAlpha));
                }
            }
        }
    }

    private static int packColor(float red, float green, float blue, float alpha) {
        int r = Math.round(Math.max(0.0F, Math.min(1.0F, red)) * 255.0F);
        int g = Math.round(Math.max(0.0F, Math.min(1.0F, green)) * 255.0F);
        int b = Math.round(Math.max(0.0F, Math.min(1.0F, blue)) * 255.0F);
        int a = Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F);
        return a << 24 | r << 16 | g << 8 | b;
    }
}
