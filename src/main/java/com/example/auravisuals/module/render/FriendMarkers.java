package com.example.auravisuals.module.render;

import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.friend.FriendManager;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.friend.FriendHubClient;
import com.example.auravisuals.util.friend.FriendHubClient$State;
import com.example.auravisuals.util.render.WorldOcclusion;
import java.awt.Color;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class FriendMarkers extends Module {
    private final BooleanSetting connectHub = new BooleanSetting("Auto connect", true);
    private final BooleanSetting showOfflineHubFriends = new BooleanSetting("Use local list", true);
    private final NumberSetting range = new NumberSetting("Range", 96.0, 16.0, 160.0, 4.0);
    private final NumberSetting scale = new NumberSetting("Scale", 1.0, 0.6, 1.8, 0.05);
    private final NumberSetting height = new NumberSetting("Height", 0.35, 0.0, 1.2, 0.05);
    private final ColorSetting color = new ColorSetting("Color", new Color(90, 255, 150));
    private long lastPresenceMs;

    public FriendMarkers() {
        super("FriendMarkers", "Shows marks above friends and group members", Category.MARKS);
        this.addSettings(this.connectHub, this.showOfflineHubFriends, this.range, this.scale, this.height, this.color);
        this.setEnabledSilently(true);
    }

    @Override
    public void onTick() {
        if (this.connectHub.getValue()) {
            FriendHubClient hub = FriendHubClient.INSTANCE;
            if (hub.getState() == FriendHubClient$State.DISCONNECTED) {
                hub.connect(FriendHubClient.DEFAULT_URL);
            } else {
                long now = System.currentTimeMillis();
                if (hub.getState() == FriendHubClient$State.CONNECTED && now - this.lastPresenceMs >= 5000L) {
                    this.lastPresenceMs = now;
                    hub.sendPresenceFromMc();
                }
            }
        }
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.mc.player != null && this.mc.world != null) {
            float tickDelta = context.tickCounter().getTickProgress(true);
            Vec3d cameraPos = context.camera().getCameraPos();
            double maxDistanceSq = this.range.getValue() * this.range.getValue();

            for (AbstractClientPlayerEntity player : this.mc.world.getPlayers()) {
                String name;
                if (player != this.mc.player
                    && player.isAlive()
                    && !player.isInvisible()
                    && !(player.squaredDistanceTo(this.mc.player) > maxDistanceSq)
                    && this.isFriend(name = player.getName().getString())
                    && WorldOcclusion.isEntityVisible(player, cameraPos, tickDelta)) {
                    this.renderFriendLabel(context, player, name, cameraPos, tickDelta);
                }
            }
        }
    }

    private boolean isFriend(String name) {
        if (name == null || name.isBlank()) {
            return false;
        } else {
            return FriendHubClient.INSTANCE.isFriend(name)
                ? true
                : this.showOfflineHubFriends.getValue() && FriendManager.INSTANCE.getFriends().stream().anyMatch(friend -> friend.equalsIgnoreCase(name));
        }
    }

    private void renderFriendLabel(WorldRenderContext context, AbstractClientPlayerEntity player, String name, Vec3d cameraPos, float tickDelta) {
        Vec3d pos = player.getLerpedPos(tickDelta).add(0.0, player.getHeight() + this.height.getValue(), 0.0);
        double x = pos.x - cameraPos.x;
        double y = pos.y - cameraPos.y;
        double z = pos.z - cameraPos.z;
        MatrixStack matrices = context.matrixStack();
        matrices.push();

        try {
            matrices.translate(x, y, z);
            matrices.multiply(context.camera().getRotation());
            float distance = (float)this.mc.player.getEntityPos().distanceTo(player.getLerpedPos(tickDelta));
            float textScale = 0.016F * this.scale.getFloatValue() * Math.max(0.85F, distance / 7.0F);
            matrices.scale(-textScale, -textScale, textScale);
            String label = "[F] " + name;
            int labelColor = withAlpha(this.color.getValue(), 255);
            this.drawCentered(matrices, context.consumers(), label, labelColor);
        } finally {
            matrices.pop();
        }
    }

    private void drawCentered(MatrixStack matrices, VertexConsumerProvider consumers, String text, int textColor) {
        int shadow = -1342177280;
        if (FontManager.customFontRenderer != null) {
            float width = FontManager.getWidth(text);
            FontManager.customFontRenderer.drawString(matrices, text, -width / 2.0F + 1.0F, 1.0F, shadow, false);
            FontManager.customFontRenderer.drawString(matrices, text, -width / 2.0F, 0.0F, textColor, false);
        } else if (consumers != null && this.mc.textRenderer != null) {
            TextRenderer renderer = this.mc.textRenderer;
            Text label = Text.of(text);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float x = -renderer.getWidth(label) / 2.0F;
            renderer.draw(label, x + 1.0F, 1.0F, shadow, false, matrix, consumers, TextLayerType.NORMAL, 0, 15728880);
            renderer.draw(label, x, 0.0F, textColor, false, matrix, consumers, TextLayerType.NORMAL, 0, 15728880);
        }
    }

    private static int withAlpha(int color, int alpha) {
        return MathHelper.clamp(alpha, 0, 255) << 24 | color & 16777215;
    }
}
