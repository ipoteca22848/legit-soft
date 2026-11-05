package com.example.auravisuals.mixin.world;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.CustomWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Precipitation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public class BiomeWeatherMixin {
    @Inject(method = "getPrecipitation", at = @At("HEAD"), cancellable = true, require = 0)
    private void auravisuals$customPrecipitation(BlockPos pos, int seaLevel, CallbackInfoReturnable<Precipitation> cir) {
        try {
            CustomWorld customWorld = ModuleManager.INSTANCE.getModule(CustomWorld.class);
            if (!CustomWorld.shouldOverrideWeather(customWorld)) {
                return;
            }

            if (CustomWorld.weatherMode.is("Ясно")) {
                cir.setReturnValue(Precipitation.NONE);
            } else if (CustomWorld.weatherMode.is("Снег")) {
                cir.setReturnValue(Precipitation.SNOW);
            } else {
                cir.setReturnValue(Precipitation.RAIN);
            }
        } catch (Throwable var5) {
        }
    }
}
