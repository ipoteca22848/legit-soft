package com.example.auravisuals.mixin.world;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.Gamma;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"), require = 0)
    private Object auravisuals$forceGamma(SimpleOption<?> option) {
        Object value = option.getValue();
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.options != null && option == mc.options.getGamma() && this.isGammaEnabled() ? 1000.0 : value;
    }

    private boolean isGammaEnabled() {
        if (ModuleManager.INSTANCE == null) {
            return false;
        }

        Gamma gamma = ModuleManager.INSTANCE.getModule(Gamma.class);
        return gamma != null && gamma.isEnabled();
    }
}
