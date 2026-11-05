package com.example.auravisuals.mixin.world;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.CustomWorld;
import java.awt.Color;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeInterpolator;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnvironmentAttributeInterpolator.class)
public class ClientWorldMixin {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private <Value> void auravisuals$customSkyColor(EnvironmentAttribute<Value> attribute, float tickProgress, CallbackInfoReturnable<Value> cir) {
        if (attribute == EnvironmentAttributes.SKY_COLOR_VISUAL) {
            try {
                CustomWorld customWorld = ModuleManager.INSTANCE.getModule(CustomWorld.class);
                if (CustomWorld.shouldOverrideSky(customWorld)) {
                    Color color = customWorld.getActiveSkyColor();
                    Integer rgb = 0xFF000000 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
                    cir.setReturnValue((Value)(Object)rgb);
                }
            } catch (Throwable var7) {
            }
        }
    }
}
