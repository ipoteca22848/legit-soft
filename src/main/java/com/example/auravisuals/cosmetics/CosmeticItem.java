package com.example.auravisuals.cosmetics;

import net.minecraft.util.Identifier;

public class CosmeticItem {
    private final String id;
    private final String slug;
    private final String name;
    private final String category;
    private final String modelPath;
    private final String texturePath;
    private final String previewPath;
    private final String animationPath;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final Identifier textureId;
    private final Identifier previewId;
    private final float[] orientationRotation = new float[]{0.0F, 0.0F, 0.0F};
    private final float[] orientationSign = new float[]{1.0F, 1.0F, 1.0F};
    private BedrockModel model;
    private float[] pulseTranslation,pulseRotation;private float pulseScale=1;
    private int textureFrames=1,textureFrameTicks=1;
    public void setTextureAnimation(int frames,int ticks){textureFrames=Math.max(1,frames);textureFrameTicks=Math.max(1,ticks);if(model!=null)model.setTextureAnimation(textureFrames,textureFrameTicks);}
    public int getTextureFrames(){return textureFrames;}
    public void setPulsePlacement(float[] translation,float[] rotation,float scale){pulseTranslation=translation.clone();pulseRotation=rotation.clone();pulseScale=scale;}
    public boolean applyPulsePlacement(net.minecraft.client.util.math.MatrixStack matrices){
        if(pulseTranslation==null)return false;
        matrices.translate(pulseTranslation[0],pulseTranslation[1],pulseTranslation[2]);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(pulseRotation[1]));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(pulseRotation[0]));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(pulseRotation[2]));
        matrices.scale(pulseScale,pulseScale,pulseScale);return true;
    }
    private BedrockAnimation animation;

    public CosmeticItem(
        String id,
        String slug,
        String name,
        String category,
        String modelPath,
        String texturePath,
        String previewPath,
        String animationPath,
        float offsetX,
        float offsetY,
        float offsetZ
    ) {
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.category = category;
        this.modelPath = modelPath;
        this.texturePath = texturePath;
        this.previewPath = previewPath != null ? previewPath : texturePath;
        this.animationPath = animationPath;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.textureId = Identifier.of("auravisuals", texturePath);
        this.previewId = Identifier.of("auravisuals", this.previewPath);
    }

    public String getId() {
        return this.id;
    }

    public String getSlug() {
        return this.slug;
    }

    public String getName() {
        return this.name;
    }

    public String getCategory() {
        return this.category;
    }

    public String getModelPath() {
        return this.modelPath;
    }

    public String getTexturePath() {
        return this.texturePath;
    }

    public String getPreviewPath() {
        return this.previewPath;
    }

    public String getAnimationPath() {
        return this.animationPath;
    }

    public float getOffsetX() {
        return this.offsetX;
    }

    public float getOffsetY() {
        return this.offsetY;
    }

    public float getOffsetZ() {
        return this.offsetZ;
    }

    public Identifier getTextureId() {
        return this.textureId;
    }

    public Identifier getPreviewId() {
        return this.previewId;
    }

    public void setOrientation(float[] rotation, float[] scale) {
        copyVector(rotation, this.orientationRotation);
        if (scale != null && scale.length >= 3) {
            for (int index = 0; index < 3; index++) {
                if (Float.isFinite(scale[index]) && scale[index] != 0.0F) {
                    this.orientationSign[index] = Math.signum(scale[index]);
                }
            }
        }
    }

    public float getRotationX() {
        return this.orientationRotation[0];
    }

    public float getRotationY() {
        return this.orientationRotation[1];
    }

    public float getRotationZ() {
        return this.orientationRotation[2];
    }

    public float getOrientationSignX() {
        return this.orientationSign[0];
    }

    public float getOrientationSignY() {
        return this.orientationSign[1];
    }

    public float getOrientationSignZ() {
        return this.orientationSign[2];
    }

    private static void copyVector(float[] source, float[] target) {
        if (source != null && source.length >= 3) {
            for (int index = 0; index < 3; index++) {
                float value = source[index];
                if (Float.isFinite(value)) {
                    target[index] = value;
                }
            }
        }
    }

    public BedrockModel getModel() {
        return this.model;
    }

    public void setModel(BedrockModel model) {
        this.model = model;
        if(model!=null)model.setTextureAnimation(textureFrames,textureFrameTicks);
    }

    public BedrockAnimation getAnimation() {
        return this.animation;
    }

    public void setAnimation(BedrockAnimation animation) {
        this.animation = animation;
    }
}
