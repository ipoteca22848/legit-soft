package com.example.auravisuals.module.misc;

import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.client.gui.font.icon.TtfIconFonts;
import com.example.auravisuals.client.gui.font.icon.TtfWorldIcon;
import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BindSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.setting.StringSetting;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;

public class FastLabel extends Module {
    static final Identifier LABEL_TEXTURE = Identifier.of("auravisuals", "textures/metka.png");
    static final double MAX_DISTANCE = 70.0;
    static final float BASE_SCALE = 0.004F;
    final StringSetting lifeTime = new StringSetting("Время жизни: (сек)", "30");
    final NumberSetting opacity = new NumberSetting("Прозрачность", 255.0, 0.0, 255.0, 1.0);
    final ColorSetting color = new ColorSetting("Цвет", new Color(255, 255, 255));
    final BindSetting activateKey = new BindSetting("Кнопка активации", -1);
    FastLabel$LabelData label;
    boolean wasPressed = false;

    public FastLabel() {
        super("FastLabel", "Ставит метку", Category.MISC);
        this.addSettings(this.lifeTime, this.opacity, this.color, this.activateKey);
    }

    @Override
    public void onDisable() {
        this.label = null;
        this.wasPressed = false;
    }

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.world != null) {
            int key = this.activateKey.getValue();
            if (key != -1) {
                boolean pressed = BindSetting.isPressed(this.mc.getWindow().getHandle(), key);
                if (pressed && !this.wasPressed && !BindSetting.shouldBlockKeyInput()) {
                    this.placeLabel();
                }

                this.wasPressed = pressed;
            }
        }
    }

    void placeLabel() {
        Vec3d start = this.mc.player.getCameraPosVec(1.0F);
        RaycastContext raycastContext = new RaycastContext(
            start, start.add(this.mc.player.getRotationVec(1.0F).multiply(70.0)), ShapeType.OUTLINE, FluidHandling.NONE, this.mc.player
        );
        BlockHitResult hit = this.mc.world.raycast(raycastContext);
        if (hit != null && hit.getType() != Type.MISS) {
            this.label = new FastLabel$LabelData(hit.getPos(), System.currentTimeMillis());
        }
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.label != null && this.mc.player != null && this.mc.world != null) {
            float life = this.getLifeTimeSeconds();
            float age = (float)(System.currentTimeMillis() - this.label.createdAt) / 1000.0F;
            if (age >= life) {
                this.label = null;
            } else {
                float fade = this.getFade(age, life);
                float opacityFactor = this.opacity.getFloatValue() / 255.0F;
                int baseColor = this.color.getValue();
                int baseAlpha = baseColor >> 24 & 0xFF;
                int r = baseColor >> 16 & 0xFF;
                int g = baseColor >> 8 & 0xFF;
                int b = baseColor & 0xFF;
                int finalAlpha = (int)(baseAlpha * opacityFactor * fade);
                if (finalAlpha > 0) {
                    Vec3d cameraPos = context.camera().getCameraPos();
                    Vec3d pos = this.label.pos;
                    double x = pos.x - cameraPos.x;
                    double y = pos.y - cameraPos.y;
                    double z = pos.z - cameraPos.z;
                    MatrixStack matrices = context.matrixStack();
                    matrices.push();

                    try {
                        matrices.translate(x, y, z);
                        matrices.multiply(context.camera().getRotation());
                        float distance = (float)this.mc.player.getEntityPos().distanceTo(pos);
                        float scale = 0.004F * Math.max(distance, 3.0F);
                        matrices.scale(-scale, -scale, scale);
                        float halfSize = 16.0F;
                        if (TtfIconFonts.hasIcons()) {
                            TtfWorldIcon.drawIconChar(matrices, 'q', r, g, b, finalAlpha, halfSize * 2.0F);
                        } else {
                            Tessellator tessellator = Tessellator.getInstance();
                            BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                            Matrix4f matrix = matrices.peek().getPositionMatrix();
                            buffer.vertex(matrix, -halfSize, halfSize, 0.0F).texture(0.0F, 1.0F).color(r, g, b, finalAlpha);
                            buffer.vertex(matrix, -halfSize, -halfSize, 0.0F).texture(0.0F, 0.0F).color(r, g, b, finalAlpha);
                            buffer.vertex(matrix, halfSize, -halfSize, 0.0F).texture(1.0F, 0.0F).color(r, g, b, finalAlpha);
                            buffer.vertex(matrix, halfSize, halfSize, 0.0F).texture(1.0F, 1.0F).color(r, g, b, finalAlpha);
                            AuraRender.drawTexAuto(buffer, LABEL_TEXTURE, false, false);
                        }

                        VertexConsumerProvider consumers = context.consumers();
                        String distanceText = Math.round(distance) + "m";
                        float textWidth = FontManager.getWidth(distanceText);
                        float textX = -textWidth / 2.0F;
                        float textY = halfSize - 2.0F;
                        int textColor = finalAlpha << 24 | r << 16 | g << 8 | b;
                        matrices.push();

                        try {
                            matrices.scale(-1.0F, 1.0F, 1.0F);
                            if (FontManager.customFontRenderer != null) {
                                FontManager.customFontRenderer.drawString(matrices, distanceText, textX, textY, textColor, false);
                            } else if (consumers != null) {
                                TextRenderer textRenderer = this.mc.textRenderer;
                                Text text = Text.of(distanceText);
                                float vanillaWidth = textRenderer.getWidth(text);
                                float vanillaX = -vanillaWidth / 2.0F;
                                Matrix4f textMatrix = matrices.peek().getPositionMatrix();
                                textRenderer.draw(text, vanillaX, textY, textColor, false, textMatrix, consumers, TextLayerType.SEE_THROUGH, 0, 15728880);
                            }
                        } finally {
                            matrices.pop();
                        }
                    } finally {
                        matrices.pop();
                    }
                }
            }
        }
    }

    float getLifeTimeSeconds() {
        String value = this.lifeTime.getValue();
        if (value == null) {
            return 30.0F;
        }

        String normalized = value.replace(',', '.').trim();

        try {
            float parsed = Float.parseFloat(normalized);
            return !Float.isNaN(parsed) && !Float.isInfinite(parsed) ? Math.max(1.0F, Math.min(1000.0F, parsed)) : 30.0F;
        } catch (NumberFormatException e) {
            return 30.0F;
        }
    }

    float getFade(float age, float life) {
        float fadeDur = Math.min(0.25F, life * 0.25F);
        if (fadeDur <= 0.0F) {
            return 1.0F;
        }

        float t = age < fadeDur ? age / fadeDur : (life - age < fadeDur ? (life - age) / fadeDur : 1.0F);
        t = Math.max(0.0F, Math.min(1.0F, t));
        return t * t * (3.0F - 2.0F * t);
    }
}
