package com.example.auravisuals.client;

import com.example.auravisuals.account.AccountManager;
import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.client.gui.hud.Notifications;
import com.example.auravisuals.client.hud.HUD;
import com.example.auravisuals.command.CommandManager;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.config.ConfigManager;
import com.example.auravisuals.cosmetics.CosmeticsManager;
import com.example.auravisuals.cosmetics.CosmeticsNetworkClient;
import com.example.auravisuals.live.LiveScriptManager;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.misc.AutoEvent;
import com.example.auravisuals.module.misc.AutoLogin;
import com.example.auravisuals.module.misc.Globals;
import com.example.auravisuals.module.render.ClickGui;
import com.example.auravisuals.module.render.FogBlur;
import com.example.auravisuals.module.render.GuiCustomized;
import com.example.auravisuals.module.render.Hud;
import com.example.auravisuals.module.render.Saturation;
import com.example.auravisuals.setting.BindSetting;
import com.example.auravisuals.theme.ThemeManager;
import com.example.auravisuals.util.MessengerService;
import com.example.auravisuals.util.integration.DiscordRPC;
import com.example.auravisuals.util.policy.HolyServerPolicy;
import com.example.auravisuals.util.render.StableWorldFrameCapture;
import com.example.auravisuals.util.render.gui.Gifs;
import com.example.auravisuals.util.render.gui.LiveUploader;
import com.example.auravisuals.util.server.AutoReconnectTracker;
import com.example.auravisuals.util.web.DpiBypassProxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStarted;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.Game;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents.AllowChat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.EndMain;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class AuraVisualsClient implements ClientModInitializer {
    static KeyBinding openGuiKey;
    static boolean clickGuiBindWasPressed;

    public void onInitializeClient() {
        com.bamber.client.BamberRuntime.init();
        FontManager.init();
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new AuraVisualsClient$1(this));
        CosmeticsManager.INSTANCE.load();
        ThemeManager.INSTANCE.init();
        openGuiKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.bamber.opengui", Type.KEYSYM, 344, Category.create(Identifier.of("auravisuals", "main")))
        );
        ClientSendMessageEvents.ALLOW_CHAT.register((AllowChat)message -> !com.bamber.client.BamberRuntime.isActive() || !CommandManager.INSTANCE.execute(message));
        this.registerRenderCallbacks();
        this.registerLogicCallbacks();
        ClientPlayConnectionEvents.JOIN.register((Join)(handler, sender, client) -> {
            if (!com.bamber.client.BamberRuntime.isActive()) return;
            AutoReconnectTracker.captureCurrentServer(client);
            HolyServerPolicy.enforceDisabledModulesForCurrentServer();
            CosmeticsNetworkClient.INSTANCE.sendHello();
            CosmeticsNetworkClient.INSTANCE.requestSync();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> AutoReconnectTracker.handleDisconnect());
        ClientLifecycleEvents.CLIENT_STARTED.register((ClientStarted)client -> {
            AccountManager.restoreSelectedSession();
            Gifs.init();
            GuiCustomized.updateModes();
            ConfigManager.INSTANCE.loadConfig("default");
            ThemeManager.INSTANCE.loadPersistedSelectedTheme();
            CosmeticsNetworkClient.INSTANCE.init();
            LiveScriptManager.INSTANCE.init();
            DiscordRPC.start();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register((ClientStopping)client -> {
            LiveScriptManager.INSTANCE.clearVisuals();
            if (com.bamber.client.BamberRuntime.isActive()) ConfigManager.INSTANCE.saveConfig("default");
            DiscordRPC.shutdown();
        });
    }

    void registerRenderCallbacks() {
        HUD hudRenderer = new HUD();
        HudRenderCallback.EVENT.register(hudRenderer);
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.CROSSHAIR, Identifier.of("auravisuals", "notifications_above_crosshair"), hudRenderer::renderNotifications
        );
        WorldRenderEvents.END_MAIN.register((EndMain)fabricContext -> {
            if (!com.bamber.client.BamberRuntime.isActive()) return;
            StableWorldFrameCapture.captureCleanWorld();
            FogBlur.renderIfEnabled();
            Saturation.renderIfEnabled();
            WorldRenderContext context = new WorldRenderContext(fabricContext);
            int depth = ModuleManager.matrixDepth(context.matrixStack());

            try {
                ModuleManager.INSTANCE.onWorldRender(context);
                CosmeticsManager.INSTANCE.renderWorld(context);
                LiveScriptManager.INSTANCE.render3D(context);
            } catch (Throwable var7) {
            } finally {
                ModuleManager.restoreMatrixStack(context.matrixStack(), depth);
                ModuleManager.clearMatrixStack(context.matrixStack());
            }
        });
    }

    void registerLogicCallbacks() {
        ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
            if (!com.bamber.client.BamberRuntime.isActive()) return;
            LiveScriptManager.INSTANCE.tick();
            MessengerService.tick();
            CosmeticsNetworkClient.INSTANCE.tick();
            LiveUploader.tick();
            Globals.tick();
            boolean guiToggleRequested = false;

            while (openGuiKey.wasPressed()) {
                guiToggleRequested = true;
            }

            if (client.player != null) {
                Integer clickGuiBind = ClickGui.openBind.getValue();
                boolean clickGuiPressed = clickGuiBind != null && clickGuiBind != -1 && BindSetting.isPressed(client.getWindow().getHandle(), clickGuiBind);
                guiToggleRequested |= clickGuiPressed && !clickGuiBindWasPressed;
                clickGuiBindWasPressed = clickGuiPressed;
            } else {
                clickGuiBindWasPressed = false;
            }

            if (guiToggleRequested) {
                boolean handledByScreen = ClickGui.consumeToggleHandledByScreen();
                if (!handledByScreen && !BindSetting.shouldBlockKeyInput()) {
                    ClickGui.toggleMainGui(client);
                }
            }

            if (client.player != null) {
                ModuleManager.INSTANCE.onUpdate();

                for (Module module : ModuleManager.INSTANCE.getModules()) {
                    if (module.getKey() != 0) {
                        boolean isPressed = BindSetting.isPressed(client.getWindow().getHandle(), module.getKey());
                        if (isPressed && !module.wasPressed && !BindSetting.shouldBlockKeyInput() && !HolyServerPolicy.shouldBlockKeybindToggle(module)) {
                            module.toggle();
                        }

                        module.wasPressed = isPressed;
                    }
                }
            } else {
                Hud hudModule = ModuleManager.INSTANCE.getModule(Hud.class);
                if (hudModule != null && hudModule.isEnabled()) {
                    hudModule.onTick();
                }
            }
        });
        ClientReceiveMessageEvents.GAME.register((Game)(message, overlay) -> {
            if (!com.bamber.client.BamberRuntime.isActive()) return;
            String text = Formatting.strip(message.getString());
            if (text == null) {
                text = message.getString();
            }

            AutoLogin autoLogin = ModuleManager.INSTANCE.getModule(AutoLogin.class);
            if (autoLogin != null && autoLogin.isEnabled()) {
                autoLogin.onGameMessage(text);
            }

            if (text.startsWith("[") && text.contains("]")) {
                int idx = text.indexOf(93);
                if (idx >= 0 && idx + 1 < text.length()) {
                    text = text.substring(idx + 1).trim();
                }
            }

            if (text.contains("Координаты:")) {
                String coords = text.substring(text.indexOf("Координаты:") + "Координаты:".length()).trim();
                if (!coords.isEmpty()) {
                    if (Hud.notifications.getValue()) {
                        Notifications.postMessage(Hud.text("Ивент активен: ", "Event active: ") + coords);
                    }

                    AutoEvent autoEvent = ModuleManager.INSTANCE.getModule(AutoEvent.class);
                    if (autoEvent != null && autoEvent.isEnabled() && autoEvent.autoPoint.getValue()) {
                        double[] parsed = parseCoords(coords);
                        if (parsed != null) {
                            CommandManager.INSTANCE.execute(".gps " + parsed[0] + " " + parsed[1]);
                        }
                    }
                }
            }

            if ((text.contains("До следующего ивента:") || text.contains("Начнётся через")) && Hud.notifications.getValue()) {
                String notification = text;
                if (Hud.isEnglish()) {
                    notification = text.replace("До следующего ивента:", "Until next event:").replace("Начнётся через", "Starts in");
                }

                Notifications.postMessage(notification);
            }
        });
    }

    static double[] parseCoords(String text) {
        Matcher matcher = Pattern.compile("-?\\d+(?:[.,]\\d+)?").matcher(text);
        double[] values = new double[3];
        int count = 0;

        while (matcher.find() && count < 3) {
            String value = matcher.group().replace(',', '.');

            try {
                values[count++] = Double.parseDouble(value);
            } catch (NumberFormatException var6) {
            }
        }

        if (count >= 3) {
            return new double[]{values[0], values[2]};
        } else {
            return count == 2 ? new double[]{values[0], values[1]} : null;
        }
    }
}
