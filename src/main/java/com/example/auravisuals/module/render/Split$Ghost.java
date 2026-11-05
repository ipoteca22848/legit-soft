package com.example.auravisuals.module.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPart.Quad;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

public class Split$Ghost {
    double x;
    double y;
    double z;
    float bodyYaw;
    float pitch;
    boolean swimming;
    boolean sneaking;
    long creationTime;
    List<Split$TransformedQuad> quads = new ArrayList<>();

    Split$Ghost(Split split, PlayerEntity player, PlayerEntityModel model) {
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        this.bodyYaw = player.bodyYaw;
        this.pitch = player.getPitch();
        this.swimming = player.isInSwimmingPose();
        this.sneaking = player.isSneaking();
        this.creationTime = System.currentTimeMillis();
        this.setupModelAngles(model, player);
        MatrixStack stack = new MatrixStack();
        this.collect(model.head, stack);
        this.collect(model.body, stack);
        this.collect(model.rightArm, stack);
        this.collect(model.leftArm, stack);
        this.collect(model.rightLeg, stack);
        this.collect(model.leftLeg, stack);
    }

    public void setupModelAngles(PlayerEntityModel model, PlayerEntity player) {
        model.head.resetTransform();
        model.body.resetTransform();
        model.rightArm.resetTransform();
        model.leftArm.resetTransform();
        model.rightLeg.resetTransform();
        model.leftLeg.resetTransform();
        float limbSwing = player.limbAnimator.getAnimationProgress();
        float limbSwingAmount = player.limbAnimator.getSpeed();
        model.rightArm.pitch = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F;
        model.leftArm.pitch = MathHelper.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        model.rightLeg.pitch = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        model.leftLeg.pitch = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        model.rightLeg.yaw = 0.0F;
        model.leftLeg.yaw = 0.0F;
        model.rightArm.yaw = 0.0F;
        model.leftArm.yaw = 0.0F;
        float headYaw = player.getHeadYaw() - player.bodyYaw;
        float headPitch = player.getPitch();
        model.head.yaw = (float)Math.toRadians(headYaw);
        model.head.pitch = (float)Math.toRadians(headPitch);
        if (this.sneaking) {
            model.body.pitch = 0.5F;
            model.rightArm.pitch += 0.4F;
            model.leftArm.pitch += 0.4F;
            model.head.originY = 4.2F;
            model.body.originY = 3.2F;
            model.leftArm.originY = 3.2F;
            model.rightArm.originY = 3.2F;
        } else {
            model.body.pitch = 0.0F;
            model.head.originY = 0.0F;
            model.body.originY = 0.0F;
            model.leftArm.originY = 2.0F;
            model.rightArm.originY = 2.0F;
        }
    }

    public void collect(ModelPart part, MatrixStack stack) {
        if (part.visible) {
            part.forEachCuboid(stack, (entry, path, index, cuboid) -> {
                for (Quad quad : cuboid.sides) {
                    this.quads.add(new Split$TransformedQuad(quad, entry.getPositionMatrix()));
                }
            });
        }
    }
}
