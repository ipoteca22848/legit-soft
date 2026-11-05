package com.example.auravisuals.mixin.world;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.CustomWorld;
import net.minecraft.client.world.ClientWorld.Properties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Properties.class)
public abstract class ClientWorldPropertiesMixin {
    @Shadow
    private long timeOfDay;

    @Inject(method = "setTimeOfDay", at = @At("HEAD"), cancellable = true, require = 0)
    public void setTimeOfDayHook(long timeOfDay, CallbackInfo ci) {
        CustomWorld customWorld = ModuleManager.INSTANCE.getModule(CustomWorld.class);
        if (CustomWorld.shouldOverrideTime(customWorld)) {
            this.timeOfDay = CustomWorld.time.getValue().longValue();
            ci.cancel();
        }
    }

    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true, require = 0)
    public void getTimeOfDayHook(CallbackInfoReturnable<Long> cir) {
        CustomWorld customWorld = ModuleManager.INSTANCE.getModule(CustomWorld.class);
        if (CustomWorld.shouldOverrideTime(customWorld)) {
            cir.setReturnValue(CustomWorld.time.getValue().longValue());
        }
    }
}
