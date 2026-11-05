package com.example.auravisuals.mixin.render;

import com.example.auravisuals.module.render.Animations;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public class AnimationsMixin$PlayerListHudAnim {
    @Inject(method = "render", at = @At("HEAD"), require = 0)
    public void auravisuals$onRenderHead(DrawContext context, int windowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        Animations anims = Animations.INSTANCE;
        float progress;
        if (anims != null && anims.isEnabled() && anims.isModeEnabled("Таб") && (progress = anims.getTabProgress()) < 1.0F) {
            context.getMatrices().pushMatrix();
            float yOffset = -500.0F * (1.0F - progress);
            context.getMatrices().translate(0.0F, yOffset);
        }
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    public void auravisuals$onRenderReturn(DrawContext context, int windowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        Animations anims = Animations.INSTANCE;
        if (anims != null && anims.isEnabled() && anims.isModeEnabled("Таб") && anims.getTabProgress() < 1.0F) {
            context.getMatrices().popMatrix();
        }
    }
}
