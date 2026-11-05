package com.example.auravisuals.mixin.accessor;

import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HeldItemRenderer.class)
public interface HeldItemRendererAccessor {
    @Invoker("swingArm")
    void invokeSwingArm(float var1, MatrixStack var2, int var3, Arm var4);
}
