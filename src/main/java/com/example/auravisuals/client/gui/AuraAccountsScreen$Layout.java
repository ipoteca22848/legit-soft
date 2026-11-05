package com.example.auravisuals.client.gui;

import java.util.Objects;

public final class AuraAccountsScreen$Layout {
    int panelX;
    int panelY;
    int panelW;
    int panelH;
    int titleY;
    int topY;
    int bottomY;
    int controlH;
    int nickX;
    int nickW;
    int randomX;
    int randomW;
    int exitX;
    int exitW;
    int searchX;
    int searchW;
    int sortX;
    int sortW;
    int clearX;
    int clearW;
    int pad;
    int cardGap;
    int rowGap;
    int cardW;
    int cardH;

    public AuraAccountsScreen$Layout(
        int panelX,
        int panelY,
        int panelW,
        int panelH,
        int titleY,
        int topY,
        int bottomY,
        int controlH,
        int nickX,
        int nickW,
        int randomX,
        int randomW,
        int exitX,
        int exitW,
        int searchX,
        int searchW,
        int sortX,
        int sortW,
        int clearX,
        int clearW,
        int pad,
        int cardGap,
        int rowGap,
        int cardW,
        int cardH
    ) {
        this.panelX = panelX;
        this.panelY = panelY;
        this.panelW = panelW;
        this.panelH = panelH;
        this.titleY = titleY;
        this.topY = topY;
        this.bottomY = bottomY;
        this.controlH = controlH;
        this.nickX = nickX;
        this.nickW = nickW;
        this.randomX = randomX;
        this.randomW = randomW;
        this.exitX = exitX;
        this.exitW = exitW;
        this.searchX = searchX;
        this.searchW = searchW;
        this.sortX = sortX;
        this.sortW = sortW;
        this.clearX = clearX;
        this.clearW = clearW;
        this.pad = pad;
        this.cardGap = cardGap;
        this.rowGap = rowGap;
        this.cardW = cardW;
        this.cardH = cardH;
    }

    public int panelX() {
        return this.panelX;
    }

    public int panelY() {
        return this.panelY;
    }

    public int panelW() {
        return this.panelW;
    }

    public int panelH() {
        return this.panelH;
    }

    public int titleY() {
        return this.titleY;
    }

    public int topY() {
        return this.topY;
    }

    public int bottomY() {
        return this.bottomY;
    }

    public int controlH() {
        return this.controlH;
    }

    public int nickX() {
        return this.nickX;
    }

    public int nickW() {
        return this.nickW;
    }

    public int randomX() {
        return this.randomX;
    }

    public int randomW() {
        return this.randomW;
    }

    public int exitX() {
        return this.exitX;
    }

    public int exitW() {
        return this.exitW;
    }

    public int searchX() {
        return this.searchX;
    }

    public int searchW() {
        return this.searchW;
    }

    public int sortX() {
        return this.sortX;
    }

    public int sortW() {
        return this.sortW;
    }

    public int clearX() {
        return this.clearX;
    }

    public int clearW() {
        return this.clearW;
    }

    public int pad() {
        return this.pad;
    }

    public int cardGap() {
        return this.cardGap;
    }

    public int rowGap() {
        return this.rowGap;
    }

    public int cardW() {
        return this.cardW;
    }

    public int cardH() {
        return this.cardH;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof AuraAccountsScreen$Layout other)
                ? false
                : Objects.equals(this.panelX, other.panelX)
                    && Objects.equals(this.panelY, other.panelY)
                    && Objects.equals(this.panelW, other.panelW)
                    && Objects.equals(this.panelH, other.panelH)
                    && Objects.equals(this.titleY, other.titleY)
                    && Objects.equals(this.topY, other.topY)
                    && Objects.equals(this.bottomY, other.bottomY)
                    && Objects.equals(this.controlH, other.controlH)
                    && Objects.equals(this.nickX, other.nickX)
                    && Objects.equals(this.nickW, other.nickW)
                    && Objects.equals(this.randomX, other.randomX)
                    && Objects.equals(this.randomW, other.randomW)
                    && Objects.equals(this.exitX, other.exitX)
                    && Objects.equals(this.exitW, other.exitW)
                    && Objects.equals(this.searchX, other.searchX)
                    && Objects.equals(this.searchW, other.searchW)
                    && Objects.equals(this.sortX, other.sortX)
                    && Objects.equals(this.sortW, other.sortW)
                    && Objects.equals(this.clearX, other.clearX)
                    && Objects.equals(this.clearW, other.clearW)
                    && Objects.equals(this.pad, other.pad)
                    && Objects.equals(this.cardGap, other.cardGap)
                    && Objects.equals(this.rowGap, other.rowGap)
                    && Objects.equals(this.cardW, other.cardW)
                    && Objects.equals(this.cardH, other.cardH);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            this.panelX,
            this.panelY,
            this.panelW,
            this.panelH,
            this.titleY,
            this.topY,
            this.bottomY,
            this.controlH,
            this.nickX,
            this.nickW,
            this.randomX,
            this.randomW,
            this.exitX,
            this.exitW,
            this.searchX,
            this.searchW,
            this.sortX,
            this.sortW,
            this.clearX,
            this.clearW,
            this.pad,
            this.cardGap,
            this.rowGap,
            this.cardW,
            this.cardH
        );
    }

    @Override
    public String toString() {
        return "AuraAccountsScreen$Layout[panelX="
            + this.panelX
            + ", panelY="
            + this.panelY
            + ", panelW="
            + this.panelW
            + ", panelH="
            + this.panelH
            + ", titleY="
            + this.titleY
            + ", topY="
            + this.topY
            + ", bottomY="
            + this.bottomY
            + ", controlH="
            + this.controlH
            + ", nickX="
            + this.nickX
            + ", nickW="
            + this.nickW
            + ", randomX="
            + this.randomX
            + ", randomW="
            + this.randomW
            + ", exitX="
            + this.exitX
            + ", exitW="
            + this.exitW
            + ", searchX="
            + this.searchX
            + ", searchW="
            + this.searchW
            + ", sortX="
            + this.sortX
            + ", sortW="
            + this.sortW
            + ", clearX="
            + this.clearX
            + ", clearW="
            + this.clearW
            + ", pad="
            + this.pad
            + ", cardGap="
            + this.cardGap
            + ", rowGap="
            + this.rowGap
            + ", cardW="
            + this.cardW
            + ", cardH="
            + this.cardH
            + "]";
    }
}
