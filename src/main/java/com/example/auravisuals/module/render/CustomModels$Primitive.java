package com.example.auravisuals.module.render;

import net.minecraft.util.Identifier;

public class CustomModels$Primitive {
    final float[] positions;
    final float[] uvs;
    final int[] indices;
    final Identifier textureId;
    final int color;
    final float minX;
    final float minY;
    final float minZ;
    final float maxX;
    final float maxY;
    final float maxZ;
    final float centerX;
    final float centerY;
    final float centerZ;

    CustomModels$Primitive(float[] positions, float[] uvs, int[] indices, Identifier textureId, int color) {
        this.positions = positions;
        this.uvs = uvs;
        this.indices = indices;
        this.textureId = textureId;
        this.color = color;
        float localMinX = Float.MAX_VALUE;
        float localMinY = Float.MAX_VALUE;
        float localMinZ = Float.MAX_VALUE;
        float localMaxX = -Float.MAX_VALUE;
        float localMaxY = -Float.MAX_VALUE;
        float localMaxZ = -Float.MAX_VALUE;

        for (int i = 0; i < positions.length; i += 3) {
            localMinX = Math.min(localMinX, positions[i]);
            localMinY = Math.min(localMinY, positions[i + 1]);
            localMinZ = Math.min(localMinZ, positions[i + 2]);
            localMaxX = Math.max(localMaxX, positions[i]);
            localMaxY = Math.max(localMaxY, positions[i + 1]);
            localMaxZ = Math.max(localMaxZ, positions[i + 2]);
        }

        this.minX = localMinX;
        this.minY = localMinY;
        this.minZ = localMinZ;
        this.maxX = localMaxX;
        this.maxY = localMaxY;
        this.maxZ = localMaxZ;
        this.centerX = (this.minX + this.maxX) * 0.5F;
        this.centerY = (this.minY + this.maxY) * 0.5F;
        this.centerZ = (this.minZ + this.maxZ) * 0.5F;
    }
}
