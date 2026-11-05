package com.example.auravisuals.module.render;

import com.example.auravisuals.theme.ThemeManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.math.MathHelper;

public final class JumpCircle$StyleSnapshot {
    public final String mode;
    public final float circleSize;
    public final int primaryColor;
    public final int secondaryColor;
    public final boolean gradient;
    public final boolean glow;
    public final float newCircleRadius;
    public final boolean vaporGlow;
    public final float glassStrength;
    public final float glassRadius;
    public final float glassTime;

    public JumpCircle$StyleSnapshot(
        String mode,
        float circleSize,
        int primaryColor,
        int secondaryColor,
        boolean gradient,
        boolean glow,
        float newCircleRadius,
        boolean vaporGlow,
        float glassStrength,
        float glassRadius,
        float glassTime
    ) {
        this.mode = mode;
        this.circleSize = circleSize;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.gradient = gradient;
        this.glow = glow;
        this.newCircleRadius = newCircleRadius;
        this.vaporGlow = vaporGlow;
        this.glassStrength = glassStrength;
        this.glassRadius = glassRadius;
        this.glassTime = glassTime;
    }

    public static JumpCircle$StyleSnapshot capture() {
        boolean pentagon = JumpCircle.mode.is("Пятиугольник");
        boolean customColor = pentagon || JumpCircle.useCustomColor.getValue();
        int primary = customColor ? JumpCircle.color.getValue() : ThemeManager.INSTANCE.getVisualModulesColor();
        int secondary = pentagon ? JumpCircle.gradientColor.getValue() : primary;
        return new JumpCircle$StyleSnapshot(
            JumpCircle.mode.getValue(),
            JumpCircle.circleSize.getFloatValue(),
            primary,
            secondary,
            pentagon,
            pentagon,
            JumpCircle.newCircleRadius.getFloatValue(),
            true,
            JumpCircle.glassStrength.getFloatValue(),
            JumpCircle.glassRadius.getFloatValue(),
            JumpCircle.glassTime.getFloatValue()
        );
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("mode", this.mode);
        json.addProperty("circleSize", this.circleSize);
        json.addProperty("primaryColor", this.primaryColor);
        json.addProperty("secondaryColor", this.secondaryColor);
        json.addProperty("gradient", this.gradient);
        json.addProperty("glow", this.glow);
        json.addProperty("newCircleRadius", this.newCircleRadius);
        json.addProperty("vaporGlow", this.vaporGlow);
        json.addProperty("glassStrength", this.glassStrength);
        json.addProperty("glassRadius", this.glassRadius);
        json.addProperty("glassTime", this.glassTime);
        return json;
    }

    public static JumpCircle$StyleSnapshot fromJson(JsonObject json) {
        String parsedMode = allowedString(json, "mode", "JumpCircle$Circle", JumpCircle.mode.getModes().toArray(String[]::new));
        boolean pentagon = "Пятиугольник".equalsIgnoreCase(parsedMode);
        return new JumpCircle$StyleSnapshot(
            parsedMode,
            number(json, "circleSize", 1.0F, 0.5F, 2.0F),
            integer(json, "primaryColor", -1),
            integer(json, "secondaryColor", -1),
            pentagon || bool(json, "gradient", false),
            pentagon || bool(json, "glow", true),
            number(json, "newCircleRadius", 2.0F, 0.5F, 2.0F),
            true,
            number(json, "glassStrength", 0.8F, 0.2F, 2.0F),
            number(json, "glassRadius", 1.0F, 0.5F, 3.0F),
            number(json, "glassTime", 1250.0F, 500.0F, 2500.0F)
        );
    }

    public boolean isMode(String value) {
        return this.mode.equalsIgnoreCase(value);
    }

    public static String allowedString(JsonObject json, String key, String fallback, String... allowed) {
        try {
            String value = json.get(key).getAsString();

            for (String option : allowed) {
                if (option.equalsIgnoreCase(value)) {
                    return option;
                }
            }
        } catch (Exception var9) {
        }

        return fallback;
    }

    public static float number(JsonObject json, String key, float fallback, float min, float max) {
        try {
            float value = json.get(key).getAsFloat();
            if (Float.isFinite(value)) {
                return MathHelper.clamp(value, min, max);
            }
        } catch (Exception var6) {
        }

        return fallback;
    }

    public static int integer(JsonObject json, String key, int fallback) {
        try {
            JsonElement value = json.get(key);
            return value == null ? fallback : value.getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static boolean bool(JsonObject json, String key, boolean fallback) {
        try {
            JsonElement value = json.get(key);
            return value == null ? fallback : value.getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
