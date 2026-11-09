package com.anvil.client.performance.occlusion;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos.Mutable;

@Environment(EnvType.CLIENT)
public final class WorldDataProvider implements DataProvider {
   private final MinecraftClient a = MinecraftClient.getInstance();
   private final Mutable b = new Mutable();
   private ClientWorld c;

   @Override
   public boolean a(int var1, int var2) {
      this.c = this.a.world;
      return this.c != null;
   }

   @Override
   public boolean a(int var1, int var2, int var3) {
      return this.c.getBlockState(this.b.set(var1, var2, var3)).isOpaqueFullCube();
   }

   @Override
   public void a() {
      this.c = null;
   }
}
