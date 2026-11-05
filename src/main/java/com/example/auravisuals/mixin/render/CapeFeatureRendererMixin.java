package com.example.auravisuals.mixin.render;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.Cape;
import com.example.auravisuals.module.render.CustomModels;
import com.example.auravisuals.util.render.RenderStateExtension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo.TextureAssetInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeFeatureRenderer.class)
public class CapeFeatureRendererMixin {
    @Inject(
        method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void AuraVisuals$injectCape(
        MatrixStack matrixStack,
        OrderedRenderCommandQueue vertexConsumerProvider,
        int light,
        PlayerEntityRenderState state,
        float limbAngle,
        float limbDistance,
        CallbackInfo ci
    ) {
        if (CustomModels.shouldReplaceState(state)) {
            ci.cancel();
        } else {
            Cape cape = ModuleManager.INSTANCE.getModule(Cape.class);
            if (cape != null && cape.isEnabled()) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) {
                    RenderStateExtension ext;
                    boolean isLocalPlayer = state instanceof RenderStateExtension && (ext = (RenderStateExtension)state).AuraVisuals$isLocalPlayer();
                    if (!cape.onlyMe.getValue() || isLocalPlayer) {
                        state.capeVisible = true;
                        SkinTextures old = state.skinTextures;
                        if (old != null) {
                            state.skinTextures = new SkinTextures(
                                old.body(), new TextureAssetInfo(cape.getCapeTexture(), cape.getCapeTexture()), old.elytra(), old.model(), old.secure()
                            );
                        }
                    }
                }
            }
        }
    }
}
