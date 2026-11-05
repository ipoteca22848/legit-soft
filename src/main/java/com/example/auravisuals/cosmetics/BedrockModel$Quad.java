package com.example.auravisuals.cosmetics;

import org.joml.Vector3f;

public class BedrockModel$Quad {
    public final BedrockModel$Vertex[] vertices = new BedrockModel$Vertex[4];
    public Vector3f normal = new Vector3f(0.0F, 1.0F, 0.0F);

    public BedrockModel$Quad(BedrockModel$Vertex v1, BedrockModel$Vertex v2, BedrockModel$Vertex v3, BedrockModel$Vertex v4, Vector3f normal) {
        this.vertices[0] = v1;
        this.vertices[1] = v2;
        this.vertices[2] = v3;
        this.vertices[3] = v4;
        this.normal = normal;
    }
}
