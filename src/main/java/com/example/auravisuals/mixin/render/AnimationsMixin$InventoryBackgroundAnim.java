package com.example.auravisuals.mixin.render;

import com.example.auravisuals.module.render.Animations;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class AnimationsMixin$InventoryBackgroundAnim {
    @Unique
    public boolean auravisuals$inventoryBackgroundPushed = false;

    @Inject(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V", shift = Shift.BEFORE),
        require = 0
    )
    public void auravisuals$beforeInventoryBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        this.auravisuals$inventoryBackgroundPushed = false;
        if ((Object)this instanceof InventoryScreen) {
            Animations anims = Animations.INSTANCE;
            if (anims != null && anims.isEnabled() && anims.isModeEnabled("Открытие инвентаря")) {
                float progress = anims.getInventoryProgress();
                if (!(progress <= 0.02F) && !(progress >= 1.0F)) {
                    int width = ((HandledScreen)(Object)this).width;
                    int height = ((HandledScreen)(Object)this).height;
                    context.getMatrices().pushMatrix();
                    context.getMatrices().translate(width / 2.0F, height / 2.0F);
                    float scale = 0.88F + 0.12F * progress;
                    context.getMatrices().scale(scale, scale);
                    context.getMatrices().translate(-width / 2.0F, -height / 2.0F);
                    this.auravisuals$inventoryBackgroundPushed = true;
                }
            }
        }
    }

    @Inject(method = "renderBackground", at = @At("RETURN"), require = 0)
    public void auravisuals$afterInventoryBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.auravisuals$inventoryBackgroundPushed) {
            context.getMatrices().popMatrix();
            this.auravisuals$inventoryBackgroundPushed = false;
        }
    }
}
