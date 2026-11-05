package com.example.auravisuals.client.gui;

import com.example.auravisuals.client.gui.font.FontManager;
import net.minecraft.util.Identifier;

public class AuraMainMenuScreen$MenuButton {
    final String label;
    final Identifier icon;
    final char iconChar;
    final boolean exitPng;
    final int x;
    final int y;
    final int size;
    final Runnable action;
    float anim;
    float separatorAnim;

    AuraMainMenuScreen$MenuButton(String label, Identifier icon, char iconChar, boolean exitPng, int x, int y, int size, Runnable action) {
        this.label = label;
        this.icon = icon;
        this.iconChar = iconChar;
        this.exitPng = exitPng;
        this.x = x;
        this.y = y;
        this.size = size;
        this.action = action;
    }

    boolean contains(double mouseX, double mouseY) {
        float labelW = FontManager.getScaledTextWidth(this.label, 0.36F);
        float expandW = this.size + (labelW + 21.0F) * this.anim;
        float drawX = this.x + this.size - expandW;
        return mouseX >= drawX && mouseX <= this.x + this.size && mouseY >= this.y && mouseY <= this.y + this.size;
    }
}
