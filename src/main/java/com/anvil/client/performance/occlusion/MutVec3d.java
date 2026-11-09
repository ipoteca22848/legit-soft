package com.anvil.client.performance.occlusion;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MutVec3d {
   public double a;
   public double b;
   public double c;

   public MutVec3d(double var1, double var3, double var5) {
      this.a = var1;
      this.b = var3;
      this.c = var5;
   }

   public double a() {
      return this.a;
   }

   public double b() {
      return this.b;
   }

   public double c() {
      return this.c;
   }

   public void a(double var1, double var3, double var5) {
      this.a = var1;
      this.b = var3;
      this.c = var5;
   }

   public void a(MutVec3d var1, double var2, double var4, double var6) {
      this.a = var1.a + var2;
      this.b = var1.b + var4;
      this.c = var1.c + var6;
   }

   public MutVec3d a(MutVec3d var1) {
      this.a = this.a / var1.a;
      this.b = this.b / var1.b;
      this.c = this.c / var1.c;
      return this;
   }

   public MutVec3d d() {
      double var1 = Math.sqrt(this.a * this.a + this.b * this.b + this.c * this.c);
      this.a /= var1;
      this.b /= var1;
      this.c /= var1;
      return this;
   }
}
