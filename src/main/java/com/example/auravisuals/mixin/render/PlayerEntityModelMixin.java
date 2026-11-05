package com.example.auravisuals.mixin.render;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public class PlayerEntityModelMixin extends BipedEntityModel<PlayerEntityRenderState> {
    @Shadow
    @Final
    public ModelPart leftSleeve;
    @Shadow
    @Final
    public ModelPart rightSleeve;
    @Shadow
    @Final
    public ModelPart leftPants;
    @Shadow
    @Final
    public ModelPart rightPants;
    @Shadow
    @Final
    public ModelPart jacket;

    public PlayerEntityModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("RETURN"), require = 0)
    private void auravisuals$sync3DSkinLayers(PlayerEntityRenderState state, CallbackInfo ci) {
        copyPartTransform(this.leftSleeve, this.leftArm);
        copyPartTransform(this.rightSleeve, this.rightArm);
        copyPartTransform(this.leftPants, this.leftLeg);
        copyPartTransform(this.rightPants, this.rightLeg);
        copyPartTransform(this.jacket, this.body);
        copyPartTransform(this.hat, this.head);
    }

    private static void copyPartTransform(ModelPart dest, ModelPart src) {
        if (dest != null && src != null) {
            dest.pitch = src.pitch;
            dest.yaw = src.yaw;
            dest.roll = src.roll;
            dest.originX = src.originX;
            dest.originY = src.originY;
            dest.originZ = src.originZ;
            dest.xScale = src.xScale;
            dest.yScale = src.yScale;
            dest.zScale = src.zScale;
        }
    }
}
