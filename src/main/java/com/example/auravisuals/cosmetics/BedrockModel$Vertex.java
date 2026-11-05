package com.example.auravisuals.cosmetics;

import org.joml.Vector3f;

public class BedrockModel$Vertex {
    public final Vector3f pos;
    public final float u;
    public final float v;

    public BedrockModel$Vertex(float x, float y, float z, float u, float v) {
        this.pos = new Vector3f(x, y, z);
        this.u = u;
        this.v = v;
    }
}
