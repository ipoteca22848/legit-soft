package com.example.auravisuals.mixin.render;

import com.example.auravisuals.module.render.HitColor;
import com.example.auravisuals.util.render.HitColorRenderLayers;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.command.ModelCommandRenderer.CrumblingOverlayCommand;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EquipmentRenderer.class)
public class EquipmentRendererMixin {
    private static final String RENDER = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V";
    private static final String SUBMIT = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V";

    @Redirect(
        method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayers;armorCutoutNoCull(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"),
        require = 0
    )
    private RenderLayer auravisuals$hitColorArmorLayer(Identifier texture) {
        if (!HitColor.armorTintActive) {
            return RenderLayers.armorCutoutNoCull(texture);
        } else {
            return HitColor.isFullPaintMode() ? HitColorRenderLayers.full(texture) : HitColorRenderLayers.tint(texture);
        }
    }

    @Redirect(
        method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
        ),
        require = 0
    )
    private void auravisuals$renderHitColoredArmor(
        RenderCommandQueue queue,
        Model model,
        Object state,
        MatrixStack matrices,
        RenderLayer layer,
        int light,
        int overlay,
        int color,
        Sprite sprite,
        int outlineColor,
        CrumblingOverlayCommand crumblingOverlay
    ) {
        if (!HitColor.armorTintActive) {
            queue.submitModel(model, state, matrices, layer, light, overlay, color, sprite, outlineColor, crumblingOverlay);
        } else {
            queue.submitModel(model, state, matrices, layer, light, overlay, HitColor.armorTintColorValue, sprite, outlineColor, crumblingOverlay);
        }
    }
}
