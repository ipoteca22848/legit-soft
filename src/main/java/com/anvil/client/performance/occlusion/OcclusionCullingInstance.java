package com.anvil.client.performance.occlusion;

import java.util.Arrays;
import java.util.BitSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class OcclusionCullingInstance {
   private static final int a = 1;
   private static final int b = 2;
   private static final int c = 4;
   private static final int d = 8;
   private static final int e = 16;
   private static final int f = 32;
   private final int g;
   private final double h;
   private final DataProvider i;
   private final OcclusionCache j;
   private final BitSet k = new BitSet();
   private final MutVec3d[] l = new MutVec3d[15];
   private final MutVec3d m = new MutVec3d(0.0, 0.0, 0.0);
   private final MutVec3d n = new MutVec3d(0.0, 0.0, 0.0);
   private final MutVec3d o = new MutVec3d(0.0, 0.0, 0.0);
   private final int[] p = new int[3];
   private final boolean[] q = new boolean[14];
   private boolean r = false;
   private final int[] s = new int[3];
   private boolean t = false;

   private static int a(double var0) {
      int var2 = (int)var0;
      return var0 < var2 ? var2 - 1 : var2;
   }

   public OcclusionCullingInstance(int var1, DataProvider var2) {
      this(var1, var2, new ArrayOcclusionCache(var1), 0.5);
   }

   public OcclusionCullingInstance(int var1, DataProvider var2, OcclusionCache var3, double var4) {
      this.g = var1;
      this.i = var2;
      this.j = var3;
      this.h = var4;

      for (int var6 = 0; var6 < this.l.length; var6++) {
         this.l[var6] = new MutVec3d(0.0, 0.0, 0.0);
      }
   }

   public void a() {
      this.j.a();
   }

   public boolean a(MutVec3d var1, MutVec3d var2, MutVec3d var3) {
      try {
         int var4 = a(var2.a + this.h);
         int var5 = a(var2.b + this.h);
         int var6 = a(var2.c + this.h);
         int var7 = a(var1.a - this.h);
         int var8 = a(var1.b - this.h);
         int var9 = a(var1.c - this.h);
         this.p[0] = a(var3.a);
         this.p[1] = a(var3.b);
         this.p[2] = a(var3.c);
         OcclusionCullingInstance.Relative var10 = OcclusionCullingInstance.Relative.a(var7, var4, this.p[0]);
         OcclusionCullingInstance.Relative var11 = OcclusionCullingInstance.Relative.a(var8, var5, this.p[1]);
         OcclusionCullingInstance.Relative var12 = OcclusionCullingInstance.Relative.a(var9, var6, this.p[2]);
         if (var10 == OcclusionCullingInstance.Relative.a && var11 == OcclusionCullingInstance.Relative.a && var12 == OcclusionCullingInstance.Relative.a) {
            return true;
         } else {
            this.k.clear();
            int var13 = 0;

            for (int var14 = var7; var14 <= var4; var14++) {
               for (int var15 = var8; var15 <= var5; var15++) {
                  for (int var16 = var9; var16 <= var6; var16++) {
                     int var17 = this.a(var14, var15, var16);
                     if (var17 == 1) {
                        return true;
                     }

                     if (var17 != 0) {
                        this.k.set(var13);
                     }

                     var13++;
                  }
               }
            }

            this.r = false;
            var13 = 0;

            for (int var25 = var7; var25 <= var4; var25++) {
               byte var26 = 0;
               byte var29 = 0;
               var26 = (byte)(var26 | (byte)(var25 == var7 ? 1 : 0));
               var26 = (byte)(var26 | (byte)(var25 == var4 ? 2 : 0));
               var29 = (byte)(var29 | (byte)(var25 == var7 && var10 == OcclusionCullingInstance.Relative.b ? 1 : 0));
               var29 = (byte)(var29 | (byte)(var25 == var4 && var10 == OcclusionCullingInstance.Relative.c ? 2 : 0));

               for (int var32 = var8; var32 <= var5; var32++) {
                  byte var18 = (byte)(var26 | (var32 == var8 ? 4 : 0));
                  var18 = (byte)(var18 | (byte)(var32 == var5 ? 8 : 0));
                  byte var19 = (byte)(var29 | (var32 == var8 && var11 == OcclusionCullingInstance.Relative.b ? 4 : 0));
                  var19 = (byte)(var19 | (byte)(var32 == var5 && var11 == OcclusionCullingInstance.Relative.c ? 8 : 0));

                  for (int var20 = var9; var20 <= var6; var20++) {
                     byte var21 = (byte)(var18 | (var20 == var9 ? 16 : 0));
                     var21 = (byte)(var21 | (byte)(var20 == var6 ? 32 : 0));
                     byte var22 = (byte)(var19 | (var20 == var9 && var12 == OcclusionCullingInstance.Relative.b ? 16 : 0));
                     var22 = (byte)(var22 | (byte)(var20 == var6 && var12 == OcclusionCullingInstance.Relative.c ? 32 : 0));
                     if (this.k.get(var13)) {
                        var13++;
                     } else {
                        if (var22 != 0) {
                           this.m.a(var25, var32, var20);
                           if (this.a(var3, this.m, var21, var22)) {
                              return true;
                           }
                        }

                        var13++;
                     }
                  }
               }
            }

            return false;
         }
      } catch (Throwable var23) {
         var23.printStackTrace();
         return true;
      }
   }

   private boolean a(MutVec3d var1, MutVec3d var2, byte var3, byte var4) {
      int var5 = 0;
      Arrays.fill(this.q, false);
      if ((var4 & 1) == 1) {
         this.q[0] = true;
         if ((var3 & -2) != 0) {
            this.q[1] = true;
            this.q[4] = true;
            this.q[5] = true;
         }

         this.q[8] = true;
      }

      if ((var4 & 4) == 4) {
         this.q[0] = true;
         if ((var3 & -5) != 0) {
            this.q[3] = true;
            this.q[4] = true;
            this.q[7] = true;
         }

         this.q[9] = true;
      }

      if ((var4 & 16) == 16) {
         this.q[0] = true;
         if ((var3 & -17) != 0) {
            this.q[1] = true;
            this.q[4] = true;
            this.q[5] = true;
         }

         this.q[10] = true;
      }

      if ((var4 & 2) == 2) {
         this.q[4] = true;
         if ((var3 & -3) != 0) {
            this.q[5] = true;
            this.q[6] = true;
            this.q[7] = true;
         }

         this.q[11] = true;
      }

      if ((var4 & 8) == 8) {
         this.q[1] = true;
         if ((var3 & -9) != 0) {
            this.q[2] = true;
            this.q[5] = true;
            this.q[6] = true;
         }

         this.q[12] = true;
      }

      if ((var4 & 32) == 32) {
         this.q[2] = true;
         if ((var3 & -33) != 0) {
            this.q[3] = true;
            this.q[6] = true;
            this.q[7] = true;
         }

         this.q[13] = true;
      }

      if (this.q[0]) {
         this.l[var5++].a(var2, 0.050000000102797884, 0.050000000102797884, 0.050000000102797884);
      }

      if (this.q[1]) {
         this.l[var5++].a(var2, 0.050000000102797884, 0.9500001550966999, 0.050000000102797884);
      }

      if (this.q[2]) {
         this.l[var5++].a(var2, 0.050000000102797884, 0.9500001550966999, 0.9500001550966999);
      }

      if (this.q[3]) {
         this.l[var5++].a(var2, 0.050000000102797884, 0.050000000102797884, 0.9500001550966999);
      }

      if (this.q[4]) {
         this.l[var5++].a(var2, 0.9500001550966999, 0.050000000102797884, 0.050000000102797884);
      }

      if (this.q[5]) {
         this.l[var5++].a(var2, 0.9500001550966999, 0.9500001550966999, 0.050000000102797884);
      }

      if (this.q[6]) {
         this.l[var5++].a(var2, 0.9500001550966999, 0.9500001550966999, 0.9500001550966999);
      }

      if (this.q[7]) {
         this.l[var5++].a(var2, 0.9500001550966999, 0.050000000102797884, 0.9500001550966999);
      }

      if (this.q[8]) {
         this.l[var5++].a(var2, 0.050000000102797884, 0.5, 0.5);
      }

      if (this.q[9]) {
         this.l[var5++].a(var2, 0.5, 0.050000000102797884, 0.5);
      }

      if (this.q[10]) {
         this.l[var5++].a(var2, 0.5, 0.5, 0.050000000102797884);
      }

      if (this.q[11]) {
         this.l[var5++].a(var2, 0.9500001550966999, 0.5, 0.5);
      }

      if (this.q[12]) {
         this.l[var5++].a(var2, 0.5, 0.9500001550966999, 0.5);
      }

      if (this.q[13]) {
         this.l[var5++].a(var2, 0.5, 0.5, 0.9500001550966999);
      }

      return this.a(var1, this.l, var5);
   }

   private boolean a(int[] var1, MutVec3d var2, MutVec3d var3) {
      this.o.a(1.0 / var3.a, 1.0 / var3.b, 1.0 / var3.c);
      double var4 = (var1[0] - var2.a) * this.o.a;
      double var6 = (var1[0] + 1 - var2.a) * this.o.a;
      double var8 = (var1[1] - var2.b) * this.o.b;
      double var10 = (var1[1] + 1 - var2.b) * this.o.b;
      double var12 = (var1[2] - var2.c) * this.o.c;
      double var14 = (var1[2] + 1 - var2.c) * this.o.c;
      double var16 = Math.max(Math.max(Math.min(var4, var6), Math.min(var8, var10)), Math.min(var12, var14));
      double var18 = Math.min(Math.min(Math.max(var4, var6), Math.max(var8, var10)), Math.max(var12, var14));
      return var18 > 0.0 ? false : !(var16 > var18);
   }

   private boolean a(MutVec3d var1, MutVec3d[] var2, int var3) {
      int var4 = this.p[0];
      int var5 = this.p[1];
      int var6 = this.p[2];

      for (int var7 = 0; var7 < var3; var7++) {
         MutVec3d var8 = var2[var7];
         double var9 = var1.a - var8.a();
         double var11 = var1.b - var8.b();
         double var13 = var1.c - var8.c();
         this.n.a(var9, var11, var13);
         if (!this.r || !this.a(this.s, var1, this.n.d())) {
            double var15 = Math.abs(var9);
            double var17 = Math.abs(var11);
            double var19 = Math.abs(var13);
            double var21 = 1.0 / var15;
            double var23 = 1.0 / var17;
            double var25 = 1.0 / var19;
            int var27 = 1;
            byte var28;
            double var29;
            if (var15 == 0.0) {
               var28 = 0;
               var29 = var21;
            } else if (var8.a > var1.a) {
               var28 = 1;
               var27 += a(var8.a) - var4;
               var29 = (float)((var4 + 1 - var1.a) * var21);
            } else {
               var28 = -1;
               var27 += var4 - a(var8.a);
               var29 = (float)((var1.a - var4) * var21);
            }

            byte var31;
            double var32;
            if (var17 == 0.0) {
               var31 = 0;
               var32 = var23;
            } else if (var8.b > var1.b) {
               var31 = 1;
               var27 += a(var8.b) - var5;
               var32 = (float)((var5 + 1 - var1.b) * var23);
            } else {
               var31 = -1;
               var27 += var5 - a(var8.b);
               var32 = (float)((var1.b - var5) * var23);
            }

            byte var34;
            double var35;
            if (var19 == 0.0) {
               var34 = 0;
               var35 = var25;
            } else if (var8.c > var1.c) {
               var34 = 1;
               var27 += a(var8.c) - var6;
               var35 = (float)((var6 + 1 - var1.c) * var25);
            } else {
               var34 = -1;
               var27 += var6 - a(var8.c);
               var35 = (float)((var1.c - var6) * var25);
            }

            boolean var37 = this.a(var1, var4, var5, var6, var21, var23, var25, var27, var28, var31, var34, var32, var29, var35);
            this.i.a();
            if (var37) {
               this.a(var2[0], true);
               return true;
            }

            this.r = true;
         }
      }

      this.a(var2[0], false);
      return false;
   }

   private boolean a(
      MutVec3d var1,
      int var2,
      int var3,
      int var4,
      double var5,
      double var7,
      double var9,
      int var11,
      int var12,
      int var13,
      int var14,
      double var15,
      double var17,
      double var19
   ) {
      this.t = true;

      while (var11 > 1) {
         int var21 = this.a(var2, var3, var4);
         if (var21 == 2 && !this.t) {
            this.s[0] = var2;
            this.s[1] = var3;
            this.s[2] = var4;
            return false;
         }

         if (var21 == 0) {
            int var22 = var2 >> 4;
            int var23 = var4 >> 4;
            if (!this.i.a(var22, var23)) {
               return false;
            }

            if (this.i.a(var2, var3, var4)) {
               if (!this.t) {
                  this.j.c();
                  this.s[0] = var2;
                  this.s[1] = var3;
                  this.s[2] = var4;
                  return false;
               }
            } else {
               this.t = false;
               this.j.b();
            }
         }

         if (var21 == 1) {
            this.t = false;
         }

         if (var15 < var17 && var15 < var19) {
            var3 += var13;
            var15 += var7;
         } else if (var17 < var15 && var17 < var19) {
            var2 += var12;
            var17 += var5;
         } else {
            var4 += var14;
            var19 += var9;
         }

         var11--;
      }

      return true;
   }

   private int a(int var1, int var2, int var3) {
      var1 -= this.p[0];
      var2 -= this.p[1];
      var3 -= this.p[2];
      return Math.abs(var1) <= this.g - 2 && Math.abs(var2) <= this.g - 2 && Math.abs(var3) <= this.g - 2
         ? this.j.c(var1 + this.g, var2 + this.g, var3 + this.g)
         : -1;
   }

   private void a(MutVec3d var1, boolean var2) {
      int var3 = a(var1.a) - this.p[0] + this.g;
      int var4 = a(var1.b) - this.p[1] + this.g;
      int var5 = a(var1.c) - this.p[2] + this.g;
      if (var2) {
         this.j.a(var3, var4, var5);
      } else {
         this.j.b(var3, var4, var5);
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum Relative {
      a,
      b,
      c;

      static OcclusionCullingInstance.Relative a(int var0, int var1, int var2) {
         if (var1 > var2 && var0 > var2) {
            return b;
         } else {
            return var0 < var2 && var1 < var2 ? c : a;
         }
      }

      private static OcclusionCullingInstance.Relative[] a() {
         return new OcclusionCullingInstance.Relative[]{a, b, c};
      }
   }
}
