package com.example.auravisuals.mixin.render;

import com.example.auravisuals.module.render.Animations;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class AnimationsMixin$InventoryScreenAnim {
    @Unique
    public boolean auravisuals$inventoryAnimPushed = false;

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    public void auravisuals$onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Animations anims = Animations.INSTANCE;
        if (anims != null && anims.isEnabled()) {
            anims.updateAnimations();
        }

        this.auravisuals$inventoryAnimPushed = false;
        this.auravisuals$inventoryAnimPushed = this.auravisuals$pushInventoryScale(context);
    }

    @Unique
    public boolean auravisuals$pushInventoryScale(DrawContext context) {
        Animations anims = Animations.INSTANCE;
        if (anims != null && anims.isEnabled() && anims.isModeEnabled("Открытие инвентаря")) {
            float progress = anims.getInventoryProgress();
            if (!(progress <= 0.02F) && !(progress >= 1.0F)) {
                int width = ((InventoryScreen)(Object)this).width;
                int height = ((InventoryScreen)(Object)this).height;
                context.getMatrices().pushMatrix();
                context.getMatrices().translate(width / 2.0F, height / 2.0F);
                float scale = 0.88F + 0.12F * progress;
                context.getMatrices().scale(scale, scale);
                context.getMatrices().translate(-width / 2.0F, -height / 2.0F);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    public void auravisuals$onRenderReturn(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.auravisuals$inventoryAnimPushed) {
            context.getMatrices().popMatrix();
            this.auravisuals$inventoryAnimPushed = false;
        }
    }
}
