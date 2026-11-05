package com.example.auravisuals.cosmetics;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.Gui;
import com.example.auravisuals.compat.Gui$Tex;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;

public class BedrockModel {
    public final float textureWidth;
    public final float textureHeight;
    public float minY = 0.0F;
    public float maxY = 24.0F;
    public float minX = 0.0F;
    public float maxX = 0.0F;
    public float minZ = 0.0F;
    public float maxZ = 0.0F;
    public final List<BedrockModel$Bone> bones = new ArrayList<>();

    public BedrockModel(float textureWidth, float textureHeight) {
        this.textureWidth = textureWidth > 0.0F ? textureWidth : 64.0F;
        this.textureHeight = textureHeight > 0.0F ? textureHeight : 64.0F;
    }

    public void render(MatrixStack matrices, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha) {
        this.render(matrices, consumer, light, overlay, red, green, blue, alpha, null, 0.0F);
    }

    public void render(
        MatrixStack matrices,
        VertexConsumer consumer,
        int light,
        int overlay,
        float red,
        float green,
        float blue,
        float alpha,
        BedrockAnimation animation,
        float animTime
    ) {
        this.render(matrices.peek(), consumer, light, overlay, red, green, blue, alpha, animation, animTime);
    }

    public void render(
        Entry entry,
        VertexConsumer consumer,
        int light,
        int overlay,
        float red,
        float green,
        float blue,
        float alpha,
        BedrockAnimation animation,
        float animTime
    ) {
        for (BedrockModel$Bone bone : this.bones) {
            if (bone.parent == null || bone.parent.isEmpty()) {
                bone.render(entry, consumer, light, overlay, red, green, blue, alpha, animation, animTime);
            }
        }
    }

    public boolean renderGui(DrawContext context, Identifier texture, MatrixStack matrices, BedrockAnimation animation, float animTime) {
        List<BedrockModel$GuiQuad> quads = new ArrayList<>();

        for (BedrockModel$Bone bone : this.bones) {
            if (bone.parent == null || bone.parent.isEmpty()) {
                bone.collectGuiQuads(matrices.peek(), quads, animation, animTime);
            }
        }

        if (quads.isEmpty()) {
            return false;
        }

        quads.sort(Comparator.comparingDouble(BedrockModel$GuiQuad::depth));
        Gui$Tex batch = Gui.texNearest(context, texture, AuraRender.POS_TEX_COLOR_QUADS_NODEPTH, quads.size() * 4);

        for (BedrockModel$GuiQuad quad : quads) {
            for (BedrockModel$GuiVertex vertex : quad.vertices) {
                batch.vertex(vertex.x, vertex.y, vertex.u, vertex.v, -1);
            }
        }

        batch.submit(context);
        return true;
    }

    public void recalculateBounds() {
        float[] bounds = new float[]{
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY
        };
        Entry identity = new Entry();

        for (BedrockModel$Bone bone : this.bones) {
            if (bone.parent == null || bone.parent.isEmpty()) {
                bone.collectBounds(identity, bounds);
            }
        }

        if (Float.isFinite(bounds[0])) {
            this.minX = bounds[0];
            this.minY = bounds[1];
            this.minZ = bounds[2];
            this.maxX = bounds[3];
            this.maxY = bounds[4];
            this.maxZ = bounds[5];
        }
    }
}
