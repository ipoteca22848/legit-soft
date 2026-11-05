package com.example.auravisuals.module.render;

import net.minecraft.client.model.ModelPart.Quad;
import net.minecraft.client.model.ModelPart.Vertex;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Split$TransformedQuad {
    final Vector3f[] vertices = new Vector3f[4];

    Split$TransformedQuad(Quad source, Matrix4f transform) {
        for (int i = 0; i < 4; i++) {
            Vertex v = source.vertices()[i];
            float vx = v.x() / 16.0F;
            float vy = v.y() / 16.0F;
            float vz = v.z() / 16.0F;
            Vector3f pos = new Vector3f(vx, vy, vz);
            transform.transformPosition(pos);
            this.vertices[i] = pos;
        }
    }
}
