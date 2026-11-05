package com.example.auravisuals.module.render;

import com.example.auravisuals.client.gui.FriendLabelsScreen;
import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.client.gui.font.icon.TtfIconFonts;
import com.example.auravisuals.client.gui.font.icon.TtfWorldIcon;
import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.ActionSetting;
import com.example.auravisuals.setting.BindSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.util.friendlabels.FriendLabelsClient;
import com.example.auravisuals.util.friendlabels.FriendLabelsClient$LabelMessage;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class FriendLabels extends Module {
    static final long LIFE_MS = 8000L;
    static final long RATE_WINDOW_MS = 15000L;
    static final int RATE_LIMIT = 7;
    static final double RANGE = 70.0;
    static final Identifier ICON = Identifier.of("auravisuals", "textures/metka.png");
    final ActionSetting openPanel = new ActionSetting("Открыть панель меток", this::openPanel);
    final BindSetting labelBind = new BindSetting("Поставить метку", BindSetting.encodeMouseButton(2), () -> false);
    final ColorSetting labelColor = new ColorSetting("Цвет метки", Color.WHITE, () -> false);
    final List<FriendLabels$Label> labels = new ArrayList<>();
    final ArrayDeque<Long> sentTimes = new ArrayDeque<>();
    boolean wasPressed;

    public FriendLabels() {
        super("FriendLabels", "Общие метки для друзей", Category.VISUALS);
        this.addSettings(this.openPanel, this.labelBind, this.labelColor);
    }

    public BindSetting getLabelBind() {
        return this.labelBind;
    }

    public ColorSetting getLabelColor() {
        return this.labelColor;
    }

    public FriendLabelsClient getClient() {
        return FriendLabelsClient.INSTANCE;
    }

    void openPanel() {
        this.mc.setScreen(new FriendLabelsScreen(this.mc.currentScreen, this));
    }

    @Override
    public void onEnable() {
        FriendLabelsClient.INSTANCE.connect();
    }

    @Override
    public void onDisable() {
        FriendLabelsClient.INSTANCE.disconnect();
        this.labels.clear();
        this.sentTimes.clear();
        this.wasPressed = false;
    }

    @Override
    public void onTick() {
        FriendLabelsClient hub = FriendLabelsClient.INSTANCE;
        hub.ensureConnectedIdentity();

        FriendLabelsClient$LabelMessage incoming;
        while ((incoming = hub.pollLabel()) != null) {
            this.receive(incoming);
        }

        long now = System.currentTimeMillis();
        this.labels.removeIf(label -> now - label.createdAt > 8000L);
        if (this.mc.player != null && this.mc.world != null && this.mc.getWindow() != null) {
            boolean pressed = BindSetting.isPressed(this.mc.getWindow().getHandle(), this.labelBind.getValue());
            if (pressed && !this.wasPressed && !BindSetting.shouldBlockKeyInput()) {
                this.placeLabel();
            }

            this.wasPressed = pressed;
        }
    }

    void receive(FriendLabelsClient$LabelMessage data) {
        if (this.mc.world != null) {
            if (data.dimension().isBlank() || data.dimension().equals(this.dimension())) {
                this.addLabel(
                    new FriendLabels$Label(
                        data.sender(), new Vec3d(data.x(), data.y(), data.z()), data.color(), data.targetUuid(), data.targetName(), System.currentTimeMillis()
                    )
                );

                while (this.labels.size() > 64) {
                    this.labels.remove(0);
                }
            }
        }
    }

    void placeLabel() {
        long now = System.currentTimeMillis();

        while (!this.sentTimes.isEmpty() && now - this.sentTimes.peekFirst() > 15000L) {
            this.sentTimes.removeFirst();
        }

        if (this.sentTimes.size() < 7 && this.mc.player != null && this.mc.world != null) {
            HitResult hit = this.mc.crosshairTarget;
            if (hit == null || hit.getType() == Type.MISS || hit.getPos().squaredDistanceTo(this.mc.player.getEyePos()) > 4900.0) {
                hit = this.mc.player.raycast(70.0, 1.0F, false);
            }

            if (hit != null && hit.getType() != Type.MISS) {
                String uuid = "";
                String targetName = "";
                Vec3d pos = hit.getPos();
                EntityHitResult entityHit;
                Entity class_12972;
                if (hit instanceof EntityHitResult && (class_12972 = (entityHit = (EntityHitResult)hit).getEntity()) instanceof PlayerEntity) {
                    PlayerEntity player = (PlayerEntity)class_12972;
                    uuid = player.getUuidAsString();
                    targetName = player.getName().getString();
                    pos = player.getEntityPos();
                }

                String sender = this.mc.player.getName().getString();
                int color = this.labelColor.getValue() | 0xFF000000;
                this.addLabel(new FriendLabels$Label(sender, pos, color, uuid, targetName, now));
                this.sentTimes.addLast(now);
                FriendLabelsClient.INSTANCE.sendLabel(pos.x, pos.y, pos.z, color, this.dimension(), uuid, targetName);
            }
        }
    }

    void addLabel(FriendLabels$Label label) {
        if (label != null) {
            if (!label.targetUuid.isBlank()) {
                this.labels.removeIf(previous -> !previous.targetUuid.isBlank() && previous.targetUuid.equalsIgnoreCase(label.targetUuid));
            } else if (!label.targetName.isBlank()) {
                this.labels
                    .removeIf(
                        previous -> previous.targetUuid.isBlank() && !previous.targetName.isBlank() && previous.targetName.equalsIgnoreCase(label.targetName)
                    );
            }

            this.labels.add(label);
        }
    }

    String dimension() {
        return this.mc.world == null ? "" : this.mc.world.getRegistryKey().getValue().toString();
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.mc.player != null && this.mc.world != null) {
            long now = System.currentTimeMillis();
            Iterator<FriendLabels$Label> iterator = this.labels.iterator();

            while (iterator.hasNext()) {
                FriendLabels$Label label = iterator.next();
                float age = (float)(now - label.createdAt) / 1000.0F;
                if (age >= 8.0F) {
                    iterator.remove();
                } else {
                    float alpha = fade(age);
                    Entity target = this.findTarget(label);
                    Vec3d anchor = target == null ? label.position : target.getEntityPos();
                    if (target != null || label.targetUuid.isBlank()) {
                        if (target == null) {
                            this.renderCircle(context.matrixStack(), context.camera().getCameraPos(), anchor, label.color, alpha);
                        } else {
                            this.renderSkeleton(context.matrixStack(), context.camera().getCameraPos(), target, label.color, alpha);
                        }

                        double top = target == null ? 1.18 : target.getHeight() + 0.72;
                        this.renderBillboard(context, anchor.add(0.0, top, 0.0), label, label.color, alpha);
                    }
                }
            }
        }
    }

    Entity findTarget(FriendLabels$Label label) {
        if (!label.targetUuid.isBlank() && this.mc.world != null) {
            try {
                UUID wanted = UUID.fromString(label.targetUuid);

                for (PlayerEntity player : this.mc.world.getPlayers()) {
                    if (player.getUuid().equals(wanted)) {
                        return player;
                    }
                }
            } catch (Exception var5) {
            }

            for (PlayerEntity player : this.mc.world.getPlayers()) {
                if (player.getName().getString().equalsIgnoreCase(label.targetName)) {
                    return player;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    void renderCircle(MatrixStack matrices, Vec3d camera, Vec3d pos, int color, float alpha) {
        Color base = new Color(color, true);
        matrices.push();
        matrices.translate(pos.x - camera.x, pos.y - camera.y + 0.035, pos.z - camera.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        innerGlow(buffer, matrix, 0.62F, base, alpha * 0.18F);
        ring(buffer, matrix, 0.62F, 0.79F, base, alpha * 0.95F);
        AuraRender.drawColor(buffer, AuraRender.POS_COLOR_TRIS_ADD);
        matrices.pop();
    }

    static void innerGlow(BufferBuilder buffer, Matrix4f matrix, float radius, Color color, float alpha) {
        int centerAlpha = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        int edgeAlpha = Math.round(centerAlpha * 0.12F);

        for (int i = 0; i < 64; i++) {
            float p0 = (float) (Math.PI * 2) * i / 64.0F;
            float p1 = (float) (Math.PI * 2) * (i + 1) / 64.0F;
            vertex(buffer, matrix, 0.0F, 0.0F, color, centerAlpha);
            vertex(buffer, matrix, MathHelper.cos(p0) * radius, MathHelper.sin(p0) * radius, color, edgeAlpha);
            vertex(buffer, matrix, MathHelper.cos(p1) * radius, MathHelper.sin(p1) * radius, color, edgeAlpha);
        }
    }

    static void ring(BufferBuilder buffer, Matrix4f matrix, float inner, float outer, Color color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));

        for (int i = 0; i < 64; i++) {
            float p0 = (float) (Math.PI * 2) * i / 64.0F;
            float p1 = (float) (Math.PI * 2) * (i + 1) / 64.0F;
            float c0 = MathHelper.cos(p0);
            float s0 = MathHelper.sin(p0);
            float c1 = MathHelper.cos(p1);
            float s1 = MathHelper.sin(p1);
            vertex(buffer, matrix, c0 * inner, s0 * inner, color, a / 4);
            vertex(buffer, matrix, c0 * outer, s0 * outer, color, a);
            vertex(buffer, matrix, c1 * outer, s1 * outer, color, a);
            vertex(buffer, matrix, c0 * inner, s0 * inner, color, a / 4);
            vertex(buffer, matrix, c1 * outer, s1 * outer, color, a);
            vertex(buffer, matrix, c1 * inner, s1 * inner, color, a / 4);
        }
    }

    static void vertex(BufferBuilder b, Matrix4f m, float x, float z, Color c, int a) {
        b.vertex(m, x, 0.0F, z).color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    void renderSkeleton(MatrixStack matrices, Vec3d camera, Entity entity, int color, float alpha) {
        Vec3d p = entity.getEntityPos().subtract(camera);
        matrices.push();
        matrices.translate(p.x, p.y, p.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getYaw()));
        Matrix4f m = matrices.peek().getPositionMatrix();
        Color c = new Color(color, true);
        int a = Math.round(230.0F * alpha);
        BufferBuilder b = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        line(b, m, 0.0F, 0.72F, 0.0F, 0.0F, 1.55F, 0.0F, c, a);
        line(b, m, -0.42F, 1.42F, 0.0F, 0.42F, 1.42F, 0.0F, c, a);
        line(b, m, -0.42F, 1.42F, 0.0F, -0.48F, 0.92F, 0.0F, c, a);
        line(b, m, 0.42F, 1.42F, 0.0F, 0.48F, 0.92F, 0.0F, c, a);
        line(b, m, 0.0F, 0.72F, 0.0F, -0.28F, 0.05F, 0.0F, c, a);
        line(b, m, 0.0F, 0.72F, 0.0F, 0.28F, 0.05F, 0.0F, c, a);
        line(b, m, -0.18F, 1.55F, 0.0F, 0.18F, 1.55F, 0.0F, c, a);
        line(b, m, 0.18F, 1.55F, 0.0F, 0.18F, 1.88F, 0.0F, c, a);
        line(b, m, 0.18F, 1.88F, 0.0F, -0.18F, 1.88F, 0.0F, c, a);
        line(b, m, -0.18F, 1.88F, 0.0F, -0.18F, 1.55F, 0.0F, c, a);
        AuraRender.drawColorLines(b, AuraRender.POS_COLOR_LINES_NODEPTH, 2.2F);
        matrices.pop();
    }

    static void line(BufferBuilder b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, Color c, int a) {
        b.vertex(m, x1, y1, z1).color(c.getRed(), c.getGreen(), c.getBlue(), a);
        b.vertex(m, x2, y2, z2).color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    void renderBillboard(WorldRenderContext context, Vec3d pos, FriendLabels$Label label, int color, float alpha) {
        Vec3d camera = context.camera().getCameraPos();
        float distance = (float)this.mc.player.getEntityPos().distanceTo(pos);
        float cameraDistance = (float)Math.max(0.35, camera.distanceTo(pos));
        float scale = cameraDistance * 0.00305F;
        MatrixStack matrices = context.matrixStack();
        matrices.push();
        matrices.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
        matrices.multiply(context.camera().getRotation());
        matrices.scale(-scale, -scale, scale);
        int tinted = applyAlpha(color, alpha);
        if (TtfIconFonts.hasIcons()) {
            matrices.push();
            matrices.translate(0.0F, 3.0F, 0.0F);
            TtfWorldIcon.drawIconChar(matrices, 'q', color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, tinted >>> 24 & 0xFF, 32.0F);
            matrices.pop();
        } else {
            drawIconTexture(matrices, tinted);
        }

        this.drawCentered(matrices, context.consumers(), label.sender, 22.0F, tinted, 1.16F);
        this.drawCentered(matrices, context.consumers(), Math.round(distance) + " м", 35.0F, tinted, 1.16F);
        matrices.pop();
    }

    static void drawIconTexture(MatrixStack matrices, int color) {
        Matrix4f m = matrices.peek().getPositionMatrix();
        BufferBuilder b = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        float s = 16.0F;
        float y = -9.0F;
        b.vertex(m, -s, y + s, 0.0F).texture(0.0F, 1.0F).color(color);
        b.vertex(m, s, y + s, 0.0F).texture(1.0F, 1.0F).color(color);
        b.vertex(m, s, y - s, 0.0F).texture(1.0F, 0.0F).color(color);
        b.vertex(m, -s, y - s, 0.0F).texture(0.0F, 0.0F).color(color);
        AuraRender.drawTexAuto(b, ICON, false, false);
    }

    void drawCentered(MatrixStack matrices, VertexConsumerProvider consumers, String value, float y, int color, float textScale) {
        matrices.push();
        matrices.scale(-textScale, textScale, textScale);
        float x = -FontManager.getWidth(value) / 2.0F;
        float scaledY = y / textScale;
        if (FontManager.customFontRenderer != null) {
            FontManager.customFontRenderer.drawString(matrices, value, x, scaledY, color, false);
        } else {
            this.mc
                .textRenderer
                .draw(Text.literal(value), x, scaledY, color, false, matrices.peek().getPositionMatrix(), consumers, TextLayerType.SEE_THROUGH, 0, 15728880);
        }

        matrices.pop();
    }

    static float fade(float age) {
        if (age < 0.25F) {
            return age / 0.25F;
        } else {
            return age > 7.5F ? Math.max(0.0F, (8.0F - age) / 0.5F) : 1.0F;
        }
    }

    static int applyAlpha(int color, float alpha) {
        return Math.round(255.0F * MathHelper.clamp(alpha, 0.0F, 1.0F)) << 24 | color & 16777215;
    }
}
