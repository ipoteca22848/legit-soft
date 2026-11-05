package com.example.auravisuals.mixin.render;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.ItemHelper;
import com.example.auravisuals.module.render.ItemPhysic;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
    @Unique
    private final Map<ItemEntityRenderState, Item> auravisuals$itemsByRenderState = new WeakHashMap<>();
    @Unique
    private boolean scalePushed = false;
    @Unique
    private boolean itemPhysicPushed = false;

    @Inject(method = "updateRenderState", at = @At("TAIL"), require = 0)
    private void onUpdateRenderState(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        this.auravisuals$itemsByRenderState.put(state, entity.getStack().getItem());
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;sin(F)F"), require = 0)
    private float onItemBobbing(float value) {
        ItemPhysic physic = ModuleManager.INSTANCE.getModule(ItemPhysic.class);
        return physic != null && physic.isEnabled() ? 0.0F : (float)Math.sin(value);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/RotationAxis;rotation(F)Lorg/joml/Quaternionf;"), require = 0)
    private Quaternionf onItemSpin(RotationAxis axis, float angle) {
        ItemPhysic physic = ModuleManager.INSTANCE.getModule(ItemPhysic.class);
        return physic != null && physic.isEnabled() ? new Quaternionf() : axis.rotation(angle);
    }

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void onRenderHead(ItemEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo ci) {
        this.itemPhysicPushed = false;
        ItemPhysic physic = ModuleManager.INSTANCE.getModule(ItemPhysic.class);
        if (physic != null && physic.isEnabled()) {
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            this.itemPhysicPushed = true;
        }

        this.scalePushed = false;
        Item renderItem = this.auravisuals$itemsByRenderState.get(state);
        float scale;
        ItemHelper helper;
        if (renderItem != null
            && (helper = ModuleManager.INSTANCE.getModule(ItemHelper.class)) != null
            && helper.isEnabled()
            && (scale = helper.getScale(renderItem)) != 1.0F) {
            matrices.push();
            matrices.scale(scale, scale, scale);
            this.scalePushed = true;
        }
    }

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void onRenderTail(ItemEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo ci) {
        if (this.scalePushed) {
            matrices.pop();
            this.scalePushed = false;
        }

        if (this.itemPhysicPushed) {
            matrices.pop();
            this.itemPhysicPushed = false;
        }
    }
}
