package com.example.auravisuals.module.render;

public class Markers$MarkerData {
    public final String name;
    public final double x;
    public final double y;
    public final double z;
    public final char iconChar;
    public final int backgroundColor;
    public final int textColor;
    public final int iconColor;
    public final int iconBackgroundColor;
    public final int opacity;
    public final float size;

    public Markers$MarkerData(
        String name, double x, double y, double z, char iconChar, int backgroundColor, int textColor, int iconColor, int opacity, float size
    ) {
        this(name, x, y, z, iconChar, backgroundColor, textColor, iconColor, opacity, size, Markers.getIconBackgroundColor());
    }

    public Markers$MarkerData(
        String name,
        double x,
        double y,
        double z,
        char iconChar,
        int backgroundColor,
        int textColor,
        int iconColor,
        int opacity,
        float size,
        int iconBackgroundColor
    ) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.iconChar = iconChar;
        this.backgroundColor = Markers.opaqueColor(backgroundColor);
        this.textColor = Markers.opaqueColor(textColor);
        this.iconColor = Markers.opaqueColor(iconColor);
        this.iconBackgroundColor = Markers.opaqueColor(iconBackgroundColor);
        this.opacity = 255;
        this.size = size;
    }

    public String getName() {
        return this.name;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public char getIconChar() {
        return this.iconChar;
    }

    public int getBackgroundColor() {
        return this.backgroundColor;
    }

    public int getTextColor() {
        return this.textColor;
    }

    public int getIconColor() {
        return this.iconColor;
    }

    public int getIconBackgroundColor() {
        return this.iconBackgroundColor;
    }

    public int getOpacity() {
        return this.opacity;
    }

    public float getSize() {
        return this.size;
    }
}
