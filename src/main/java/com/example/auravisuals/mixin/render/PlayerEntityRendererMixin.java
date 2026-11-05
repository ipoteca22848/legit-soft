package com.example.auravisuals.mixin.render;

import com.example.auravisuals.cosmetics.CosmeticFeatureRenderer;
import com.example.auravisuals.mixin.accessor.LivingEntityRendererAccessor;
import com.example.auravisuals.module.render.CustomModels;
import com.example.auravisuals.util.graphics.WhiteTexture;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void auravisuals$addCosmeticFeature(Context context, boolean thinArms, CallbackInfo ci) {
        FeatureRendererContext renderer = (FeatureRendererContext)(Object)this;
        ((LivingEntityRendererAccessor)(Object)this).auravisuals$addFeature(new CosmeticFeatureRenderer(renderer));
    }

    @Redirect(
        method = "renderArm",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;)V"
        ),
        require = 0
    )
    private void auravisuals$renderPatrickFirstPersonArm(
        OrderedRenderCommandQueue queue, ModelPart part, MatrixStack matrices, RenderLayer layer, int light, int overlay, Sprite sprite
    ) {
        if (!CustomModels.shouldUsePatrickFirstPersonHands()) {
            queue.submitModelPart(part, matrices, layer, light, overlay, sprite);
        } else {
            queue.submitModelPart(part, matrices, RenderLayers.entityTranslucent(WhiteTexture.get()), 15728880, OverlayTexture.DEFAULT_UV, null, -29818, null);
        }
    }
}
