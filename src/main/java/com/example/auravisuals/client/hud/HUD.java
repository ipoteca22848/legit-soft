package com.example.auravisuals.client.hud;

import com.example.auravisuals.client.gui.IrcChatMessengerScreen;
import com.example.auravisuals.live.LiveScriptManager;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.Hud;
import com.example.auravisuals.module.render.Scoreboard;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class HUD implements HudRenderCallback {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (!com.anvil.client.AnvilRuntime.isActive()) return;
        if (!this.mc.options.hudHidden) {
            Hud hudModule = ModuleManager.INSTANCE.getModule(Hud.class);
            if (hudModule == null || hudModule.isEnabled()) {
                com.example.auravisuals.client.gui.hud.Hud.INSTANCE.render(context);
            }

            boolean hideNonScoreboardModules = this.mc.getDebugHud().shouldShowDebugHud();

            for (Module module : ModuleManager.INSTANCE.getModules()) {
                if (module.isEnabled() && !(module instanceof Hud) && (!hideNonScoreboardModules || module instanceof Scoreboard)) {
                    try {
                        module.onHudRender(context);
                    } catch (Throwable var9) {
                        ModuleManager.INSTANCE.reportRenderFailure(module, var9);
                    }
                }
            }

            try {
                LiveScriptManager.INSTANCE.render2D(context, tickCounter.getTickProgress(false));
            } catch (Throwable var8) {
            }
        }
    }

    public void renderNotifications(DrawContext context, RenderTickCounter tickCounter) {
        if (!com.anvil.client.AnvilRuntime.isActive()) return;
        if (!this.mc.options.hudHidden) {
            Hud hudModule = ModuleManager.INSTANCE.getModule(Hud.class);
            if (hudModule == null || hudModule.isEnabled()) {
                com.example.auravisuals.client.gui.hud.Hud.INSTANCE.renderNotifications(context);
            }

            IrcChatMessengerScreen.renderToasts(context, tickCounter.getTickProgress(false));
        }
    }
}
