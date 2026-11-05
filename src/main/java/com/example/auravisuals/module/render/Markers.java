package com.example.auravisuals.module.render;

import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.client.gui.font.icon.TtfIconFonts;
import com.example.auravisuals.client.gui.font.icon.TtfWorldIcon;
import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.util.language.LanguageManager;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class Markers extends Module {
    public static final char[] MARK_ICON_CHARS = new char[]{'A', 'm', 'o', 'q', 'r', 't', '[', ']', '<', '^', '='};
    static final Identifier FALLBACK_MARKER_TEXTURE = Identifier.of("auravisuals", "textures/metka.png");
    static final float BASE_SCALE = 0.055F;
    static final float MAX_RENDER_DIST = 110.0F;
    static final float MIN_SCALE_DISTANCE = 6.0F;
    static final float SCREEN_SIZE_MULTIPLIER = 0.861F;
    static final float WORLD_MARKER_SIZE_MULTIPLIER = 1.05F;
    static final float WORLD_MARKER_TEXT_MULTIPLIER = 1.05F;
    static final int CORNER_SEGMENTS = 6;
    public static final ColorSetting iconBackgroundColor = new ColorSetting("Фон иконок", new Color(16, 36, 82));
    final List<Markers$MarkerData> markers = new ArrayList<>();

    public Markers() {
        super("Markers", "Пользовательские метки", Category.MARKS);
        this.addSettings(iconBackgroundColor);
        this.setEnabledSilently(true);
    }

    public List<Markers$MarkerData> getMarkers() {
        return this.markers;
    }

    public void addMarker(Markers$MarkerData marker) {
        if (marker != null) {
            this.markers.add(marker);
        }
    }

    public void setMarkers(List<Markers$MarkerData> newMarkers) {
        this.markers.clear();
        if (newMarkers != null) {
            this.markers.addAll(newMarkers);
        }
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (!this.markers.isEmpty() && this.mc.player != null && this.mc.world != null) {
            Vec3d cameraPos = context.camera().getCameraPos();
            MatrixStack matrices = context.matrixStack();

            for (Markers$MarkerData marker : this.markers) {
                Vec3d markerPos = new Vec3d(marker.x, marker.y, marker.z);
                double dx = markerPos.x - cameraPos.x;
                double dy = markerPos.y - cameraPos.y;
                double dz = markerPos.z - cameraPos.z;
                float distance = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (distance < 0.5F) {
                    distance = 0.5F;
                }

                double renderDx = dx;
                double renderDy = dy;
                double renderDz = dz;
                float renderDistance = Math.min(distance, 110.0F);
                if (distance > 110.0F) {
                    double ratio = 110.0F / distance;
                    renderDx = dx * ratio;
                    renderDy = dy * ratio;
                    renderDz = dz * ratio;
                }

                float scaleDistance = Math.max(6.0F, renderDistance);
                float distanceScale = scaleDistance * 0.055F;
                float sizeMultiplier = 1.05F + (marker.size - 1.0F) * 1.25F;
                float pixelScale = 0.019F * distanceScale * sizeMultiplier * 0.861F * 1.05F;
                matrices.push();

                try {
                    matrices.translate(renderDx, renderDy + 0.5 * distanceScale, renderDz);
                    matrices.multiply(context.camera().getRotation());
                    matrices.scale(pixelScale, -pixelScale, pixelScale);
                    String distanceText = Math.round(distance) + LanguageManager.translateForCategory(Category.MARKS, " м");
                    int nameWidth = FontManager.getWidth(marker.name);
                    int distWidth = FontManager.getWidth(distanceText);
                    int textHeight = FontManager.getHeight();
                    float textScale = 1.1445F;
                    int textWidth = Math.round(Math.max(nameWidth, distWidth) * textScale);
                    float iconSize = 36.0F;
                    float padding = 3.0F;
                    float textSpacing = 1.0F;
                    float contentHeight = textHeight * textScale * 2.0F + textSpacing;
                    float totalContentH = Math.max(iconSize, contentHeight);
                    float rectHeight = totalContentH + padding * 2.0F;
                    float rectWidth = padding * 2.0F + iconSize + 5.0F + textWidth + 6.0F;
                    float rectX = -rectWidth / 2.0F;
                    float rectY = -rectHeight / 2.0F;
                    float radius = 4.0F;
                    int bgColor = opaqueColor(marker.backgroundColor);
                    this.drawRoundedGradientRect(matrices, rectX, rectY, rectWidth, rectHeight, radius, bgColor);
                    float iconX = rectX + padding;
                    float iconY = rectY + (rectHeight - iconSize) / 2.0F;
                    float iconCenterX = iconX + iconSize / 2.0F;
                    float iconCenterY = iconY + iconSize / 2.0F;
                    float iconBgSize = iconSize * 0.728F;
                    this.drawRoundedGradientRect(
                        matrices,
                        iconCenterX - iconBgSize / 2.0F,
                        iconCenterY - iconBgSize / 2.0F,
                        iconBgSize,
                        iconBgSize,
                        3.0F,
                        getIconBackgroundGradientTop(marker.getIconBackgroundColor()),
                        getIconBackgroundGradientBottom(marker.getIconBackgroundColor())
                    );
                    int ir = marker.iconColor >> 16 & 0xFF;
                    int ig = marker.iconColor >> 8 & 0xFF;
                    int ib = marker.iconColor & 0xFF;
                    int ia = 255;
                    float glyphSize = iconSize * 0.92F * 1.5F * 0.7125F;
                    if (TtfIconFonts.hasIcons()) {
                        matrices.push();
                        matrices.translate(iconCenterX, iconCenterY, 0.0F);
                        TtfWorldIcon.drawIconChar(matrices, marker.getIconChar(), ir, ig, ib, ia, glyphSize);
                        matrices.pop();
                    } else {
                        float drawSz = glyphSize;
                        float offF = (iconSize - drawSz) * 0.5F;
                        float fx = iconX + offF;
                        float fy = iconY + offF;
                        Tessellator tessellator = Tessellator.getInstance();
                        BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                        Matrix4f matrix = matrices.peek().getPositionMatrix();
                        buffer.vertex(matrix, fx, fy + drawSz, 0.0F).texture(0.0F, 1.0F).color(ir, ig, ib, ia);
                        buffer.vertex(matrix, fx, fy, 0.0F).texture(0.0F, 0.0F).color(ir, ig, ib, ia);
                        buffer.vertex(matrix, fx + drawSz, fy, 0.0F).texture(1.0F, 0.0F).color(ir, ig, ib, ia);
                        buffer.vertex(matrix, fx + drawSz, fy + drawSz, 0.0F).texture(1.0F, 1.0F).color(ir, ig, ib, ia);
                        AuraRender.drawTextured(buffer, AuraRender.POS_TEX_COLOR_QUADS_NODEPTH, FALLBACK_MARKER_TEXTURE);
                    }

                    int nameColor = opaqueColor(marker.textColor);
                    int distColor = marker.textColor & 16777215 | -1442840576;
                    float textX = iconX + iconSize + 1.5F;
                    float nameY = rectY + (rectHeight - contentHeight) / 2.0F - 1.0F;
                    float lineStep = textHeight * textScale + textSpacing;
                    matrices.push();

                    try {
                        float blockCx = textX;
                        float blockCy = nameY + contentHeight / 2.0F;
                        matrices.translate(blockCx, blockCy, 0.0F);
                        matrices.scale(textScale, textScale, 1.0F);
                        matrices.translate(-blockCx, -blockCy, 0.0F);
                        this.drawText(matrices, marker.name, textX, nameY, nameColor);
                        this.drawText(matrices, distanceText, textX, nameY + lineStep, distColor);
                    } finally {
                        matrices.pop();
                    }
                } finally {
                    matrices.pop();
                }
            }
        }
    }

    void renderBeam(MatrixStack matrices, double dx, double dy, double dz, Markers$MarkerData marker, float pixelScale) {
        int bgColor = marker.backgroundColor;
        float r = (bgColor >> 16 & 0xFF) / 255.0F;
        float g = (bgColor >> 8 & 0xFF) / 255.0F;
        float b = (bgColor & 0xFF) / 255.0F;
        float alpha = 0.4F;
        float beamWidth = pixelScale * 1.5F;
        float beamHeight = pixelScale * 30.0F;
        matrices.push();

        try {
            matrices.translate(dx, dy - beamHeight * 0.3F, dz);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float hw = beamWidth / 2.0F;
            buffer.vertex(matrix, -hw, 0.0F, -hw).color(r, g, b, alpha);
            buffer.vertex(matrix, hw, 0.0F, -hw).color(r, g, b, alpha);
            buffer.vertex(matrix, hw, beamHeight, -hw).color(r, g, b, 0.0F);
            buffer.vertex(matrix, -hw, beamHeight, -hw).color(r, g, b, 0.0F);
            buffer.vertex(matrix, hw, 0.0F, hw).color(r, g, b, alpha);
            buffer.vertex(matrix, -hw, 0.0F, hw).color(r, g, b, alpha);
            buffer.vertex(matrix, -hw, beamHeight, hw).color(r, g, b, 0.0F);
            buffer.vertex(matrix, hw, beamHeight, hw).color(r, g, b, 0.0F);
            buffer.vertex(matrix, -hw, 0.0F, hw).color(r, g, b, alpha);
            buffer.vertex(matrix, -hw, 0.0F, -hw).color(r, g, b, alpha);
            buffer.vertex(matrix, -hw, beamHeight, -hw).color(r, g, b, 0.0F);
            buffer.vertex(matrix, -hw, beamHeight, hw).color(r, g, b, 0.0F);
            buffer.vertex(matrix, hw, 0.0F, -hw).color(r, g, b, alpha);
            buffer.vertex(matrix, hw, 0.0F, hw).color(r, g, b, alpha);
            buffer.vertex(matrix, hw, beamHeight, hw).color(r, g, b, 0.0F);
            buffer.vertex(matrix, hw, beamHeight, -hw).color(r, g, b, 0.0F);
            AuraRender.drawColor(buffer, AuraRender.POS_COLOR_QUADS_NODEPTH);
        } finally {
            matrices.pop();
        }
    }

    void drawText(MatrixStack matrices, String text, float x, float y, int color) {
        matrices.push();

        try {
            if (FontManager.customFontRenderer != null) {
                FontManager.customFontRenderer.drawString(matrices, text, x, y, color, false);
            } else {
                Immediate consumers = this.mc.getBufferBuilders().getEntityVertexConsumers();
                TextRenderer textRenderer = this.mc.textRenderer;
                Text mcText = Text.of(text);
                Matrix4f textMatrix = matrices.peek().getPositionMatrix();
                textRenderer.draw(mcText, x, y, color, false, textMatrix, consumers, TextLayerType.SEE_THROUGH, 0, 15728880);
                consumers.draw();
            }
        } finally {
            matrices.pop();
        }
    }

    void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, float radius, int color) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float r = Math.min(radius, Math.min(width, height) / 2.0F);
        float cx = x + width / 2.0F;
        float cy = y + height / 2.0F;
        buffer.vertex(matrix, cx, cy, 0.0F).color(red, green, blue, alpha);
        this.appendCorner(buffer, matrix, x + width - r, y + r, r, 0, red, green, blue, alpha);
        this.appendCorner(buffer, matrix, x + r, y + r, r, -1, red, green, blue, alpha);
        this.appendCorner(buffer, matrix, x + r, y + height - r, r, -2, red, green, blue, alpha);
        this.appendCorner(buffer, matrix, x + width - r, y + height - r, r, -3, red, green, blue, alpha);
        buffer.vertex(matrix, x + width, y + r, 0.0F).color(red, green, blue, alpha);
        AuraRender.drawColor(buffer, AuraRender.POS_COLOR_TRI_FAN_NODEPTH);
    }

    void drawRoundedGradientRect(MatrixStack matrices, float x, float y, float width, float height, float radius, int color) {
        this.drawRoundedGradientRect(matrices, x, y, width, height, radius, lighten(color, 0.18F), darken(color, 0.34F));
    }

    void drawRoundedGradientRect(MatrixStack matrices, float x, float y, float width, float height, float radius, int topColor, int bottomColor) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float r = Math.min(radius, Math.min(width, height) / 2.0F);
        float cx = x + width / 2.0F;
        float cy = y + height / 2.0F;
        this.vertexGradient(buffer, matrix, cx, cy, y, y + height, topColor, bottomColor);
        this.appendGradientCorner(buffer, matrix, x + width - r, y + r, r, 0, y, y + height, topColor, bottomColor);
        this.appendGradientCorner(buffer, matrix, x + r, y + r, r, -1, y, y + height, topColor, bottomColor);
        this.appendGradientCorner(buffer, matrix, x + r, y + height - r, r, -2, y, y + height, topColor, bottomColor);
        this.appendGradientCorner(buffer, matrix, x + width - r, y + height - r, r, -3, y, y + height, topColor, bottomColor);
        this.vertexGradient(buffer, matrix, x + width, y + r, y, y + height, topColor, bottomColor);
        AuraRender.drawColor(buffer, AuraRender.POS_COLOR_TRI_FAN_NODEPTH);
    }

    void appendCorner(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float r, int quadrant, float red, float green, float blue, float alpha) {
        for (int i = 0; i <= 6; i++) {
            double angle = quadrant * Math.PI / 2.0 - (Math.PI / 2) * i / 6.0;
            float px = cx + (float)Math.cos(angle) * r;
            float py = cy + (float)Math.sin(angle) * r;
            buffer.vertex(matrix, px, py, 0.0F).color(red, green, blue, alpha);
        }
    }

    void appendGradientCorner(
        BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float r, int quadrant, float minY, float maxY, int topColor, int bottomColor
    ) {
        for (int i = 0; i <= 6; i++) {
            double angle = quadrant * Math.PI / 2.0 - (Math.PI / 2) * i / 6.0;
            float px = cx + (float)Math.cos(angle) * r;
            float py = cy + (float)Math.sin(angle) * r;
            this.vertexGradient(buffer, matrix, px, py, minY, maxY, topColor, bottomColor);
        }
    }

    void vertexGradient(BufferBuilder buffer, Matrix4f matrix, float x, float y, float minY, float maxY, int topColor, int bottomColor) {
        float t = (y - minY) / Math.max(1.0F, maxY - minY);
        t = Math.max(0.0F, Math.min(1.0F, t));
        int shaded = blendColor(topColor, bottomColor, t);
        float red = (shaded >> 16 & 0xFF) / 255.0F;
        float green = (shaded >> 8 & 0xFF) / 255.0F;
        float blue = (shaded & 0xFF) / 255.0F;
        buffer.vertex(matrix, x, y, 0.0F).color(red, green, blue, 1.0F);
    }

    static int blendColor(int from, int to, float progress) {
        int fr = from >> 16 & 0xFF;
        int fg = from >> 8 & 0xFF;
        int fb = from & 0xFF;
        int tr = to >> 16 & 0xFF;
        int tg = to >> 8 & 0xFF;
        int tb = to & 0xFF;
        int r = Math.round(fr + (tr - fr) * progress);
        int g = Math.round(fg + (tg - fg) * progress);
        int b = Math.round(fb + (tb - fb) * progress);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    static int lighten(int color, float amount) {
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        r = Math.round(r + (255 - r) * amount);
        g = Math.round(g + (255 - g) * amount);
        b = Math.round(b + (255 - b) * amount);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    static int darken(int color, float amount) {
        int r = Math.round((color >> 16 & 0xFF) * (1.0F - amount));
        int g = Math.round((color >> 8 & 0xFF) * (1.0F - amount));
        int b = Math.round((color & 0xFF) * (1.0F - amount));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    public static int opaqueColor(int color) {
        return color & 16777215 | 0xFF000000;
    }

    public static int getIconBackgroundColor() {
        return opaqueColor(iconBackgroundColor.getValue());
    }

    public static int getIconBackgroundGradientTop() {
        return getIconBackgroundGradientTop(getIconBackgroundColor());
    }

    public static int getIconBackgroundGradientBottom() {
        return getIconBackgroundGradientBottom(getIconBackgroundColor());
    }

    public static int getIconBackgroundGradientTop(int color) {
        return lighten(opaqueColor(color), 0.3F);
    }

    public static int getIconBackgroundGradientBottom(int color) {
        return darken(opaqueColor(color), 0.5F);
    }

    public static char migrateIconFromStored(String stored) {
        if (stored != null && !stored.isEmpty()) {
            if (stored.length() == 1) {
                return stored.charAt(0);
            }

            return switch (stored) {
                case "textures/metka.png" -> 'A';
                case "textures/metkadom.png" -> 'm';
                case "textures/metkaplayer.png" -> 'o';
                case "textures/metkatnt.png" -> 'q';
                case "textures/metkachest.png" -> 'r';
                case "textures/settings.png" -> 't';
                default -> 'A';
            };
        } else {
            return 'A';
        }
    }
}
