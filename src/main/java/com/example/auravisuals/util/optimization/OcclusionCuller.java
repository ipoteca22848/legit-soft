package com.example.auravisuals.util.optimization;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos.Mutable;

public final class OcclusionCuller {
    static final int[] BUDGET = new int[]{16, 24, 32};
    static final long[] HIDDEN_TTL = new long[]{150L, 120L, 90L};
    static final long[] VISIBLE_TTL = new long[]{450L, 350L, 250L};
    static final double MIN_DIST_SQ = 64.0;
    static final double MAX_ENTITY_SIZE = 6.0;
    static final int MAX_RAY_STEPS = 172;
    static final Map<Integer, OcclusionCuller$Entry> CACHE = new HashMap<>();
    static final Mutable POS = new Mutable();
    static ClientWorld lastLevel;
    static int checksThisFrame;
    static long spentNanos;
    static final long[] TIME_BUDGET_NANOS={250000,400000,600000};
    static long lastSweep;
    static long frameTimeMs;

    OcclusionCuller() {
    }

    public static boolean isVisible(Entity entity, double d, double d2, double d3, int n) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ClientWorld clientWorld = minecraftClient.world;
        if (clientWorld != null && entity != minecraftClient.player && !entity.isGlowing()) {
            double d62 = entity.getX() - d;
            double d52 = entity.getY() - d2;
            double d4 = entity.getZ() - d3;
            if (d62 * d62 + d52 * d52 + d4 * d4 < 64.0) {
                return true;
            }

            Box box = entity.getBoundingBox();
            if (!(box.getLengthX() > 6.0) && !(box.getLengthY() > 6.0) && !(box.getLengthZ() > 6.0)) {
                int n2 = MathHelper.clamp(n, 0, 2);
                long l = frameTimeMs != 0L ? frameTimeMs : System.currentTimeMillis();
                OcclusionCuller$Entry entry = CACHE.get(entity.getId());
                if (entry != null && l < entry.next) {
                    return entry.visible;
                }

                if (checksThisFrame < BUDGET[n2] && spentNanos < TIME_BUDGET_NANOS[n2]) {
                    checksThisFrame++;
                    long start=System.nanoTime();
                    boolean bl = raycastVisible(clientWorld, d, d2, d3, box.expand(0.1));
                    spentNanos+=System.nanoTime()-start;
                    if (entry == null) {
                        entry = new OcclusionCuller$Entry();
                        CACHE.put(entity.getId(), entry);
                    }

                    entry.visible = bl;
                    entry.next = l + (bl ? VISIBLE_TTL[n2] : HIDDEN_TTL[n2]) + (entity.getId() * 31 & 63);
                    return bl;
                } else {
                    return entry == null || entry.visible;
                }
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public static void beginFrame() {
        checksThisFrame = 0;
        spentNanos=0;
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.world != lastLevel) {
            CACHE.clear();
            lastLevel = minecraftClient.world;
        }

        long l;
        if ((l = frameTimeMs = System.currentTimeMillis()) - lastSweep > 5000L) {
            lastSweep = l;
            Iterator<OcclusionCuller$Entry> iterator = CACHE.values().iterator();

            while (iterator.hasNext()) {
                if (l - iterator.next().next > 5000L) {
                    iterator.remove();
                }
            }
        }
    }

    static boolean sightBlocked(ClientWorld clientWorld, double d, double d2, double d3, double d4, double d5, double d6) {
        double d7 = Double.MAX_VALUE;
        double d8 = Double.MAX_VALUE;
        double d10 = d4 - d;
        double d11 = d5 - d2;
        double d12 = d6 - d3;
        int n3 = MathHelper.floor(d);
        int n4 = MathHelper.floor(d2);
        int n5 = MathHelper.floor(d3);
        int n6 = MathHelper.floor(d4);
        int n7 = MathHelper.floor(d5);
        int n8 = MathHelper.floor(d6);
        int n2;
        if (d10 > 0.0) {
            n2 = 1;
        } else {
            n2 = d10 < 0.0 ? -1 : 0;
        }

        int n;
        if (d11 > 0.0) {
            n = 1;
        } else {
            n = d11 < 0.0 ? -1 : 0;
        }

        int n11 = d12 > 0.0 ? 1 : (d12 < 0.0 ? -1 : 0);
        double d13 = n2 == 0 ? Double.MAX_VALUE : Math.abs(1.0 / d10);
        double d14 = n == 0 ? Double.MAX_VALUE : Math.abs(1.0 / d11);
        double d9 = n11 == 0 ? Double.MAX_VALUE : Math.abs(1.0 / d12);
        if (n2 == 0) {
            double d16 = Double.MAX_VALUE;
        } else {
            d8 = (n2 > 0 ? Math.floor(d) + 1.0 - d : d - Math.floor(d)) * d13;
        }

        if (n == 0) {
            double d17 = Double.MAX_VALUE;
        } else {
            d7 = (n > 0 ? Math.floor(d2) + 1.0 - d2 : d2 - Math.floor(d2)) * d14;
        }

        double d18 = n11 == 0 ? Double.MAX_VALUE : (n11 > 0 ? Math.floor(d3) + 1.0 - d3 : d3 - Math.floor(d3)) * d9;

        for (int i = 0; i < 172; i++) {
            if (Math.min(d8, Math.min(d7, d18)) >= 1.0) {
                return false;
            }

            if (d8 <= d7 && d8 <= d18) {
                n3 += n2;
                d8 += d13;
            } else if (d7 <= d18) {
                n4 += n;
                d7 += d14;
            } else {
                n5 += n11;
                d18 += d9;
            }

            if (n3 == n6 && n4 == n7 && n5 == n8) {
                return false;
            }

            BlockState blockState = clientWorld.getBlockState(POS.set(n3, n4, n5));
            if (!blockState.isAir() && blockState.isOpaque()) {
                return true;
            }
        }

        return false;
    }

    static boolean raycastVisible(ClientWorld clientWorld, double d, double d2, double d3, Box box) {
        if (!sightBlocked(clientWorld, d, d2, d3, (box.minX + box.maxX) * 0.5, (box.minY + box.maxY) * 0.5, (box.minZ + box.maxZ) * 0.5)) {
            return true;
        }

        for (int i = 0; i < 8; i++) {
            double d5 = (i & 1) == 0 ? box.minX : box.maxX;
            double d6 = (i & 2) == 0 ? box.minY : box.maxY;
            double d4 = (i & 4) == 0 ? box.minZ : box.maxZ;
            if (!sightBlocked(clientWorld, d, d2, d3, d5, d6, d4)) {
                return true;
            }
        }

        return false;
    }
}
