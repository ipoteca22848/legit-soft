package com.example.auravisuals.module.render;

import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BindSetting;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.render.WorldOcclusion;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;

public class HitCounter extends Module {
    public static final double RANGE_BLOCKS = 4.0;
    static final double RANGE_SQ = 16.0;
    public static final long RETAIN_MS = 480000L;
    static final float PANEL_Z = -0.02F;
    static final int CORNER_SEGMENTS = 8;
    static final double PLAYER_SEARCH_EXPAND = 192.0;
    static final long SWING_TO_HURT_WINDOW_MS = 720L;
    static final long SWING_COUNT_DEBOUNCE_MS = 500L;
    static final long HIT_PAIR_DEBOUNCE_MS = 500L;
    static final int COLOR_PREFIX_ORANGE = -30720;
    static final int COLOR_WHITE = -1;
    final NumberSetting labelScale = new NumberSetting("Размер", 0.8, 0.35, 1.3, 0.05);
    final NumberSetting labelAbove = new NumberSetting("Высота", 0.62, 0.15, 2.2, 0.05);
    final BooleanSetting hideMyHits = new BooleanSetting("Скрывать мои попадания", false);
    final BooleanSetting renderSelfFirstPerson = new BooleanSetting("Рендерить себя от 1 лица", false, () -> !this.hideMyHits.getValue());
    final BooleanSetting hideIfNoHits = new BooleanSetting("Скрывать если нету попаданий", false);
    public final BindSetting resetStatsBind = new BindSetting("Сбросить статистику: ", -1);
    final Map<UUID, HitCounter$Stats> data = new HashMap<>();
    final Map<UUID, Long> lastSwingAtMs = new HashMap<>();
    final Map<UUID, Long> lastSwingStatBumpMs = new HashMap<>();
    final Map<String, Long> recentHitPairMs = new HashMap<>();
    boolean resetStatsKeyWasPressed;

    public HitCounter() {
        super("HitCounter", "Счёт попаданий по игрокам", Category.VISUALS);
        this.addSettings(this.labelScale, this.labelAbove, this.hideMyHits, this.renderSelfFirstPerson, this.hideIfNoHits, this.resetStatsBind);
    }

    @Override
    public void onEnable() {
        this.resetAllStatistics();
        this.resetStatsKeyWasPressed = false;
    }

    @Override
    public void onDisable() {
        this.resetAllStatistics();
        this.resetStatsKeyWasPressed = false;
    }

    @Override
    public void onTick() {
        if (this.isEnabled() && this.mc.player != null && this.mc.world != null) {
            this.expireStale();
            this.pruneAuxMaps();
            this.tickResetStatsBind();
        }
    }

    public void resetAllStatistics() {
        this.data.clear();
        this.lastSwingAtMs.clear();
        this.lastSwingStatBumpMs.clear();
        this.recentHitPairMs.clear();
    }

    public void onLocalMeleeHitPlayer(PlayerEntity victim) {
        if (this.isEnabled() && this.mc.player != null && this.mc.world != null && victim != null) {
            if (victim != this.mc.player && victim.isAlive()) {
                if (this.mc.currentScreen == null) {
                    this.addHitForPair(victim.getUuid(), this.mc.player.getUuid());
                }
            }
        }
    }

    void tickResetStatsBind() {
        int key = this.resetStatsBind.getValue();
        if (key <= 0) {
            this.resetStatsKeyWasPressed = false;
        } else {
            boolean down = BindSetting.isPressed(this.mc.getWindow().getHandle(), key);
            if (BindSetting.shouldBlockKeyInput()) {
                this.resetStatsKeyWasPressed = down;
            } else {
                if (down && !this.resetStatsKeyWasPressed) {
                    this.resetAllStatistics();
                }

                this.resetStatsKeyWasPressed = down;
            }
        }
    }

    public void onEntityDamageS2C(EntityDamageS2CPacket packet, World world) {
        if (this.isEnabled() && world != null && this.mc.player != null) {
            if (world.getEntityById(packet.entityId()) instanceof PlayerEntity victim) {
                DamageSource source = packet.createDamageSource(world);
                Entity attacker = source.getAttacker();
                PlayerEntity ap;
                if (attacker instanceof PlayerEntity && (ap = (PlayerEntity)attacker) != victim) {
                    this.addHitForPair(victim.getUuid(), ap.getUuid());
                } else {
                    Entity srcEntity = source.getSource();
                    PlayerEntity ap2;
                    if (srcEntity instanceof PlayerEntity && (ap2 = (PlayerEntity)srcEntity) != victim) {
                        this.addHitForPair(victim.getUuid(), ap2.getUuid());
                    } else {
                        this.tryAttributeMeleeHit(victim);
                    }
                }
            }
        }
    }

    public void onLocalAttackSwing() {
        if (this.isEnabled() && this.mc.player != null && this.mc.world != null) {
            if (this.mc.currentScreen == null) {
                Vec3d p = this.mc.player.getEntityPos();
                boolean otherNear = false;

                for (PlayerEntity o : this.mc.world.getPlayers()) {
                    if (o != this.mc.player && o.squaredDistanceTo(p) <= 16.0) {
                        otherNear = true;
                        break;
                    }
                }

                if (otherNear) {
                    long now = System.currentTimeMillis();
                    this.lastSwingAtMs.put(this.mc.player.getUuid(), now);
                    UUID uuid = this.mc.player.getUuid();
                    Long lb = this.lastSwingStatBumpMs.get(uuid);
                    if (lb == null || now - lb >= 500L) {
                        this.lastSwingStatBumpMs.put(uuid, now);
                        HitCounter$Stats s = this.data.computeIfAbsent(uuid, u -> new HitCounter$Stats());
                        s.totalNearSwings++;
                        s.lastActivityMs = now;
                    }
                }
            }
        }
    }

    public void onNetworkArmSwing(PlayerEntity swinger) {
        if (this.isEnabled() && this.mc.world != null && swinger != null) {
            long now = System.currentTimeMillis();
            this.lastSwingAtMs.put(swinger.getUuid(), now);
            if (swinger != this.mc.player) {
                if (this.hasOtherPlayerInRange(swinger)) {
                    UUID su = swinger.getUuid();
                    Long lb = this.lastSwingStatBumpMs.get(su);
                    if (lb == null || now - lb >= 500L) {
                        this.lastSwingStatBumpMs.put(su, now);
                        HitCounter$Stats s = this.data.computeIfAbsent(su, u -> new HitCounter$Stats());
                        s.totalNearSwings++;
                        s.lastActivityMs = now;
                    }
                }
            }
        }
    }

    boolean hasOtherPlayerInRange(PlayerEntity center) {
        if (this.mc.world == null) {
            return false;
        }

        Vec3d p = center.getEntityPos();

        for (PlayerEntity o : this.mc.world.getPlayers()) {
            if (o != center && o.squaredDistanceTo(p) <= 16.0) {
                return true;
            }
        }

        return false;
    }

    void pruneAuxMaps() {
        HashSet<UUID> alive = new HashSet<>();

        for (PlayerEntity p : this.mc.world.getPlayers()) {
            alive.add(p.getUuid());
        }

        for (UUID uuid : new ArrayList<>(this.lastSwingAtMs.keySet())) {
            if (!alive.contains(uuid)) {
                this.lastSwingAtMs.remove(uuid);
            }
        }

        for (UUID uuid : new ArrayList<>(this.lastSwingStatBumpMs.keySet())) {
            if (!alive.contains(uuid)) {
                this.lastSwingStatBumpMs.remove(uuid);
            }
        }

        long t = System.currentTimeMillis();

        for (String key : new ArrayList<>(this.recentHitPairMs.keySet())) {
            Long last = this.recentHitPairMs.get(key);
            if (last == null || t - last > 60000L) {
                this.recentHitPairMs.remove(key);
            }
        }
    }

    void tryAttributeMeleeHit(PlayerEntity victim) {
        long now = System.currentTimeMillis();
        PlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;

        for (PlayerEntity attacker : this.mc.world.getPlayers()) {
            double d;
            Long swingT;
            if (attacker != victim
                && (swingT = this.lastSwingAtMs.get(attacker.getUuid())) != null
                && now - swingT <= 720L
                && !(attacker.squaredDistanceTo(victim) > 16.0)
                && (d = attacker.squaredDistanceTo(victim)) < bestDist) {
                bestDist = d;
                best = attacker;
            }
        }

        if (best != null) {
            this.addHitForPair(victim.getUuid(), best.getUuid());
        }
    }

    void addHitForPair(UUID victimUuid, UUID attackerUuid) {
        if (attackerUuid != null && victimUuid != null) {
            long now = System.currentTimeMillis();
            String k = victimUuid + ":" + attackerUuid;
            Long last = this.recentHitPairMs.get(k);
            if (last == null || now - last >= 500L) {
                this.recentHitPairMs.put(k, now);
                HitCounter$Stats s = this.data.computeIfAbsent(attackerUuid, u -> new HitCounter$Stats());
                s.hits++;
                s.lastActivityMs = now;
            }
        }
    }

    void expireStale() {
        long now = System.currentTimeMillis();
        this.data.entrySet().removeIf(e -> now - e.getValue().lastActivityMs > 480000L);
    }

    static int colorForPercent(int pct) {
        if (pct <= 30) {
            return -11145643;
        } else if (pct <= 60) {
            return -171;
        } else {
            return pct <= 84 ? -21965 : -48060;
        }
    }

    static boolean useCustomFont() {
        return true;
    }

    int stringWidth(String s) {
        if (s.isEmpty()) {
            return 0;
        } else {
            return useCustomFont() ? FontManager.getWidth(s) : this.mc.textRenderer.getWidth(Text.literal(s));
        }
    }

    int labelFontHeight() {
        return useCustomFont() ? FontManager.getHeight() : 9;
    }

    List<PlayerEntity> collectPlayersToLabel(Vec3d cam) {
        LinkedHashSet<UUID> seen = new LinkedHashSet<>();
        ArrayList<PlayerEntity> out = new ArrayList<>();
        Box search = new Box(cam, cam).expand(192.0);

        for (PlayerEntity p : this.mc.world.getEntitiesByClass(PlayerEntity.class, search, pe -> true)) {
            if (seen.add(p.getUuid())) {
                out.add(p);
            }
        }

        ClientPlayerEntity self = this.mc.player;
        if (self != null && seen.add(self.getUuid())) {
            out.add(self);
        }

        return out;
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.isEnabled() && this.mc.player != null && this.mc.world != null) {
            this.expireStale();
            Vec3d cam = context.camera().getCameraPos();
            float tickDelta = context.tickCounter().getTickProgress(true);
            Immediate immediate = this.mc.getBufferBuilders().getEntityVertexConsumers();
            float scale = this.labelScale.getFloatValue();
            float base = 0.025F * scale;
            boolean frame = false;

            for (PlayerEntity player : this.collectPlayersToLabel(cam)) {
                if ((player == this.mc.player || !player.isInvisibleTo(this.mc.player))
                    && (!this.hideMyHits.getValue() || player != this.mc.player)
                    && (player != this.mc.player || !this.mc.options.getPerspective().isFirstPerson() || this.renderSelfFirstPerson.getValue())
                    && (player == this.mc.player || WorldOcclusion.isEntityVisible(player, cam, tickDelta))) {
                    HitCounter$Stats st = this.data.get(player.getUuid());
                    int hits = st != null ? st.hits : 0;
                    int total = st != null ? st.totalNearSwings : 0;
                    if (!this.hideIfNoHits.getValue() || hits != 0) {
                        int pct;
                        if (total > 0) {
                            pct = (int)Math.round(100.0 * hits / total);
                            pct = Math.min(100, Math.max(0, pct));
                        } else {
                            pct = hits > 0 ? 100 : 0;
                        }

                        int pctColor = colorForPercent(pct);
                        String sPrefix = "Попадания: ";
                        String sHits = String.valueOf(hits);
                        String sSlash = "/";
                        String sTotal = String.valueOf(total);
                        String sOpen = " (";
                        String sPct = pct + "%)";
                        int fullW = this.stringWidth(sPrefix)
                            + this.stringWidth(sHits)
                            + this.stringWidth(sSlash)
                            + this.stringWidth(sTotal)
                            + this.stringWidth(sOpen)
                            + this.stringWidth(sPct);
                        double px = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX());
                        double py = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY());
                        double pz = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ());
                        double ax = px;
                        double ay = py + player.getHeight() + this.labelAbove.getValue();
                        double az = pz;
                        Vec3d anchor = new Vec3d(ax, ay, az);
                        Vec3d toCam = cam.subtract(anchor);
                        if (toCam.lengthSquared() > 1.0E-6) {
                            Vec3d nudge = toCam.normalize().multiply(0.12);
                            ax += nudge.x;
                            ay += nudge.y;
                            az += nudge.z;
                        }

                        double dx = ax - cam.x;
                        double dy = ay - cam.y;
                        double dz = az - cam.z;
                        MatrixStack matrices = context.matrixStack();
                        matrices.push();

                        try {
                            matrices.translate(dx, dy, dz);
                            matrices.multiply(context.camera().getRotation());
                            matrices.scale(-base, -base, base);
                            matrices.scale(-1.0F, 1.0F, 1.0F);
                            this.renderHologramLabel(matrices, immediate, frame, fullW, sPrefix, sHits, sSlash, sTotal, sOpen, sPct, pctColor);
                        } finally {
                            matrices.pop();
                        }
                    }
                }
            }
        }
    }

    void renderHologramLabel(
        MatrixStack matrices,
        Immediate immediate,
        boolean drawFrame,
        int fullW,
        String sPrefix,
        String sHits,
        String sSlash,
        String sTotal,
        String sOpen,
        String sPct,
        int pctColor
    ) {
        float padX = 3.0F;
        float padY = 2.0F;
        float fh = this.labelFontHeight();
        float boxHalfH = (fh + 4.0F) * 0.5F;
        float top = -boxHalfH;
        float bottom = boxHalfH;
        float midY = (top + bottom) * 0.5F;
        float textY = midY - fh * 0.4F;
        float x = -fullW / 2.0F;
        float left = x - 3.0F;
        float right = x + fullW + 3.0F;
        float width = right - left;
        float height = bottom - top;
        float radius = Math.min(4.0F, Math.min(width, height) * 0.35F);
        float pulse = 0.88F + 0.12F * MathHelper.sin((float)System.currentTimeMillis() * 0.004F);
        Matrix4f mat = matrices.peek().getPositionMatrix();
        if (drawFrame) {
            Tessellator tess = Tessellator.getInstance();
            int glowLayers = 3;

            for (int i = 3; i >= 1; i--) {
                float off = 0.26F * i;
                double t = (double)i / (glowLayers + 1);
                int ai = Math.max(0, (int)(38.0 * (1.0 - t) * (1.0 - t * 0.5)));
                float ga = ai / 255.0F * 0.38F * pulse;
                drawRoundedRectFan(
                    tess,
                    mat,
                    left - off,
                    top - off,
                    width + 2.0F * off,
                    height + 2.0F * off,
                    radius + off * 0.45F,
                    -0.02F - i * 4.0E-4F,
                    0.25F,
                    0.85F,
                    1.0F,
                    ga
                );
            }

            drawRoundedRectFan(tess, mat, left, top, width, height, radius, -0.02F, 0.1F, 0.42F, 0.55F, 0.42F);
        }

        float cx = x;
        cx = this.drawLabelText(matrices, immediate, cx, textY, sPrefix, -30720);
        cx = this.drawLabelText(matrices, immediate, cx, textY, sHits, -1);
        cx = this.drawLabelText(matrices, immediate, cx, textY, sSlash, -1);
        cx = this.drawLabelText(matrices, immediate, cx, textY, sTotal, -1);
        cx = this.drawLabelText(matrices, immediate, cx, textY, sOpen, -1);
        this.drawLabelText(matrices, immediate, cx, textY, sPct, pctColor);
        if (!useCustomFont()) {
            immediate.draw();
        }
    }

    static void drawRoundedRectFan(
        Tessellator tess, Matrix4f mat, float x, float y, float w, float h, float radius, float z, float cr, float cg, float cb, float ca
    ) {
        float r = Math.min(radius, Math.min(w, h) / 2.0F);
        float cx = x + w / 2.0F;
        float cy = y + h / 2.0F;
        BufferBuilder buffer = tess.begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buffer.vertex(mat, cx, cy, z).color(cr, cg, cb, ca);
        appendCornerFan(buffer, mat, x + w - r, y + r, r, 0, z, cr, cg, cb, ca);
        appendCornerFan(buffer, mat, x + r, y + r, r, -1, z, cr, cg, cb, ca);
        appendCornerFan(buffer, mat, x + r, y + h - r, r, -2, z, cr, cg, cb, ca);
        appendCornerFan(buffer, mat, x + w - r, y + h - r, r, -3, z, cr, cg, cb, ca);
        buffer.vertex(mat, x + w, y + r, z).color(cr, cg, cb, ca);
        AuraRender.drawColor(buffer, AuraRender.POS_COLOR_TRI_FAN);
    }

    static void appendCornerFan(
        BufferBuilder buffer, Matrix4f matrix, float cxi, float cyi, float rad, int quadrant, float z, float cr, float cg, float cb, float ca
    ) {
        for (int i = 0; i <= 8; i++) {
            double angle = quadrant * Math.PI / 2.0 - (Math.PI / 2) * i / 8.0;
            float px = cxi + (float)Math.cos(angle) * rad;
            float py = cyi + (float)Math.sin(angle) * rad;
            buffer.vertex(matrix, px, py, z).color(cr, cg, cb, ca);
        }
    }

    float drawLabelText(MatrixStack matrices, Immediate immediate, float x, float y, String s, int argb) {
        if (s.isEmpty()) {
            return x;
        } else if (useCustomFont()) {
            FontManager.customFontRenderer.drawString(matrices, s, x, y, argb, true);
            return x + FontManager.getWidth(s);
        } else {
            TextRenderer tr = this.mc.textRenderer;
            Matrix4f m = matrices.peek().getPositionMatrix();
            tr.draw(Text.literal(s), x, y, argb, true, m, immediate, TextLayerType.NORMAL, 0, 15728880);
            return x + tr.getWidth(Text.literal(s));
        }
    }
}
