package com.example.auravisuals.emotions;

import com.example.auravisuals.client.gui.render.RoundedRenderer;
import com.example.auravisuals.compat.Gui;
import com.example.auravisuals.cosmetics.CosmeticsNetworkClient;
import com.example.auravisuals.module.render.Emotions;
import com.example.auravisuals.setting.BindSetting;
import com.example.auravisuals.util.render.MirageGlassPipeline;
import com.example.auravisuals.util.render.RenderUtil;
import com.mojang.authlib.GameProfile;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public final class EmotionWheelManager {
    static final int PREVIEW_ENTITY_ID = -784231;
    static final float FADE_IN_MS = 280.0F;
    static final float FADE_OUT_MS = 320.0F;
    static final EmotionType[] EMOTIONS = EmotionType.values();
    public static final EmotionWheelManager INSTANCE = new EmotionWheelManager();
    final MinecraftClient mc = MinecraftClient.getInstance();
    final Map<UUID, EmotionWheelManager$ActiveEmotion> active = new ConcurrentHashMap<>();
    final Map<String, EmotionWheelManager$ActiveEmotion> activeByName = new ConcurrentHashMap<>();
    final float[] slotHover = new float[EMOTIONS.length];
    boolean wheelOpen;
    boolean closingVisual;
    float closeProgress;
    boolean keyWasDown;
    float wheelAnim;
    long lastAnimMs;
    double cursorX;
    double cursorY;
    int stickySlot = -1;
    EmotionType hovered;
    EmotionType previewEmotion = EmotionType.HELLO;
    long previewStartedAtMs;
    OtherClientPlayerEntity stevePreview;
    ClientWorld stevePreviewWorld;
    EmotionType stevePreviewEmotion = EmotionType.HELLO;

    EmotionWheelManager() {
    }

    public void tick(Emotions module) {
        if (this.mc.player != null && this.mc.world != null) {
            boolean down = BindSetting.isPressed(this.mc.getWindow().getHandle(), module.wheelBind.getValue());
            if (down && !this.keyWasDown && this.mc.currentScreen == null) {
                this.openWheel();
            }

            if (!down && this.keyWasDown && this.wheelOpen) {
                EmotionType pick = this.hovered;
                this.closeWheel(false);
                if (pick != null) {
                    this.play(this.mc.player, pick, true);
                    CosmeticsNetworkClient.INSTANCE.sendEmotion(pick.name());
                }
            }

            this.keyWasDown = down;
            if (this.wheelOpen) {
                this.readCursor();
                this.updateHover();
            }

            Iterator<Entry<UUID, EmotionWheelManager$ActiveEmotion>> it = this.active.entrySet().iterator();

            while (it.hasNext()) {
                Entry<UUID, EmotionWheelManager$ActiveEmotion> entry = it.next();
                if (entry.getValue().expired()) {
                    it.remove();
                }
            }

            Iterator<Entry<String, EmotionWheelManager$ActiveEmotion>> itName = this.activeByName.entrySet().iterator();

            while (itName.hasNext()) {
                Entry<String, EmotionWheelManager$ActiveEmotion> entry = itName.next();
                if (entry.getValue().expired()) {
                    itName.remove();
                }
            }
        } else {
            this.closeWheel(true);
            this.active.clear();
        }
    }

    public void playRemoteEmotion(String playerName, String emotionName) {
        if (playerName != null && emotionName != null) {
            String cleanEmotion = emotionName.trim();
            EmotionType type = null;

            for (EmotionType t : EmotionType.values()) {
                if (t.name().equalsIgnoreCase(cleanEmotion)
                    || t.title().equalsIgnoreCase(cleanEmotion)
                    || t.name().replace("_", "").equalsIgnoreCase(cleanEmotion.replace("_", ""))) {
                    type = t;
                    break;
                }
            }

            if (type != null) {
                EmotionWheelManager$ActiveEmotion emo = new EmotionWheelManager$ActiveEmotion(type, System.currentTimeMillis(), false);
                this.activeByName.put(playerName.trim().toLowerCase(Locale.ROOT), emo);
                if (this.mc.world != null) {
                    for (PlayerEntity player : this.mc.world.getPlayers()) {
                        String pName = player.getName() != null ? player.getName().getString() : "";
                        String gName = player.getGameProfile() != null && player.getGameProfile().name() != null ? player.getGameProfile().name() : "";
                        if (pName.equalsIgnoreCase(playerName) || gName.equalsIgnoreCase(playerName)) {
                            if (this.mc.player != null && player.getUuid().equals(this.mc.player.getUuid())) {
                                return;
                            }

                            this.active.put(player.getUuid(), emo);
                            return;
                        }
                    }
                }
            }
        }
    }

    public void render(DrawContext context) {
        long now = System.currentTimeMillis();
        float dt = this.lastAnimMs == 0L ? 0.016F : MathHelper.clamp((float)(now - this.lastAnimMs) / 1000.0F, 0.001F, 0.05F);
        this.lastAnimMs = now;
        this.wheelAnim = approach(this.wheelAnim, this.wheelOpen ? 1.0F : 0.0F, dt, 10.0F);
        if (this.closingVisual) {
            this.closeProgress = Math.min(1.0F, this.closeProgress + dt / 0.5F);
            if (this.closeProgress >= 1.0F) {
                this.closingVisual = false;
                this.wheelAnim = 0.0F;
                return;
            }
        }

        if (this.wheelAnim <= 0.001F && !this.wheelOpen && !this.closingVisual) {
            this.closingVisual = false;
        } else {
            float anim = easeOutCubic(MathHelper.clamp(this.wheelAnim, 0.0F, 1.0F));
            float closeFade = this.closingVisual ? 1.0F - smoothStep(MathHelper.clamp(this.closeProgress / 0.72F, 0.0F, 1.0F)) : 1.0F;
            float visualAlpha = this.closingVisual ? closeFade : anim;
            int w = context.getScaledWindowWidth();
            int h = context.getScaledWindowHeight();
            int cx = w / 2;
            int cy = h / 2;
            if (this.wheelOpen) {
                this.readCursor();
                this.updateHover();
            }

            context.fill(0, 0, w, h, alpha(0, (int)(86.0F * visualAlpha)));
            float scale = this.closingVisual ? 1.0F : 0.76F + 0.24F * anim;
            float outer = 142.0F * scale;
            float inner = 48.0F * scale;
            float iconRing = 96.0F * scale;
            float fly = this.closingVisual ? smoothStep(this.closeProgress) * 42.0F : 0.0F;
            MirageGlassPipeline.captureFrame();
            MirageGlassPipeline.drawBlurAt(context, cx - outer, cy - outer, outer * 2.0F, outer * 2.0F, outer, 22.0F, visualAlpha * 0.92F);

            for (int i = 0; i < EMOTIONS.length; i++) {
                float target = this.stickySlot == i ? 1.0F : 0.0F;
                this.slotHover[i] = approach(this.slotHover[i], target, dt, 14.0F);
                double a = Math.toRadians(-90.0 + i * 60.0);
                float offsetX = (float)Math.cos(a) * fly;
                float offsetY = (float)Math.sin(a) * fly;
                float sectorCx = cx + offsetX;
                float sectorCy = cy + offsetY;
                float start = -120.0F + i * 60.0F;
                int sector = lerp(592397, 1908516, this.slotHover[i]);
                RoundedRenderer.drawArc(
                    context, sectorCx, sectorCy, inner, outer, start, 60.0F, alpha(sector, (int)((228.0F + 15.0F * this.slotHover[i]) * visualAlpha))
                );
                double edgeAngle = Math.toRadians(start);
                drawLine(
                    context,
                    sectorCx + (float)Math.cos(edgeAngle) * (inner + 1.0F),
                    sectorCy + (float)Math.sin(edgeAngle) * (inner + 1.0F),
                    sectorCx + (float)Math.cos(edgeAngle) * (outer - 1.0F),
                    sectorCy + (float)Math.sin(edgeAngle) * (outer - 1.0F),
                    0.72F,
                    alpha(6909043, (int)(72.0F * visualAlpha))
                );
                int sx = Math.round(sectorCx + (float)Math.cos(a) * iconRing);
                int sy = Math.round(sectorCy + (float)Math.sin(a) * iconRing);
                int icon = Math.max(2, Math.round((39.0F + 4.0F * this.slotHover[i]) * scale));
                int iconAlpha = MathHelper.clamp(Math.round(255.0F * visualAlpha), 0, 255);
                boolean livePreview = this.wheelOpen && this.stickySlot == i && this.slotHover[i] > 0.28F && this.mc.player != null;
                int textureAlpha = livePreview ? MathHelper.clamp(Math.round(iconAlpha * (1.0F - this.slotHover[i])), 0, 255) : iconAlpha;
                if (textureAlpha > 2) {
                    Gui.texQuad(
                        context,
                        EMOTIONS[i].texture(),
                        sx - icon / 2.0F,
                        sy - icon / 2.0F,
                        sx + icon / 2.0F,
                        sy + icon / 2.0F,
                        0.0F,
                        0.0F,
                        1.0F,
                        1.0F,
                        textureAlpha << 24 | 16777215
                    );
                }

                if (livePreview) {
                    this.renderEmotionPreview(context, sx, sy, EMOTIONS[i]);
                }
            }

            float centerCloseScale = this.closingVisual ? 1.0F - 0.5F * smoothStep(MathHelper.clamp(this.closeProgress / 0.72F, 0.0F, 1.0F)) : 1.0F;
            float centerRadius = (inner - 1.2F) * centerCloseScale;
            if (centerRadius > 0.25F) {
                RenderUtil.drawCircle(context, cx, cy, centerRadius, alpha(395018, (int)(247.0F * visualAlpha)));
                RoundedRenderer.drawArc(
                    context, cx, cy, Math.max(0.0F, centerRadius - 0.65F), centerRadius + 0.65F, 0.0F, 360.0F, alpha(6448752, (int)(92.0F * visualAlpha))
                );
            }

            int cross = alpha(this.hovered == null ? 14211550 : 7830147, (int)(215.0F * visualAlpha));
            float crossHalf = 6.5F * scale * centerCloseScale;
            if (crossHalf > 0.25F) {
                drawLine(context, cx - crossHalf, cy - crossHalf, cx + crossHalf, cy + crossHalf, 1.45F, cross);
                drawLine(context, cx + crossHalf, cy - crossHalf, cx - crossHalf, cy + crossHalf, 1.45F, cross);
            }
        }
    }

    public void applyToModel(
        PlayerEntityRenderState state,
        ModelPart head,
        ModelPart hat,
        ModelPart body,
        ModelPart rightArm,
        ModelPart leftArm,
        ModelPart rightLeg,
        ModelPart leftLeg
    ) {
        if (this.mc.world != null) {
            boolean isStevePreview = state.id == -784231 && this.stevePreview != null;
            Entity entity = isStevePreview ? this.stevePreview : this.mc.world.getEntityById(state.id);
            EmotionWheelManager$ActiveEmotion activeEmotion = null;
            PlayerEntity pe;
            PlayerEntity player = entity instanceof PlayerEntity ? (pe = (PlayerEntity)entity) : null;
            if (player != null) {
                activeEmotion = this.active.get(player.getUuid());
                if (activeEmotion == null && player.getName() != null) {
                    activeEmotion = this.activeByName.get(player.getName().getString().trim().toLowerCase(Locale.ROOT));
                }

                if (activeEmotion == null && player.getGameProfile() != null && player.getGameProfile().name() != null) {
                    activeEmotion = this.activeByName.get(player.getGameProfile().name().trim().toLowerCase(Locale.ROOT));
                }
            }

            if (activeEmotion == null && state.playerName != null) {
                activeEmotion = this.activeByName.get(state.playerName.getString().trim().toLowerCase(Locale.ROOT));
            }

            if (activeEmotion == null && state.displayName != null) {
                activeEmotion = this.activeByName.get(state.displayName.getString().trim().toLowerCase(Locale.ROOT));
            }

            EmotionType type = null;
            float blend = 0.0F;
            long started = 0L;
            if (isStevePreview) {
                type = this.stevePreviewEmotion;
                started = this.previewStartedAtMs;
                float previewIn = MathHelper.clamp((float)(System.currentTimeMillis() - started) / 520.0F, 0.0F, 1.0F);
                blend = smoothStep(previewIn);
            } else if (activeEmotion != null) {
                type = activeEmotion.type;
                blend = activeEmotion.blend();
                started = activeEmotion.startedAt;
            }

            if (type != null && !(blend <= 0.001F)) {
                long elapsed = Math.max(0L, System.currentTimeMillis() - started);
                boolean hands = isStevePreview || player != null && player != this.mc.player || !this.mc.options.getPerspective().isFirstPerson();
                switch (EmotionWheelManager$2.$SwitchMap$com$example$auravisuals$emotions$EmotionType[type.ordinal()]) {
                    case 1:
                        this.poseDeb(blend, hands, head, rightArm, leftArm);
                        break;
                    case 2:
                        this.poseFloss(blend, elapsed, hands, body, rightArm, leftArm, rightLeg, leftLeg);
                        break;
                    case 3:
                        this.poseMasturbate(blend, elapsed, hands, player, body, rightArm, leftArm, rightLeg, leftLeg);
                        break;
                    case 4:
                        this.poseHello(blend, elapsed, hands, rightArm);
                        break;
                    case 5:
                        this.poseGriddy(blend, elapsed, hands, head, body, rightArm, leftArm, rightLeg, leftLeg);
                        break;
                    case 6:
                        this.poseHappy(blend, hands, rightArm, leftArm);
                }

                if (isStevePreview) {
                    head.pitch = 0.0F;
                    head.yaw = 0.0F;
                    head.roll = 0.0F;
                }

                hat.pitch = head.pitch;
                hat.yaw = head.yaw;
                hat.roll = head.roll;
            }
        }
    }

    public void closeWheel(boolean force) {
        boolean wasOpen = this.wheelOpen;
        this.wheelOpen = false;
        if (wasOpen && !force) {
            this.closingVisual = true;
            this.closeProgress = 0.0F;
        }

        if (force) {
            this.hovered = null;
            this.stickySlot = -1;
        }

        if (wasOpen && this.mc.currentScreen == null && this.mc.getWindow() != null) {
            this.mc.mouse.lockCursor();
        }
    }

    public boolean isWheelOpen() {
        return this.wheelOpen;
    }

    void openWheel() {
        this.wheelOpen = true;
        this.closingVisual = false;
        this.closeProgress = 0.0F;
        this.cursorY = 0.0;
        this.cursorX = 0.0;
        this.hovered = null;
        this.stickySlot = -1;
        this.previewEmotion = EmotionType.HELLO;
        this.previewStartedAtMs = System.currentTimeMillis();
        this.mc.mouse.unlockCursor();
        GLFW.glfwSetCursorPos(this.mc.getWindow().getHandle(), this.mc.getWindow().getWidth() * 0.5, this.mc.getWindow().getHeight() * 0.5);
    }

    public void play(PlayerEntity player, EmotionType type, boolean local) {
        EmotionWheelManager$ActiveEmotion emo = new EmotionWheelManager$ActiveEmotion(type, System.currentTimeMillis(), local);
        this.active.put(player.getUuid(), emo);
        if (player.getName() != null) {
            this.activeByName.put(player.getName().getString().trim().toLowerCase(Locale.ROOT), emo);
        }

        if (player.getGameProfile() != null && player.getGameProfile().name() != null) {
            this.activeByName.put(player.getGameProfile().name().trim().toLowerCase(Locale.ROOT), emo);
        }

        if (local) {
            CosmeticsNetworkClient.INSTANCE.sendEmotion(type.name());
        }
    }

    void updateHover() {
        float visualScale = 0.76F + 0.24F * easeOutCubic(MathHelper.clamp(this.wheelAnim, 0.0F, 1.0F));
        double len = Math.hypot(this.cursorX, this.cursorY);
        if (len < 48.0 * visualScale) {
            this.hovered = null;
            this.stickySlot = -1;
        } else {
            double angle = Math.toDegrees(Math.atan2(this.cursorY, this.cursorX));
            double shifted = (angle + 120.0) % 360.0;
            if (shifted < 0.0) {
                shifted += 360.0;
            }

            this.stickySlot = MathHelper.clamp((int)Math.floor(shifted / 60.0), 0, EMOTIONS.length - 1);
            this.hovered = EMOTIONS[this.stickySlot];
            if (this.previewEmotion != this.hovered) {
                this.previewEmotion = this.hovered;
                this.previewStartedAtMs = System.currentTimeMillis();
            }
        }
    }

    void readCursor() {
        double mx = this.mc.mouse.getX() / this.mc.getWindow().getScaleFactor();
        double my = this.mc.mouse.getY() / this.mc.getWindow().getScaleFactor();
        this.cursorX = mx - this.mc.getWindow().getScaledWidth() * 0.5;
        this.cursorY = my - this.mc.getWindow().getScaledHeight() * 0.5;
    }

    void renderEmotionPreview(DrawContext context, int centerX, int centerY, EmotionType type) {
        if (this.mc.world != null) {
            if (this.stevePreview == null || this.stevePreviewWorld != this.mc.world) {
                this.stevePreviewWorld = this.mc.world;
                this.stevePreview = new EmotionWheelManager$1(this, this.mc.world, new GameProfile(new UUID(0L, 0L), "Steve"));
                this.stevePreview.setId(-784231);
            }

            this.stevePreviewEmotion = type;

            try {
                InventoryScreen.drawEntity(context, centerX - 25, centerY - 28, centerX + 25, centerY + 29, 23, 0.0625F, 6.0F, 0.0F, this.stevePreview);
            } catch (Throwable var6) {
            }
        }
    }

    static void drawLine(DrawContext context, float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = Math.max(0.001F, (float)Math.sqrt(dx * dx + dy * dy));
        float nx = -dy / length * thickness * 0.5F;
        float ny = dx / length * thickness * 0.5F;
        Gui.color(context).quad(x1 + nx, y1 + ny, x2 + nx, y2 + ny, x2 - nx, y2 - ny, x1 - nx, y1 - ny, color).submit(context);
    }

    void poseDeb(float b, boolean hands, ModelPart head, ModelPart rightArm, ModelPart leftArm) {
        if (hands) {
            armXZ(rightArm, b, -0.7F, -2.0F);
            armXZ(leftArm, b, -0.7F, -2.0F);
        }

        head.pitch = MathHelper.lerp(b, head.pitch, 0.7F);
        head.yaw = MathHelper.lerp(b, head.yaw, 0.5F);
        head.roll = MathHelper.lerp(b, head.roll, 0.0F);
    }

    void poseFloss(float b, long t, boolean hands, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        float action = tri(t, 250L);
        if (hands) {
            armXZ(rightArm, b, 0.5F - tri(t, 500L), 1.0F - 2.0F * action);
            armXZ(leftArm, b, 0.5F - tri(t + 250L, 500L), 1.5F - 2.0F * action);
        }

        body.roll = MathHelper.lerp(b, body.roll, 0.3F * action - 0.15F);
        rightLeg.originX = MathHelper.lerp(b, rightLeg.originX, -0.5F - 3.0F * action);
        leftLeg.originX = MathHelper.lerp(b, leftLeg.originX, 3.0F - 2.5F * action);
    }

    void poseHello(float b, long t, boolean hands, ModelPart rightArm) {
        if (hands) {
            armXZ(rightArm, b, -0.2F, 2.75F - 0.25F * tri(t, 250L));
        }
    }

    void poseHappy(float b, boolean hands, ModelPart rightArm, ModelPart leftArm) {
        if (hands) {
            armXZ(rightArm, b, 0.0F, 2.75F);
            armXZ(leftArm, b, 0.0F, -2.75F);
        }
    }

    void poseMasturbate(
        float b, long t, boolean hands, PlayerEntity player, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg
    ) {
        float action = tri(t, 100L);
        if (player != null && player.isSneaking()) {
            if (hands) {
                armXZ(rightArm, b, 0.7F - 0.7F * action, -0.55F);
            }
        } else {
            if (hands) {
                armXZ(rightArm, b, -0.3F - 0.7F * action, -0.55F);
                armXZ(leftArm, b, 0.3F, 1.0F);
            }

            body.pitch = MathHelper.lerp(b, body.pitch, -0.1F);
            rightLeg.originZ = MathHelper.lerp(b, rightLeg.originZ, -1.0F);
            leftLeg.originZ = MathHelper.lerp(b, leftLeg.originZ, -1.0F);
        }
    }

    void poseGriddy(
        float b, long t, boolean hands, ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg
    ) {
        float action = tri(t, 350L);
        float eye = griddyEye(t);
        if (hands) {
            float armX = 1.0F - 1.5F * action - 3.5F * eye;
            armXZ(rightArm, b, armX, 0.5F - 0.75F * action);
            armXZ(leftArm, b, armX, -0.5F + 0.75F * action);
        }

        body.pitch = MathHelper.lerp(b, body.pitch, 0.2F - 0.05F * action);
        rightLeg.originZ = MathHelper.lerp(b, rightLeg.originZ, 2.0F - 0.5F * action);
        leftLeg.originZ = MathHelper.lerp(b, leftLeg.originZ, 2.0F - 0.5F * action);
        rightLeg.pitch = MathHelper.lerp(b, rightLeg.pitch, 0.3F * tri(t, 300L) - 0.3F);
        leftLeg.pitch = MathHelper.lerp(b, leftLeg.pitch, -0.3F * tri(t, 300L) - 0.1F);
        head.yaw = MathHelper.lerp(b, head.yaw, -0.4F + 0.4F * eye);
        head.pitch = MathHelper.lerp(b, head.pitch, -0.3F * eye);
    }

    static void armXZ(ModelPart arm, float blend, float pitch, float roll) {
        arm.pitch = MathHelper.lerp(blend, arm.pitch, pitch);
        arm.roll = MathHelper.lerp(blend, arm.roll, roll);
    }

    static float tri(long elapsed, long halfMs) {
        long cycle = halfMs * 2L;
        long m = elapsed % cycle;
        return m < halfMs ? (float)m / (float)halfMs : 2.0F - (float)m / (float)halfMs;
    }

    static float griddyEye(long elapsed) {
        long cycle = elapsed % 3100L;
        return cycle < 2100L ? 0.0F : tri(cycle - 2100L, 500L);
    }

    static float approach(float current, float target, float dt, float speed) {
        float t = 1.0F - (float)Math.exp(-speed * dt);
        return current + (target - current) * t;
    }

    static float easeOutCubic(float x) {
        float inv = 1.0F - x;
        return 1.0F - inv * inv * inv;
    }

    static float smoothStep(float x) {
        float t = MathHelper.clamp(x, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    static int alpha(int rgb, int a) {
        return MathHelper.clamp(a, 0, 255) << 24 | rgb & 16777215;
    }

    static int lerp(int a, int b, float t) {
        int ar = a >> 16 & 0xFF;
        int ag = a >> 8 & 0xFF;
        int ab = a & 0xFF;
        int br = b >> 16 & 0xFF;
        int bg = b >> 8 & 0xFF;
        int bb = b & 0xFF;
        int r = (int)(ar + (br - ar) * t);
        int g = (int)(ag + (bg - ag) * t);
        int bl = (int)(ab + (bb - ab) * t);
        return r << 16 | g << 8 | bl;
    }
}
