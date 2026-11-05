package com.example.auravisuals.module.render;

import net.minecraft.util.Identifier;

public class CustomModels$MaterialInfo {
    public Identifier textureId;
    public int color;

    public Identifier textureId() {
        return this.textureId;
    }

    public int color() {
        return this.color;
    }

    public CustomModels$MaterialInfo(Identifier textureId, int color) {
        this.textureId = textureId;
        this.color = color;
    }
}
