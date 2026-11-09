package com.example.auravisuals.cosmetics;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;

public class BedrockModel$Bone {
    public String name;
    public String parent;
    public float[] pivot = new float[]{0.0F, 0.0F, 0.0F};
    public float[] rotation = new float[]{0.0F, 0.0F, 0.0F};
    public final List<BedrockModel$Cube> cubes = new ArrayList<>();
    public final List<BedrockModel$Bone> children = new ArrayList<>();

    public void render(
        Entry parentEntry,
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
        Entry entry = parentEntry.copy();
        entry.translate(this.pivot[0] / 16.0F, this.pivot[1] / 16.0F, this.pivot[2] / 16.0F);
        Vector3f animRot = null;
        Vector3f animPos = null;
        if (animation != null && this.name != null) {
            animRot = new Vector3f();
            animPos = new Vector3f();
            animation.apply(this.name, animTime, animRot, animPos);
        }

        if (animPos != null && (animPos.x != 0.0F || animPos.y != 0.0F || animPos.z != 0.0F)) {
            entry.translate(animPos.x / 16.0F, animPos.y / 16.0F, animPos.z / 16.0F);
        }

        float rotZ = this.rotation[2] + (animRot != null ? animRot.z : 0.0F);
        float rotY = this.rotation[1] + (animRot != null ? animRot.y : 0.0F);
        float rotX = this.rotation[0] + (animRot != null ? animRot.x : 0.0F);
        if (rotZ != 0.0F) {
            entry.rotate(RotationAxis.POSITIVE_Z.rotationDegrees(rotZ));
        }

        if (rotY != 0.0F) {
            entry.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
        }

        if (rotX != 0.0F) {
            entry.rotate(RotationAxis.POSITIVE_X.rotationDegrees(rotX));
        }

        if(animation!=null&&animation.hasScale(this.name)){var scale=new Vector3f(1);animation.applyScale(this.name,animTime,scale);entry.scale(Math.abs(scale.x)<.00001f?.00001f:scale.x,Math.abs(scale.y)<.00001f?.00001f:scale.y,Math.abs(scale.z)<.00001f?.00001f:scale.z);}
        entry.translate(-this.pivot[0] / 16.0F, -this.pivot[1] / 16.0F, -this.pivot[2] / 16.0F);

        for (BedrockModel$Cube cube : this.cubes) {
            cube.render(entry, consumer, light, overlay, red, green, blue, alpha);
        }

        for (BedrockModel$Bone child : this.children) {
            child.render(entry, consumer, light, overlay, red, green, blue, alpha, animation, animTime);
        }
    }

    void collectGuiQuads(Entry parentEntry, List<BedrockModel$GuiQuad> output, BedrockAnimation animation, float animTime) {
        Entry entry = parentEntry.copy();
        entry.translate(this.pivot[0] / 16.0F, this.pivot[1] / 16.0F, this.pivot[2] / 16.0F);
        Vector3f animRot = null;
        Vector3f animPos = null;
        if (animation != null && this.name != null) {
            animRot = new Vector3f();
            animPos = new Vector3f();
            animation.apply(this.name, animTime, animRot, animPos);
        }

        if (animPos != null && (animPos.x != 0.0F || animPos.y != 0.0F || animPos.z != 0.0F)) {
            entry.translate(animPos.x / 16.0F, animPos.y / 16.0F, animPos.z / 16.0F);
        }

        float rotZ = this.rotation[2] + (animRot != null ? animRot.z : 0.0F);
        float rotY = this.rotation[1] + (animRot != null ? animRot.y : 0.0F);
        float rotX = this.rotation[0] + (animRot != null ? animRot.x : 0.0F);
        if (rotZ != 0.0F) {
            entry.rotate(RotationAxis.POSITIVE_Z.rotationDegrees(rotZ));
        }

        if (rotY != 0.0F) {
            entry.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
        }

        if (rotX != 0.0F) {
            entry.rotate(RotationAxis.POSITIVE_X.rotationDegrees(rotX));
        }

        if(animation!=null&&animation.hasScale(this.name)){var scale=new Vector3f(1);animation.applyScale(this.name,animTime,scale);entry.scale(Math.abs(scale.x)<.00001f?.00001f:scale.x,Math.abs(scale.y)<.00001f?.00001f:scale.y,Math.abs(scale.z)<.00001f?.00001f:scale.z);}
        entry.translate(-this.pivot[0] / 16.0F, -this.pivot[1] / 16.0F, -this.pivot[2] / 16.0F);

        for (BedrockModel$Cube cube : this.cubes) {
            cube.collectGuiQuads(entry, output);
        }

        for (BedrockModel$Bone child : this.children) {
            child.collectGuiQuads(entry, output, animation, animTime);
        }
    }

    void collectBounds(Entry parentEntry, float[] bounds) {
        Entry entry = parentEntry.copy();
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

        for (BedrockModel$Cube cube : this.cubes) {
            cube.collectBounds(entry, bounds);
        }

        for (BedrockModel$Bone child : this.children) {
            child.collectBounds(entry, bounds);
        }
    }
}
