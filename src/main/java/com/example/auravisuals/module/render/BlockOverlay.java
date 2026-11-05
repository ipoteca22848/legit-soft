package com.example.auravisuals.module.render;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.theme.ThemeManager;
import com.example.auravisuals.util.color.ColorUtil;
import com.example.auravisuals.util.render.BlockOutlineShaderUtil;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

public class BlockOverlay extends Module {
    private final ModeSetting mode = new ModeSetting("Режим", "Обычный", "Обычный", "Шейдер");
    private final ModeSetting shader = new ModeSetting(
        "Шейдер",
        "Волны",
        () -> this.mode.is("Шейдер"),
        "Волны",
        "Плазма",
        "Вода",
        "Тень",
        "Звёзды",
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
    private final NumberSetting shaderSpeed = new NumberSetting("Скорость шейдера", 1.0, 0.1, 1.0, 0.01, () -> this.mode.is("Шейдер"));
    private final NumberSetting alpha = new NumberSetting("Прозрачность контура", 150.0, 0.0, 255.0, 1.0, () -> !this.mode.is("Шейдер"));
    private final NumberSetting switchAnimationSpeed = new NumberSetting("Скорость анимации", 2.0, 1.0, 3.0, 0.1);
    private final BooleanSetting threeD = new BooleanSetting("3D", true);
    private final BooleanSetting fill = new BooleanSetting("Показывать заливку", true, () -> !this.mode.is("Шейдер"));
    private final NumberSetting fillAlpha = new NumberSetting(
        "Прозрачность заливки", 50.0, 0.0, 255.0, 1.0, () -> this.mode.is("Шейдер") || this.fill.getValue()
    );
    private final BooleanSetting customColor = new BooleanSetting("Свой цвет", false, () -> !this.mode.is("Шейдер"));
    private final ColorSetting color1 = new ColorSetting("Цвет 1", new Color(255, 255, 255), () -> !this.mode.is("Шейдер") && this.customColor.getValue());
    private final BooleanSetting useGradient = new BooleanSetting("Градиент", true, () -> !this.mode.is("Шейдер"));
    private final ColorSetting color2 = new ColorSetting("Цвет 2", new Color(0, 150, 255), () -> !this.mode.is("Шейдер") && this.useGradient.getValue());
    private static final float LINE_WIDTH = 6.5F;
    private static final float BASE_SWITCH_ANIMATION_MS = 140.0F;
    private BlockPos animatedBlockPos;
    private List<Box> previousBoxes = new ArrayList<>();
    private List<Box> targetBoxes = new ArrayList<>();
    private long switchStartedAt;

    public BlockOverlay() {
        super("BlockOverlay", "Обводка блоков", Category.VISUALS);
        this.addSettings(
            this.mode,
            this.shader,
            this.shaderSpeed,
            this.switchAnimationSpeed,
            this.alpha,
            this.threeD,
            this.fill,
            this.fillAlpha,
            this.customColor,
            this.color1,
            this.useGradient,
            this.color2
        );
    }

    public static boolean shouldHideVanillaOutline() {
        BlockOverlay module = ModuleManager.INSTANCE.getModule(BlockOverlay.class);
        return module != null && module.isEnabled();
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.mc.player != null && this.mc.world != null) {
            boolean shaderMode = this.mode.is("Шейдер");
            boolean drawFill = (shaderMode || this.fill.getValue()) && this.fillAlpha.getIntValue() > 0;
            boolean drawOutline = !shaderMode && this.alpha.getIntValue() > 0;
            if (drawFill || drawOutline) {
                HitResult hitResult = this.mc.crosshairTarget;
                if (hitResult != null && hitResult.getType() == Type.BLOCK) {
                    BlockHitResult blockHitResult = (BlockHitResult)hitResult;
                    BlockPos blockPos = blockHitResult.getBlockPos();
                    Vec3d cameraPos = context.camera().getCameraPos();
                    if (cameraPos.squaredDistanceTo(blockPos.getX(), blockPos.getY(), blockPos.getZ()) > 10000.0) {
                        this.resetSwitchAnimation();
                    } else {
                        VoxelShape shape = this.mc.world.getBlockState(blockPos).getOutlineShape(this.mc.world, blockPos);
                        if (shape.isEmpty()) {
                            this.resetSwitchAnimation();
                        } else {
                            ArrayList<Box> rawBoxes = new ArrayList<>();
                            shape.forEachBox(
                                (minX, minY, minZ, maxX, maxY, maxZ) -> rawBoxes.add(new Box(minX, minY, minZ, maxX, maxY, maxZ).offset(blockPos).expand(0.002))
                            );
                            List<Box> boxes = this.animateSwitch(blockPos, rawBoxes);
                            boolean depthTest = !this.threeD.getValue();
                            MatrixStack matrices = context.matrixStack();
                            if (shaderMode) {
                                this.renderShaderFill(matrices, boxes, cameraPos);
                            } else {
                                if (drawFill) {
                                    this.renderThemeFill(matrices, boxes, cameraPos, depthTest);
                                }

                                if (drawOutline) {
                                    this.renderThemeOutline(matrices, boxes, cameraPos, depthTest);
                                }
                            }
                        }
                    }
                } else {
                    this.resetSwitchAnimation();
                }
            }
        }
    }

    private List<Box> animateSwitch(BlockPos blockPos, List<Box> newBoxes) {
        if (newBoxes.isEmpty()) {
            this.resetSwitchAnimation();
            return newBoxes;
        }

        if (this.animatedBlockPos == null) {
            this.animatedBlockPos = blockPos.toImmutable();
            this.previousBoxes = this.copyBoxes(newBoxes);
            this.targetBoxes = this.copyBoxes(newBoxes);
            this.switchStartedAt = System.currentTimeMillis() - this.getSwitchAnimationDurationMs();
            return this.targetBoxes;
        }

        if (!this.animatedBlockPos.equals(blockPos)) {
            List<Box> currentBoxes = this.currentAnimatedBoxes();
            this.previousBoxes = currentBoxes.isEmpty() ? this.copyBoxes(newBoxes) : currentBoxes;
            this.targetBoxes = this.copyBoxes(newBoxes);
            this.animatedBlockPos = blockPos.toImmutable();
            this.switchStartedAt = System.currentTimeMillis();
        } else if (!this.sameBoxes(this.targetBoxes, newBoxes)) {
            this.previousBoxes = this.currentAnimatedBoxes();
            this.targetBoxes = this.copyBoxes(newBoxes);
            this.switchStartedAt = System.currentTimeMillis();
        }

        return this.currentAnimatedBoxes();
    }

    private List<Box> currentAnimatedBoxes() {
        if (this.targetBoxes.isEmpty()) {
            return this.targetBoxes;
        }

        if (this.previousBoxes.size() != this.targetBoxes.size()) {
            return this.targetBoxes;
        }

        float progress = (float)(System.currentTimeMillis() - this.switchStartedAt) / (float)this.getSwitchAnimationDurationMs();
        if (progress >= 1.0F) {
            return this.targetBoxes;
        }

        progress = this.smoothStep(Math.max(0.0F, Math.min(1.0F, progress)));
        ArrayList<Box> boxes = new ArrayList<>(this.targetBoxes.size());

        for (int i = 0; i < this.targetBoxes.size(); i++) {
            boxes.add(this.lerpBox(this.previousBoxes.get(i), this.targetBoxes.get(i), progress));
        }

        return boxes;
    }

    private Box lerpBox(Box from, Box to, float progress) {
        return new Box(
            this.lerp(from.minX, to.minX, progress),
            this.lerp(from.minY, to.minY, progress),
            this.lerp(from.minZ, to.minZ, progress),
            this.lerp(from.maxX, to.maxX, progress),
            this.lerp(from.maxY, to.maxY, progress),
            this.lerp(from.maxZ, to.maxZ, progress)
        );
    }

    private double lerp(double from, double to, float progress) {
        return from + (to - from) * progress;
    }

    private float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private long getSwitchAnimationDurationMs() {
        float speed = Math.max(1.0F, Math.min(3.0F, this.switchAnimationSpeed.getFloatValue()));
        return Math.max(35L, Math.round(280.0F / speed));
    }

    private boolean sameBoxes(List<Box> first, List<Box> second) {
        if (first.size() != second.size()) {
            return false;
        }

        for (int i = 0; i < first.size(); i++) {
            Box a = first.get(i);
            Box b = second.get(i);
            if (Math.abs(a.minX - b.minX) > 1.0E-5
                || Math.abs(a.minY - b.minY) > 1.0E-5
                || Math.abs(a.minZ - b.minZ) > 1.0E-5
                || Math.abs(a.maxX - b.maxX) > 1.0E-5
                || Math.abs(a.maxY - b.maxY) > 1.0E-5
                || Math.abs(a.maxZ - b.maxZ) > 1.0E-5) {
                return false;
            }
        }

        return true;
    }

    private List<Box> copyBoxes(List<Box> boxes) {
        return new ArrayList<>(boxes);
    }

    private void resetSwitchAnimation() {
        this.animatedBlockPos = null;
        this.previousBoxes.clear();
        this.targetBoxes.clear();
    }

    @Override
    public void onDisable() {
        this.resetSwitchAnimation();
    }

    private void renderThemeFill(MatrixStack matrices, List<Box> boxes, Vec3d cameraPos, boolean depthTest) {
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (Box box : boxes) {
            this.renderBoxFill(buffer, matrices, box, cameraPos, this.fillAlpha.getIntValue());
        }

        AuraRender.drawColor(buffer, depthTest ? AuraRender.POS_COLOR_QUADS : AuraRender.POS_COLOR_QUADS_NODEPTH);
    }

    private void renderThemeOutline(MatrixStack matrices, List<Box> boxes, Vec3d cameraPos, boolean depthTest) {
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (Box box : boxes) {
            this.renderBoxOutline(buffer, matrices, box, cameraPos, this.alpha.getIntValue());
        }

        AuraRender.drawColorLines(buffer, depthTest ? AuraRender.POS_COLOR_LINES : AuraRender.POS_COLOR_LINES_NODEPTH, 6.5F);
    }

    private void renderShaderFill(MatrixStack matrices, List<Box> boxes, Vec3d cameraPos) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        ArrayList<Float> vertices = new ArrayList<>();

        for (Box box : boxes) {
            this.addShaderFill(vertices, box, cameraPos);
        }

        int tint = this.customColor.getValue() ? this.color1.getColor().getRGB() & 16777215 : ThemeManager.INSTANCE.getVisualModulesColor() & 16777215;
        BlockOutlineShaderUtil.render(
            this.toArray(vertices),
            4,
            matrix,
            this.fillAlpha.getIntValue() / 255.0F,
            6.5F,
            this.shader.getValue(),
            this.shaderSpeed.getFloatValue(),
            0.0F,
            tint,
            this.threeD.getValue()
        );
    }

    private int getColor(double x, double y, double z, int alphaVal) {
        int color = this.customColor.getValue() ? this.color1.getColor().getRGB() : ThemeManager.INSTANCE.getVisualModulesColor();
        if (this.useGradient.getValue()) {
            int second = this.customColor.getValue() ? this.color2.getColor().getRGB() : ColorUtil.brighten(color, 0.45F);
            float phase = (float)((Math.sin((x + y + z + System.currentTimeMillis() * 0.004) * 0.7) + 1.0) * 0.5);
            color = ColorUtil.lerpColor(color, second, phase);
        }

        return alphaVal << 24 | color & 16777215;
    }

    private void vertex(BufferBuilder buffer, Matrix4f matrix, double x, double y, double z, int alphaValue) {
        int color = this.getColor(x, y, z, alphaValue);
        buffer.vertex(matrix, (float)x, (float)y, (float)z)
            .color((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, (color >> 24 & 0xFF) / 255.0F);
    }

    private void renderBoxFill(BufferBuilder buffer, MatrixStack matrices, Box box, Vec3d cam, int alphaValue) {
        Matrix4f mat = matrices.peek().getPositionMatrix();
        double minX = box.minX - cam.x;
        double minY = box.minY - cam.y;
        double minZ = box.minZ - cam.z;
        double maxX = box.maxX - cam.x;
        double maxY = box.maxY - cam.y;
        double maxZ = box.maxZ - cam.z;
        if (this.threeD.getValue() || cam.y < box.minY) {
            this.vertex(buffer, mat, minX, minY, minZ, alphaValue);
            this.vertex(buffer, mat, maxX, minY, minZ, alphaValue);
            this.vertex(buffer, mat, maxX, minY, maxZ, alphaValue);
            this.vertex(buffer, mat, minX, minY, maxZ, alphaValue);
        }

        if (this.threeD.getValue() || cam.y > box.maxY) {
            this.vertex(buffer, mat, minX, maxY, minZ, alphaValue);
            this.vertex(buffer, mat, minX, maxY, maxZ, alphaValue);
            this.vertex(buffer, mat, maxX, maxY, maxZ, alphaValue);
            this.vertex(buffer, mat, maxX, maxY, minZ, alphaValue);
        }

        if (this.threeD.getValue() || cam.z < box.minZ) {
            this.vertex(buffer, mat, minX, minY, minZ, alphaValue);
            this.vertex(buffer, mat, minX, maxY, minZ, alphaValue);
            this.vertex(buffer, mat, maxX, maxY, minZ, alphaValue);
            this.vertex(buffer, mat, maxX, minY, minZ, alphaValue);
        }

        if (this.threeD.getValue() || cam.z > box.maxZ) {
            this.vertex(buffer, mat, maxX, minY, maxZ, alphaValue);
            this.vertex(buffer, mat, maxX, maxY, maxZ, alphaValue);
            this.vertex(buffer, mat, minX, maxY, maxZ, alphaValue);
            this.vertex(buffer, mat, minX, minY, maxZ, alphaValue);
        }

        if (this.threeD.getValue() || cam.x < box.minX) {
            this.vertex(buffer, mat, minX, minY, maxZ, alphaValue);
            this.vertex(buffer, mat, minX, maxY, maxZ, alphaValue);
            this.vertex(buffer, mat, minX, maxY, minZ, alphaValue);
            this.vertex(buffer, mat, minX, minY, minZ, alphaValue);
        }

        if (this.threeD.getValue() || cam.x > box.maxX) {
            this.vertex(buffer, mat, maxX, minY, minZ, alphaValue);
            this.vertex(buffer, mat, maxX, maxY, minZ, alphaValue);
            this.vertex(buffer, mat, maxX, maxY, maxZ, alphaValue);
            this.vertex(buffer, mat, maxX, minY, maxZ, alphaValue);
        }
    }

    private void renderBoxOutline(BufferBuilder buffer, MatrixStack matrices, Box box, Vec3d cam, int alphaValue) {
        Matrix4f mat = matrices.peek().getPositionMatrix();
        double minX = box.minX - cam.x;
        double minY = box.minY - cam.y;
        double minZ = box.minZ - cam.z;
        double maxX = box.maxX - cam.x;
        double maxY = box.maxY - cam.y;
        double maxZ = box.maxZ - cam.z;
        this.addLine(buffer, mat, minX, minY, minZ, maxX, minY, minZ, alphaValue);
        this.addLine(buffer, mat, maxX, minY, minZ, maxX, minY, maxZ, alphaValue);
        this.addLine(buffer, mat, maxX, minY, maxZ, minX, minY, maxZ, alphaValue);
        this.addLine(buffer, mat, minX, minY, maxZ, minX, minY, minZ, alphaValue);
        this.addLine(buffer, mat, minX, maxY, minZ, maxX, maxY, minZ, alphaValue);
        this.addLine(buffer, mat, maxX, maxY, minZ, maxX, maxY, maxZ, alphaValue);
        this.addLine(buffer, mat, maxX, maxY, maxZ, minX, maxY, maxZ, alphaValue);
        this.addLine(buffer, mat, minX, maxY, maxZ, minX, maxY, minZ, alphaValue);
        this.addLine(buffer, mat, minX, minY, minZ, minX, maxY, minZ, alphaValue);
        this.addLine(buffer, mat, maxX, minY, minZ, maxX, maxY, minZ, alphaValue);
        this.addLine(buffer, mat, maxX, minY, maxZ, maxX, maxY, maxZ, alphaValue);
        this.addLine(buffer, mat, minX, minY, maxZ, minX, maxY, maxZ, alphaValue);
    }

    private void addLine(BufferBuilder buffer, Matrix4f matrix, double x1, double y1, double z1, double x2, double y2, double z2, int alphaValue) {
        this.vertex(buffer, matrix, x1, y1, z1, alphaValue);
        this.vertex(buffer, matrix, x2, y2, z2, alphaValue);
    }

    private void addShaderFill(List<Float> out, Box box, Vec3d cam) {
        double minX = box.minX - cam.x;
        double minY = box.minY - cam.y;
        double minZ = box.minZ - cam.z;
        double maxX = box.maxX - cam.x;
        double maxY = box.maxY - cam.y;
        double maxZ = box.maxZ - cam.z;
        if (this.threeD.getValue() || cam.y < box.minY) {
            this.addFace(out, minX, minY, minZ, maxX, maxY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ);
        }

        if (this.threeD.getValue() || cam.y > box.maxY) {
            this.addFace(out, minX, minY, minZ, maxX, maxY, maxZ, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ);
        }

        if (this.threeD.getValue() || cam.z < box.minZ) {
            this.addFace(out, minX, minY, minZ, maxX, maxY, maxZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ);
        }

        if (this.threeD.getValue() || cam.z > box.maxZ) {
            this.addFace(out, minX, minY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ);
        }

        if (this.threeD.getValue() || cam.x < box.minX) {
            this.addFace(out, minX, minY, minZ, maxX, maxY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ);
        }

        if (this.threeD.getValue() || cam.x > box.maxX) {
            this.addFace(out, minX, minY, minZ, maxX, maxY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ);
        }
    }

    private void addFace(
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
        this.addPoint(out, x1, y1, z1, minX, minY, minZ, maxX, maxY, maxZ);
        this.addPoint(out, x2, y2, z2, minX, minY, minZ, maxX, maxY, maxZ);
        this.addPoint(out, x3, y3, z3, minX, minY, minZ, maxX, maxY, maxZ);
        this.addPoint(out, x1, y1, z1, minX, minY, minZ, maxX, maxY, maxZ);
        this.addPoint(out, x3, y3, z3, minX, minY, minZ, maxX, maxY, maxZ);
        this.addPoint(out, x4, y4, z4, minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void addPoint(List<Float> out, double x, double y, double z, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        out.add((float)x);
        out.add((float)y);
        out.add((float)z);
        out.add(this.normalizeLocal(x, minX, maxX));
        out.add(this.normalizeLocal(y, minY, maxY));
        out.add(this.normalizeLocal(z, minZ, maxZ));
    }

    private float normalizeLocal(double value, double min, double max) {
        double size = max - min;
        return Math.abs(size) < 1.0E-6 ? 0.0F : (float)((value - min) / size);
    }

    private float[] toArray(List<Float> values) {
        float[] result = new float[values.size()];

        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }

        return result;
    }
}
