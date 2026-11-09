package com.anvil.client.mixin;

import com.anvil.client.atmosphere.AtmosphereModule;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.client.render.state.WeatherRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({WeatherRendering.class})
public class AtmosphereWeatherMixin {
    @Inject(
        method = {"buildPrecipitationPieces"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void mytheria$customSnow(World level, int ticks, float partialTick, Vec3d camera, WeatherRenderState state, CallbackInfo ci) {
        if (AtmosphereModule.val1(level)) {
            state.clear();
            ci.cancel();
        }
    }

    @Inject(
        method = {"addParticlesAndSound"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void mytheria$quietSnow(ClientWorld level, Camera camera, int ticks, ParticlesMode particles, int radius, CallbackInfo ci) {
        if (AtmosphereModule.val1(level)) {
            ci.cancel();
        }
    }
}
