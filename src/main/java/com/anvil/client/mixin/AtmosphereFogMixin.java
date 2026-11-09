package com.anvil.client.mixin;

import com.anvil.client.atmosphere.AtmosphereModule;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({FogRenderer.class})
public class AtmosphereFogMixin {
    @Unique
    private boolean mytheria$customFog;
    @Unique
    private boolean mytheria$winterFog;

    @Inject(
        method = {"getFogColor"},
        at = {@At("RETURN")}
    )
    private void fogColor(
        Camera camera, float partialTick, ClientWorld level, int renderDistance, float darkenWorldAmount, CallbackInfoReturnable<Vector4f> cir
    ) {
        this.mytheria$winterFog = AtmosphereModule.val2()
            && camera.getSubmersionType() == CameraSubmersionType.NONE
            && !(
                camera.getFocusedEntity() instanceof LivingEntity living
                    && (living.hasStatusEffect(StatusEffects.BLINDNESS) || living.hasStatusEffect(StatusEffects.DARKNESS))
            );
        Vector4f color = (Vector4f)cir.getReturnValue();
        if (this.mytheria$winterFog && color != null) {
            AtmosphereModule.val4(color);
        }


    }

    @ModifyVariable(
        method = {"applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"},
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private float mytheria$fogStart(float value) {
        float start = value;
        return this.mytheria$winterFog ? AtmosphereModule.val7(start) : start;
    }

    @ModifyVariable(
        method = {"applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"},
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 1
    )
    private float mytheria$fogEnd(float value) {
        float end = value;
        return this.mytheria$winterFog ? AtmosphereModule.val9(end) : end;
    }
}
