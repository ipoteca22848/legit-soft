package com.example.auravisuals.mixin.render;

import com.example.auravisuals.module.render.Animations;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
public class AnimationsMixin$CameraAnim {
    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F"), index = 0, require = 0)
    public float auravisuals$modifyCameraDistance(float originalDistance) {
        Animations anims = Animations.INSTANCE;
        return anims != null && anims.isEnabled() && anims.isModeEnabled("Смена перспективы") ? anims.getPerspectiveDistance() : originalDistance;
    }
}
