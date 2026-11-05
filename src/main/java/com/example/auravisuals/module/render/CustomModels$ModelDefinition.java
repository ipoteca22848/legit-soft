package com.example.auravisuals.module.render;

import net.minecraft.util.Identifier;

public class CustomModels$ModelDefinition {
    public String name;
    public Identifier modelId;
    public Identifier textureId;
    public float scale;
    public float height;
    public float width;

    public String name() {
        return this.name;
    }

    public Identifier modelId() {
        return this.modelId;
    }

    public Identifier textureId() {
        return this.textureId;
    }

    public float scale() {
        return this.scale;
    }

    public float height() {
        return this.height;
    }

    public float width() {
        return this.width;
    }

    public CustomModels$ModelDefinition(String name, Identifier modelId, Identifier textureId, float scale, float height, float width) {
        this.name = name;
        this.modelId = modelId;
        this.textureId = textureId;
        this.scale = scale;
        this.height = height;
        this.width = width;
    }
}
