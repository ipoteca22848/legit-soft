package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.theme.ThemeManager;
import java.awt.Color;
import java.util.WeakHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class FrameESP extends Module {
    public static final ModeSetting colorMode = new ModeSetting("Режим цвета", "Кастом", "Кастом", "Разноцветный", "Тема");
    public static final ColorSetting color = new ColorSetting("Цвет", new Color(220, 190, 255), () -> colorMode.is("Кастом"));
    public static final NumberSetting glowStrength = new NumberSetting("Сила свечения", 0.5, 0.2, 1.0, 0.05);
    public static final NumberSetting glowSize = new NumberSetting("Размер свечения", 5.0, 1.0, 10.0, 1.0);
    public static final NumberSetting range = new NumberSetting("Дистанция", 64.0, 16.0, 256.0, 8.0);
    private static final WeakHashMap<Entity, long[]> VISIBILITY_CACHE = new WeakHashMap<>();

    public FrameESP() {
        super("FrameESP", "Свечение вокруг игроков", Category.VISUALS);
        this.addSettings(colorMode, color);
    }

    private static double getDefaultRenderDistance() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.options != null && client.options.getViewDistance() != null
            ? Math.max(16.0, client.options.getViewDistance().getValue().intValue() * 16.0)
            : 192.0;
    }

    public static double getMaxDistance() {
        double maxDist = range.getValue();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null && client.options.getViewDistance() != null) {
            double renderDistance = client.options.getViewDistance().getValue().intValue() * 16.0;
            return Math.min(maxDist, renderDistance);
        } else {
            return maxDist;
        }
    }

    public static boolean manages(Entity entity) {
        if (!(entity instanceof PlayerEntity player)) {
            return false;
        } else {
            FrameESP module = ModuleManager.INSTANCE.getModule(FrameESP.class);
            if (module != null && module.isEnabled()) {
                MinecraftClient client = MinecraftClient.getInstance();
                return client.player != null && player != client.player;
            } else {
                return false;
            }
        }
    }

    public static boolean shouldOutline(Entity entity) {
        if (!(entity instanceof PlayerEntity player)) {
            return false;
        } else {
            FrameESP module = ModuleManager.INSTANCE.getModule(FrameESP.class);
            if (module != null && module.isEnabled()) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player == null || client.world == null || player == client.player) {
                    return false;
                } else if (player.isAlive() && !player.isSpectator()) {
                    double maxDistance = getMaxDistance();
                    return player.squaredDistanceTo(client.player) > maxDistance * maxDistance ? false : isVisibleThroughBlocks(client, player);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    private static boolean isVisibleThroughBlocks(MinecraftClient client, PlayerEntity target) {
        if (client.player != null && client.world != null) {
            long now = client.world.getTime();
            long[] cached = VISIBILITY_CACHE.get(target);
            if (cached != null && cached[0] == now) {
                return cached[1] != 0L;
            }

            boolean visible = computeVisibility(client, target);
            VISIBILITY_CACHE.put(target, new long[]{now, visible ? 1L : 0L});
            return visible;
        } else {
            return true;
        }
    }

    private static boolean computeVisibility(MinecraftClient client, PlayerEntity target) {
        Vec3d eye = client.player.getCameraPosVec(1.0F);
        double height = Math.max(0.1, target.getHeight());

        for (double f : new double[]{0.1, 0.5, 0.9}) {
            Vec3d point = new Vec3d(target.getX(), target.getY() + height * f, target.getZ());
            RaycastContext context = new RaycastContext(eye, point, ShapeType.COLLIDER, FluidHandling.NONE, client.player);
            BlockHitResult hit = client.world.raycast(context);
            if (hit == null || hit.getType() == Type.MISS || hit.getPos().squaredDistanceTo(point) < 0.75) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasFullArmor(PlayerEntity player) {
        return player == null
            ? false
            : !player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
                && !player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
                && !player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
                && !player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
    }

    public static int outlineColor(Entity entity) {
        int rgb;
        if (colorMode.is("Разноцветный")) {
            float hue = (float)(System.currentTimeMillis() % 4000L) / 4000.0F;
            rgb = Color.HSBtoRGB(hue, 0.75F, 1.0F);
        } else {
            rgb = colorMode.is("Тема") ? ThemeManager.INSTANCE.getVisualModulesColor() : color.getValue();
        }

        return withStrength(rgb);
    }

    private static int withStrength(int rgb) {
        int alpha = MathHelper.clamp(Math.round(255.0F * glowStrength.getFloatValue()), 0, 255);
        return alpha << 24 | rgb & 16777215;
    }

    public static int outlineColor() {
        return outlineColor(null);
    }
}
