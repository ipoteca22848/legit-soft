package com.example.auravisuals.module.render;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.MultiBooleanSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.theme.ThemeManager;
import com.example.auravisuals.util.render.BlockOutlineShaderUtil;
import com.example.auravisuals.util.render.WorldOcclusion;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class CustomHitBox extends Module {
    private static final double MAX_RENDER_DISTANCE_SQ = 9216.0;
    private static final String RENDER_PLAYERS = "Игроки";
    private static final String RENDER_MOBS = "Мобы";
    private static final String RENDER_ANIMALS = "Животные";
    public final MultiBooleanSetting renderOn = new MultiBooleanSetting("Отображать на", "Игроки", "Мобы", "Животные");
    public final BooleanSetting fill = new BooleanSetting("Заливка", true);
    public final ModeSetting fillMode = new ModeSetting("Режим заливки", "Обычная", () -> this.fill.getValue(), "Обычная", "Шейдер");
    public final ModeSetting shader = new ModeSetting(
        "Шейдер",
        "Волны",
        () -> this.fill.getValue() && this.fillMode.is("Шейдер"),
        "Волны",
        "Плазма",
        "Вода",
        "Тень",
        "WebShader",
        "Aurora",
        "Pulse",
        "Neon",
        "Smoke",
        "Ripple",
        "Crystal",
        "Plasma2",
        "Nebula",
        "Galaxy",
        "Ocean",
        "Sunset",
        "Cloud",
        "Shimmer",
        "Prism",
        "Radiance"
    );
    public final NumberSetting shaderSpeed = new NumberSetting(
        "Скорость шейдера", 1.0, 0.1, 1.0, 0.01, () -> this.fill.getValue() && this.fillMode.is("Шейдер")
    );
    public final NumberSetting shaderAlpha = new NumberSetting(
        "Прозрачность шейдера", 120.0, 0.0, 255.0, 1.0, () -> this.fill.getValue() && this.fillMode.is("Шейдер")
    );
    public final BooleanSetting customColor = new BooleanSetting("Свой цвет", false, () -> !this.isShaderFill());
    public final ColorSetting fillColor = new ColorSetting(
        "Цвет заливки", new Color(255, 255, 255, 90), () -> this.fill.getValue() && this.customColor.getValue() && !this.isShaderFill()
    );
    public final ColorSetting outlineColor = new ColorSetting(
        "Цвет обводки", new Color(255, 255, 255, 255), () -> this.customColor.getValue() && !this.isShaderFill()
    );

    public CustomHitBox() {
        super("CustomHitBox", "Кастомный цвет хитбоксов", Category.VISUALS);
        this.addSettings(
            this.renderOn, this.fill, this.fillMode, this.shader, this.shaderSpeed, this.shaderAlpha, this.customColor, this.fillColor, this.outlineColor
        );
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        this.renderCustomHitBoxDirect(context.matrixStack(), this.mc.getRenderTickCounter().getTickProgress(true));
    }

    public static void renderInGui(DrawContext context, LivingEntity entity) {
        CustomHitBox module = ModuleManager.INSTANCE.getModule(CustomHitBox.class);
        if (module != null && module.isEnabled()) {
            MatrixStack matrices = new MatrixStack();
            double w = entity.getWidth() / 2.0;
            double h = entity.getHeight();
            Box box = new Box(-w, 0.0, -w, w, h, w);
            box = box.expand(0.002);
            if (module.fill.getValue()) {
                Color fColor = new Color(module.getActiveFillColor(), true);
                renderBoxFillGui(matrices, box, fColor);
            }

            if (!module.isShaderFill()) {
                Color oColor = new Color(module.getActiveOutlineColor(), true);
                renderBoxOutlineGui(matrices, box, oColor, 1.0F);
            }
        }
    }

    private int getActiveFillColor() {
        return this.customColor.getValue() ? this.fillColor.getValue() : this.applyThemeAlpha(this.fillColor.getValue());
    }

    private int getActiveOutlineColor() {
        return this.customColor.getValue() ? this.outlineColor.getValue() : this.applyThemeAlpha(this.outlineColor.getValue());
    }

    private int applyThemeAlpha(int baseColor) {
        int alpha = baseColor >> 24 & 0xFF;
        int themeColor = ThemeManager.INSTANCE.getVisualModulesColor();
        return alpha << 24 | themeColor & 16777215;
    }

    private boolean isShaderFill() {
        return this.fill.getValue() && this.fillMode.is("Шейдер");
    }

    private static void renderBoxFillGui(MatrixStack matrices, Box box, Color color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float minX = (float)box.minX;
        float minY = (float)box.minY;
        float minZ = (float)box.minZ;
        float maxX = (float)box.maxX;
        float maxY = (float)box.maxY;
        float maxZ = (float)box.maxZ;
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        float a = color.getAlpha() / 255.0F;
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        AuraRender.drawColor(buffer, AuraRender.POS_COLOR_QUADS);
    }

    private static void renderBoxOutlineGui(MatrixStack matrices, Box box, Color color, float lineWidth) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        float minX = (float)box.minX;
        float minY = (float)box.minY;
        float minZ = (float)box.minZ;
        float maxX = (float)box.maxX;
        float maxY = (float)box.maxY;
        float maxZ = (float)box.maxZ;
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        float a = color.getAlpha() / 255.0F;
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        AuraRender.drawColorLines(buffer, AuraRender.POS_COLOR_LINES, lineWidth);
    }

    private void renderCustomHitBoxDirect(MatrixStack matrices, float tickDelta) {
        if (this.mc.world != null && this.mc.player != null && this.mc.gameRenderer != null && this.mc.gameRenderer.getCamera() != null) {
            Vec3d cameraPos = this.mc.gameRenderer.getCamera().getCameraPos();
            boolean shaderFill = this.isShaderFill();
            boolean doFill = this.fill.getValue();
            float lw = 1.0F;
            Color fColor = !shaderFill && doFill ? new Color(this.getActiveFillColor(), true) : null;
            Color oColor = !shaderFill ? new Color(this.getActiveOutlineColor(), true) : null;

            for (Entity entity : this.mc.world.getEntities()) {
                PlayerEntity player;
                if (this.isRenderableEntity(entity, cameraPos)
                    && (!(entity instanceof PlayerEntity) || WorldOcclusion.isEntityVisible(player = (PlayerEntity)entity, cameraPos, tickDelta))) {
                    try {
                        Box worldBox = entity.getBoundingBox();
                        if (worldBox != null) {
                            worldBox = worldBox.expand(0.002);
                            Vec3d interpolatedPos = entity.getLerpedPos(tickDelta);
                            Box interpolatedBox;
                            if (this.isFinite(interpolatedPos)
                                && this.isFinite(interpolatedBox = worldBox.offset(interpolatedPos.subtract(entity.getEntityPos())))) {
                                if (shaderFill) {
                                    this.renderEntityBoxShaderFill(matrices, interpolatedBox, cameraPos, entity);
                                } else if (doFill) {
                                    this.renderEntityBoxFill(matrices, interpolatedBox, fColor);
                                }

                                if (!shaderFill) {
                                    this.renderEntityBoxOutline(matrices, interpolatedBox, oColor, lw);
                                }
                            }
                        }
                    } catch (Throwable var15) {
                    }
                }
            }
        }
    }

    private boolean isRenderableEntity(Entity entity, Vec3d cameraPos) {
        if (entity == null || entity == this.mc.player) {
            return false;
        }

        if (entity.isRemoved() || !entity.isAlive()) {
            return false;
        }

        if (entity.isInvisible()) {
            return false;
        }

        LivingEntity living;
        if (entity instanceof LivingEntity && (living = (LivingEntity)entity).hasStatusEffect(StatusEffects.INVISIBILITY)) {
            return false;
        }

        if (!this.shouldRenderType(entity)) {
            return false;
        }

        Vec3d entityPos = entity.getEntityPos();
        return !this.isFinite(entityPos) ? false : !(cameraPos.squaredDistanceTo(entityPos) > 9216.0);
    }

    private boolean shouldRenderType(Entity entity) {
        if (entity instanceof PlayerEntity) {
            return this.renderOn.isEnabled("Игроки");
        } else if (entity instanceof AnimalEntity) {
            return this.renderOn.isEnabled("Животные");
        } else {
            return entity instanceof MobEntity ? this.renderOn.isEnabled("Мобы") : false;
        }
    }

    private boolean isFinite(Vec3d vec) {
        return vec != null && Double.isFinite(vec.x) && Double.isFinite(vec.y) && Double.isFinite(vec.z);
    }

    private boolean isFinite(Box box) {
        return box != null
            && Double.isFinite(box.minX)
            && Double.isFinite(box.minY)
            && Double.isFinite(box.minZ)
            && Double.isFinite(box.maxX)
            && Double.isFinite(box.maxY)
            && Double.isFinite(box.maxZ);
    }

    private void renderEntityBoxFill(MatrixStack matrices, Box box, Color color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = this.mc.gameRenderer.getCamera().getCameraPos();
        float minX = (float)(box.minX - camera.x);
        float minY = (float)(box.minY - camera.y);
        float minZ = (float)(box.minZ - camera.z);
        float maxX = (float)(box.maxX - camera.x);
        float maxY = (float)(box.maxY - camera.y);
        float maxZ = (float)(box.maxZ - camera.z);
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        float a = color.getAlpha() / 255.0F;
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        AuraRender.drawColor(buffer, AuraRender.POS_COLOR_QUADS);
    }

    private void renderEntityBoxShaderFill(MatrixStack matrices, Box box, Vec3d camera, Entity entity) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        ArrayList<Float> vertices = new ArrayList<>();
        double minX = box.minX - camera.x;
        double minY = box.minY - camera.y;
        double minZ = box.minZ - camera.z;
        double maxX = box.maxX - camera.x;
        double maxY = box.maxY - camera.y;
        double maxZ = box.maxZ - camera.z;
        this.addShaderFace(vertices, minX, minY, minZ, maxX, maxY, maxZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ);
        this.addShaderFace(vertices, minX, minY, minZ, maxX, maxY, maxZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ);
        this.addShaderFace(vertices, minX, minY, minZ, maxX, maxY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ);
        this.addShaderFace(vertices, minX, minY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ);
        this.addShaderFace(vertices, minX, minY, minZ, maxX, maxY, maxZ, minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ);
        this.addShaderFace(vertices, minX, minY, minZ, maxX, maxY, maxZ, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ);
        BlockOutlineShaderUtil.render(
            this.toFloatArray(vertices),
            4,
            matrix,
            this.shaderAlpha.getIntValue() / 255.0F,
            1.0F,
            this.shader.getValue(),
            this.shaderSpeed.getFloatValue(),
            this.getShaderSeed(entity),
            this.getActiveFillColor() & 16777215
        );
    }

    private float getShaderSeed(Entity entity) {
        if (entity == null) {
            return 0.0F;
        }

        float h = (float)Math.sin(entity.getId() * 12.9898F) * 43758.547F;
        return h - (float)Math.floor(h);
    }

    private void renderEntityBoxOutline(MatrixStack matrices, Box box, Color color, float lineWidth) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = this.mc.gameRenderer.getCamera().getCameraPos();
        float adjustedLineWidth = Math.max(1.0F, Math.min(8.0F, lineWidth));
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        float minX = (float)(box.minX - camera.x);
        float minY = (float)(box.minY - camera.y);
        float minZ = (float)(box.minZ - camera.z);
        float maxX = (float)(box.maxX - camera.x);
        float maxY = (float)(box.maxY - camera.y);
        float maxZ = (float)(box.maxZ - camera.z);
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        float a = color.getAlpha() / 255.0F;
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        AuraRender.drawColorLines(buffer, AuraRender.POS_COLOR_LINES, adjustedLineWidth);
    }

    private void addShaderFace(
        List<Float> out,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        double x1,
        double y1,
        double z1,
        double x2,
        double y2,
        double z2,
        double x3,
        double y3,
        double z3,
        double x4,
        double y4,
        double z4
    ) {
        this.addShaderPoint(out, x1, y1, z1, minX, minY, minZ, maxX, maxY, maxZ);
        this.addShaderPoint(out, x2, y2, z2, minX, minY, minZ, maxX, maxY, maxZ);
        this.addShaderPoint(out, x3, y3, z3, minX, minY, minZ, maxX, maxY, maxZ);
        this.addShaderPoint(out, x1, y1, z1, minX, minY, minZ, maxX, maxY, maxZ);
        this.addShaderPoint(out, x3, y3, z3, minX, minY, minZ, maxX, maxY, maxZ);
        this.addShaderPoint(out, x4, y4, z4, minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void addShaderPoint(List<Float> out, double x, double y, double z, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        out.add((float)x);
        out.add((float)y);
        out.add((float)z);
        out.add(this.normalizeShaderLocal(x, minX, maxX));
        out.add(this.normalizeShaderLocal(y, minY, maxY));
        out.add(this.normalizeShaderLocal(z, minZ, maxZ));
    }

    private float normalizeShaderLocal(double value, double min, double max) {
        double size = max - min;
        return Math.abs(size) < 1.0E-6 ? 0.0F : (float)((value - min) / size);
    }

    private float[] toFloatArray(List<Float> values) {
        float[] result = new float[values.size()];

        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }

        return result;
    }
}
