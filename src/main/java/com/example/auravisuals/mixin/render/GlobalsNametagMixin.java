package com.example.auravisuals.mixin.render;

import com.example.auravisuals.module.misc.Globals;
import com.example.auravisuals.util.registry.GlobalsIconText;
import com.example.auravisuals.util.registry.GlobalsRegistry;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class GlobalsNametagMixin {
    @Inject(
        method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
        at = @At("HEAD")
    )
    private void auravisuals$markGlobalsNametag(
        EntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci
    ) {
        if (state instanceof PlayerEntityRenderState pState) {
            if (Globals.shouldShowIcon()) {
                if (state.displayName != null) {
                    if (pState.playerName != null && !pState.playerName.getString().isEmpty()) {
                        if (GlobalsRegistry.isGlobalsPlayer(pState.playerName.getString())) {
                            state.displayName = GlobalsIconText.prefix(state.displayName);
                        }
                    }
                }
            }
        }
    }
}
