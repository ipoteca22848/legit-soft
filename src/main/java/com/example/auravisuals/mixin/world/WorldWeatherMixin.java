package com.example.auravisuals.mixin.world;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.CustomWorld;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldWeatherMixin {
    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true, require = 0)
    private void auravisuals$customRainGradient(float delta, CallbackInfoReturnable<Float> cir) {
        if ((Object)this instanceof ClientWorld) {
            try {
                CustomWorld customWorld = ModuleManager.INSTANCE.getModule(CustomWorld.class);
                if (CustomWorld.shouldOverrideWeather(customWorld)) {
                    cir.setReturnValue(CustomWorld.getRainGradientOverride());
                }
            } catch (Throwable var4) {
            }
        }
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true, require = 0)
    private void auravisuals$customThunderGradient(float delta, CallbackInfoReturnable<Float> cir) {
        if ((Object)this instanceof ClientWorld) {
            try {
                CustomWorld customWorld = ModuleManager.INSTANCE.getModule(CustomWorld.class);
                if (CustomWorld.shouldOverrideWeather(customWorld)) {
                    cir.setReturnValue(CustomWorld.getThunderGradientOverride());
                }
            } catch (Throwable var4) {
            }
        }
    }
}
