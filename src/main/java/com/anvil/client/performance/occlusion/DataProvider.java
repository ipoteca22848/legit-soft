package com.anvil.client.performance.occlusion;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface DataProvider {
   boolean a(int var1, int var2);

   boolean a(int var1, int var2, int var3);

   default void a() {
   }
}
