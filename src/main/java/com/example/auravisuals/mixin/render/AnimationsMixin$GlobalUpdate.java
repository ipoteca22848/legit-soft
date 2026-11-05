package com.example.auravisuals.mixin.render;

import com.example.auravisuals.module.render.Animations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class AnimationsMixin$GlobalUpdate {
    @Shadow
    public PlayerListHud playerListHud;

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    public void auravisuals$updateAnimations(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Animations anims = Animations.INSTANCE;
        if (anims != null && anims.isEnabled()) {
            anims.updateAnimations();
            MinecraftClient mc = MinecraftClient.getInstance();
            if (anims.forceTabRender && !mc.options.playerListKey.isPressed() && mc.world != null) {
                Scoreboard scoreboard = mc.world.getScoreboard();
                ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
                this.playerListHud.render(context, context.getScaledWindowWidth(), scoreboard, objective);
            }
        }
    }
}
