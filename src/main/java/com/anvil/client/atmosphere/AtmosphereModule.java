package com.anvil.client.atmosphere;

import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.setting.*;
import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import java.util.List;
import java.util.Random;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class AtmosphereModule extends Module {
    private static AtmosphereModule instance;
    private final BooleanSetting setting1 = new BooleanSetting("Снегопад", true);
    private final NumberSetting setting2 = new NumberSetting(
        "Плотность снега", 0.65F, 0.1F, 1.0F, 0.05F, () -> this.setting1.getValue()
    );
    private final NumberSetting radius = new NumberSetting(
        "Радиус", 32.0F, 12.0F, 64.0F, 1.0F, () -> this.setting1.getValue()
    );
    private final NumberSetting setting4 = new NumberSetting(
        "Скорость падения", 0.8F, 0.2F, 2.0F, 0.1F, () -> this.setting1.getValue()
    );
    private final NumberSetting setting5 = new NumberSetting(
        "Ветер", 0.35F, 0.0F, 1.5F, 0.05F, () -> this.setting1.getValue()
    );
    private final BooleanSetting setting6 = new BooleanSetting("Зимние текстуры", true);
    private final BooleanSetting setting7 = new BooleanSetting("Зимний туман", true);
    private final NumberSetting setting8 = new NumberSetting(
        "Сила тумана", 0.5F, 0.0F, 1.0F, 0.05F, () -> this.setting7.getValue()
    );
    private final Random field2 = new Random();
    private final List<AtmosphereModule.WeatherState> val1 = new ArrayList<>();
    private ClientWorld field3;
    private Vec3d field4;
    private int count;

    public AtmosphereModule() {
        super("Атмосфера", "Зимние текстуры, объёмный снегопад и морозная дымка.", Category.VISUALS);
        instance=this;
        this.addSettings(
            new Setting[]{
                this.setting1,
                this.setting2,
                this.radius,
                this.setting4,
                this.setting5,
                this.setting6,
                this.setting7,
                this.setting8
            }
        );
    }

    public static AtmosphereModule sub_5() { return compute(); }
    public static boolean val1() { AtmosphereModule m = compute(); return m != null && m.usesWinterTextures(); }
    public static AtmosphereModule compute() {return instance;}

    public boolean usesWinterTextures() {
        return this.isEnabled() && this.setting6.getValue();
    }

    public static boolean isValid() {
        return false;
    }

    public static boolean val1(World var0) { return isValid2(var0); }
    public static boolean isValid2(World var0) {
        AtmosphereModule var1 = compute();
        return var1 != null && var1.isEnabled() && var1.setting1.getValue() && var0 != null && var0.getDimension().hasSkyLight();
    }

    public static boolean val2() { return isValid3(); }
    public static void val4(org.joml.Vector4f color) { renderComponent(color); }
    public static float val7(float start) { return calcOffset(start); }
    public static float val9(float end) { return compute2(end); }
    public static boolean isValid3() {
        AtmosphereModule var0 = compute();
        return var0 != null
            && var0.isEnabled()
            && var0.setting7.getValue()
            && var0.setting8.getFloatValue() > 0.0F
            && MinecraftClient.getInstance().world != null
            && MinecraftClient.getInstance().world.getRegistryKey().equals(World.OVERWORLD);
    }

    public static float calcOffset(float var0) {
        AtmosphereModule var1 = compute();
        return isValid3()
            ? Math.min(
                var0,
                compute2(3.4028235E38F)
                    * (0.7F - var1.setting8.getFloatValue() * 0.35F)
            )
            : var0;
    }

    public static float compute2(float var0) {
        AtmosphereModule var1 = compute();
        return isValid3()
            ? Math.min(
                var0, MathHelper.lerp(var1.setting8.getFloatValue(), 192.0F, 56.0F)
            )
            : var0;
    }

    public static void renderComponent(Vector4f var0) {
        AtmosphereModule var1 = compute();
        if (isValid3()) {
            float var2 = var1.setting8.getFloatValue() * 0.75F;
            float var3 = Math.max(0.08F, Math.max(var0.x, Math.max(var0.y, var0.z)));
            var0.x = MathHelper.lerp(var2, var0.x, var3 * 0.9F);
            var0.y = MathHelper.lerp(var2, var0.y, var3 * 0.95F);
            var0.z = MathHelper.lerp(var2, var0.z, var3);
        }
    }

    @Override
    public void onEnable() {
        this.renderComponent2();
    }

    @Override
    public void onDisable() {
        this.renderComponent2();
    }

    private void renderComponent2() {
        SurfaceHeightUtils.clear();
        this.val1.clear();
        this.field3 = null;
        this.field4 = null;
        this.count = 0;
    }

    public void onTick() {
        if (this.field3 != this.mc.world) {
            this.renderComponent2();
            this.field3 = this.mc.world;
        }

        if (this.mc.player == null || !isValid2(this.mc.world)) {
            this.val1.clear();
        } else if (!this.mc.isPaused()) {
            Vec3d var2 = this.mc.gameRenderer.getCamera().getCameraPos();
            if (this.field4 != null
                && var2.squaredDistanceTo(this.field4)
                    > 256.0) {
                this.val1.clear();
            }

            this.field4 = var2;
            this.count++;
            double var3 = this.radius.getFloatValue();
            this.val1.removeIf(y -> !this.isValid4(y, var2, var3));
            int var5 = Math.min(
                6500,
                (int)(
                    var3
                        * var3
                        * this.setting2.getFloatValue()
                        * 3.0
                )
            );
            if (this.val1.size() > var5) {
                this.val1.subList(var5, this.val1.size()).clear();
            }

            boolean var6 = this.val1.size()
                < var5 * 0.8;
            int var7 = Math.min(800, (var5 - this.val1.size()) * 3);

            for (int var8 = 0; var8 < var7 && this.val1.size() < var5; var8++) {
                this.renderComponent3(var2, var3, var6);
            }
        }
    }

    private boolean isValid4(AtmosphereModule.WeatherState var1, Vec3d x, double y) {
        var1.field6 = var1.field5;
        var1.count2++;
        double var5 = (this.count + var1.anim2)
            * 0.04;
        double var7 = this.setting5.getFloatValue()
            * (
                0.045
                    + 0.022
                        * Math.sin(
                            this.count * 0.008
                        )
            );
        var1.field5 = var1.field5
            .add(
                var7 * var1.anim3
                    + Math.sin(var5) * 0.009,
                -var1.offset * this.setting4.getFloatValue(),
                var7 * 0.3
                    + Math.cos(var5 * 0.7)
                        * 0.007
            );
        double var9 = var1.field5.x - x.x;
        double var11 = var1.field5.z - x.z;
        BlockPos var13 = BlockPos.ofFloored(var1.field5);
        return var1.count2 < 1000
            && var9 * var9 + var11 * var11 < y * y
            && var1.field5.y
                > x.y - 10.0
            && var1.field5.y
                < x.y + 24.0
            && MinecraftClient.getInstance().world.isChunkLoaded(var13)
            && var1.field5.y
                > SurfaceHeightUtils.val2(this.mc.world, var13.getX(), var13.getZ());
    }

    private void renderComponent3(Vec3d var1, double x, boolean y) {
        double var5 = this.field2.nextDouble()
            * 3.14159265
            * 2.0;
        double var7 = this.field2.nextFloat() < 0.25F
            ? Math.min(x, 9.0)
            : x;
        double var9 = Math.sqrt(this.field2.nextDouble()) * var7;
        double var11 = y
            ? -8.0
                + this.field2.nextDouble() * 30.0
            : 15.0
                + this.field2.nextDouble()
                    * 7.0;
        Vec3d var13 = var1.add(Math.cos(var5) * var9, var11, Math.sin(var5) * var9);
        BlockPos var14 = BlockPos.ofFloored(var13);
        if (this.mc.world.isChunkLoaded(var14)
            && !(var13.y <= SurfaceHeightUtils.val2(this.mc.world, var14.getX(), var14.getZ()))) {
            float var15 = this.field2.nextFloat();
            this.val1
                .add(
                    new AtmosphereModule.WeatherState(
                        var13,
                        0.025F + var15 * var15 * 0.055F,
                        0.055
                            + this.field2.nextDouble()
                                * 0.09,
                        this.field2.nextFloat() * 1000.0F,
                        0.7F + this.field2.nextFloat() * 0.6F
                    )
                );
        }
    }

    public void render(MatrixStack var1, Camera x, float y) {
        if (this.field3 == this.mc.world
            && isValid2(this.mc.world)
            && !this.val1.isEmpty()) {
                BufferBuilder var5 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                Matrix4f var6 = var1.peek().getPositionMatrix();
                Vector3f var7 = new Vector3f(1.0F, 0.0F, 0.0F).rotate(x.getRotation());
                Vector3f var8 = new Vector3f(0.0F, 1.0F, 0.0F).rotate(x.getRotation());
                Vec3d var9 = x.getCameraPos();

                for (AtmosphereModule.WeatherState var11 : this.val1) {
                    Vec3d var12 = var11.field6.lerp(var11.field5, y).subtract(var9);
                    float var13 = (float)var12.length();
                    float var14 = (float)Math.hypot(var12.x, var12.z);
                    float var15 = MathHelper.clamp(
                            (this.radius.getFloatValue() - var14) / 8.0F, 0.0F, 1.0F
                        )
                        * Math.min(1.0F, (var11.count2 + y) / 16.0F)
                        * MathHelper.clamp((var13 - 0.25F) / 0.8F, 0.0F, 1.0F)
                        * MathHelper.clamp(((float)var12.y + 10.0F) / 3.0F, 0.0F, 1.0F);
                    int var16 = (int)(
                        var15
                            * MathHelper.lerp(
                                Math.min(1.0F, var13 / 48.0F), 215.0F, 125.0F
                            )
                    );
                    if (var16 > 0) {
                        float var17 = var11.anim2 + (this.count + y) * 0.012F;
                        float var18 = (float)Math.cos(var17);
                        float var19 = (float)Math.sin(var17);
                        float var20 = var11.anim * (var13 < 2.0F ? 1.3F : 1.0F);
                        float var21 = (var7.x * var18 + var8.x * var19) * var20;
                        float var22 = (var7.y * var18 + var8.y * var19) * var20;
                        float var23 = (var7.z * var18 + var8.z * var19) * var20;
                        float var24 = (var8.x * var18 - var7.x * var19) * var20 * 1.1F;
                        float var25 = (var8.y * var18 - var7.y * var19) * var20 * 1.1F;
                        float var26 = (var8.z * var18 - var7.z * var19) * var20 * 1.1F;
                        float var27 = (float)var12.x;
                        float var28 = (float)var12.y;
                        float var29 = (float)var12.z;
                        var5.vertex(var6, var27 - var21 - var24, var28 - var22 - var25, var29 - var23 - var26)
                            .texture(0.0F, 1.0F)
                            .color(239, 246, 255, var16);
                        var5.vertex(var6, var27 + var21 - var24, var28 + var22 - var25, var29 + var23 - var26)
                            .texture(1.0F, 1.0F)
                            .color(239, 246, 255, var16);
                        var5.vertex(var6, var27 + var21 + var24, var28 + var22 + var25, var29 + var23 + var26)
                            .texture(1.0F, 0.0F)
                            .color(239, 246, 255, var16);
                        var5.vertex(var6, var27 - var21 + var24, var28 - var22 + var25, var29 - var23 + var26)
                            .texture(0.0F, 0.0F)
                            .color(239, 246, 255, var16);
                    }
                }

                AuraRender.drawTextured(var5,AuraRender.POS_TEX_COLOR_QUADS,Identifier.of("anvil","textures/atmosphere_snow.png"));
        }
    }
    @Override public void onWorldRender(WorldRenderContext context){render(context.matrixStack(),context.camera(),context.tickCounter().getTickProgress(true));}
    private static final class WeatherState {
        private Vec3d field5;
        private Vec3d field6;
        private final float anim;
        private final double offset;
        private final float anim2;
        private final float anim3;
        private int count2;

        private WeatherState(Vec3d var1, float x, double y, float width, float height) {
            this.field5 = var1;
            this.field6 = var1;
            this.anim = x;
            this.offset = y;
            this.anim2 = width;
            this.anim3 = height;
        }
    }
}
