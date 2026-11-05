package com.example.auravisuals.mixin.render;

import com.example.auravisuals.mixin.accessor.HeldItemRendererAccessor;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.ChangeHand;
import com.example.auravisuals.module.render.CustomModels;
import com.example.auravisuals.module.render.SwingAnimation;
import com.example.auravisuals.module.render.ViewModel;
import com.example.auravisuals.util.render.HandFxPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Unique
    private boolean auravisuals$patrickArmTransform;
    @Unique
    private boolean auravisuals$patrickHoldingArmTransform;

    @Inject(
        method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
            ordinal = 0,
            shift = Shift.AFTER
        ),
        require = 0
    )
    private void auravisuals$captureMainHandSplit(
        float tickProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, ClientPlayerEntity player, int light, CallbackInfo ci
    ) {
        if (HandFxPipeline.isCaptureArmed()) {
            MinecraftClient client = MinecraftClient.getInstance();
            client.gameRenderer.getEntityRenderDispatcher().render();
            client.getBufferBuilders().getEntityVertexConsumers().draw();
            HandFxPipeline.captureMainHandSplit();
        }
    }

    @ModifyVariable(method = "renderFirstPersonItem", at = @At("STORE"), ordinal = 0, require = 0)
    private Arm auravisuals$swapRenderedArm(Arm arm) {
        ChangeHand module = ModuleManager.INSTANCE.getModule(ChangeHand.class);
        return module != null && module.isVisuallySwapped() ? arm.getOpposite() : arm;
    }

    @Redirect(
        method = "renderFirstPersonItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V", ordinal = 2),
        require = 0
    )
    private void auravisuals$redirectSwingArm(HeldItemRenderer instance, float swingProgress, MatrixStack matrices, int armX, Arm arm) {
        if (ViewModel.isEditorSessionActive()) {
            ((HeldItemRendererAccessor)instance).invokeSwingArm(0.0F, matrices, armX, arm);
        } else {
            MinecraftClient mc = MinecraftClient.getInstance();
            boolean mainHand = mc.player != null && arm == mc.player.getMainArm();
            SwingAnimation module = ModuleManager.INSTANCE.getModule(SwingAnimation.class);
            if (mainHand && module != null && module.isEnabled()) {
                SwingAnimation.applyAnimation(matrices, Hand.MAIN_HAND, swingProgress);
            } else {
                ((HeldItemRendererAccessor)instance).invokeSwingArm(swingProgress, matrices, armX, arm);
            }
        }
    }

    @Inject(
        method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
        at = @At("HEAD"),
        require = 0
    )
    private void auravisuals$applyViewModel(
        LivingEntity entity, ItemStack stack, ItemDisplayContext renderMode, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci
    ) {
        ViewModel viewModel = ModuleManager.INSTANCE.getModule(ViewModel.class);
        if (viewModel != null && viewModel.isEnabled()) {
            if (renderMode == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                matrices.translate(ViewModel.renderedX(true), ViewModel.renderedY(true), ViewModel.renderedZ(true));
                float s = ViewModel.renderedScale(true);
                matrices.scale(s, s, s);
            } else if (renderMode == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
                matrices.translate(ViewModel.renderedX(false), ViewModel.renderedY(false), ViewModel.renderedZ(false));
                float s = ViewModel.renderedScale(false);
                matrices.scale(s, s, s);
            }
        }
    }

    @Inject(method = "renderArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Arm;)V", at = @At("HEAD"), require = 0)
    private void auravisuals$patrickArmHead(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Arm arm, CallbackInfo ci) {
        this.auravisuals$patrickArmTransform = false;
        if (CustomModels.shouldUsePatrickFirstPersonHands()) {
            float side = arm == Arm.RIGHT ? 1.0F : -1.0F;
            matrices.push();
            matrices.translate(side * 0.015F, -0.015F, -0.035F);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 2.5F));
            matrices.scale(1.13F, 1.13F, 1.13F);
            this.auravisuals$patrickArmTransform = true;
        }
    }

    @Inject(method = "renderArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Arm;)V", at = @At("RETURN"), require = 0)
    private void auravisuals$patrickArmReturn(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Arm arm, CallbackInfo ci) {
        if (this.auravisuals$patrickArmTransform) {
            matrices.pop();
            this.auravisuals$patrickArmTransform = false;
        }
    }

    @Inject(method = "renderArmHoldingItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IFFLnet/minecraft/util/Arm;)V", at = @At("HEAD"), require = 0)
    private void auravisuals$patrickHoldingArmHead(
        MatrixStack matrices, OrderedRenderCommandQueue queue, int light, float equipProgress, float swingProgress, Arm arm, CallbackInfo ci
    ) {
        this.auravisuals$patrickHoldingArmTransform = false;
        if (CustomModels.shouldUsePatrickFirstPersonHands()) {
            float side = arm == Arm.RIGHT ? 1.0F : -1.0F;
            matrices.push();
            matrices.translate(side * 0.02F, -0.02F, -0.045F);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 2.0F));
            matrices.scale(1.14F, 1.14F, 1.14F);
            this.auravisuals$patrickHoldingArmTransform = true;
        }
    }

    @Inject(method = "renderArmHoldingItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IFFLnet/minecraft/util/Arm;)V", at = @At("RETURN"), require = 0)
    private void auravisuals$patrickHoldingArmReturn(
        MatrixStack matrices, OrderedRenderCommandQueue queue, int light, float equipProgress, float swingProgress, Arm arm, CallbackInfo ci
    ) {
        if (this.auravisuals$patrickHoldingArmTransform) {
            matrices.pop();
            this.auravisuals$patrickHoldingArmTransform = false;
        }
    }
}
