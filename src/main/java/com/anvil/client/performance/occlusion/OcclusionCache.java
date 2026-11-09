package com.anvil.client.performance.occlusion;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface OcclusionCache {
   void a();

   void a(int var1, int var2, int var3);

   void b(int var1, int var2, int var3);

   int c(int var1, int var2, int var3);

   void b();

   void c();
}
