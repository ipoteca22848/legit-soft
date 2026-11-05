package com.example.auravisuals.mixin.render;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.HitColor;
import com.example.auravisuals.util.render.HitColorRenderLayers;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FeatureRenderer.class)
public abstract class FeatureRendererMixin {
    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true, require = 0)
    private static void auravisuals$tintArmor(
        Model model,
        Identifier texture,
        MatrixStack matrices,
        OrderedRenderCommandQueue queue,
        int light,
        LivingEntityRenderState state,
        int color,
        int queueOrder,
        CallbackInfo ci
    ) {
        if (ModuleManager.INSTANCE != null) {
            HitColor hitColor = ModuleManager.INSTANCE.getModule(HitColor.class);
            if (hitColor != null && hitColor.isEnabled() && HitColor.colorArmor.getValue()) {
                if (state != null && state.hurt) {
                    if (texture != null && texture.getPath().contains("armor")) {
                        int tint = hitColor.getColorValue();
                        queue.getBatchingQueue(queueOrder)
                            .submitModel(
                                model,
                                state,
                                matrices,
                                HitColor.isFullPaintMode() ? HitColorRenderLayers.full(texture) : HitColorRenderLayers.tint(texture),
                                light,
                                LivingEntityRenderer.getOverlay(state, 0.0F),
                                tint,
                                null,
                                state.outlineColor,
                                null
                            );
                        ci.cancel();
                    }
                }
            }
        }
    }
}
