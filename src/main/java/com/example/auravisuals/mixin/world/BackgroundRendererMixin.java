package com.example.auravisuals.mixin.world;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.CustomWorld;
import com.example.auravisuals.module.render.NoFluids;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import java.awt.Color;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.render.fog.FogRenderer.FogType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FogRenderer.class)
public class BackgroundRendererMixin {
    @Shadow
    private static boolean fogEnabled;
    @Unique
    private static boolean auravisuals$fogWasForced;
    @Unique
    private static boolean auravisuals$fogEnabledBeforeForce;

    @Inject(method = "getFogBuffer", at = @At("HEAD"))
    private void auravisuals$forceCustomFogBuffer(FogType fogType, CallbackInfoReturnable<GpuBufferSlice> cir) {
        try {
            if (CustomWorld.shouldOverrideFog(getCustomWorld())) {
                if (!auravisuals$fogWasForced) {
                    auravisuals$fogEnabledBeforeForce = fogEnabled;
                    auravisuals$fogWasForced = true;
                }

                fogEnabled = true;
            } else if (auravisuals$fogWasForced) {
                fogEnabled = auravisuals$fogEnabledBeforeForce;
                auravisuals$fogWasForced = false;
            }
        } catch (Throwable var4) {
        }
    }

    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true)
    private void auravisuals$overrideFogColor(
        Camera camera, float tickProgress, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir
    ) {
        try {
            CustomWorld customWorld = getCustomWorld();
            if (shouldHideFluid(camera) && !CustomWorld.shouldOverrideFog(customWorld)) {
                int skyColor = camera.getEnvironmentAttributeInterpolator().get(EnvironmentAttributes.SKY_COLOR_VISUAL, tickProgress);
                cir.setReturnValue(
                    new Vector4f(
                        ColorHelper.getRedFloat(skyColor),
                        ColorHelper.getGreenFloat(skyColor),
                        ColorHelper.getBlueFloat(skyColor),
                        ColorHelper.getAlphaFloat(skyColor)
                    )
                );
                return;
            }

            if (CustomWorld.shouldOverrideFog(customWorld)) {
                Color color = customWorld.getActiveFogColor();
                cir.setReturnValue(new Vector4f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, 1.0F));
            }
        } catch (Throwable var9) {
        }
    }

    @ModifyArgs(
        method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V")
    )
    private void auravisuals$overrideFogDistances(
        Args args, Camera camera, int viewDistance, RenderTickCounter renderTickCounter, float skyDarkness, ClientWorld world
    ) {
        try {
            if (shouldHideFluid(camera)) {
                float renderStart = (Float)args.get(5);
                float renderEnd = (Float)args.get(6);
                args.set(3, renderStart);
                args.set(4, renderEnd);
                args.set(7, renderEnd);
                args.set(8, renderEnd);
            }

            float renderEnd;
            if (CustomWorld.shouldOverrideFog(getCustomWorld()) && Float.isFinite(renderEnd = (Float)args.get(6)) && renderEnd > 0.01F) {
                float ratio = Math.min(0.95F, Math.max(0.0F, CustomWorld.fogStart.getValue().floatValue()));
                args.set(5, Math.min(renderEnd - 0.01F, renderEnd * ratio));
            }
        } catch (Throwable var10) {
        }
    }

    private static CustomWorld getCustomWorld() {
        return ModuleManager.INSTANCE == null ? null : ModuleManager.INSTANCE.getModule(CustomWorld.class);
    }

    private static boolean shouldHideFluid(Camera camera) {
        if (camera == null) {
            return false;
        }

        CameraSubmersionType type = camera.getSubmersionType();
        return type == CameraSubmersionType.WATER && NoFluids.isActive(NoFluids.water) || type == CameraSubmersionType.LAVA && NoFluids.isActive(NoFluids.lava);
    }
}
