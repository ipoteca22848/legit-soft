package com.anvil.client.mixin.performance;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.anvil.client.performance.PulseOptimization;

@Environment(EnvType.CLIENT)
@Mixin({Block.class})
public class LeavesCullingMixin {
   @ModifyReturnValue(
      method = {"shouldDrawSide"},
      at = {@At("RETURN")}
   )
   private static boolean pulse$cullLeaves(boolean var0, BlockState var1, BlockState var2, Direction var3) {
      return var1.getBlock() instanceof LeavesBlock && var2.getBlock() instanceof LeavesBlock && PulseOptimization.instance()!=null && PulseOptimization.instance().o() ? false : var0;
   }
}
