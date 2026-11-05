package com.example.auravisuals.module.render;

public class ViewModel$EditorSelection {
    public float centerX;
    public float centerY;
    public float otherCenterX;
    public float otherCenterY;
    public float opacity;
    public float expansion;
    public boolean mainHand;

    public float centerX() {
        return this.centerX;
    }

    public float centerY() {
        return this.centerY;
    }

    public float otherCenterX() {
        return this.otherCenterX;
    }

    public float otherCenterY() {
        return this.otherCenterY;
    }

    public float opacity() {
        return this.opacity;
    }

    public float expansion() {
        return this.expansion;
    }

    public boolean mainHand() {
        return this.mainHand;
    }

    public ViewModel$EditorSelection(float centerX, float centerY, float otherCenterX, float otherCenterY, float opacity, float expansion, boolean mainHand) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.otherCenterX = otherCenterX;
        this.otherCenterY = otherCenterY;
        this.opacity = opacity;
        this.expansion = expansion;
        this.mainHand = mainHand;
    }
}
