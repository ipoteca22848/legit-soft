package com.example.auravisuals.module;

import com.example.auravisuals.AuraVisuals;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.misc.AnarchyFinder;
import com.example.auravisuals.module.misc.AucSearch;
import com.example.auravisuals.module.misc.AutoEvent;
import com.example.auravisuals.module.misc.AutoLogin;
import com.example.auravisuals.module.misc.AutoMessage;
import com.example.auravisuals.module.misc.AutoReconnect;
import com.example.auravisuals.module.misc.AutoResell;
import com.example.auravisuals.module.misc.ChatHelper;
import com.example.auravisuals.module.misc.ClanInvest;
import com.example.auravisuals.module.misc.CustomSword;
import com.example.auravisuals.module.misc.FTHelper;
import com.example.auravisuals.module.misc.FakePlayer;
import com.example.auravisuals.module.misc.FastLabel;
import com.example.auravisuals.module.misc.FreeFPS;
import com.example.auravisuals.module.misc.Freelook;
import com.example.auravisuals.module.misc.HWHelper;
import com.example.auravisuals.module.misc.Joiner;
import com.example.auravisuals.module.misc.Macros;
import com.example.auravisuals.module.misc.NoCapsLock;
import com.example.auravisuals.module.misc.PickUpLogger;
import com.example.auravisuals.module.misc.PvPSafe;
import com.example.auravisuals.module.misc.StreamerMode;
import com.example.auravisuals.module.misc.TotemTracker;
import com.example.auravisuals.module.misc.Xeaklt;
import com.example.auravisuals.module.player.AutoEat;
import com.example.auravisuals.module.player.AutoSprint;
import com.example.auravisuals.module.player.AutoSwap;
import com.example.auravisuals.module.player.FastExp;
import com.example.auravisuals.module.player.HealHelper;
import com.example.auravisuals.module.player.ItemScroller;
import com.example.auravisuals.module.player.LockSlot;
import com.example.auravisuals.module.player.QuickRespawn;
import com.example.auravisuals.module.player.ShiftTap;
import com.example.auravisuals.module.player.TapeMouse;
import com.example.auravisuals.module.render.Ambience;
import com.example.auravisuals.module.render.Animations;
import com.example.auravisuals.module.render.ArmorNotifier;
import com.example.auravisuals.module.render.AspectRatio;
import com.example.auravisuals.module.render.BlockOverlay;
import com.example.auravisuals.module.render.Cape;
import com.example.auravisuals.module.render.ChangeHand;
import com.example.auravisuals.module.render.ClickGui;
import com.example.auravisuals.module.render.Cooldowns;
import com.example.auravisuals.module.render.Crosshair;
import com.example.auravisuals.module.render.Cubes;
import com.example.auravisuals.module.render.CustomHand;
import com.example.auravisuals.module.render.CustomHitBox;
import com.example.auravisuals.module.render.CustomModels;
import com.example.auravisuals.module.render.CustomWorld;
import com.example.auravisuals.module.render.EffectRemind;
import com.example.auravisuals.module.render.Emotions;
import com.example.auravisuals.module.render.Firefly;
import com.example.auravisuals.module.render.FogBlur;
import com.example.auravisuals.module.render.FrameESP;
import com.example.auravisuals.module.render.FriendLabels;
import com.example.auravisuals.module.render.FriendScreen;
import com.example.auravisuals.module.render.Gamma;
import com.example.auravisuals.module.render.GuiCustomized;
import com.example.auravisuals.module.render.Hands;
import com.example.auravisuals.module.render.Hats;
import com.example.auravisuals.module.render.HitBubbles;
import com.example.auravisuals.module.render.HitColor;
import com.example.auravisuals.module.render.HitCounter;
import com.example.auravisuals.module.render.HitRange;
import com.example.auravisuals.module.render.Hud;
import com.example.auravisuals.module.render.ItemHelper;
import com.example.auravisuals.module.render.ItemPhysic;
import com.example.auravisuals.module.render.JumpCircle;
import com.example.auravisuals.module.render.Karaoke;
import com.example.auravisuals.module.render.KillEffect;
import com.example.auravisuals.module.render.MainMenu;
import com.example.auravisuals.module.render.Markers;
import com.example.auravisuals.module.render.MeteorFly;
import com.example.auravisuals.module.render.NameTag;
import com.example.auravisuals.module.render.NoFluids;
import com.example.auravisuals.module.render.NoRender;
import com.example.auravisuals.module.render.Optimization;
import com.example.auravisuals.module.render.Particles;
import com.example.auravisuals.module.render.PotionHighlight;
import com.example.auravisuals.module.render.Prediction;
import com.example.auravisuals.module.render.Saturation;
import com.example.auravisuals.module.render.Scoreboard;
import com.example.auravisuals.module.render.SeeInvisible;
import com.example.auravisuals.module.render.ShulkerPreview;
import com.example.auravisuals.module.render.SpeechBubbles;
import com.example.auravisuals.module.render.Split;
import com.example.auravisuals.module.render.SwingAnimation;
import com.example.auravisuals.module.render.TargetESP;
import com.example.auravisuals.module.render.TntTimer;
import com.example.auravisuals.module.render.Trails;
import com.example.auravisuals.module.render.ViewModel;
import com.example.auravisuals.module.render.Wave;
import com.example.auravisuals.module.render.WorldLines;
import com.example.auravisuals.module.render.Zoom;
import com.example.auravisuals.util.render.RenderUtil;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();
    private static final double FAR_WORLD_COORD = 5000000.0;
    private static final double EXTREME_WORLD_COORD = 1.8E7;
    private static final double TELEPORT_DISTANCE_SQ = 262144.0;
    private static final long TELEPORT_SETTLE_MS = 2500L;
    private final List<Module> modules = new ArrayList<>();
    private final Map<Class<?>, Module> moduleByClass = new IdentityHashMap<>();
    private final Map<Class<?>, Long> lastRenderErrorLog = new IdentityHashMap<>();
    private final Set<Module> farWorldRenderModules = Collections.newSetFromMap(new IdentityHashMap<>());
    private Vec3d lastWorldRenderCameraPos = null;
    private long lastFarWorldTeleportMs = 0L;

    private ModuleManager() {
        this.addModule(new com.anvil.client.atmosphere.AtmosphereModule());
        this.addModule(new com.example.auravisuals.module.misc.DiscordRCP());
        this.addModule(new com.anvil.client.Unhook());
        this.addModule(new QuickRespawn());
        this.addModule(new TapeMouse());
        this.addModule(new LockSlot());
        this.addModule(new ItemScroller());
        this.addModule(new ShiftTap());
        this.addModule(new AutoSprint());
        this.addModule(new MainMenu());
        this.addModule(new Hud());
        this.addModule(new NoRender());
        this.addModule(new SeeInvisible());
        this.addModule(new NoFluids());
        this.addModule(new Crosshair());
        this.addModule(new AspectRatio());
        this.addModule(new HitColor());
        this.addFarWorldRenderModule(new Ambience());
        this.addFarWorldRenderModule(new HitBubbles());
        this.addFarWorldRenderModule(new Cubes());
        this.addFarWorldRenderModule(new Particles());
        this.addFarWorldRenderModule(new JumpCircle());
        this.addFarWorldRenderModule(new Wave());
        this.addFarWorldRenderModule(new WorldLines());
        this.addModule(new HitRange());
        this.addModule(new HitCounter());
        this.addModule(new AutoSwap());
        this.addModule(new AutoEat());
        this.addModule(new CustomModels());
        this.addFarWorldRenderModule(new TargetESP());
        this.addModule(new FrameESP());
        this.addFarWorldRenderModule(new Trails());
        this.addModule(new CustomHitBox());
        this.addModule(new BlockOverlay());
        this.addModule(new TntTimer());
        this.addModule(new Cooldowns());
        this.addModule(new Scoreboard());
        this.addModule(new CustomWorld());
        this.addModule(new Emotions());
        this.addFarWorldRenderModule(new Split());
        this.addModule(new ItemPhysic());
        this.addFarWorldRenderModule(new Firefly());
        this.addFarWorldRenderModule(new KillEffect());
        this.addFarWorldRenderModule(new Prediction());
        this.addModule(new ItemHelper());
        this.addModule(new FTHelper());
        this.addModule(new AucSearch());
        this.addModule(new AutoReconnect());
        this.addModule(new PickUpLogger());
        this.addModule(new AutoEvent());
        this.addModule(new AutoResell());
        this.addModule(new ClanInvest());
        this.addModule(new AutoMessage());
        this.addModule(new AutoLogin());
        this.addModule(new NoCapsLock());
        this.addModule(new CustomSword());
        this.addModule(new HWHelper());
        this.addModule(new Joiner());
        this.addModule(new Macros());
        this.addModule(new PvPSafe());
        this.addModule(new ChangeHand());
        this.addModule(new CustomHand());
        this.addModule(new ViewModel());
        this.addModule(new ClickGui());
        this.addModule(new Markers());
        this.addFarWorldRenderModule(new Hats());
        this.addModule(new SwingAnimation());
        this.addModule(new StreamerMode());
        this.addModule(new FreeFPS());
        this.addModule(new Freelook());
        this.addModule(new FakePlayer());
        this.addModule(new NameTag());
        this.addModule(new Hands());
        this.addModule(new SpeechBubbles());
        this.addModule(new FastLabel());
        this.addModule(new Xeaklt());
        this.addModule(new TotemTracker());
        this.addModule(new Cape());
        this.addFarWorldRenderModule(new MeteorFly());
        this.addModule(new Zoom());
        this.addModule(new Gamma());
        this.addModule(new Animations());
        this.addModule(new FastExp());
        this.addModule(new ChatHelper());
        this.addModule(new FriendScreen());
        this.addFarWorldRenderModule(new FriendLabels());
        this.addFarWorldRenderModule(new Karaoke());
        this.addModule(new ArmorNotifier());
        this.addModule(new AnarchyFinder());
        this.addModule(new ShulkerPreview());
        this.addModule(new HealHelper());
        this.addModule(new EffectRemind());
        this.addModule(new GuiCustomized());
        this.addModule(new PotionHighlight());
        this.addModule(new Optimization());
        this.addModule(new Saturation());
        this.addModule(new FogBlur());
    }

    public <T extends Module> T getModule(Class<T> clazz) {
        return clazz.cast(this.moduleByClass.get(clazz));
    }

    private void addModule(Module module) {
        if (module != null && ModuleProfile.shouldRegister(module.getName())) {
            this.modules.add(module);
            this.moduleByClass.put(module.getClass(), module);
        }
    }

    private void addFarWorldRenderModule(Module module) {
        if (module != null && ModuleProfile.shouldRegister(module.getName())) {
            this.modules.add(module);
            this.moduleByClass.put(module.getClass(), module);
            this.farWorldRenderModules.add(module);
        }
    }

    public void onUpdate() {
        for (Module module : this.modules) {
            if (module.isEnabled()) {
                module.onTick();
            }
        }
    }

    public void onHudRender(DrawContext context) {
        for (Module module : this.modules) {
            if (module.isEnabled()) {
                try {
                    module.onHudRender(context);
                } catch (Throwable throwable) {
                    this.reportRenderFailure(module, throwable);
                }
            }
        }
    }

    public void onWorldRender(WorldRenderContext context) {
        this.updateFarWorldTeleport(context);

        for (Module module : this.modules) {
            if (module.isEnabled() && !this.shouldSkipFarWorldRender(module, context)) {
                int depth = matrixDepth(context.matrixStack());

                try {
                    module.onWorldRender(context);
                } catch (Throwable throwable) {
                    this.reportRenderFailure(module, throwable);
                } finally {
                    restoreMatrixStack(context.matrixStack(), depth);
                    this.restoreWorldRenderState();
                }
            }
        }
    }

    public void reportRenderFailure(Module module, Throwable throwable) {
        if (module != null && throwable != null) {
            long now = System.currentTimeMillis();
            Long last = this.lastRenderErrorLog.get(module.getClass());
            if (last == null || now - last >= 10000L) {
                this.lastRenderErrorLog.put(module.getClass(), now);
                AuraVisuals.LOGGER.error("Render failure in module {} ({})", new Object[]{module.getName(), module.getClass().getSimpleName(), throwable});
            }
        }
    }

    private boolean shouldSkipFarWorldRender(Module module, WorldRenderContext context) {
        try {
            Vec3d pos = context.camera().getCameraPos();
            double maxCoord = Math.max(Math.abs(pos.x), Math.abs(pos.z));
            if (maxCoord >= 1.8E7) {
                return this.farWorldRenderModules.contains(module)
                    || module instanceof CustomModels
                    || module instanceof CustomHitBox
                    || module instanceof BlockOverlay;
            } else {
                return maxCoord < 5000000.0
                    ? false
                    : this.farWorldRenderModules.contains(module) && System.currentTimeMillis() - this.lastFarWorldTeleportMs < 2500L;
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void updateFarWorldTeleport(WorldRenderContext context) {
        try {
            Vec3d pos = context.camera().getCameraPos();
            double maxCoord = Math.max(Math.abs(pos.x), Math.abs(pos.z));
            if (this.lastWorldRenderCameraPos != null && maxCoord >= 5000000.0 && pos.squaredDistanceTo(this.lastWorldRenderCameraPos) > 262144.0) {
                this.lastFarWorldTeleportMs = System.currentTimeMillis();
            }

            this.lastWorldRenderCameraPos = pos;
        } catch (Throwable var5) {
        }
    }

    private void restoreWorldRenderState() {
        try {
            RenderUtil.resetShaderColor();
        } catch (Throwable var2) {
        }
    }

    public static int matrixDepth(MatrixStack matrices) {
        try {
            for (Field field : matrices.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if (field.get(matrices) instanceof Collection collection) {
                    return collection.size();
                }
            }
        } catch (Throwable var7) {
        }

        return -1;
    }

    public static void restoreMatrixStack(MatrixStack matrices, int depth) {
        if (depth >= 0) {
            try {
                while (matrixDepth(matrices) > depth) {
                    matrices.pop();
                }
            } catch (Throwable var3) {
            }
        }
    }

    public static void clearMatrixStack(MatrixStack matrices) {
        try {
            while (!matrices.isEmpty()) {
                matrices.pop();
            }
        } catch (Throwable var2) {
        }
    }

    public List<Module> getModules() {
        return this.modules;
    }

    public List<Module> getModulesByCategory(Category category) {
        return this.modules.stream().filter(module -> module.getCategory() == category).collect(Collectors.toList());
    }
}
