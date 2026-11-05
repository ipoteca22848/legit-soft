package com.example.auravisuals.mixin.client;

import com.example.auravisuals.module.misc.FreeFPS;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftClient.class)
public abstract class FreeFpsMixin {
    @ModifyReturnValue(method = "getCurrentFps", at = @At("RETURN"))
    private int auravisuals$fakeReportedFps(int actualFps) {
        return FreeFPS.displayFps(actualFps);
    }
}
