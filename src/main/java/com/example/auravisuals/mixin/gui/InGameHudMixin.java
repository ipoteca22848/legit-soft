package com.example.auravisuals.mixin.gui;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.Animations;
import com.example.auravisuals.module.render.Cooldowns;
import com.example.auravisuals.module.render.Crosshair;
import com.example.auravisuals.module.render.Hud;
import com.example.auravisuals.module.render.ItemHelper;
import com.example.auravisuals.module.render.NoRender;
import com.example.auravisuals.module.render.Scoreboard;
import com.example.auravisuals.module.render.Zoom;
import com.example.auravisuals.util.render.MirageGlassPipeline;
import com.example.auravisuals.util.render.RenderUtil;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    private static final Identifier HOTBAR_SELECTION = Identifier.of("minecraft", "hud/hotbar_selection");

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void auravisuals$captureStableGlassFrame(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MirageGlassPipeline.captureHudFrame();
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true, require = 0)
    private void auravisuals$replaceVanillaHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (Hud.customHotbar.getValue() && client.player != null && !client.player.isSpectator()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        try {
            if (MinecraftClient.getInstance().currentScreen != null) {
                ci.cancel();
                return;
            }

            if (ModuleManager.INSTANCE != null
                && ModuleManager.INSTANCE.getModule(Crosshair.class) != null
                && ModuleManager.INSTANCE.getModule(Crosshair.class).isEnabled()) {
                ci.cancel();
            }
        } catch (Throwable var5) {
        }
    }

    @Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRenderSpyglassOverlay(DrawContext context, float scale, CallbackInfo ci) {
        try {
            if (ModuleManager.INSTANCE != null) {
                NoRender noRender = ModuleManager.INSTANCE.getModule(NoRender.class);
                if (noRender != null && noRender.isEnabled() && NoRender.pumpkin.getValue()) {
                    ci.cancel();
                    return;
                }

                Zoom zoom = ModuleManager.INSTANCE.getModule(Zoom.class);
                if (zoom != null && zoom.isZooming()) {
                    ci.cancel();
                }
            }
        } catch (Throwable var6) {
        }
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRenderOverlay(DrawContext context, Identifier texture, float opacity, CallbackInfo ci) {
        try {
            NoRender noRender;
            if (texture != null
                && texture.getPath().contains("pumpkinblur")
                && ModuleManager.INSTANCE != null
                && (noRender = ModuleManager.INSTANCE.getModule(NoRender.class)) != null
                && noRender.isEnabled()
                && NoRender.pumpkin.getValue()) {
                ci.cancel();
            }
        } catch (Throwable var6) {
        }
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRenderScoreboardSidebar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        try {
            if (ModuleManager.INSTANCE != null
                && (
                    ModuleManager.INSTANCE.getModule(NoRender.class).isEnabled() && NoRender.scoreboard.getValue()
                        || ModuleManager.INSTANCE.getModule(Scoreboard.class) != null && ModuleManager.INSTANCE.getModule(Scoreboard.class).isEnabled()
                )) {
                ci.cancel();
            }
        } catch (Throwable var5) {
        }
    }

    @Inject(method = "renderHotbar", at = @At("TAIL"), require = 0)
    private void onRenderHotbarTail(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        try {
            if (Hud.customHotbar.getValue()) {
                return;
            }

            Animations module = ModuleManager.INSTANCE.getModule(Animations.class);
            if (module == null || !module.isEnabled() || !module.isModeEnabled("Смена слотов")) {
                return;
            }

            if (MinecraftClient.getInstance().player == null) {
                return;
            }

            int center = context.getScaledWindowWidth() / 2;
            int y = context.getScaledWindowHeight() - 23;
            float slot = module.getSmoothedSlot();
            int x = Math.round(center - 91 + slot * 20.0F);
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION, x - 1, y - 1, 24, 23);
        } catch (Throwable var9) {
        }
    }

    @Redirect(
        method = "renderHotbar",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V"),
        require = 0
    )
    private void auravisuals$redirectHotbarSelection(DrawContext context, RenderPipeline renderLayers, Identifier texture, int x, int y, int width, int height) {
        try {
            Animations module = ModuleManager.INSTANCE.getModule(Animations.class);
            boolean smoothSlots = module != null && module.isEnabled() && module.isModeEnabled("Смена слотов");
            if (smoothSlots && HOTBAR_SELECTION.equals(texture)) {
                return;
            }
        } catch (Throwable var11) {
        }

        context.drawGuiTexture(renderLayers, texture, x, y, width, height);
    }

    @Inject(method = "renderHotbarItem", at = @At("HEAD"), require = 0)
    private void onRenderHotbarItem(
        DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci
    ) {
        try {
            int color;
            ItemHelper itemHelper;
            if (ModuleManager.INSTANCE != null
                && (itemHelper = ModuleManager.INSTANCE.getModule(ItemHelper.class)) != null
                && itemHelper.isEnabled()
                && (color = itemHelper.getColor(stack.getItem())) != -1) {
                float radius = itemHelper.isCircleShape() ? 6.0F : 0.0F;
                RenderUtil.drawRoundedRect(context, x, y, 16.0F, 16.0F, radius, color);
            }
        } catch (Throwable var12) {
        }
    }

    @Inject(method = "renderHotbarItem", at = @At("TAIL"), require = 0)
    private void auravisuals$renderHotbarCooldown(
        DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci
    ) {
        Cooldowns.renderHotbarCooldown(context, stack, x, y);
    }
}
