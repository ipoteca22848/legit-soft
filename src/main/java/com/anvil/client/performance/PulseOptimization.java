package com.anvil.client.performance;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.Category;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.anvil.client.performance.occlusion.MutVec3d;
import com.anvil.client.performance.occlusion.OcclusionCullingInstance;
import com.anvil.client.performance.occlusion.WorldDataProvider;

@Environment(EnvType.CLIENT)
public class PulseOptimization extends Module {
   private static PulseOptimization instance;
   private static net.minecraft.client.world.ClientWorld lastWorld;
   public static PulseOptimization instance(){return instance;}
   public PulseOptimization(){super("Optimization","Оптимизация Pulse: графика, FPS в фоне, куллинг, частицы, предметы, текст и чанки",Category.MISC);instance=this;addSettings(u,v,w,x,y,z,A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z,aa,ab,ac,new PulseSeparatorSetting("Атлас: после перезагрузки ресурсов"),ad,ae,af,ag,ah,ai);}
   public static void worldTick(net.minecraft.client.MinecraftClient client){if(instance!=null&&lastWorld!=client.world){lastWorld=client.world;instance.t();if(instance.ah.a()&&!k){client.targetedEntity=null;client.crosshairTarget=null;}}}
   private static final Logger a = LogManager.getLogger("Optimization");
   private static final boolean b = a("sodium");
   private static final boolean c = a("immediatelyfast");
   private static final boolean f = a("entityculling");
   private static final boolean g = a("cullleaves");
   private static final boolean h = a("moreculling");
   private static final boolean i = a("dynamic_fps");
   private static final boolean j = a("badoptimizations");
   private static final boolean k = a("memoryleakfix");
   private static final long l = 250L;
   private static final long m = 3000L;
   private static final double n = 64.0;
   private static final int o = 256;
   private static final int p = 96;
   private static final int q = 64;
   private static final double r = 4096.0;
   private static final long s = 3L;
   private static final int t = 4;
   private final PulseSeparatorSetting u = new PulseSeparatorSetting("Настройки Minecraft");
   private final PulseBooleanSetting v = new PulseBooleanSetting(
      "Настройки Minecraft", "Подкручивает графику Minecraft при включении и возвращает как было при выключении", true
   );
   private final PulseNumberSetting w = new PulseNumberSetting(
         "Прогрузка чанков", "Дистанция прорисовки (уменьшается только если текущая больше)", 8.0F, 4.0F, 16.0F, 1.0F
      )
      .a(this.v::a);
   private final PulseBooleanSetting x = new PulseBooleanSetting(
         "Быстрая графика", "Графика Fast + без сглаживания освещения (заметнее по картинке, но больше FPS)", false
      )
      .a(this.v::a);
   private final PulseSeparatorSetting y = new PulseSeparatorSetting("FPS");
   private final PulseBooleanSetting z = new PulseBooleanSetting("Динамический FPS", "Снижает FPS, когда окно игры не в фокусе (порт Dynamic FPS)", true);
   private final PulseNumberSetting A = new PulseNumberSetting("FPS в фоне", 10.0F, 5.0F, 60.0F, 5.0F).a(this.z::a);
   private final PulseSeparatorSetting B = new PulseSeparatorSetting("Скрытие за стенами");
   private final PulseBooleanSetting C = new PulseBooleanSetting("Скрытие сущностей", "Не рендерит сущностей, полностью скрытых за стенами (идея Entity Culling)", true);
   private final PulseBooleanSetting D = new PulseBooleanSetting("Скрывать игроков", "Применять скрытие за стенами и к игрокам", false).a(this.C::a);
   private final PulseBooleanSetting E = new PulseBooleanSetting("Скрытие блок-энтити", "Не рендерит сундуки, таблички и т.п. за стенами и дальше дистанции", true);
   private final PulseNumberSetting F = new PulseNumberSetting(
         "Дистанция блок-энтити", "Дальше этой дистанции блок-энтити не рендерятся (идея More Culling)", 64.0F, 16.0F, 128.0F, 8.0F
      )
      .a(this.E::a);
   private final PulseSeparatorSetting G = new PulseSeparatorSetting("Частицы");
   private final PulseBooleanSetting H = new PulseBooleanSetting("Лимит частиц", "Ограничивает спавн частиц (дистанция, потолок, за стенами)", true);
   private final PulseNumberSetting I = new PulseNumberSetting("Дистанция частиц", 32.0F, 8.0F, 64.0F, 4.0F).a(this.H::a);
   private final PulseNumberSetting J = new PulseNumberSetting(
         "Потолок частиц", "Новые частицы не спавнятся, когда в мире их уже больше", 1500.0F, 250.0F, 4000.0F, 250.0F
      )
      .a(this.H::a);
   private final PulseBooleanSetting K = new PulseBooleanSetting("Не спавнить за стенами", "Частицы, скрытые за стенами, не создаются вообще", true).a(this.H::a);
   private final PulseSeparatorSetting L = new PulseSeparatorSetting("Предметы");
   private final PulseBooleanSetting M = new PulseBooleanSetting("Лимит предметов", "Защита от лаг-куч: рендерит не больше N предметов в одном блоке", true);
   private final PulseNumberSetting N = new PulseNumberSetting("Рендер на блок", "Сколько предметов рендерить в одном блоке", 4.0F, 1.0F, 16.0F, 1.0F).a(this.M::a);
   private final PulseBooleanSetting O = new PulseBooleanSetting("Не тикать лишние", "Лишние предметы в куче не обсчитываются клиентом (физика/коллизии)", true)
      .a(this.M::a);
   private final PulseSeparatorSetting P = new PulseSeparatorSetting("Рамки и таблички");
   private final PulseBooleanSetting Q = new PulseBooleanSetting("Дистанция рамок", "Рамки дальше своей дистанции не рендерятся (идея More Culling)", true);
   private final PulseNumberSetting R = new PulseNumberSetting("Рамки с предметом", "Дистанция рендера рамок с предметом или пустых", 64.0F, 16.0F, 128.0F, 8.0F)
      .a(this.Q::a);
   private final PulseNumberSetting S = new PulseNumberSetting(
         "Рамки с картой", "Карта в рамке — самый дорогой рендер (mapart-стены), дистанция короче", 32.0F, 8.0F, 128.0F, 8.0F
      )
      .a(this.Q::a);
   private final PulseBooleanSetting T = new PulseBooleanSetting(
      "Текст табличек вблизи", "Дальше дистанции рендерится доска без текста (текст всё равно не читается)", true
   );
   private final PulseNumberSetting U = new PulseNumberSetting("Дистанция текста", 16.0F, 4.0F, 64.0F, 4.0F).a(this.T::a);
   private final PulseSeparatorSetting V = new PulseSeparatorSetting("Микро-оптимизации");
   private final PulseBooleanSetting W = new PulseBooleanSetting("Упрощённая листва", "Отсекает грани листвы, прилегающие к другой листве (порт Cull Leaves)", true);
   private final PulseBooleanSetting X = new PulseBooleanSetting("Кэш цвета неба", "Пересчитывает цвет неба раз в 3 тика вместо каждого кадра", true);
   private final PulseBooleanSetting Y = new PulseBooleanSetting("Реже обновлять свет", "Обновляет lightmap-текстуру раз в 4 тика вместо каждого", true);
   private final PulseBooleanSetting Z = new PulseBooleanSetting("Пропуск пустых тостов", "Не вызывает отрисовку тостов, когда их нет", true);
   private final PulseSeparatorSetting aa = new PulseSeparatorSetting("Текст и шрифт");
   private final PulseBooleanSetting ab = new PulseBooleanSetting("Батчинг текста", "Не ищет вершинный буфер заново на каждую букву (порт ImmediatelyFast)", true);
   private final PulseBooleanSetting ac = new PulseBooleanSetting(
      "Крупный атлас шрифта",
      "Атлас глифов 1024 вместо 256 — меньше переключений текстур (порт ImmediatelyFast). Применяется к новым атласам (после перезагрузки ресурсов)",
      true
   );
   private final PulseSeparatorSetting ad = new PulseSeparatorSetting("Прогрузка чанков");
   private final PulseBooleanSetting ae = new PulseBooleanSetting(
      "Сглаживание прогрузки", "Убирает фриз при лавине чанков (телепорт, вход в мир): обработка размазывается по кадрам", true
   );
   private final PulseNumberSetting af = new PulseNumberSetting("Чанков за кадр", "Максимум чанк-обновлений, обрабатываемых за один кадр", 128.0F, 32.0F, 512.0F, 32.0F)
      .a(this.ae::a);
   private final PulseSeparatorSetting ag = new PulseSeparatorSetting("Память");
   private final PulseBooleanSetting ah = new PulseBooleanSetting("Фиксы утечек памяти", "Отпускает мир при выходе с сервера (MC-101260) и чистит кэши", true);
   private final PulseInputSetting ai = new PulseInputSetting("Снапшот графики", "").c(() -> false);
   private volatile boolean aj = false;
   private final Int2ObjectOpenHashMap<PulseOptimization.CullEntry> ak = new Int2ObjectOpenHashMap<>();
   private final Long2ObjectOpenHashMap<PulseOptimization.CullEntry> al = new Long2ObjectOpenHashMap<>();
   private final Long2ObjectOpenHashMap<PulseOptimization.CullEntry> am = new Long2ObjectOpenHashMap<>();
   private int an = 0;
   private OcclusionCullingInstance ao;
   private final MutVec3d ap = new MutVec3d(0.0, 0.0, 0.0);
   private final MutVec3d aq = new MutVec3d(0.0, 0.0, 0.0);
   private final MutVec3d ar = new MutVec3d(0.0, 0.0, 0.0);
   private long as = Long.MIN_VALUE;
   private int at;
   private long au;
   private Vec3d av;
   private int aw = 0;
   private int ax = 0;
   private int ay = 0;
   private final Long2IntOpenHashMap az = new Long2IntOpenHashMap();
   private final Long2IntOpenHashMap aA = new Long2IntOpenHashMap();
   private long aB = -1L;

   private static boolean a(String var0) {
      return FabricLoader.getInstance().isModLoaded(var0);
   }

   @Override
      public void onEnable() {
      
      this.aj = this.W.a() && !g && !h;
      if (this.aj) {
         this.y();
      }
   }

   @Override
      public void onDisable() {
      if (mc.options != null) {
         this.x();
      }
      this.t();
      boolean reload = this.aj;
      this.aj = false;
      if (reload) {
         this.y();
      }
      
   }

   public void onTick() {
      PulseBooleanSetting leaves = this.W;
      var entityCache = this.ak;
      var blockCache = this.al;
      var otherCache = this.am;
      boolean enabled;
      try {
         enabled = this.isEnabled() && leaves.a() && !g && !h;
      } catch (RuntimeException | Error error) {
         this.v();
         throw error;
      }
      if (enabled != this.aj) {
         this.aj = enabled;
         this.y();
      }
      this.v();
      this.ay = 0;
      if (mc.particleManager != null) {
         try {
            this.ax = Integer.parseInt(mc.particleManager.getDebugString());
         } catch (NumberFormatException error) {
            this.ax = 0;
         } catch (RuntimeException | Error error) {
            this.ax = 0;
            throw error;
         }
      }
      if (++this.an >= 100) {
         this.an = 0;
         long now = System.currentTimeMillis();
         entityCache.values().removeIf(entry -> PulseOptimization.c(now, entry));
         blockCache.values().removeIf(entry -> PulseOptimization.b(now, entry));
         otherCache.values().removeIf(entry -> PulseOptimization.a(now, entry));
      }
   }

      public void beginFrame() {
      this.az.clear();
   }

   private void t() {
      this.ak.clear();
      this.al.clear();
      this.am.clear();
      this.az.clear();
      this.aA.clear();
      this.av = null;
      this.as = Long.MIN_VALUE;
      if (this.ao != null) {
         this.ao.a();
      }
   }

   private void v() {
      if (this.isEnabled() && mc.options != null) {
         if (this.v.a()) {
            if (this.ai.c()) {
               this.w();
            }
         } else if (!this.ai.c()) {
            this.x();
         }
      }
   }

   private void w() {
      GameOptions var1 = mc.options;
      PulseOptimization.VideoSettings var2 = this.a(var1);
      var1.getMaxFps().setValue(260);
      var1.getEnableVsync().setValue(false);
      var1.getViewDistance().setValue(Math.min(var2.c, this.w.c()));
      var1.getSimulationDistance().setValue(Math.max(5,Math.min(var2.d, this.w.c())));
      var1.getCloudRenderMode().setValue(var2.e==CloudRenderMode.OFF?CloudRenderMode.OFF:CloudRenderMode.FAST);
      var1.getEntityShadows().setValue(false);
      var1.getParticles().setValue(var2.g==ParticlesMode.MINIMAL?ParticlesMode.MINIMAL:ParticlesMode.DECREASED);
      var1.getBiomeBlendRadius().setValue(Math.min(var2.h, 2));
      if (this.x.a()) {
         var1.getPreset().setValue(GraphicsMode.FAST);
         var1.getAo().setValue(false);
      }

      JsonObject var3 = new JsonObject();
      var3.add("original", a(var2));
      var3.add("applied", a(this.a(var1)));
      this.ai.b(var3.toString());
      var1.write();
   }

   private void x() {
      if (!this.ai.c()) {
         try {
            JsonObject var1 = JsonParser.parseString(this.ai.i()).getAsJsonObject();
            PulseOptimization.VideoSettings var2 = a(var1.getAsJsonObject("original"));
            PulseOptimization.VideoSettings var3 = a(var1.getAsJsonObject("applied"));
            GameOptions var4 = mc.options;
            this.a(var4.getMaxFps(), var3.a, var2.a);
            this.a(var4.getEnableVsync(), var3.b, var2.b);
            this.a(var4.getViewDistance(), var3.c, var2.c);
            this.a(var4.getSimulationDistance(), var3.d, var2.d);
            this.a(var4.getCloudRenderMode(), var3.e, var2.e);
            this.a(var4.getEntityShadows(), var3.f, var2.f);
            this.a(var4.getParticles(), var3.g, var2.g);
            this.a(var4.getBiomeBlendRadius(), var3.h, var2.h);
            this.a(var4.getPreset(), var3.i, var2.i);
            this.a(var4.getAo(), var3.j, var2.j);
            var4.write();
         } catch (Exception var5) {
            a.error("Не удалось восстановить настройки графики", (Throwable)var5);
         }

         this.ai.b("");
      }
   }

   private <T> void a(SimpleOption<T> var1, T var2, T var3) {
      if (var1.getValue().equals(var2)) {
         var1.setValue(var3);
      }
   }

   private static JsonObject a(PulseOptimization.VideoSettings var0) {
      JsonObject var1 = new JsonObject();
      var1.addProperty("maxFps", var0.a);
      var1.addProperty("vsync", var0.b);
      var1.addProperty("viewDistance", var0.c);
      var1.addProperty("simulationDistance", var0.d);
      var1.addProperty("clouds", var0.e.name());
      var1.addProperty("entityShadows", var0.f);
      var1.addProperty("particles", var0.g.name());
      var1.addProperty("biomeBlend", var0.h);
      var1.addProperty("graphics", var0.i.name());
      var1.addProperty("ao", var0.j);
      return var1;
   }

   private static PulseOptimization.VideoSettings a(JsonObject var0) {
      PulseOptimization.VideoSettings var1 = new PulseOptimization.VideoSettings();
      var1.a = var0.get("maxFps").getAsInt();
      var1.b = var0.get("vsync").getAsBoolean();
      var1.c = var0.get("viewDistance").getAsInt();
      var1.d = var0.get("simulationDistance").getAsInt();
      var1.e = CloudRenderMode.valueOf(var0.get("clouds").getAsString());
      var1.f = var0.get("entityShadows").getAsBoolean();
      var1.g = ParticlesMode.valueOf(var0.get("particles").getAsString());
      var1.h = var0.get("biomeBlend").getAsInt();
      var1.i = GraphicsMode.valueOf(var0.get("graphics").getAsString());
      var1.j = var0.get("ao").getAsBoolean();
      return var1;
   }

   private PulseOptimization.VideoSettings a(GameOptions var1) {
      PulseOptimization.VideoSettings var2 = new PulseOptimization.VideoSettings();
      var2.a = var1.getMaxFps().getValue();
      var2.b = var1.getEnableVsync().getValue();
      var2.c = var1.getViewDistance().getValue();
      var2.d = var1.getSimulationDistance().getValue();
      var2.e = var1.getCloudRenderMode().getValue();
      var2.f = var1.getEntityShadows().getValue();
      var2.g = var1.getParticles().getValue();
      var2.h = var1.getBiomeBlendRadius().getValue();
      var2.i = var1.getPreset().getValue();
      var2.j = var1.getAo().getValue();
      return var2;
   }

   private void y() {
      if (mc.worldRenderer != null && mc.world != null) {
         mc.worldRenderer.reload();
      }
   }

   public int b(int var1) {
      return !i && this.isEnabled() && this.z.a() && !mc.isWindowFocused() ? Math.min(var1, this.A.c()) : var1;
   }

   public boolean a(Entity var1) {
      if (!this.isEnabled()) {
         return false;
      } else if (var1 instanceof ItemEntity && this.M.a() && this.a(var1, this.az)) {
         return true;
      } else {
         if (var1 instanceof ItemFrameEntity var2 && this.Q.a() && mc.gameRenderer != null) {
            double var3 = var2.getMapId(var2.getHeldItemStack()) != null ? this.S.b() : this.R.b();
            if (var1.squaredDistanceTo(mc.gameRenderer.getCamera().getCameraPos()) > var3 * var3) {
               return true;
            }
         }

         if (f) {
            return false;
         } else if (!this.C.a()) {
            return false;
         } else if (mc.world == null || mc.player == null || mc.gameRenderer == null) {
            return false;
         } else if (var1 == mc.player || var1 == mc.getCameraEntity() || var1.isGlowing()) {
            return false;
         } else if (var1 instanceof PlayerEntity && !this.D.a()) {
            return false;
         } else if (mc.player.getVehicle() == var1) {
            return false;
         } else {
            Vec3d var8 = mc.gameRenderer.getCamera().getCameraPos();
            double var9 = var1.squaredDistanceTo(var8);
            if (!(var9 < 64.0) && !(var9 > 4096.0)) {
               long var5 = System.currentTimeMillis();
               PulseOptimization.CullEntry var7 = this.ak.get(var1.getId());
               if (var7 == null || var5 - var7.a > 250L) {
                  var7 = new PulseOptimization.CullEntry(var5, this.a(var1.getBoundingBox().expand(0.10000001789966936), var8));
                  this.ak.put(var1.getId(), var7);
               }

               return !var7.b;
            } else {
               return false;
            }
         }
      }
   }

   public boolean a(BlockPos var1, BlockEntityType<?> var2) {
      if (!this.isEnabled() || !this.E.a()) {
         return false;
      } else if (mc.world == null || mc.player == null || mc.gameRenderer == null) {
         return false;
      } else if (var1 == null) {
         return false;
      } else if (var2 == BlockEntityType.BEACON) {
         return false;
      } else {
         Vec3d var3 = mc.gameRenderer.getCamera().getCameraPos();
         double var4 = var3.squaredDistanceTo(var1.getX() + 0.5, var1.getY() + 0.5, var1.getZ() + 0.5);
         double var6 = this.F.b();
         if (var4 > var6 * var6) {
            return true;
         } else if (f) {
            return false;
         } else {
            return !(var4 < 64.0) && !(var4 > 4096.0) ? !this.a(var1, var3) : false;
         }
      }
   }

   public boolean n() {
      return this.H.a() && this.isEnabled();
   }

   public boolean a(double var1, double var3, double var5) {
      if (!this.isEnabled() || !this.H.a()) {
         return false;
      } else if (mc.gameRenderer != null && mc.gameRenderer.getCamera() != null) {
         Vec3d var7 = mc.gameRenderer.getCamera().getCameraPos();
         double var8 = this.I.b();
         double var10 = var7.squaredDistanceTo(var1, var3, var5);
         if (var10 > var8 * var8) {
            return true;
         } else if (this.ax + this.ay >= this.J.c()) {
            return true;
         } else if (this.K.a() && mc.world != null && var10 > 64.0 && !this.b(BlockPos.ofFloored(var1, var3, var5), var7)) {
            return true;
         } else {
            this.ay++;
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean a(ItemEntity var1) {
      if (this.isEnabled() && this.M.a() && this.O.a() && mc.world != null) {
         long var2 = mc.world.getTime();
         if (var2 != this.aB) {
            this.aB = var2;
            this.aA.clear();
         }

         return this.a(var1, this.aA);
      } else {
         return false;
      }
   }

   private boolean a(Entity var1, Long2IntOpenHashMap var2) {
      long var3 = BlockPos.asLong(MathHelper.floor(var1.getX()), MathHelper.floor(var1.getY()), MathHelper.floor(var1.getZ()));
      return var2.addTo(var3, 1) >= this.N.c();
   }

   public boolean o() {
      return this.aj;
   }

   public Integer a(Vec3d var1) {
      if (!j && this.isEnabled() && this.X.a() && mc.world != null && this.av != null) {
         if (mc.world.getTime() - this.au > 3L) {
            return null;
         } else {
            return var1.squaredDistanceTo(this.av) > 256.0 ? null : this.at;
         }
      } else {
         return null;
      }
   }

   public void a(Vec3d var1, int var2) {
      if (!j && this.isEnabled() && this.X.a() && mc.world != null) {
         this.at = var2;
         this.av = var1;
         this.au = mc.world.getTime();
      }
   }

   public boolean p() {
      if (!j && this.isEnabled() && this.Y.a()) {
         return ++this.aw % 4 != 0;
      } else {
         this.aw = 0;
         return false;
      }
   }

   public boolean q() {
      return !j && this.isEnabled() && this.Z.a();
   }

   public boolean a(BlockPos var1) {
      if (!this.isEnabled() || !this.T.a()) {
         return false;
      } else if (var1 == null) {
         return false;
      } else if (mc.gameRenderer != null && mc.gameRenderer.getCamera() != null) {
         double var2 = this.U.b();
         return mc.gameRenderer.getCamera().getCameraPos().squaredDistanceTo(var1.getX() + 0.5, var1.getY() + 0.5, var1.getZ() + 0.5) > var2 * var2;
      } else {
         return false;
      }
   }

   public boolean a(Particle var1) {
      return false;
   }

   public boolean r() {
      return !c && this.isEnabled() && this.ab.a();
   }

   public boolean s() {
      return !c && this.isEnabled() && this.ac.a();
   }

   public int c(int var1) {
      return this.isEnabled() && this.ae.a() ? Math.min(var1, this.af.c()) : var1;
   }

   private OcclusionCullingInstance b(Vec3d var1) {
      if (this.ao == null) {
         this.ao = new OcclusionCullingInstance(96, new WorldDataProvider());
      }

      long var2 = BlockPos.asLong(MathHelper.floor(var1.x), MathHelper.floor(var1.y), MathHelper.floor(var1.z));
      if (var2 != this.as) {
         this.as = var2;
         this.ao.a();
      }

      return this.ao;
   }

   private boolean a(Box var1, Vec3d var2) {
      OcclusionCullingInstance var3 = this.b(var2);
      this.ap.a(var1.minX, var1.minY, var1.minZ);
      this.aq.a(var1.maxX, var1.maxY, var1.maxZ);
      this.ar.a(var2.x, var2.y, var2.z);
      return var3.a(this.ap, this.aq, this.ar);
   }

   private boolean a(BlockPos var1, Vec3d var2) {
      long var3 = var1.asLong();
      long var5 = System.currentTimeMillis();
      PulseOptimization.CullEntry var7 = this.am.get(var3);
      if (var7 == null || var5 - var7.a > 250L) {
         var7 = new PulseOptimization.CullEntry(var5, this.a(new Box(var1), var2));
         this.am.put(var3, var7);
      }

      return var7.b;
   }

   private boolean b(BlockPos var1, Vec3d var2) {
      long var3 = var1.asLong();
      long var5 = System.currentTimeMillis();
      PulseOptimization.CullEntry var7 = this.al.get(var3);
      if (var7 == null || var5 - var7.a > 250L) {
         Box var8 = new Box(var1);
         var7 = new PulseOptimization.CullEntry(var5, this.b(var8, var2));
         this.al.put(var3, var7);
      }

      return var7.b;
   }

   private boolean b(Box var1, Vec3d var2) {
      if (!this.a(var2, var1.getCenter())) {
         return true;
      } else {
         double[] var3 = new double[]{var1.minX, var1.maxX};
         double[] var4 = new double[]{var1.minY, var1.maxY};
         double[] var5 = new double[]{var1.minZ, var1.maxZ};

         for (double var9 : var3) {
            for (double var14 : var4) {
               for (double var19 : var5) {
                  if (!this.a(var2, new Vec3d(var9, var14, var19))) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   private boolean a(Vec3d var1, Vec3d var2) {
      int var3 = MathHelper.floor(var1.x);
      int var4 = MathHelper.floor(var1.y);
      int var5 = MathHelper.floor(var1.z);
      int var6 = MathHelper.floor(var2.x);
      int var7 = MathHelper.floor(var2.y);
      int var8 = MathHelper.floor(var2.z);
      double var9 = var2.x - var1.x;
      double var11 = var2.y - var1.y;
      double var13 = var2.z - var1.z;
      int var15 = var9 > 0.0 ? 1 : (var9 < 0.0 ? -1 : 0);
      int var16 = var11 > 0.0 ? 1 : (var11 < 0.0 ? -1 : 0);
      int var17 = var13 > 0.0 ? 1 : (var13 < 0.0 ? -1 : 0);
      double var18 = var15 == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / var9);
      double var20 = var16 == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / var11);
      double var22 = var17 == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / var13);
      double var24 = var15 == 0 ? Double.POSITIVE_INFINITY : var18 * (var15 > 0 ? var3 + 1 - var1.x : var1.x - var3);
      double var26 = var16 == 0 ? Double.POSITIVE_INFINITY : var20 * (var16 > 0 ? var4 + 1 - var1.y : var1.y - var4);
      double var28 = var17 == 0 ? Double.POSITIVE_INFINITY : var22 * (var17 > 0 ? var5 + 1 - var1.z : var1.z - var5);
      Mutable var30 = new Mutable();

      for (int var31 = 0; var31 < 256; var31++) {
         if (var3 == var6 && var4 == var7 && var5 == var8) {
            return false;
         }

         if (var24 < var26) {
            if (var24 < var28) {
               var3 += var15;
               var24 += var18;
            } else {
               var5 += var17;
               var28 += var22;
            }
         } else if (var26 < var28) {
            var4 += var16;
            var26 += var20;
         } else {
            var5 += var17;
            var28 += var22;
         }

         if (var3 == var6 && var4 == var7 && var5 == var8) {
            return false;
         }

         if (mc.world.getBlockState(var30.set(var3, var4, var5)).isOpaqueFullCube()) {
            return true;
         }
      }

      return false;
   }

   public static boolean a(long var0, PulseOptimization.CullEntry var2) {
      return var0 - var2.a > 3000L;
   }

   public static boolean b(long var0, PulseOptimization.CullEntry var2) {
      return var0 - var2.a > 3000L;
   }

   public static boolean c(long var0, PulseOptimization.CullEntry var2) {
      return var0 - var2.a > 3000L;
   }


   @Environment(EnvType.CLIENT)
   static final class CullEntry {
      final long a;
      final boolean b;

      CullEntry(long var1, boolean var3) {
         this.a = var1;
         this.b = var3;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class VideoSettings {
      int a;
      boolean b;
      int c;
      int d;
      CloudRenderMode e;
      boolean f;
      ParticlesMode g;
      int h;
      GraphicsMode i;
      boolean j;
   }
}
