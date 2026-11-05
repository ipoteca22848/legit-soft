package com.example.auravisuals.module.render;

import com.example.auravisuals.client.gui.render.RoundedRenderer;
import com.example.auravisuals.compat.ProjectionHolder;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.render.MirageGlassPipeline;
import com.example.auravisuals.util.render.WorldOcclusion;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class SpeechBubbles extends Module {
    static final Map<UUID, SpeechBubbles$Bubble> BUBBLES = new ConcurrentHashMap<>();
    public final NumberSetting duration = new NumberSetting("Длительность (с)", 5.0, 2.0, 15.0, 0.5);
    public final NumberSetting size = new NumberSetting("Размер", 1.0, 0.5, 2.5, 0.05);
    public final NumberSetting maxWidth = new NumberSetting("Ширина строки", 120.0, 60.0, 220.0, 10.0);
    public final NumberSetting heightOffset = new NumberSetting("Смещение вверх", 0.55, 0.0, 2.0, 0.05);
    public final NumberSetting visibilityDistance = new NumberSetting("Дистанция видимости", 25.0, 4.0, 48.0, 1.0);
    public final BooleanSetting showOwn = new BooleanSetting("Своё сообщение", true);
    public final BooleanSetting background = new BooleanSetting("Фон", true);
    public final ColorSetting glassColor = new SpeechBubbles$1(this, "Цвет стекла", new Color(120, 160, 255, 90), this.background::getValue);
    public final ColorSetting textColor = new ColorSetting("Цвет текста", new Color(255, 255, 255));

    public SpeechBubbles() {
        super("SpeechBubbles", "Сообщения из чата облачком над головой", Category.VISUALS);
        this.addSettings(this.duration, this.size, this.maxWidth, this.heightOffset, this.showOwn, this.background, this.glassColor, this.textColor);
    }

    public static void ingest(String raw) {
        SpeechBubbles module = ModuleManager.INSTANCE == null ? null : ModuleManager.INSTANCE.getModule(SpeechBubbles.class);
        if (module != null && module.isEnabled()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world != null && raw != null) {
                String stripped = Formatting.strip(raw);
                if (stripped != null && !stripped.isEmpty()) {
                    String lower = stripped.toLowerCase(Locale.ROOT);
                    PlayerEntity best = null;
                    int bestIndex = Integer.MAX_VALUE;
                    int bestNameEnd = 0;

                    for (PlayerEntity player : mc.world.getPlayers()) {
                        String name = player.getName().getString();
                        int index2;
                        if (name != null
                            && name.length() >= 2
                            && (index2 = lower.indexOf(name.toLowerCase(Locale.ROOT))) >= 0
                            && index2 < 40
                            && index2 < bestIndex) {
                            bestIndex = index2;
                            best = player;
                            bestNameEnd = index2 + name.length();
                        }
                    }

                    if (best != null) {
                        String body = stripped.substring(bestNameEnd);
                        int index = 0;

                        while (index < body.length() && (Character.isWhitespace(body.charAt(index)) || ">:»|-›".indexOf(body.charAt(index)) >= 0)) {
                            index++;
                        }

                        if (!(body = body.substring(index).trim()).isEmpty() && body.length() <= 128) {
                            if (best != mc.player || module.showOwn.getValue()) {
                                BUBBLES.put(best.getUuid(), new SpeechBubbles$Bubble(body, System.currentTimeMillis()));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        BUBBLES.clear();
    }

    @Override
    public void onHudRender(DrawContext context) {
        if (this.mc.player != null && this.mc.world != null && this.mc.textRenderer != null && !BUBBLES.isEmpty()) {
            Camera camera = this.mc.gameRenderer.getCamera();
            if (camera != null && ProjectionHolder.lastProjectionMatrix != null) {
                long now = System.currentTimeMillis();
                long lifeMs = Math.round(this.duration.getValue() * 1000.0);
                float tickDelta = this.mc.getRenderTickCounter().getTickProgress(true);
                Vec3d cameraPos = camera.getCameraPos();
                double maxDistanceSq = this.visibilityDistance.getValue() * this.visibilityDistance.getValue();
                BUBBLES.entrySet().removeIf(entry -> now - entry.getValue().createdMs > lifeMs);

                for (PlayerEntity player : this.mc.world.getPlayers()) {
                    SpeechBubbles$Bubble bubble = BUBBLES.get(player.getUuid());
                    float age;
                    float alpha;
                    if (bubble != null
                        && player.isAlive()
                        && (
                            player == this.mc.player
                                || !(player.getLerpedPos(tickDelta).add(0.0, player.getHeight() * 0.5, 0.0).squaredDistanceTo(cameraPos) > maxDistanceSq)
                                    && WorldOcclusion.isEntityVisible(player, cameraPos, tickDelta)
                        )
                        && (alpha = MathHelper.clamp(Math.min((age = (float)(now - bubble.createdMs) / (float)lifeMs) * 8.0F, (1.0F - age) * 6.0F), 0.0F, 1.0F))
                            > 0.01F) {
                        this.renderBubble(context, camera, player, bubble, alpha, tickDelta);
                    }
                }
            }
        }
    }

    void renderBubble(DrawContext context, Camera camera, PlayerEntity player, SpeechBubbles$Bubble bubble, float alpha, float tickDelta) {
        Vec3d anchor = player.getLerpedPos(tickDelta).add(0.0, player.getHeight() + this.heightOffset.getValue(), 0.0);
        float[] screen = this.project(camera, anchor);
        if (screen != null) {
            TextRenderer text = this.mc.textRenderer;
            int wrapWidth = Math.round(this.maxWidth.getFloatValue());
            List<String> lines = wrap(text, bubble.text, wrapWidth);
            int lineHeight = 10;
            int contentWidth = 0;

            for (String line : lines) {
                contentWidth = Math.max(contentWidth, text.getWidth(line));
            }

            float padding = 5.0F;
            float bubbleWidth = contentWidth + padding * 2.0F;
            float bubbleHeight = lines.size() * lineHeight + padding * 2.0F - 1.0F;
            float tail = 5.0F;
            float radius = Math.min(bubbleHeight * 0.5F, 9.0F);
            float renderScale = this.size.getFloatValue();
            int alphaByte = Math.round(alpha * 255.0F);
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(screen[0], screen[1]);
            context.getMatrices().scale(renderScale, renderScale);
            context.getMatrices().translate(-bubbleWidth / 2.0F, -bubbleHeight - tail);
            if (this.background.getValue()) {
                float centerX = bubbleWidth / 2.0F;
                Color tint = this.glassColor.getColor();
                drawTail(context, centerX, bubbleHeight, tail, Math.round(alpha * 150.0F) << 24 | tint.getRGB() & 16777215);
                MirageGlassPipeline.drawLiquidAt(context, 0.0F, 0.0F, bubbleWidth, bubbleHeight, radius, 6.0F, 3.0F, 0.55F, alpha, tint);
                RoundedRenderer.drawRoundedOutline(context, 0.0F, 0.0F, bubbleWidth, bubbleHeight, radius, 1.0F, Math.round(alpha * 110.0F) << 24 | 16777215);
            }

            int color = alphaByte << 24 | this.textColor.getValue() & 16777215;
            float y = padding;

            for (String line : lines) {
                int x = Math.round((bubbleWidth - text.getWidth(line)) / 2.0F);
                context.drawText(text, line, x, Math.round(y), color, this.background.getValue());
                y += lineHeight;
            }

            context.getMatrices().popMatrix();
        }
    }

    static void drawTail(DrawContext context, float centerX, float bottomY, float tail, int color) {
        float size = tail * 1.35F;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(centerX, bottomY);
        context.getMatrices().rotate((float) (Math.PI / 4));
        RoundedRenderer.drawRoundedRect(context, -size / 2.0F, -size / 2.0F, size, size, 1.5F, color);
        context.getMatrices().popMatrix();
    }

    static List<String> wrap(TextRenderer text, String message, int maxWidth) {
        ArrayList<String> lines = new ArrayList<>();
        String[] words = message.split(" ");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (text.getWidth(candidate) > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }

            if (lines.size() >= 4) {
                break;
            }
        }

        if (!current.isEmpty() && lines.size() < 5) {
            lines.add(current.toString());
        }

        if (lines.isEmpty()) {
            lines.add(message);
        }

        return lines;
    }

    float[] project(Camera camera, Vec3d worldPosition) {
        Vec3d cameraPosition = camera.getCameraPos();
        Quaternionf rotation = new Quaternionf(camera.getRotation());
        Vector3f right = rotation.transform(new Vector3f(1.0F, 0.0F, 0.0F));
        Vector3f up = rotation.transform(new Vector3f(0.0F, 1.0F, 0.0F));
        Vector3f forward = rotation.transform(new Vector3f(0.0F, 0.0F, -1.0F));
        Vec3d relative = worldPosition.subtract(cameraPosition);
        float viewX = (float)(relative.x * right.x + relative.y * right.y + relative.z * right.z);
        float viewY = (float)(relative.x * up.x + relative.y * up.y + relative.z * up.z);
        float depth = (float)(relative.x * forward.x + relative.y * forward.y + relative.z * forward.z);
        if (depth <= 0.05F) {
            return null;
        } else {
            Matrix4f projection = ProjectionHolder.lastProjectionMatrix;
            Vector4f clip = projection.transform(new Vector4f(viewX, viewY, -depth, 1.0F));
            if (clip.w <= 1.0E-4F) {
                return null;
            } else {
                float normalizedX = clip.x / clip.w;
                float normalizedY = clip.y / clip.w;
                if (!(normalizedX < -1.4F) && !(normalizedX > 1.4F) && !(normalizedY < -1.4F) && !(normalizedY > 1.4F)) {
                    float screenWidth = this.mc.getWindow().getScaledWidth();
                    float screenHeight = this.mc.getWindow().getScaledHeight();
                    return new float[]{(normalizedX * 0.5F + 0.5F) * screenWidth, (0.5F - normalizedY * 0.5F) * screenHeight, depth};
                } else {
                    return null;
                }
            }
        }
    }
}
