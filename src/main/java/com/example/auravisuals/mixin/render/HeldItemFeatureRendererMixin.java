package com.example.auravisuals.mixin.render;

import com.example.auravisuals.module.render.CustomModels;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HeldItemFeatureRenderer.class)
public class HeldItemFeatureRendererMixin {
    @Redirect(
        method = "renderItem(Lnet/minecraft/client/render/entity/state/ArmedEntityRenderState;Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderState;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;III)V")
    )
    private void auravisuals$hideVanillaHeldItem(
        ItemRenderState itemState,
        MatrixStack matrices,
        OrderedRenderCommandQueue queue,
        int light,
        int overlay,
        int outlineColor,
        ArmedEntityRenderState entityState,
        ItemRenderState originalItemState,
        ItemStack stack,
        Arm arm,
        MatrixStack originalMatrices,
        OrderedRenderCommandQueue originalQueue,
        int originalLight
    ) {
        if (!CustomModels.shouldReplaceState(entityState)) {
            itemState.render(matrices, queue, light, overlay, outlineColor);
        }
    }
}
