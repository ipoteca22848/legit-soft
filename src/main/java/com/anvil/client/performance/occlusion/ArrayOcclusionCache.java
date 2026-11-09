package com.anvil.client.performance.occlusion;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ArrayOcclusionCache implements OcclusionCache {
   private final int a;
   private final byte[] b;
   private int c;
   private int d;
   private int e;
   private int f;
   private int g;

   public ArrayOcclusionCache(int var1) {
      this.a = var1 * 2;
      this.b = new byte[this.a * this.a * this.a / 4];
      this.f = this.b.length;
      this.g = -1;
   }

   @Override
   public void a() {
      if (this.g >= this.f) {
         Arrays.fill(this.b, this.f, this.g + 1, (byte)0);
         this.f = this.b.length;
         this.g = -1;
      }
   }

   @Override
   public void a(int var1, int var2, int var3) {
      this.c = var1 + var2 * this.a + var3 * this.a * this.a;
      this.d = this.c / 4;
      this.e = this.c % 4 * 2;
      this.b[this.d] = (byte)(this.b[this.d] | (byte)(1 << this.e));
      if (this.d < this.f) {
         this.f = this.d;
      }

      if (this.d > this.g) {
         this.g = this.d;
      }
   }

   @Override
   public void b(int var1, int var2, int var3) {
      this.c = var1 + var2 * this.a + var3 * this.a * this.a;
      this.d = this.c / 4;
      this.e = this.c % 4 * 2;
      this.b[this.d] = (byte)(this.b[this.d] | (byte)(1 << this.e + 1));
      if (this.d < this.f) {
         this.f = this.d;
      }

      if (this.d > this.g) {
         this.g = this.d;
      }
   }

   @Override
   public int c(int var1, int var2, int var3) {
      this.c = var1 + var2 * this.a + var3 * this.a * this.a;
      this.d = this.c / 4;
      this.e = this.c % 4 * 2;
      return this.b[this.d] >> this.e & 3;
   }

   @Override
   public void b() {
      this.b[this.d] = (byte)(this.b[this.d] | (byte)(1 << this.e));
      if (this.d < this.f) {
         this.f = this.d;
      }

      if (this.d > this.g) {
         this.g = this.d;
      }
   }

   @Override
   public void c() {
      this.b[this.d] = (byte)(this.b[this.d] | (byte)(1 << this.e + 1));
      if (this.d < this.f) {
         this.f = this.d;
      }

      if (this.d > this.g) {
         this.g = this.d;
      }
   }
}
