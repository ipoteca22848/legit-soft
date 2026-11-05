package com.example.auravisuals.module.render;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.LabelSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.MultiBooleanSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.theme.ThemeManager;
import com.example.auravisuals.util.render.BlockOutlineShaderUtil;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class Wave extends Module {
    static final String MODE_JUMP = "Прыжок";
    static final String MODE_HIT = "Удар";
    static final float JUMP_THICKNESS = 2.5F;
    static final float HIT_THICKNESS = 1.15F;
    static final float BASE_RADIUS_SPEED = 0.02F;
    static final float HIT_SPEED_BALANCE = 0.82F;
    final MultiBooleanSetting modes = new MultiBooleanSetting("Режимы", "Прыжок", "Удар");
    final LabelSetting jumpLabel = new LabelSetting("Прыжок").centered();
    final NumberSetting jumpRange = new NumberSetting("jump_range", 25.0, 10.0, 50.0, 1.0, () -> this.modes.isEnabled("Прыжок"));
    final NumberSetting jumpSpeed = new NumberSetting("jump_speed", 1.0, 0.3, 2.0, 0.1, () -> this.modes.isEnabled("Прыжок"));
    final BooleanSetting jumpFill = new BooleanSetting("jump_fill", true, () -> this.modes.isEnabled("Прыжок"));
    final NumberSetting jumpFillAlpha = new NumberSetting(
        "jump_fill_alpha", 0.3, 0.0, 1.0, 0.05, () -> this.modes.isEnabled("Прыжок") && this.jumpFill.getValue()
    );
    final BooleanSetting jumpShaderFill = new BooleanSetting("jump_shader", false, () -> this.modes.isEnabled("Прыжок") && this.jumpFill.getValue());
    final ModeSetting jumpShaderMode = new ModeSetting(
        "jump_shader_mode",
        "Волны",
        () -> this.modes.isEnabled("Прыжок") && this.jumpFill.getValue() && this.jumpShaderFill.getValue(),
        "Волны",
        "Плазма",
        "Вода",
        "Тень"
    );
    final NumberSetting jumpShaderSpeed = new NumberSetting(
        "jump_shader_speed", 1.0, 0.1, 1.0, 0.01, () -> this.modes.isEnabled("Прыжок") && this.jumpFill.getValue() && this.jumpShaderFill.getValue()
    );
    final BooleanSetting jumpCustomColor = new BooleanSetting("jump_custom_color", false, () -> this.modes.isEnabled("Прыжок"));
    final ColorSetting jumpColor = new ColorSetting(
        "jump_color", new Color(255, 255, 255), () -> this.modes.isEnabled("Прыжок") && this.jumpCustomColor.getValue()
    );
    final LabelSetting hitLabel = new LabelSetting("Удар").centered();
    final NumberSetting hitRange = new NumberSetting("hit_range", 15.0, 5.0, 25.0, 1.0, () -> this.modes.isEnabled("Удар"));
    final NumberSetting hitSpeed = new NumberSetting("hit_speed", 0.8, 0.3, 2.0, 0.1, () -> this.modes.isEnabled("Удар"));
    final BooleanSetting hitFill = new BooleanSetting("hit_fill", true, () -> this.modes.isEnabled("Удар"));
    final NumberSetting hitFillAlpha = new NumberSetting("hit_fill_alpha", 0.3, 0.0, 1.0, 0.05, () -> this.modes.isEnabled("Удар") && this.hitFill.getValue());
    final BooleanSetting hitShaderFill = new BooleanSetting("hit_shader", false, () -> this.modes.isEnabled("Удар") && this.hitFill.getValue());
    final ModeSetting hitShaderMode = new ModeSetting(
        "hit_shader_mode",
        "Волны",
        () -> this.modes.isEnabled("Удар") && this.hitFill.getValue() && this.hitShaderFill.getValue(),
        "Волны",
        "Плазма",
        "Вода",
        "Тень"
    );
    final NumberSetting hitShaderSpeed = new NumberSetting(
        "hit_shader_speed", 1.0, 0.1, 1.0, 0.01, () -> this.modes.isEnabled("Удар") && this.hitFill.getValue() && this.hitShaderFill.getValue()
    );
    final BooleanSetting hitCustomColor = new BooleanSetting("hit_custom_color", false, () -> this.modes.isEnabled("Удар"));
    final ColorSetting hitColor = new ColorSetting("hit_color", new Color(255, 255, 255), () -> this.modes.isEnabled("Удар") && this.hitCustomColor.getValue());
    final List<Wave$WaveInstance> waves = new ArrayList<>();

    public Wave() {
        super("Wave", "Волны при прыжке или ударе", Category.VISUALS);
        this.jumpRange.setDisplayName("Дальность");
        this.jumpSpeed.setDisplayName("Скорость");
        this.jumpFill.setDisplayName("Заливка");
        this.jumpFillAlpha.setDisplayName("Прозрачность заливки");
        this.jumpShaderFill.setDisplayName("Шейдер");
        this.jumpShaderMode.setDisplayName("Режим шейдера");
        this.jumpShaderSpeed.setDisplayName("Скорость шейдера");
        this.jumpCustomColor.setDisplayName("Свой цвет");
        this.jumpColor.setDisplayName("Цвет");
        this.hitRange.setDisplayName("Дальность");
        this.hitSpeed.setDisplayName("Скорость");
        this.hitFill.setDisplayName("Заливка");
        this.hitFillAlpha.setDisplayName("Прозрачность заливки");
        this.hitShaderFill.setDisplayName("Шейдер");
        this.hitShaderMode.setDisplayName("Режим шейдера");
        this.hitShaderSpeed.setDisplayName("Скорость шейдера");
        this.hitCustomColor.setDisplayName("Свой цвет");
        this.hitColor.setDisplayName("Цвет");
        this.jumpLabel.setDependency(() -> this.modes.isEnabled("Прыжок"));
        this.hitLabel.setDependency(() -> this.modes.isEnabled("Удар"));
        this.addSettings(
            this.modes,
            this.jumpLabel,
            this.jumpRange,
            this.jumpSpeed,
            this.jumpFill,
            this.jumpFillAlpha,
            this.jumpShaderFill,
            this.jumpShaderMode,
            this.jumpShaderSpeed,
            this.jumpCustomColor,
            this.jumpColor,
            this.hitLabel,
            this.hitRange,
            this.hitSpeed,
            this.hitFill,
            this.hitFillAlpha,
            this.hitShaderFill,
            this.hitShaderMode,
            this.hitShaderSpeed,
            this.hitCustomColor,
            this.hitColor
        );
    }

    public void onJump() {
        if (this.modes.isEnabled("Прыжок") && this.mc.player != null) {
            this.trimWaves(6);
            this.waves.add(new Wave$WaveInstance(this.mc.player.getEntityPos(), "Прыжок"));
        }
    }

    public void onAttack(Vec3d pos) {
        if (this.modes.isEnabled("Удар") && pos != null) {
            this.trimWaves(6);
            this.waves.add(new Wave$WaveInstance(pos, "Удар"));
        }
    }

    @Override
    public void onDisable() {
        this.waves.clear();
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (!this.waves.isEmpty()) {
            this.cleanupWaves();
            MatrixStack matrices = context.matrixStack();
            Vec3d cam = context.camera().getCameraPos();
            Tessellator tessellator = Tessellator.getInstance();
            if (this.hasAnyFillEnabled()) {
                if (this.hasAnyShaderFillEnabled()) {
                    this.renderShaderFill(matrices, cam);
                }

                if (this.hasAnyNormalFillEnabled()) {
                    BufferBuilder fillBuffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

                    for (Wave$WaveInstance wave : this.waves) {
                        if (this.isFillEnabled(wave.kind) && !this.isShaderFillEnabled(wave.kind)) {
                            this.renderWave(wave, fillBuffer, matrices, cam, false, 1.0F);
                        }
                    }

                    try {
                        AuraRender.drawColor(fillBuffer, AuraRender.POS_COLOR_QUADS);
                    } catch (Exception var9) {
                    }
                }
            }

            BufferBuilder lineBuffer = tessellator.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            for (Wave$WaveInstance wave : this.waves) {
                this.renderWave(wave, lineBuffer, matrices, cam, true, 1.0F);
            }

            try {
                AuraRender.drawColorLines(lineBuffer, AuraRender.POS_COLOR_LINES, 6.0F);
            } catch (Exception var8) {
            }
        }
    }

    void renderWave(Wave$WaveInstance wave, BufferBuilder buffer, MatrixStack matrices, Vec3d cameraPos, boolean outline, float alphaMultiplier) {
        if (!(wave.pos.squaredDistanceTo(cameraPos) > 9216.0)) {
            float currentRadius = this.getCurrentRadius(wave);
            double maxRange;
            if (!(currentRadius > (maxRange = this.getRange(wave.kind)))) {
                float thickness = this.getThickness(wave.kind);
                int radCeil = Math.min((int)Math.ceil(currentRadius), 42);
                BlockPos origin = BlockPos.ofFloored(wave.pos);
                Color activeColor = this.getActiveColor(wave.kind);
                float r = activeColor.getRed() / 255.0F;
                float g = activeColor.getGreen() / 255.0F;
                float b = activeColor.getBlue() / 255.0F;
                float alpha = 1.0F - currentRadius / (float)maxRange;
                if (!(alpha <= 0.0F)) {
                    float finalAlpha = (outline ? alpha : alpha * this.getFillAlpha(wave.kind)) * alphaMultiplier;
                    if (!(finalAlpha <= 0.005F)) {
                        for (int x = -radCeil; x <= radCeil; x++) {
                            for (int z = -radCeil; z <= radCeil; z++) {
                                double dist = Math.sqrt(x * x + z * z);
                                BlockPos finalPos;
                                if (!(dist < currentRadius - thickness) && !(dist > currentRadius) && (finalPos = this.findBlockPos(origin, x, z)) != null) {
                                    boolean drawNorth = !this.isSolid(finalPos.north());
                                    boolean drawSouth = !this.isSolid(finalPos.south());
                                    boolean drawWest = !this.isSolid(finalPos.west());
                                    boolean drawEast = !this.isSolid(finalPos.east());
                                    matrices.push();

                                    try {
                                        matrices.translate(finalPos.getX() - cameraPos.x, finalPos.getY() - cameraPos.y, finalPos.getZ() - cameraPos.z);
                                        Matrix4f matrix = matrices.peek().getPositionMatrix();
                                        if (outline) {
                                            this.drawOutlineTop(buffer, matrix, r, g, b, finalAlpha);
                                            if (drawNorth) {
                                                this.drawOutlineSide(buffer, matrix, 2, r, g, b, finalAlpha);
                                            }

                                            if (drawSouth) {
                                                this.drawOutlineSide(buffer, matrix, 3, r, g, b, finalAlpha);
                                            }

                                            if (drawWest) {
                                                this.drawOutlineSide(buffer, matrix, 4, r, g, b, finalAlpha);
                                            }

                                            if (drawEast) {
                                                this.drawOutlineSide(buffer, matrix, 5, r, g, b, finalAlpha);
                                            }
                                        } else {
                                            this.drawFilledBoxTop(buffer, matrix, r, g, b, finalAlpha);
                                            if (drawNorth) {
                                                this.drawFilledSide(buffer, matrix, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, r, g, b, finalAlpha);
                                            }

                                            if (drawSouth) {
                                                this.drawFilledSide(buffer, matrix, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, r, g, b, finalAlpha);
                                            }

                                            if (drawWest) {
                                                this.drawFilledSide(buffer, matrix, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, r, g, b, finalAlpha);
                                            }

                                            if (drawEast) {
                                                this.drawFilledSide(buffer, matrix, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, r, g, b, finalAlpha);
                                            }
                                        }
                                    } finally {
                                        matrices.pop();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    double getRange(String kind) {
        return "Удар".equals(kind) ? this.hitRange.getValue() : this.jumpRange.getValue();
    }

    float getSpeedFactor(String kind) {
        return "Удар".equals(kind) ? this.hitSpeed.getFloatValue() * 0.82F : this.jumpSpeed.getFloatValue();
    }

    float getThickness(String kind) {
        return "Удар".equals(kind) ? 1.15F : 2.5F;
    }

    Color getActiveColor(String kind) {
        if ("Удар".equals(kind)) {
            return this.hitCustomColor.getValue() ? this.hitColor.getColor() : new Color(ThemeManager.INSTANCE.getVisualModulesColor(), true);
        } else {
            return this.jumpCustomColor.getValue() ? this.jumpColor.getColor() : new Color(ThemeManager.INSTANCE.getVisualModulesColor(), true);
        }
    }

    boolean isFillEnabled(String kind) {
        return "Удар".equals(kind) ? this.hitFill.getValue() : this.jumpFill.getValue();
    }

    float getFillAlpha(String kind) {
        return "Удар".equals(kind) ? this.hitFillAlpha.getFloatValue() : this.jumpFillAlpha.getFloatValue();
    }

    boolean isShaderFillEnabled(String kind) {
        return "Удар".equals(kind) ? this.hitShaderFill.getValue() : this.jumpShaderFill.getValue();
    }

    String getShaderMode(String kind) {
        return "Удар".equals(kind) ? this.hitShaderMode.getValue() : this.jumpShaderMode.getValue();
    }

    float getShaderSpeed(String kind) {
        return "Удар".equals(kind) ? this.hitShaderSpeed.getFloatValue() : this.jumpShaderSpeed.getFloatValue();
    }

    boolean hasAnyFillEnabled() {
        return this.modes.isEnabled("Прыжок") && this.jumpFill.getValue() || this.modes.isEnabled("Удар") && this.hitFill.getValue();
    }

    boolean hasAnyShaderFillEnabled() {
        return this.modes.isEnabled("Прыжок") && this.jumpFill.getValue() && this.jumpShaderFill.getValue()
            || this.modes.isEnabled("Удар") && this.hitFill.getValue() && this.hitShaderFill.getValue();
    }

    boolean hasAnyNormalFillEnabled() {
        return this.modes.isEnabled("Прыжок") && this.jumpFill.getValue() && !this.jumpShaderFill.getValue()
            || this.modes.isEnabled("Удар") && this.hitFill.getValue() && !this.hitShaderFill.getValue();
    }

    void renderShaderFill(MatrixStack matrices, Vec3d cameraPos) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (Wave$WaveInstance wave : this.waves) {
            float opacity;
            if (this.isFillEnabled(wave.kind)
                && this.isShaderFillEnabled(wave.kind)
                && !((opacity = this.getWaveOpacity(wave, cameraPos) * this.getFillAlpha(wave.kind)) <= 0.005F)) {
                ArrayList<Float> vertices = new ArrayList<>();
                this.addShaderWaveFill(wave, vertices, cameraPos);
                if (!vertices.isEmpty()) {
                    BlockOutlineShaderUtil.render(
                        this.toArray(vertices),
                        4,
                        matrix,
                        opacity,
                        6.0F,
                        this.getShaderMode(wave.kind),
                        this.getShaderSpeed(wave.kind),
                        this.getShaderSeed(wave)
                    );
                }
            }
        }
    }

    void addShaderWaveFill(Wave$WaveInstance wave, List<Float> out, Vec3d cameraPos) {
        if (!(wave.pos.squaredDistanceTo(cameraPos) > 9216.0)) {
            float currentRadius = this.getCurrentRadius(wave);
            if (!(currentRadius > this.getRange(wave.kind))) {
                float thickness = this.getThickness(wave.kind);
                int radCeil = Math.min((int)Math.ceil(currentRadius), 42);
                BlockPos origin = BlockPos.ofFloored(wave.pos);

                for (int x = -radCeil; x <= radCeil; x++) {
                    for (int z = -radCeil; z <= radCeil; z++) {
                        double dist = Math.sqrt(x * x + z * z);
                        BlockPos finalPos;
                        if (!(dist < currentRadius - thickness) && !(dist > currentRadius) && (finalPos = this.findBlockPos(origin, x, z)) != null) {
                            double minX = finalPos.getX() - cameraPos.x;
                            double minY = finalPos.getY() - cameraPos.y;
                            double minZ = finalPos.getZ() - cameraPos.z;
                            double maxX = minX + 1.0;
                            double maxY = minY + 1.0;
                            double maxZ = minZ + 1.0;
                            this.addShaderBlock(
                                out,
                                minX,
                                minY,
                                minZ,
                                maxX,
                                maxY,
                                maxZ,
                                !this.isSolid(finalPos.north()),
                                !this.isSolid(finalPos.south()),
                                !this.isSolid(finalPos.west()),
                                !this.isSolid(finalPos.east())
                            );
                        }
                    }
                }
            }
        }
    }

    float getWaveOpacity(Wave$WaveInstance wave, Vec3d cameraPos) {
        if (wave.pos.squaredDistanceTo(cameraPos) > 9216.0) {
            return 0.0F;
        }

        float currentRadius = this.getCurrentRadius(wave);
        double maxRange;
        return currentRadius > (maxRange = this.getRange(wave.kind)) ? 0.0F : Math.max(0.0F, 1.0F - currentRadius / (float)maxRange);
    }

    float getCurrentRadius(Wave$WaveInstance wave) {
        long age = System.currentTimeMillis() - wave.creationTime;
        return (float)age * 0.02F * this.getSpeedFactor(wave.kind);
    }

    float getShaderSeed(Wave$WaveInstance wave) {
        double value = wave.pos.x * 31.0 + wave.pos.y * 17.0 + wave.pos.z * 13.0;
        return (float)(value - Math.floor(value / 997.0) * 997.0);
    }

    void addShaderBlock(
        List<Float> out, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, boolean north, boolean south, boolean west, boolean east
    ) {
        double topY = maxY + 0.001;
        this.addShaderFace(out, minX, minY, minZ, maxX, topY, maxZ, minX, topY, minZ, minX, topY, maxZ, maxX, topY, maxZ, maxX, topY, minZ);
        if (north) {
            this.addShaderFace(out, minX, minY, minZ, maxX, maxY, maxZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ);
        }

        if (south) {
            this.addShaderFace(out, minX, minY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ);
        }

        if (west) {
            this.addShaderFace(out, minX, minY, minZ, maxX, maxY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ);
        }

        if (east) {
            this.addShaderFace(out, minX, minY, minZ, maxX, maxY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ);
        }
    }

    void addShaderFace(
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

    void addShaderPoint(List<Float> out, double x, double y, double z, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        out.add((float)x);
        out.add((float)y);
        out.add((float)z);
        out.add(this.normalizeLocal(x, minX, maxX));
        out.add(this.normalizeLocal(y, minY, maxY));
        out.add(this.normalizeLocal(z, minZ, maxZ));
    }

    float normalizeLocal(double value, double min, double max) {
        double size = max - min;
        return Math.abs(size) < 1.0E-6 ? 0.0F : (float)((value - min) / size);
    }

    float[] toArray(List<Float> values) {
        float[] result = new float[values.size()];

        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }

        return result;
    }

    BlockPos findBlockPos(BlockPos origin, int x, int z) {
        int scanRange = 18;

        for (int dy = 0; dy <= scanRange; dy++) {
            BlockPos checkDown = origin.add(x, -1 - dy, z);
            if (this.isSolid(checkDown) && !this.isSolid(checkDown.up())) {
                return checkDown;
            }

            BlockPos checkUp;
            if (dy > 0 && this.isSolid(checkUp = origin.add(x, -1 + dy, z)) && !this.isSolid(checkUp.up())) {
                return checkUp;
            }
        }

        return null;
    }

    void drawFilledBoxTop(BufferBuilder buffer, Matrix4f mat, float r, float g, float b, float a) {
        buffer.vertex(mat, 0.0F, 1.001F, 0.0F).color(r, g, b, a);
        buffer.vertex(mat, 0.0F, 1.001F, 1.0F).color(r, g, b, a);
        buffer.vertex(mat, 1.0F, 1.001F, 1.0F).color(r, g, b, a);
        buffer.vertex(mat, 1.0F, 1.001F, 0.0F).color(r, g, b, a);
    }

    void drawFilledSide(BufferBuilder buffer, Matrix4f mat, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        if (x1 == x2) {
            buffer.vertex(mat, x1, y1, z2).color(r, g, b, a);
            buffer.vertex(mat, x1, y2, z2).color(r, g, b, a);
            buffer.vertex(mat, x1, y2, z1).color(r, g, b, a);
            buffer.vertex(mat, x1, y1, z1).color(r, g, b, a);
        } else if (z1 == z2) {
            buffer.vertex(mat, x1, y1, z1).color(r, g, b, a);
            buffer.vertex(mat, x1, y2, z1).color(r, g, b, a);
            buffer.vertex(mat, x2, y2, z1).color(r, g, b, a);
            buffer.vertex(mat, x2, y1, z1).color(r, g, b, a);
        }
    }

    void drawOutlineTop(BufferBuilder buffer, Matrix4f mat, float r, float g, float b, float a) {
        buffer.vertex(mat, 0.0F, 1.0F, 0.0F).color(r, g, b, a);
        buffer.vertex(mat, 1.0F, 1.0F, 0.0F).color(r, g, b, a);
        buffer.vertex(mat, 1.0F, 1.0F, 0.0F).color(r, g, b, a);
        buffer.vertex(mat, 1.0F, 1.0F, 1.0F).color(r, g, b, a);
        buffer.vertex(mat, 1.0F, 1.0F, 1.0F).color(r, g, b, a);
        buffer.vertex(mat, 0.0F, 1.0F, 1.0F).color(r, g, b, a);
        buffer.vertex(mat, 0.0F, 1.0F, 1.0F).color(r, g, b, a);
        buffer.vertex(mat, 0.0F, 1.0F, 0.0F).color(r, g, b, a);
    }

    void drawOutlineSide(BufferBuilder buffer, Matrix4f mat, int side, float r, float g, float b, float a) {
        if (side == 2) {
            buffer.vertex(mat, 0.0F, 1.0F, 0.0F).color(r, g, b, a);
            buffer.vertex(mat, 0.0F, 0.0F, 0.0F).color(r, g, b, a);
            buffer.vertex(mat, 0.0F, 0.0F, 0.0F).color(r, g, b, a);
            buffer.vertex(mat, 1.0F, 0.0F, 0.0F).color(r, g, b, a);
            buffer.vertex(mat, 1.0F, 0.0F, 0.0F).color(r, g, b, a);
            buffer.vertex(mat, 1.0F, 1.0F, 0.0F).color(r, g, b, a);
        } else if (side == 3) {
            buffer.vertex(mat, 0.0F, 1.0F, 1.0F).color(r, g, b, a);
            buffer.vertex(mat, 0.0F, 0.0F, 1.0F).color(r, g, b, a);
            buffer.vertex(mat, 0.0F, 0.0F, 1.0F).color(r, g, b, a);
            buffer.vertex(mat, 1.0F, 0.0F, 1.0F).color(r, g, b, a);
            buffer.vertex(mat, 1.0F, 0.0F, 1.0F).color(r, g, b, a);
            buffer.vertex(mat, 1.0F, 1.0F, 1.0F).color(r, g, b, a);
        } else if (side == 4) {
            buffer.vertex(mat, 0.0F, 1.0F, 1.0F).color(r, g, b, a);
            buffer.vertex(mat, 0.0F, 0.0F, 1.0F).color(r, g, b, a);
            buffer.vertex(mat, 0.0F, 0.0F, 1.0F).color(r, g, b, a);
            buffer.vertex(mat, 0.0F, 0.0F, 0.0F).color(r, g, b, a);
            buffer.vertex(mat, 0.0F, 0.0F, 0.0F).color(r, g, b, a);
            buffer.vertex(mat, 0.0F, 1.0F, 0.0F).color(r, g, b, a);
        } else if (side == 5) {
            buffer.vertex(mat, 1.0F, 1.0F, 1.0F).color(r, g, b, a);
            buffer.vertex(mat, 1.0F, 0.0F, 1.0F).color(r, g, b, a);
            buffer.vertex(mat, 1.0F, 0.0F, 1.0F).color(r, g, b, a);
            buffer.vertex(mat, 1.0F, 0.0F, 0.0F).color(r, g, b, a);
            buffer.vertex(mat, 1.0F, 0.0F, 0.0F).color(r, g, b, a);
            buffer.vertex(mat, 1.0F, 1.0F, 0.0F).color(r, g, b, a);
        }
    }

    boolean isSolid(BlockPos pos) {
        if (this.mc.world == null) {
            return false;
        } else {
            BlockState state = this.mc.world.getBlockState(pos);
            if (state.isAir()) {
                return false;
            } else {
                return state.getBlock() instanceof LeavesBlock ? false : state.isFullCube(this.mc.world, pos) && state.isOpaqueFullCube();
            }
        }
    }

    void cleanupWaves() {
        this.waves.removeIf(wave -> {
            long age = System.currentTimeMillis() - wave.creationTime;
            float radius = (float)age * 0.02F * this.getSpeedFactor(wave.kind);
            return radius > this.getRange(wave.kind) + 5.0;
        });
    }

    void trimWaves(int max) {
        while (this.waves.size() >= max) {
            this.waves.remove(0);
        }
    }
}
