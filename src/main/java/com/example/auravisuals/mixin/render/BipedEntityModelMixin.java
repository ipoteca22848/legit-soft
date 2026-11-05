package com.example.auravisuals.mixin.render;

import com.example.auravisuals.emotions.EmotionWheelManager;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public class BipedEntityModelMixin {
    @Shadow
    public ModelPart head;
    @Shadow
    public ModelPart hat;
    @Shadow
    public ModelPart body;
    @Shadow
    public ModelPart rightArm;
    @Shadow
    public ModelPart leftArm;
    @Shadow
    public ModelPart rightLeg;
    @Shadow
    public ModelPart leftLeg;

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V", at = @At("RETURN"), require = 0)
    private void auravisuals$emotionsAfterSetAngles(BipedEntityRenderState state, CallbackInfo ci) {
        if (state instanceof PlayerEntityRenderState playerState) {
            EmotionWheelManager.INSTANCE
                .applyToModel(
                    playerState, this.head, this.hat, this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg
                );
        }
    }
}
