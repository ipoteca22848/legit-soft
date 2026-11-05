package com.example.auravisuals.mixin.render;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.CustomModels;
import com.example.auravisuals.module.render.HitColor;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public class ArmorFeatureRendererMixin {
    private static final String RENDER_DESC = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V";

    @Inject(
        method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void onRenderHead(
        MatrixStack matrices, OrderedRenderCommandQueue queue, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci
    ) {
        if (CustomModels.shouldReplaceState(state)) {
            ci.cancel();
        } else if (ModuleManager.INSTANCE != null) {
            HitColor hitColor = ModuleManager.INSTANCE.getModule(HitColor.class);
            if (hitColor != null && hitColor.isEnabled() && HitColor.colorArmor.getValue() && state.hurt) {
                HitColor.armorTintActive = true;
                HitColor.armorTintColorValue = hitColor.getColorValue();
            }
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V", at = @At("RETURN"), require = 0)
    private void onRenderReturn(
        MatrixStack matrices, OrderedRenderCommandQueue queue, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci
    ) {
        HitColor.armorTintActive = false;
    }
}
