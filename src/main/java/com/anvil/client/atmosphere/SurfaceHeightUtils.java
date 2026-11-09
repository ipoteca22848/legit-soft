package com.anvil.client.atmosphere;

import java.util.HashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap.Type;

@Environment(EnvType.CLIENT)
public final class SurfaceHeightUtils {
    private static final HashMap<Long, Double> sub_1 = new HashMap<>();
    private static ClientWorld sub_2;
    private static long sub_3;

    private SurfaceHeightUtils() {
    }

    public static void clear(){sub_1.clear();sub_2=null;sub_3=0;}

    public static double val2(ClientWorld var0, int x, int y) { return sub_4(var0, x, y); }
    public static double sub_4(ClientWorld var0, int x, int y) {
        long var3 = System.nanoTime();
        if (sub_2 != var0
            || var3 - sub_3 > (500000000L)
            || sub_1.size() > 16000) {
            sub_1.clear();
            sub_2 = var0;
            sub_3 = var3;
        }

        return sub_1.computeIfAbsent(
            BlockPos.asLong(x, 0, y), color -> sub_5(var0, x, y)
        );
    }

    private static double sub_5(ClientWorld var0, int x, int y) {
        if (!var0.isChunkLoaded(x >> 4, y >> 4)) {
            return Double.NEGATIVE_INFINITY;
        } else {
            Mutable var3 = new Mutable();
            int var4 = var0.getTopY(Type.WORLD_SURFACE, x, y);

            for (int var5 = var4 - 1; var5 >= var0.getBottomY(); var5--) {
                var3.set(x, var5, y);
                BlockState var6 = var0.getBlockState(var3);
                if (var6.getRenderType() != BlockRenderType.INVISIBLE) {
                    VoxelShape var7 = var6.getCollisionShape(var0, var3);
                    if (!var7.isEmpty()) {
                        return var5 + var7.getMax(Axis.Y);
                    }

                    if (!var6.getFluidState().isEmpty()) {
                        return var5 + 1;
                    }
                }
            }

            return Double.NEGATIVE_INFINITY;
        }
    }

    public static double sub_24(ClientWorld var0, int x, int y) { return sub_4(var0, x, y); }
}
