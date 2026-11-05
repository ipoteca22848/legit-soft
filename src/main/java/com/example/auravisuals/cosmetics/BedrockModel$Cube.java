package com.example.auravisuals.cosmetics;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class BedrockModel$Cube {
    public float[] origin = new float[3];
    public float[] size = new float[3];
    public float[] rotation = new float[]{0.0F, 0.0F, 0.0F};
    public float[] pivot = new float[]{0.0F, 0.0F, 0.0F};
    public float inflate = 0.0F;
    public boolean mirror = false;
    public final List<BedrockModel$Quad> quads = new ArrayList<>();

    public void render(Entry parentEntry, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha) {
        Entry entry = parentEntry.copy();
        if (this.rotation[0] != 0.0F || this.rotation[1] != 0.0F || this.rotation[2] != 0.0F) {
            entry.translate(this.pivot[0] / 16.0F, this.pivot[1] / 16.0F, this.pivot[2] / 16.0F);
            if (this.rotation[2] != 0.0F) {
                entry.rotate(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotation[2]));
            }

            if (this.rotation[1] != 0.0F) {
                entry.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotation[1]));
            }

            if (this.rotation[0] != 0.0F) {
                entry.rotate(RotationAxis.POSITIVE_X.rotationDegrees(this.rotation[0]));
            }

            entry.translate(-this.pivot[0] / 16.0F, -this.pivot[1] / 16.0F, -this.pivot[2] / 16.0F);
        }

        Matrix4f posMatrix = entry.getPositionMatrix();

        for (BedrockModel$Quad quad : this.quads) {
            for (BedrockModel$Vertex v : quad.vertices) {
                consumer.vertex(posMatrix, v.pos.x, v.pos.y, v.pos.z)
                    .color(red, green, blue, alpha)
                    .texture(v.u, v.v)
                    .overlay(overlay)
                    .light(light)
                    .normal(entry, quad.normal.x, quad.normal.y, quad.normal.z);
            }
        }
    }

    void collectGuiQuads(Entry parentEntry, List<BedrockModel$GuiQuad> output) {
        Entry entry = parentEntry.copy();
        if (this.rotation[0] != 0.0F || this.rotation[1] != 0.0F || this.rotation[2] != 0.0F) {
            entry.translate(this.pivot[0] / 16.0F, this.pivot[1] / 16.0F, this.pivot[2] / 16.0F);
            if (this.rotation[2] != 0.0F) {
                entry.rotate(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotation[2]));
            }

            if (this.rotation[1] != 0.0F) {
                entry.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotation[1]));
            }

            if (this.rotation[0] != 0.0F) {
                entry.rotate(RotationAxis.POSITIVE_X.rotationDegrees(this.rotation[0]));
            }

            entry.translate(-this.pivot[0] / 16.0F, -this.pivot[1] / 16.0F, -this.pivot[2] / 16.0F);
        }

        Matrix4f matrix = entry.getPositionMatrix();

        for (BedrockModel$Quad quad : this.quads) {
            BedrockModel$GuiVertex[] vertices = new BedrockModel$GuiVertex[4];
            float depth = 0.0F;

            for (int index = 0; index < quad.vertices.length; index++) {
                BedrockModel$Vertex source = quad.vertices[index];
                Vector3f position = matrix.transformPosition(new Vector3f(source.pos));
                vertices[index] = new BedrockModel$GuiVertex(position.x, position.y, position.z, source.u, source.v);
                depth += position.z;
            }

            output.add(new BedrockModel$GuiQuad(vertices, depth * 0.25F));
        }
    }

    void collectBounds(Entry parentEntry, float[] bounds) {
        Entry entry = parentEntry.copy();
        if (this.rotation[0] != 0.0F || this.rotation[1] != 0.0F || this.rotation[2] != 0.0F) {
            entry.translate(this.pivot[0] / 16.0F, this.pivot[1] / 16.0F, this.pivot[2] / 16.0F);
            if (this.rotation[2] != 0.0F) {
                entry.rotate(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotation[2]));
            }

            if (this.rotation[1] != 0.0F) {
                entry.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotation[1]));
            }

            if (this.rotation[0] != 0.0F) {
                entry.rotate(RotationAxis.POSITIVE_X.rotationDegrees(this.rotation[0]));
            }

            entry.translate(-this.pivot[0] / 16.0F, -this.pivot[1] / 16.0F, -this.pivot[2] / 16.0F);
        }

        Matrix4f matrix = entry.getPositionMatrix();
        Vector3f transformed = new Vector3f();

        for (BedrockModel$Quad quad : this.quads) {
            for (BedrockModel$Vertex vertex : quad.vertices) {
                matrix.transformPosition(vertex.pos, transformed);
                bounds[0] = Math.min(bounds[0], transformed.x);
                bounds[1] = Math.min(bounds[1], transformed.y);
                bounds[2] = Math.min(bounds[2], transformed.z);
                bounds[3] = Math.max(bounds[3], transformed.x);
                bounds[4] = Math.max(bounds[4], transformed.y);
                bounds[5] = Math.max(bounds[5], transformed.z);
            }
        }
    }
}
