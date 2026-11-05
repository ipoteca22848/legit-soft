package com.example.auravisuals.mixin.render;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.misc.HWHelper;
import com.example.auravisuals.module.render.NoFluids;
import com.example.auravisuals.module.render.NoRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @ModifyVariable(method = "setFloatingItem", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private ItemStack auravisuals$applyTalismanFloatingVisual(ItemStack stack) {
        return HWHelper.INSTANCE == null ? stack : HWHelper.INSTANCE.applyTalismanVisual(stack);
    }

    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void onRenderFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci) {
        if (ModuleManager.INSTANCE.getModule(NoRender.class).isEnabled() && NoRender.fire.getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void auravisuals$onRenderUnderwaterOverlay(
        MinecraftClient client, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci
    ) {
        if (NoFluids.isActive(NoFluids.water)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void auravisuals$onRenderInWallOverlay(Sprite sprite, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.isSubmergedIn(FluidTags.LAVA) && NoFluids.isActive(NoFluids.lava)) {
            ci.cancel();
        }
    }
}
