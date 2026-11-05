package com.example.auravisuals.module.render;

import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BindSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.util.ClientProfile;
import com.example.auravisuals.util.friend.FriendHubClient;
import com.example.auravisuals.util.friend.FriendHubClient$State;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class PingMarker extends Module {
    static final int SHADOW_COLOR = -1342177280;
    static final float LIFE_TIME = 8.0F;
    static final double RANGE = 120.0;
    static final int MAX_PINGS = 8;
    static final Identifier PING_MARKER_TEXTURE = Identifier.of("auravisuals", "textures/ping_marker.png");
    static final int[] PALETTE = new int[]{-1509377, -11017335, -14249, -38006, -12330497, -3703297, -28851, -7542374};
    final BindSetting pingBind = new BindSetting("Bind", BindSetting.encodeMouseButton(2));
    final ColorSetting ownColor = new ColorSetting("Color", new Color(232, 247, 255));
    final List<PingMarker$Ping> pings = new ArrayList<>();
    boolean wasPressed;
    boolean callbackInstalled;

    public PingMarker() {
        super("PingMarker", "Group middle-click ping marker", Category.MARKS);
        this.addSettings(this.pingBind, this.ownColor);
        this.setEnabledSilently(true);
    }

    @Override
    public void onDisable() {
        this.pings.clear();
        this.wasPressed = false;
    }

    @Override
    public void onTick() {
        this.installCallback();
        if (this.mc.player != null && this.mc.world != null && this.mc.getWindow() != null) {
            long now = System.currentTimeMillis();
            this.pings.removeIf(ping -> (float)(now - ping.createdAt) > 8000.0F);
            FriendHubClient hub = FriendHubClient.INSTANCE;
            if (hub.getState() == FriendHubClient$State.DISCONNECTED) {
                hub.connect(FriendHubClient.DEFAULT_URL);
            }

            boolean mouseBind = BindSetting.isMouseButtonCode(this.pingBind.getValue());
            boolean pressed = BindSetting.isPressed(this.mc.getWindow().getHandle(), this.pingBind.getValue());
            if (!mouseBind && pressed && !this.wasPressed && !BindSetting.shouldBlockKeyInput()) {
                this.placeLocalPing();
            }

            this.wasPressed = pressed;
        }
    }

    public boolean handleMouseButton(int button, int action) {
        if (!this.isEnabled() || this.mc.player == null || this.mc.world == null || action != 1) {
            return false;
        }

        if (BindSetting.shouldBlockKeyInput()) {
            return false;
        }

        if (!BindSetting.isMouseButtonCode(this.pingBind.getValue())) {
            return false;
        }

        if (BindSetting.decodeMouseButton(this.pingBind.getValue()) != button) {
            return false;
        }

        this.wasPressed = true;
        this.placeLocalPing();
        return true;
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.mc.player != null && this.mc.world != null && !this.pings.isEmpty()) {
            long now = System.currentTimeMillis();
            Vec3d cameraPos = context.camera().getCameraPos();
            MatrixStack matrices = context.matrixStack();
            Iterator<PingMarker$Ping> iterator = this.pings.iterator();

            while (iterator.hasNext()) {
                PingMarker$Ping ping = iterator.next();
                float age = (float)(now - ping.createdAt) / 1000.0F;
                if (age > 8.0F) {
                    iterator.remove();
                } else {
                    float alpha = fade(age, 8.0F);
                    if (!(alpha <= 0.01F)) {
                        this.renderGroundRing(matrices, ping, cameraPos, alpha);
                        this.renderBillboard(context, matrices, ping, cameraPos, alpha);
                    }
                }
            }
        }
    }

    void installCallback() {
        if (!this.callbackInstalled) {
            this.callbackInstalled = true;
            FriendHubClient.INSTANCE.setOnGroupPingIncoming(ping -> {
                if (this.isEnabled() && this.mc != null) {
                    this.mc.execute(() -> {
                        Vec3d pos = new Vec3d(ping.x, ping.y, ping.z);
                        String sender = ping.senderName != null && !ping.senderName.isBlank() ? ping.senderName : ping.from;
                        this.addPing(pos, pos.subtract(0.0, 1.15, 0.0), ping.label, sender, true);
                    });
                }
            });
        }
    }

    void placeLocalPing() {
        HitResult hit = this.mc.crosshairTarget;
        if (hit == null || hit.getType() == Type.MISS || hit.getPos().squaredDistanceTo(this.mc.player.getEyePos()) > 14400.0) {
            hit = this.mc.player.raycast(120.0, 1.0F, false);
        }

        if (hit != null && hit.getType() != Type.MISS) {
            Vec3d labelPos = hit.getPos();
            Vec3d groundPos = hit.getPos();
            String label = "PingMarker$Ping";
            if (hit instanceof EntityHitResult entityHit) {
                Entity entity = entityHit.getEntity();
                if (entity != null) {
                    label = entity.getName().getString();
                    groundPos = entity.getEntityPos();
                    labelPos = groundPos.add(0.0, entity.getHeight() + 0.72, 0.0);
                }
            } else {
                labelPos = groundPos.add(0.0, 1.15, 0.0);
            }

            this.placePingAt(labelPos, groundPos, label);
        } else {
            Vec3d look = this.mc.player.getRotationVec(1.0F);
            Vec3d pos = this.mc.player.getEyePos().add(look.multiply(18.0));
            this.placePingAt(pos, pos, "PingMarker$Ping");
        }
    }

    void placePingAt(Vec3d labelPos, Vec3d groundPos, String label) {
        String sender = this.localServerName();
        this.addPing(labelPos, groundPos, label, sender, true);
        FriendHubClient hub = FriendHubClient.INSTANCE;
        if (hub.getState() == FriendHubClient$State.CONNECTED) {
            hub.sendGroupPing(label, sender, labelPos.x, labelPos.y, labelPos.z);
        }
    }

    void addPing(Vec3d labelPos, Vec3d groundPos, String label, String sender, boolean sound) {
        if (labelPos != null) {
            String safeSender = sender != null && !sender.isBlank() ? sender.trim() : "Unknown";
            String safeLabel = label != null && !label.isBlank() ? label.trim() : "PingMarker$Ping";
            Vec3d safeGround = groundPos != null ? groundPos : labelPos.subtract(0.0, 1.15, 0.0);
            this.pings.add(new PingMarker$Ping(labelPos, safeGround, safeLabel, safeSender, System.currentTimeMillis()));

            while (this.pings.size() > 8) {
                this.pings.remove(0);
            }

            if (sound) {
                this.playPingSound();
            }
        }
    }

    void renderGroundRing(MatrixStack matrices, PingMarker$Ping ping, Vec3d cameraPos, float alpha) {
        Vec3d ground = ping.groundPos;
        int baseColor = this.senderColor(ping.sender);
        float pulse = 0.5F + 0.5F * MathHelper.sin((float)(System.currentTimeMillis() - ping.createdAt) / 260.0F);
        float outerRadius = 1.55F + pulse * 0.06F;
        float innerRadius = 0.58F + pulse * 0.025F;
        matrices.push();

        try {
            matrices.translate(ground.x - cameraPos.x, ground.y - cameraPos.y + 0.035, ground.z - cameraPos.z);
            this.drawNewJumpCircleBase(matrices, baseColor, outerRadius, innerRadius, alpha, (float)(System.currentTimeMillis() - ping.createdAt) / 1000.0F);
        } finally {
            matrices.pop();
        }
    }

    void renderBillboard(WorldRenderContext context, MatrixStack matrices, PingMarker$Ping ping, Vec3d cameraPos, float alpha) {
        Vec3d pos = ping.labelPos;
        double dx = pos.x - cameraPos.x;
        double dy = pos.y - cameraPos.y;
        double dz = pos.z - cameraPos.z;
        float distance = (float)this.mc.player.getEntityPos().distanceTo(pos);
        float textScale = 0.0155F * Math.max(0.85F, distance / 7.0F);
        matrices.push();

        try {
            matrices.translate(dx, dy, dz);
            matrices.multiply(context.camera().getRotation());
            matrices.scale(-textScale, -textScale, textScale);
            int markerColor = applyAlpha(this.senderColor(ping.sender), alpha);
            String distanceText = ping.sender.equalsIgnoreCase(this.localServerName())
                ? Math.round(distance) + "m"
                : ping.sender + " | " + Math.round(distance) + "m";
            this.renderMarkerTexture(matrices, markerColor);
            this.drawCentered(matrices, context.consumers(), ping.label, 9.0F, -1, 1.0F);
            this.drawCentered(matrices, context.consumers(), distanceText, 20.0F, applyAlpha(-4603692, alpha), 0.85F);
        } finally {
            matrices.pop();
        }
    }

    void drawNewJumpCircleBase(MatrixStack matrices, int color, float outerRadius, float innerRadius, float alpha, float time) {
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Color base = new Color(color, true);
        this.addNewDisc(buffer, matrix, outerRadius * 0.82F, base, this.alphaByte(alpha * 0.08F));
        float outerBand = 0.18F;
        this.addNewRingBand(buffer, matrix, outerRadius - outerBand, outerRadius, 0.0F, alpha * 0.9F, time, base, true);
        this.addNewRingBand(buffer, matrix, outerRadius, outerRadius + outerBand, alpha * 0.9F, 0.0F, time, base, true);
        float innerBand = 0.075F;
        this.addNewRingBand(buffer, matrix, innerRadius - innerBand, innerRadius, 0.0F, alpha * 0.62F, time + 0.21F, base, true);
        this.addNewRingBand(buffer, matrix, innerRadius, innerRadius + innerBand, alpha * 0.62F, 0.0F, time + 0.21F, base, true);

        for (int i = 1; i <= 2; i++) {
            float rippleRadius = outerRadius - i * outerBand * 0.55F;
            if (!(rippleRadius <= innerRadius + 0.1F)) {
                float rippleWidth = 0.018F;
                this.addNewRingBand(
                    buffer, matrix, rippleRadius - rippleWidth, rippleRadius + rippleWidth, 0.0F, alpha * (0.14F / i), time + i * 0.17F, base, true
                );
            }
        }

        AuraRender.drawColor(buffer, AuraRender.POS_COLOR_TRIS_ADD);
    }

    void addNewRingBand(
        BufferBuilder buffer, Matrix4f matrix, float innerRadius, float outerRadius, float innerAlpha, float outerAlpha, float time, Color base, boolean crest
    ) {
        if (!(outerRadius <= innerRadius) && !(outerRadius <= 0.0F)) {
            int segments = 64;

            for (int i = 0; i < segments; i++) {
                float a0 = (float)((Math.PI * 2) * i / segments);
                float a1 = (float)((Math.PI * 2) * (i + 1) / segments);
                Color c0 = this.newWaveColor(base, a0, time, crest);
                Color c1 = this.newWaveColor(base, a1, time, crest);
                float cos0 = MathHelper.cos(a0);
                float sin0 = MathHelper.sin(a0);
                float cos1 = MathHelper.cos(a1);
                float sin1 = MathHelper.sin(a1);
                this.addNewVertex(buffer, matrix, cos0 * innerRadius, 0.0F, sin0 * innerRadius, c0, this.alphaByte(innerAlpha));
                this.addNewVertex(buffer, matrix, cos0 * outerRadius, 0.0F, sin0 * outerRadius, c0, this.alphaByte(outerAlpha));
                this.addNewVertex(buffer, matrix, cos1 * outerRadius, 0.0F, sin1 * outerRadius, c1, this.alphaByte(outerAlpha));
                this.addNewVertex(buffer, matrix, cos0 * innerRadius, 0.0F, sin0 * innerRadius, c0, this.alphaByte(innerAlpha));
                this.addNewVertex(buffer, matrix, cos1 * outerRadius, 0.0F, sin1 * outerRadius, c1, this.alphaByte(outerAlpha));
                this.addNewVertex(buffer, matrix, cos1 * innerRadius, 0.0F, sin1 * innerRadius, c1, this.alphaByte(innerAlpha));
            }
        }
    }

    void addNewDisc(BufferBuilder buffer, Matrix4f matrix, float radius, Color base, int alpha) {
        if (!(radius <= 0.0F) && alpha > 0) {
            int segments = 64;
            int centerAlpha = Math.round(alpha * 0.36F);

            for (int i = 0; i < segments; i++) {
                float a0 = (float)((Math.PI * 2) * i / segments);
                float a1 = (float)((Math.PI * 2) * (i + 1) / segments);
                this.addNewVertex(buffer, matrix, 0.0F, -0.001F, 0.0F, base, centerAlpha);
                this.addNewVertex(buffer, matrix, MathHelper.cos(a0) * radius, -0.001F, MathHelper.sin(a0) * radius, base, alpha);
                this.addNewVertex(buffer, matrix, MathHelper.cos(a1) * radius, -0.001F, MathHelper.sin(a1) * radius, base, alpha);
            }
        }
    }

    void addNewVertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, Color color, int alpha) {
        buffer.vertex(matrix, x, y, z).color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    Color newWaveColor(Color base, float angle, float time, boolean crest) {
        float shimmer = 0.5F + 0.5F * MathHelper.sin(angle * 3.0F + time * 2.6F);
        float whiteMix = crest ? 0.48F + shimmer * 0.12F : shimmer * 0.08F;
        return this.blend(base, Color.WHITE, whiteMix);
    }

    Color blend(Color a, Color b, float t) {
        float clamped = MathHelper.clamp(t, 0.0F, 1.0F);
        int r = Math.round(MathHelper.lerp(clamped, a.getRed(), b.getRed()));
        int g = Math.round(MathHelper.lerp(clamped, a.getGreen(), b.getGreen()));
        int bl = Math.round(MathHelper.lerp(clamped, a.getBlue(), b.getBlue()));
        int al = Math.round(MathHelper.lerp(clamped, a.getAlpha(), b.getAlpha()));
        return new Color(r, g, bl, al);
    }

    void renderMarkerTexture(MatrixStack matrices, int color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float size = 22.0F;
        float y = -22.0F;
        int tint = mix(color, -1, 0.55F);
        BufferBuilder glow = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        this.markerQuad(glow, matrix, size * 1.24F, y, applyAlpha(tint, 0.34F));
        AuraRender.drawTextured(glow, AuraRender.POS_TEX_COLOR_QUADS_ADD, PING_MARKER_TEXTURE);
        BufferBuilder body = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        this.markerQuad(body, matrix, size, y, applyAlpha(tint, 0.96F));
        AuraRender.drawTextured(body, AuraRender.POS_TEX_COLOR_QUADS_NODEPTH, PING_MARKER_TEXTURE);
    }

    void markerQuad(BufferBuilder buffer, Matrix4f matrix, float size, float centerY, int color) {
        float half = size * 0.5F;
        buffer.vertex(matrix, -half, centerY + half, 0.0F).texture(0.0F, 1.0F).color(color);
        buffer.vertex(matrix, half, centerY + half, 0.0F).texture(1.0F, 1.0F).color(color);
        buffer.vertex(matrix, half, centerY - half, 0.0F).texture(1.0F, 0.0F).color(color);
        buffer.vertex(matrix, -half, centerY - half, 0.0F).texture(0.0F, 0.0F).color(color);
    }

    void playPingSound() {
        try {
            if (this.mc != null && this.mc.getSoundManager() != null) {
                this.mc.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 0.85F));
            }
        } catch (Throwable var2) {
        }
    }

    int senderColor(String sender) {
        if (sender != null && sender.equalsIgnoreCase(this.localServerName())) {
            return this.ownColor.getValue() | 0xFF000000;
        }

        String key = sender == null ? "" : sender.toLowerCase(Locale.ROOT);
        int hash = key.hashCode();
        return PALETTE[Math.floorMod(hash, PALETTE.length)];
    }

    String localServerName() {
        return ClientProfile.getUsername();
    }

    static int mix(int a, int b, float t) {
        float clamped = MathHelper.clamp(t, 0.0F, 1.0F);
        int ar = a >> 16 & 0xFF;
        int ag = a >> 8 & 0xFF;
        int ab = a & 0xFF;
        int br = b >> 16 & 0xFF;
        int bg = b >> 8 & 0xFF;
        int bb = b & 0xFF;
        int r = Math.round(MathHelper.lerp(clamped, ar, br));
        int g = Math.round(MathHelper.lerp(clamped, ag, bg));
        int bl = Math.round(MathHelper.lerp(clamped, ab, bb));
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    int alphaByte(float alpha) {
        return Math.max(0, Math.min(255, Math.round(MathHelper.clamp(alpha, 0.0F, 1.0F) * 255.0F)));
    }

    void drawCentered(MatrixStack matrices, VertexConsumerProvider consumers, String text, float y, int color, float textScale) {
        int finalShadow = applyAlpha(-1342177280, (color >>> 24 & 0xFF) / 255.0F);
        matrices.push();

        try {
            matrices.scale(textScale, textScale, 1.0F);
            float scaledY = y / textScale;
            if (FontManager.customFontRenderer != null) {
                float width = FontManager.getWidth(text);
                FontManager.customFontRenderer.drawString(matrices, text, -width / 2.0F + 1.0F, scaledY + 1.0F, finalShadow, false);
                FontManager.customFontRenderer.drawString(matrices, text, -width / 2.0F, scaledY, color, false);
                return;
            }

            if (consumers != null && this.mc.textRenderer != null) {
                TextRenderer renderer = this.mc.textRenderer;
                Text mcText = Text.of(text);
                Matrix4f matrix = matrices.peek().getPositionMatrix();
                float x = -renderer.getWidth(mcText) / 2.0F;
                renderer.draw(mcText, x + 1.0F, scaledY + 1.0F, finalShadow, false, matrix, consumers, TextLayerType.SEE_THROUGH, 0, 15728880);
                renderer.draw(mcText, x, scaledY, color, false, matrix, consumers, TextLayerType.SEE_THROUGH, 0, 15728880);
                return;
            }
        } finally {
            matrices.pop();
        }
    }

    static float fade(float age, float life) {
        float in = MathHelper.clamp(age / 0.16F, 0.0F, 1.0F);
        float out = MathHelper.clamp((life - age) / 0.45F, 0.0F, 1.0F);
        return smooth(in) * smooth(out);
    }

    static float smooth(float value) {
        float t = MathHelper.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    static int applyAlpha(int color, float alpha) {
        int baseAlpha = color >>> 24 & 0xFF;
        if (baseAlpha == 0) {
            baseAlpha = 255;
        }

        int outAlpha = MathHelper.clamp(Math.round(baseAlpha * MathHelper.clamp(alpha, 0.0F, 1.0F)), 0, 255);
        return outAlpha << 24 | color & 16777215;
    }
}
