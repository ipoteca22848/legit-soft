package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.util.optimization.OcclusionCuller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public final class Optimization extends Module {
    private static final int[] RENDER_DISTANCE_CAP = new int[]{0, 12, 8};
    private static final double[] VIEW_SCALE = new double[]{1.0, 0.8, 0.6};
    private static final double[] PARTICLE_DIST_SQ = new double[]{2304.0, 1024.0, 484.0};
    private static final double[] BLOCK_ENTITY_DIST_SQ = new double[]{2304.0, 1296.0, 576.0};
    private static Optimization instance;
    public final ModeSetting mode = new ModeSetting("Режим", "Средний", "Низкий", "Средний", "Ультра");
    public final BooleanSetting entityCulling = new BooleanSetting("Куллинг сущностей", true);
    public final BooleanSetting limitRenderDistance = new BooleanSetting("Ограничение прорисовки", true, () -> this.tier() >= 1);

    public Optimization() {
        super("Optimization", "Поднимает FPS: куллинг сущностей, частиц и тайлов, дистанция, облака.", Category.MISC);
        instance = this;
        this.addSettings(this.mode, this.entityCulling, this.limitRenderDistance);
    }

    public static Optimization getInstance() {
        return ModuleManager.INSTANCE.getModule(Optimization.class);
    }

    private static boolean active() {
        return instance != null && instance.isEnabled();
    }

    public static int capEffectiveRenderDistance(int n) {
        if (active() && instance.limitRenderDistance.getValue()) {
            int n2 = RENDER_DISTANCE_CAP[instance.tier()];
            return n2 > 0 ? Math.min(n, n2) : n;
        } else {
            return n;
        }
    }

    public static boolean allowBlockEntity(double d) {
        return !active() || d <= BLOCK_ENTITY_DIST_SQ[instance.tier()];
    }

    public static boolean allowParticle(double d, double d2, double d3) {
        if (!active()) {
            return true;
        }

        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.gameRenderer == null) {
            return true;
        }

        Camera camera = minecraftClient.gameRenderer.getCamera();
        if (camera == null) {
            return true;
        }

        Vec3d vec3d = camera.getCameraPos();
        double d4 = d - vec3d.x;
        double d5 = d2 - vec3d.y;
        double d6 = d3 - vec3d.z;
        return d4 * d4 + d5 * d5 + d6 * d6 <= PARTICLE_DIST_SQ[instance.tier()];
    }

    public static double scaleEntityViewScale(double d) {
        return active() ? d * VIEW_SCALE[instance.tier()] : d;
    }

    public static boolean hideEntityShadows() {
        return active() && instance.tier() >= 1;
    }

    public static boolean shouldRenderEntity(boolean bl, Entity entity, double d, double d2, double d3) {
        return bl && active() && instance.entityCulling.getValue() ? OcclusionCuller.isVisible(entity, d, d2, d3, instance.tier()) : bl;
    }

    public static boolean shaderTransparency(boolean bl) {
        return active() && instance.tier() >= 1 ? false : bl;
    }

    public static boolean cullClouds() {
        return active() && instance.tier() == 2;
    }

    public static int customParticleLimit() {
        if (!active()) {
            return 240;
        }

        return switch (instance.tier()) {
            case 1 -> 180;
            case 2 -> 120;
            default -> 240;
        };
    }

    public static double customEffectDistanceSq() {
        if (!active()) {
            return 4096.0;
        }

        return switch (instance.tier()) {
            case 1 -> 2304.0;
            case 2 -> 1296.0;
            default -> 4096.0;
        };
    }

    public static int customGlowStride() {
        return active() && instance.tier() == 2 ? 2 : 1;
    }

    public static int trailPointLimit(int fullQualityLimit) {
        if (!active()) {
            return fullQualityLimit;
        }

        return switch (instance.tier()) {
            case 1 -> Math.min(fullQualityLimit, 96);
            case 2 -> Math.min(fullQualityLimit, 72);
            default -> fullQualityLimit;
        };
    }

    public static int densifiedTrailPointLimit(int fullQualityLimit) {
        if (!active()) {
            return fullQualityLimit;
        }

        return switch (instance.tier()) {
            case 1 -> Math.min(fullQualityLimit, 224);
            case 2 -> Math.min(fullQualityLimit, 160);
            default -> fullQualityLimit;
        };
    }

    public static int transientEffectLimit(int fullQualityLimit) {
        if (!active()) {
            return fullQualityLimit;
        }

        return switch (instance.tier()) {
            case 1 -> Math.max(16, fullQualityLimit * 3 / 4);
            case 2 -> Math.max(16, fullQualityLimit / 2);
            default -> fullQualityLimit;
        };
    }

    public static long glassCaptureCoalesceNanos() {
        if (!active()) {
            return 2000000L;
        }

        return switch (instance.tier()) {
            case 1 -> 4000000L;
            case 2 -> 8000000L;
            default -> 2000000L;
        };
    }

    public int tier() {
        return this.mode.is("Ультра") ? 2 : (this.mode.is("Средний") ? 1 : 0);
    }
}
