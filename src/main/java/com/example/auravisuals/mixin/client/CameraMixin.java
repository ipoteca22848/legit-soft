package com.example.auravisuals.mixin.client;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.misc.Freelook;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private float pitch;
    @Shadow
    private float yaw;

    @Shadow
    protected abstract void setRotation(float var1, float var2);

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", shift = Shift.AFTER), require = 0)
    private void onUpdate(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        Freelook freelook = ModuleManager.INSTANCE.getModule(Freelook.class);
        if (freelook != null && freelook.isEnabled() && Freelook.isActive) {
            this.setRotation(freelook.getCameraYaw(), freelook.getCameraPitch());
        }
    }
}
