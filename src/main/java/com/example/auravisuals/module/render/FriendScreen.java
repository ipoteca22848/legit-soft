package com.example.auravisuals.module.render;

import com.example.auravisuals.client.gui.FriendInventoryScreen;
import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.client.gui.render.TextRender;
import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.ActionSetting;
import com.example.auravisuals.setting.BindSetting;
import com.example.auravisuals.setting.FriendRequestRowSetting;
import com.example.auravisuals.setting.FriendRowSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.setting.StringSetting;
import com.example.auravisuals.theme.ThemeManager;
import com.example.auravisuals.util.animation.AnimationUtil;
import com.example.auravisuals.util.friendscreen.FriendScreenClient;
import com.example.auravisuals.util.friendscreen.FriendScreenClient$Entry;
import com.example.auravisuals.util.friendscreen.FriendScreenClient$PlayerInfo;
import com.example.auravisuals.util.friendscreen.FriendScreenClient$State;
import com.example.auravisuals.util.friendscreen.FriendScreenClient$StreamQuality;
import com.example.auravisuals.util.friendscreen.FriendScreenClient$StreamView;
import com.example.auravisuals.util.render.FramebufferBlitter;
import com.example.auravisuals.util.render.RenderUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class FriendScreen extends Module {
    static final int MAX_FRIEND_ROWS = 12;
    static final int MAX_REQUEST_ROWS = 12;
    static final int HEADER_HEIGHT = 16;
    static final int INFO_HEIGHT = 20;
    static final int GPS_SECTION_WIDTH = 58;
    static final int POPUP_WIDTH = 160;
    static final int POPUP_HEIGHT = 152;
    static final Identifier HOTBAR = Identifier.ofVanilla("hud/hotbar");
    static final Identifier HOTBAR_SELECTION = Identifier.ofVanilla("hud/hotbar_selection");
    static final Identifier GPS_ARROW = Identifier.of("auravisuals", "textures/strelka.png");
    static final ExecutorService ENCODER = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "Aura-FriendScreen-Encoder");
        thread.setDaemon(true);
        return thread;
    });
    final ModeSetting section = new ModeSetting("Раздел", "Друзья", "Друзья", "Заявки");
    final ActionSetting connectionStatus = new ActionSetting(
        "Статус: Подключение...", () -> this.hub().connect(), () -> this.hub().getState() != FriendScreenClient$State.CONNECTED
    );
    final StringSetting friendName = new StringSetting("Никнейм друга", "", () -> this.section.is("Друзья"));
    final ActionSetting sendRequest = new ActionSetting("Отправить заявку", this::sendFriendRequest, () -> this.section.is("Друзья"));
    final ActionSetting noFriendsInfo = new ActionSetting("Список друзей пуст", () -> {}, () -> this.section.is("Друзья") && this.hub().getFriends().isEmpty());
    final ActionSetting noRequestsInfo = new ActionSetting(
        "Входящих заявок нет", () -> {}, () -> this.section.is("Заявки") && this.hub().getRequests().isEmpty()
    );
    final FriendRowSetting[] friendRows = new FriendRowSetting[12];
    final FriendRequestRowSetting[] requestRows = new FriendRequestRowSetting[12];
    final NumberSetting panelX = new NumberSetting("friend_screen_x", -1.0, -1.0, 10000.0, 1.0, () -> false);
    final NumberSetting panelY = new NumberSetting("friend_screen_y", 8.0, 0.0, 10000.0, 1.0, () -> false);
    final NumberSetting panelWidth = new NumberSetting("friend_screen_width", 190.0, 140.0, 520.0, 5.0, () -> false);
    final NumberSetting compactScale = new NumberSetting("friend_screen_compact_scale", 1.0, 0.5, 2.0, 0.05, () -> false);
    final Set<String> hiddenScreens = new HashSet<>();
    final Set<String> compactScreens = new HashSet<>();
    final Map<String, FriendScreenClient$StreamQuality> screenQualities = new HashMap<>();
    final Map<String, Integer> friendBinds = new HashMap<>();
    final Map<String, Boolean> friendKeyStates = new HashMap<>();
    final Map<String, FriendScreen$WorldPanel> worldPanels = new HashMap<>();
    final AtomicInteger hudCapturePending = new AtomicInteger();
    final AtomicInteger worldCapturePending = new AtomicInteger();
    SimpleFramebuffer hudStreamFramebuffer;
    SimpleFramebuffer worldStreamFramebuffer;
    long lastHudFrameAt;
    long lastWorldFrameAt;
    long lastQualitySyncAt;
    long lastInventoryMetadataAt;
    JsonArray cachedInventoryMetadata = new JsonArray();
    JsonArray cachedEquipmentMetadata = new JsonArray();
    boolean chatMouseDown;
    boolean chatRightDown;
    boolean chatDragging;
    double chatGrabX;
    double chatGrabY;
    boolean popupOpen;
    String popupFriend;
    int popupY;
    float popupAnimation;
    float popupRenderX;
    float popupStartX;
    float popupTargetX;
    float qualityCapsuleX;
    float compactToggleAnim;
    String draggedWorldFriend;
    double draggedWorldDistance;
    double draggedLocalX;
    double draggedLocalY;
    long lastWorldRenderNanos;

    public FriendScreen() {
        super("FriendScreen", "Показывает экраны друзей в реальном времени", Category.VISUALS);

        for (int i = 0; i < this.friendRows.length; i++) {
            int index = i;
            this.friendRows[i] = new FriendRowSetting(index, () -> {
                List<FriendScreenClient$Entry> friends = this.hub().getFriends();
                return index < friends.size() ? friends.get(index) : null;
            }, () -> {
                List<FriendScreenClient$Entry> friends = this.hub().getFriends();
                return index >= friends.size() ? false : this.hiddenScreens.contains(friends.get(index).login.toLowerCase(Locale.ROOT));
            }, () -> this.toggleFriend(index), () -> {
                List<FriendScreenClient$Entry> friends = this.hub().getFriends();
                return index >= friends.size() ? 0 : this.friendBinds.getOrDefault(friends.get(index).login.toLowerCase(Locale.ROOT), 0);
            }, key -> {
                List<FriendScreenClient$Entry> friends = this.hub().getFriends();
                if (index < friends.size()) {
                    this.friendBinds.put(friends.get(index).login.toLowerCase(Locale.ROOT), key);
                }
            }, () -> this.section.is("Друзья") && index < this.hub().getFriends().size());
        }

        for (int var3 = 0; var3 < this.requestRows.length; var3++) {
            int index = var3;
            this.requestRows[var3] = new FriendRequestRowSetting(index, () -> {
                List<FriendScreenClient$Entry> requests = this.hub().getRequests();
                return index < requests.size() ? requests.get(index) : null;
            }, () -> this.accept(index), () -> this.decline(index), () -> this.section.is("Заявки") && index < this.hub().getRequests().size());
        }

        this.addSettings(this.section, this.connectionStatus, this.friendName, this.sendRequest, this.noFriendsInfo);
        this.addSettings(this.friendRows);
        this.addSettings(this.noRequestsInfo);
        this.addSettings(this.requestRows);
        this.addSettings(this.panelX, this.panelY, this.panelWidth, this.compactScale);
        this.hub().setOnDataChanged(this::refreshRowNames);
        this.hub().connect();
    }

    FriendScreenClient hub() {
        return FriendScreenClient.INSTANCE;
    }

    @Override
    public void onEnable() {
        if (this.hub().getState() == FriendScreenClient$State.DISCONNECTED) {
            this.hub().connect();
        }

        this.refreshRowNames();
    }

    @Override
    public void onDisable() {
        this.hudCapturePending.set(0);
        this.worldCapturePending.set(0);
        this.draggedWorldFriend = null;
        this.lastWorldRenderNanos = 0L;
        this.worldPanels.clear();
        if (this.hudStreamFramebuffer != null) {
            this.hudStreamFramebuffer.delete();
        }

        if (this.worldStreamFramebuffer != null) {
            this.worldStreamFramebuffer.delete();
        }

        this.hudStreamFramebuffer = null;
        this.worldStreamFramebuffer = null;
    }

    @Override
    public void onTick() {
        this.hub().ensureConnectedIdentity();
        this.refreshRowNames();
        this.syncQualityRequests();
        if (!BindSetting.shouldBlockKeyInput() && this.mc.getWindow() != null) {
            long handle = this.mc.getWindow().getHandle();

            for (Entry<String, Integer> entry : this.friendBinds.entrySet()) {
                String friendLogin = entry.getKey();
                int key = entry.getValue();
                if (key > 0) {
                    boolean pressed = BindSetting.isPressed(handle, key);
                    boolean wasPressed = this.friendKeyStates.getOrDefault(friendLogin, false);
                    if (pressed && !wasPressed) {
                        this.toggleFriendByKey(friendLogin);
                    }

                    this.friendKeyStates.put(friendLogin, pressed);
                }
            }
        }
    }

    void syncQualityRequests() {
        long now = System.currentTimeMillis();
        if (now - this.lastQualitySyncAt >= 2000L && this.hub().getState() == FriendScreenClient$State.CONNECTED) {
            this.lastQualitySyncAt = now;

            for (FriendScreenClient$Entry friend : this.hub().getFriends()) {
                this.hub().requestQuality(friend.login, this.qualityFor(friend.login));
                this.hub().requestView(friend.login, this.viewFor(friend.login));
            }
        }
    }

    void refreshRowNames() {
        if (this.hub().getState() == FriendScreenClient$State.CONNECTING) {
            this.connectionStatus.setDisplayName("Статус: Подключение...");
        } else if (this.hub().getState() == FriendScreenClient$State.DISCONNECTED) {
            this.connectionStatus.setDisplayName("Статус: Не подключен (клик для подключения)");
        }
    }

    void sendFriendRequest() {
        String name = this.friendName.getValue() == null ? "" : this.friendName.getValue().trim();
        if (!name.isEmpty()) {
            this.hub().sendFriendRequest(name);
            this.friendName.setValue("");
            this.refreshRowNames();
        }
    }

    void toggleFriend(int index) {
        List<FriendScreenClient$Entry> friends = this.hub().getFriends();
        if (index < friends.size()) {
            String key = friends.get(index).login.toLowerCase(Locale.ROOT);
            if (!this.hiddenScreens.add(key)) {
                this.hiddenScreens.remove(key);
            }

            this.refreshRowNames();
        }
    }

    public void toggleFriendByKey(String login) {
        if (login != null) {
            String key = login.toLowerCase(Locale.ROOT);
            if (!this.hiddenScreens.add(key)) {
                this.hiddenScreens.remove(key);
            }

            this.refreshRowNames();
        }
    }

    void accept(int index) {
        List rows = this.hub().getRequests();
        if (index < rows.size()) {
            this.hub().accept(((FriendScreenClient$Entry)rows.get(index)).login);
            this.refreshRowNames();
        }
    }

    void decline(int index) {
        List rows = this.hub().getRequests();
        if (index < rows.size()) {
            this.hub().decline(((FriendScreenClient$Entry)rows.get(index)).login);
            this.refreshRowNames();
        }
    }

    @Override
    public void onHudRender(DrawContext context) {
        this.hub().uploadPendingFrames();
        this.handleChatEditing(context);
    }

    public void renderAfterVanillaHud(DrawContext context) {
        if (this.isEnabled()) {
            this.renderPanels(context, false);
            this.renderChatEditorHandle(context);
        }
    }

    public void captureWorldView() {
        if (this.isEnabled()) {
            this.requestFrame(FriendScreenClient$StreamView.WORLD);
        }
    }

    public void captureHudView() {
        if (this.isEnabled()) {
            this.requestFrame(FriendScreenClient$StreamView.HUD);
        }
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.mc.world != null && !this.worldPanels.isEmpty()) {
            Vec3d camera = context.camera().getCameraPos();
            this.updateWorldDrag(camera, context.camera().getYaw(), context.camera().getPitch());
            long now = System.nanoTime();
            double deltaSeconds = this.lastWorldRenderNanos == 0L
                ? 0.016666666666666666
                : MathHelper.clamp((now - this.lastWorldRenderNanos) / 1.0E9, 0.0, 0.05);
            this.lastWorldRenderNanos = now;
            double smoothing = 1.0 - Math.exp(-18.0 * deltaSeconds);
            MatrixStack matrices = context.matrixStack();

            for (Entry<String, FriendScreen$WorldPanel> entry : new HashMap<>(this.worldPanels).entrySet()) {
                Identifier texture = this.hub().getFrame(entry.getKey());
                if (texture != null) {
                    FriendScreen$WorldPanel panel = entry.getValue();
                    panel.smooth(smoothing);
                    Vec3d right = panel.right();
                    float halfWidth = panel.width * 0.5F;
                    float halfHeight = panel.height() * 0.5F;
                    matrices.push();

                    try {
                        matrices.translate(panel.center.x - camera.x, panel.center.y - camera.y, panel.center.z - camera.z);
                        Matrix4f matrix = matrices.peek().getPositionMatrix();
                        float border = MathHelper.clamp(panel.width * 0.014F, 0.025F, 0.08F);
                        BufferBuilder frame = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                        worldColorVertex(frame, matrix, right, panel.normal, -halfWidth - border, -halfHeight - border, -0.004F);
                        worldColorVertex(frame, matrix, right, panel.normal, halfWidth + border, -halfHeight - border, -0.004F);
                        worldColorVertex(frame, matrix, right, panel.normal, halfWidth + border, halfHeight + border, -0.004F);
                        worldColorVertex(frame, matrix, right, panel.normal, -halfWidth - border, halfHeight + border, -0.004F);
                        AuraRender.drawColor(frame, AuraRender.POS_COLOR_QUADS);
                        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                        worldVertex(buffer, matrix, right, -halfWidth, -halfHeight, 1.0F, 1.0F);
                        worldVertex(buffer, matrix, right, halfWidth, -halfHeight, 0.0F, 1.0F);
                        worldVertex(buffer, matrix, right, halfWidth, halfHeight, 0.0F, 0.0F);
                        worldVertex(buffer, matrix, right, -halfWidth, halfHeight, 1.0F, 0.0F);
                        AuraRender.drawTextured(buffer, AuraRender.POS_TEX_COLOR_QUADS, texture);
                    } finally {
                        matrices.pop();
                    }
                }
            }
        }
    }

    static void worldVertex(BufferBuilder buffer, Matrix4f matrix, Vec3d right, float horizontal, float vertical, float u, float v) {
        buffer.vertex(matrix, (float)right.x * horizontal, vertical, (float)right.z * horizontal).texture(u, v).color(255, 255, 255, 255);
    }

    static void worldColorVertex(BufferBuilder buffer, Matrix4f matrix, Vec3d right, Vec3d normal, float horizontal, float vertical, float depth) {
        buffer.vertex(matrix, (float)(right.x * horizontal + normal.x * depth), vertical, (float)(right.z * horizontal + normal.z * depth)).color(-16448250);
    }

    public void renderPanels(DrawContext context, boolean editorPreview) {
        int width = this.panelWidth.getIntValue();
        int height = Math.max(80, Math.round(width * 9.0F / 16.0F));
        int x = this.getPanelX(context.getScaledWindowWidth());
        int cursorY = this.panelY.getIntValue();
        int rendered = 0;

        for (FriendScreenClient$Entry friend : this.hub().getFriends()) {
            if (this.isHudVisible(friend)) {
                this.drawPanel(context, friend, x, cursorY, width, height);
                cursorY += this.panelAdvance(friend, height);
                rendered++;
            }
        }

        if (editorPreview && rendered == 0) {
            this.drawPanel(context, new FriendScreenClient$Entry("preview", "Предпросмотр друга", true), x, cursorY, width, height);
        }
    }

    int getInfoHeight(int width) {
        float hotbarScale = MathHelper.clamp((width - 80) / 182.0F, 0.4F, 0.78F);
        int hotbarHeight = Math.round(22.0F * hotbarScale);
        return Math.max(16, hotbarHeight + 4);
    }

    void drawPanel(DrawContext context, FriendScreenClient$Entry friend, int x, int y, int width, int height) {
        if (this.isCompact(friend)) {
            this.drawCompactPanel(context, friend, x, y, width);
        } else {
            int infoHeight = this.getInfoHeight(width);
            int panelTop = y - 16;
            drawPanel(context, x - 2, panelTop, width + 4, 16 + height + infoHeight, 3.5F);
            FontManager.drawCustomText(context, friend.name + (friend.online ? "" : " • не в сети"), x + 5, panelTop + 2, friend.online ? -1 : -5592406);
            Identifier frame = this.hub().getFrame(friend.login);
            int[] size = this.hub().getFrameSize(friend.login);
            if (frame != null && size != null) {
                RenderUtil.drawTexture(context, frame, x, y, width, height, 0.0F, 0.0F, size[0], size[1], size[0], size[1], true);
            } else {
                context.fill(x, y, x + width, y + height, -536278515);
                String status = friend.online ? "Ожидание трансляции..." : "Друг не в сети";
                FontManager.drawCustomText(
                    context, status, x + (width - FontManager.getWidth(status)) / 2.0F, y + height / 2.0F - FontManager.getHeight() / 2.0F, -7301987
                );
            }

            this.drawPlayerInfo(context, friend, x, y + height, width);
        }
    }

    void drawPlayerInfo(DrawContext context, FriendScreenClient$Entry friend, int x, int y, int width) {
        FriendScreenClient$PlayerInfo info = this.hub().getPlayerInfo(friend.login);
        int infoHeight = this.getInfoHeight(width);
        context.fill(x, y, x + width, y + 1, 822083583);
        String health = info == null ? "— хп" : formatHealth(info.health) + " хп";
        String distance = this.distanceText(info);
        int hpWidth = FontManager.getWidth(health);
        int gpsWidth = 19 + FontManager.getWidth(distance);
        float hotbarScale = MathHelper.clamp((width - hpWidth - gpsWidth - 22) / 182.0F, 0.35F, 0.78F);
        int hotbarWidth = Math.round(182.0F * hotbarScale);
        int hotbarHeight = Math.round(22.0F * hotbarScale);
        int contentWidth = hotbarWidth + hpWidth + gpsWidth + 16;
        int startX = x + Math.max(4, (width - contentWidth) / 2);
        int hotbarY = y + (infoHeight - hotbarHeight) / 2;
        this.drawVanillaHotbar(context, info, startX, hotbarY, hotbarScale);
        int hpX = startX + hotbarWidth + 7;
        float textY = y + (infoHeight - FontManager.getHeight()) / 2.0F - 1.0F;
        FontManager.drawCustomText(context, health, hpX, textY, -34953);
        this.drawGps(context, info, hpX + hpWidth + 7, y + infoHeight / 2, distance, Math.min(15, infoHeight - 2));
    }

    void drawVanillaHotbar(DrawContext context, FriendScreenClient$PlayerInfo info, int x, int y, float scale) {
        context.getMatrices().pushMatrix();

        try {
            context.getMatrices().translate(x, y);
            context.getMatrices().scale(scale, scale);
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR, 0, 0, 182, 22);
            if (info != null) {
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION, info.selectedSlot * 20 - 1, -1, 24, 23);
            }

            for (int i = 0; i < 9; i++) {
                ItemStack stack = info == null ? ItemStack.EMPTY : stack(info.itemIds[i], info.itemCounts[i]);
                if (!stack.isEmpty()) {
                    context.drawItem(stack, 3 + i * 20, 3);
                    context.drawStackOverlay(this.mc.textRenderer, stack, 3 + i * 20, 3);
                }
            }
        } finally {
            context.getMatrices().popMatrix();
        }
    }

    void drawCompactPanel(DrawContext context, FriendScreenClient$Entry friend, int x, int y, int ignoredWidth) {
        FriendScreenClient$PlayerInfo info = this.hub().getPlayerInfo(friend.login);
        int top = y - 16;
        float scale = this.compactScale.getFloatValue();
        int[] size = this.compactBaseSize(friend);
        context.getMatrices().pushMatrix();

        try {
            context.getMatrices().translate(x, top);
            context.getMatrices().scale(scale, scale);
            context.getMatrices().translate(-x, -top);
            this.drawCompactStyle1(context, friend, info, x, top, size[0], size[1]);
        } finally {
            context.getMatrices().popMatrix();
        }
    }

    void drawCompactStyle1(DrawContext context, FriendScreenClient$Entry friend, FriendScreenClient$PlayerInfo info, int x, int y, int width, int height) {
        float radius = 3.5F;
        RenderUtil.drawRoundedRect(context, x, y, width, height, radius, -872415232);
        float headX = x + 10.0F;
        float headY = y + 10.0F;
        float headSize = 32.0F;
        RenderUtil.drawRoundedRect(context, headX, headY, headSize, headSize, 6.0F, 855638016);
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + friend.name).getBytes(StandardCharsets.UTF_8));
        PlayerSkinDrawer.draw(context, DefaultSkinHelper.getSkinTextures(uuid), (int)headX, (int)headY, (int)headSize, -1);
        float sep1X = x + 48.0F;
        context.fill((int)sep1X, (int)(y + 18.0F), (int)(sep1X + 1.0F), (int)(y + 34.0F), 587202559);
        float contentX = x + 58.0F;
        String name = compactName(friend);
        TextRender.draw(context, name, contentX, y + 23.595F, 14.241F, -11141291);
        List<ItemStack> armor = this.compactArmor(info);
        float armorX = contentX;

        for (int i = 0; i < 4; i++) {
            ItemStack stack = i < armor.size() ? armor.get(i) : ItemStack.EMPTY;
            if (!stack.isEmpty()) {
                this.drawScaledItem(context, stack, armorX, y + 29.0F, 13.0F);
            }

            armorX += 16.0F;
        }

        float armorW = 61.0F;
        float contentW = Math.max(TextRender.width(name, 14.241F), armorW);
        float sep2X = contentX + contentW + 12.0F;
        context.fill((int)sep2X, (int)(y + 18.0F), (int)(sep2X + 1.0F), (int)(y + 34.0F), 587202559);
        float barX = sep2X + 11.0F;
        float barY = y + 12.0F;
        float barW = 3.5F;
        float barH = 28.0F;
        RenderUtil.drawRoundedRect(context, barX, barY, barW, barH, 1.75F, 1090519039);
        float fillRatio = this.healthFill(info);
        if (fillRatio > 0.01F) {
            float fillH = barH * fillRatio;
            RenderUtil.drawRoundedRect(context, barX, barY + (barH - fillH), barW, fillH, 1.75F, -48060);
        }

        String health = String.valueOf(Math.round(info == null ? 0.0F : info.health));
        float textX = barX + barW + 10.0F;
        TextRender.draw(context, health, textX, y + 31.083F, 13.224F, -48060);
    }

    int[] compactBaseSize(FriendScreenClient$Entry friend) {
        FriendScreenClient$PlayerInfo info = this.hub().getPlayerInfo(friend.login);
        String name = compactName(friend);
        float armorW = 61.0F;
        float contentW = Math.max(TextRender.width(name, 14.241F), armorW);
        String health = String.valueOf(Math.round(info == null ? 0.0F : info.health));
        float totalWidth = 58.0F + contentW + 12.0F + 1.0F + 10.0F + 3.5F + 10.0F + TextRender.width(health, 13.224F) + 14.0F;
        return new int[]{Math.round(totalWidth), 52};
    }

    List<ItemStack> compactArmor(FriendScreenClient$PlayerInfo info) {
        ArrayList<ItemStack> result = new ArrayList<>();
        if (info == null) {
            return result;
        }

        for (int i = 0; i < 4; i++) {
            result.add(stack(info.equipmentIds[i], info.equipmentCounts[i]));
        }

        return result;
    }

    static String compactName(FriendScreenClient$Entry friend) {
        String name = friend != null && friend.name != null ? friend.name : "";
        return name.length() <= 12 ? name : name.substring(0, 9) + "...";
    }

    void drawRemoteFace(DrawContext context, FriendScreenClient$Entry friend, int x, int y, int size) {
        RenderUtil.drawRoundedRect(context, x, y, size, size, 4.0F, this.targetPanelColor(1.0F));
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + friend.name).getBytes(StandardCharsets.UTF_8));
        PlayerSkinDrawer.draw(context, DefaultSkinHelper.getSkinTextures(uuid), x, y, size, -1);
    }

    void drawScaledItem(DrawContext context, ItemStack stack, float x, float y, float size) {
        context.getMatrices().pushMatrix();

        try {
            context.getMatrices().translate(x, y);
            context.getMatrices().scale(size / 16.0F, size / 16.0F);
            context.drawItem(stack, 0, 0);
        } finally {
            context.getMatrices().popMatrix();
        }
    }

    void drawGps(DrawContext context, FriendScreenClient$PlayerInfo info, int x, int centerY, String distance, int arrowSize) {
        float yawDiff = 0.0F;
        if (info != null && this.mc.player != null) {
            double dx = info.x + 0.5 - this.mc.player.getX();
            double dz = info.z + 0.5 - this.mc.player.getZ();
            float targetYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            yawDiff = MathHelper.wrapDegrees(this.mc.player.getYaw(this.mc.getRenderTickCounter().getTickProgress(true)) - targetYaw);
        }

        context.getMatrices().pushMatrix();

        try {
            context.getMatrices().translate(x + arrowSize / 2.0F, centerY);
            context.getMatrices().rotate((float)Math.toRadians(-yawDiff));
            RenderUtil.drawTexture(
                context, GPS_ARROW, -arrowSize / 2.0F, -arrowSize / 2.0F, arrowSize, arrowSize, 0.0F, 0.0F, 128.0F, 128.0F, 128.0F, 128.0F, true
            );
        } finally {
            context.getMatrices().popMatrix();
        }

        FontManager.drawCustomText(context, distance, x + arrowSize + 2, centerY - FontManager.getHeight() / 2.0F, -2762788);
    }

    String distanceText(FriendScreenClient$PlayerInfo info) {
        if (info != null && this.mc.player != null) {
            double dx = info.x - this.mc.player.getX();
            double dy = info.y - this.mc.player.getY();
            double dz = info.z - this.mc.player.getZ();
            return Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz)) + " м";
        } else {
            return "— м";
        }
    }

    static String formatHealth(float health) {
        float rounded = Math.round(health * 10.0F) / 10.0F;
        return rounded == Math.rint(rounded) ? Integer.toString(Math.round(rounded)) : String.format(Locale.ROOT, "%.1f", rounded);
    }

    static ItemStack stack(String value, int count) {
        if (value != null && !value.isBlank() && count > 0) {
            Identifier id = Identifier.tryParse(value);
            if (id == null) {
                return ItemStack.EMPTY;
            }

            Item item = Registries.ITEM.get(id);
            return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
        } else {
            return ItemStack.EMPTY;
        }
    }

    static void drawPanel(DrawContext context, int x, int y, int width, int height, float radius) {
        int base = ThemeManager.INSTANCE.getHudBackgroundColor();
        RenderUtil.drawRoundedRect(context, x, y, width, height, radius, -436207616 | base & 16777215);
    }

    static int lerpColor(int from, int to, float t) {
        int a1 = from >> 24 & 0xFF;
        int r1 = from >> 16 & 0xFF;
        int g1 = from >> 8 & 0xFF;
        int b1 = from & 0xFF;
        int a2 = to >> 24 & 0xFF;
        int r2 = to >> 16 & 0xFF;
        int g2 = to >> 8 & 0xFF;
        int b2 = to & 0xFF;
        int a = (int)(a1 + (a2 - a1) * t);
        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    void drawTargetPanel(DrawContext context, int x, int y, int width, int height, float radius, float alpha) {
        RenderUtil.drawRoundedRect(context, x, y, width, height, radius, this.targetPanelColor(alpha));
    }

    void drawTargetHpBar(DrawContext context, float x, float y, int width, float fill, int height, float radius) {
        int fillWidth = Math.max(0, Math.round(width * fill));
        RenderUtil.drawRoundedRect(context, x, y, width, height, radius, new Color(40, 40, 40, 150).getRGB());
        if (fillWidth > 0) {
            RenderUtil.drawRoundedRect(context, x, y, fillWidth, height, Math.min(radius, fillWidth / 2.0F), 0xFF000000 | Hud.accentColor.getValue() & 16777215);
        }
    }

    float healthFill(FriendScreenClient$PlayerInfo info) {
        return info != null && !(info.maxHealth <= 0.0F) ? MathHelper.clamp(info.health / info.maxHealth, 0.0F, 1.0F) : 0.0F;
    }

    int targetPanelColor(float alpha) {
        int base = ThemeManager.INSTANCE.getHudBackgroundColor();
        int a = 230;
        return applyAlpha(a << 24 | base & 16777215, alpha);
    }

    int targetTextColor() {
        return ThemeManager.INSTANCE.getHudTextColor();
    }

    int targetListTextColor() {
        return ThemeManager.INSTANCE.getHudListTextColor();
    }

    float targetRadius() {
        return 3.5F;
    }

    static int applyAlpha(int color, float alpha) {
        int base = color >>> 24 & 0xFF;
        if (base == 0) {
            base = 255;
        }

        int out = Math.max(0, Math.min(255, Math.round(base * MathHelper.clamp(alpha, 0.0F, 1.0F))));
        return out << 24 | color & 16777215;
    }

    static int modernShade(float brightness, float saturationFloor, int alpha) {
        int accent = Hud.accentColor.getValue();
        float[] hsb = Color.RGBtoHSB(accent >> 16 & 0xFF, accent >> 8 & 0xFF, accent & 0xFF, null);
        float saturation = hsb[1] < 0.08F ? Math.max(hsb[1], 0.08F) : Math.max(saturationFloor, hsb[1] * 0.94F);
        int rgb = Color.HSBtoRGB(hsb[0], MathHelper.clamp(saturation, 0.0F, 1.0F), MathHelper.clamp(brightness, 0.0F, 1.0F));
        return alpha << 24 | rgb & 16777215;
    }

    static int lighten(int color, float amount) {
        int a = color >>> 24 & 0xFF;
        if (a == 0) {
            a = 255;
        }

        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        r += Math.round((255 - r) * amount);
        g += Math.round((255 - g) * amount);
        b += Math.round((255 - b) * amount);
        return a << 24 | r << 16 | g << 8 | b;
    }

    void requestFrame(FriendScreenClient$StreamView view) {
        long now = System.currentTimeMillis();
        if (this.hub().needsCaptureView(view)) {
            FriendScreen$StreamProfile profile = FriendScreen$StreamProfile.forQuality(this.hub().getCaptureQuality());
            AtomicInteger pending = view == FriendScreenClient$StreamView.WORLD ? this.worldCapturePending : this.hudCapturePending;
            long lastFrameAt = view == FriendScreenClient$StreamView.WORLD ? this.lastWorldFrameAt : this.lastHudFrameAt;
            if (this.mc.world != null && this.hub().canSendFrame(view) && now - lastFrameAt >= profile.intervalMs && acquireCaptureSlot(pending)) {
                if (view == FriendScreenClient$StreamView.WORLD) {
                    this.lastWorldFrameAt = now;
                } else {
                    this.lastHudFrameAt = now;
                }

                try {
                    Framebuffer source = this.mc.getFramebuffer();
                    JsonObject playerInfo = this.capturePlayerInfo();
                    SimpleFramebuffer target = this.ensureStreamFramebuffer(profile, view);
                    FramebufferBlitter.blit(source.getColorAttachmentView(), target.getColorAttachmentView());
                    ScreenshotRecorder.takeScreenshot(target, image -> this.encodeAndSend(image, playerInfo, profile, view, pending));
                } catch (Throwable ignored) {
                    pending.decrementAndGet();
                }
            }
        }
    }

    static boolean acquireCaptureSlot(AtomicInteger pending) {
        int current;
        while ((current = pending.get()) < 2) {
            if (pending.compareAndSet(current, current + 1)) {
                return true;
            }
        }

        return false;
    }

    SimpleFramebuffer ensureStreamFramebuffer(FriendScreen$StreamProfile profile, FriendScreenClient$StreamView view) {
        SimpleFramebuffer framebuffer = view == FriendScreenClient$StreamView.WORLD ? this.worldStreamFramebuffer : this.hudStreamFramebuffer;
        if (framebuffer == null || framebuffer.textureWidth != profile.width || framebuffer.textureHeight != profile.height) {
            if (framebuffer != null) {
                framebuffer.delete();
            }

            framebuffer = new SimpleFramebuffer(
                "bamber client-friend-screen-stream-" + view.name().toLowerCase(Locale.ROOT), profile.width, profile.height, false
            );
            if (view == FriendScreenClient$StreamView.WORLD) {
                this.worldStreamFramebuffer = framebuffer;
            } else {
                this.hudStreamFramebuffer = framebuffer;
            }
        }

        return framebuffer;
    }

    JsonObject capturePlayerInfo() {
        JsonObject player = new JsonObject();
        if (this.mc.player == null) {
            return player;
        }

        player.addProperty("x", this.mc.player.getX());
        player.addProperty("y", this.mc.player.getY());
        player.addProperty("z", this.mc.player.getZ());
        player.addProperty("health", this.mc.player.getHealth());
        player.addProperty("max_health", this.mc.player.getMaxHealth());
        player.addProperty("selected", this.mc.player.getInventory().getSelectedSlot());
        JsonArray hotbar = new JsonArray();

        for (int i = 0; i < 9; i++) {
            ItemStack stack = this.mc.player.getInventory().getStack(i);
            JsonObject slot = new JsonObject();
            slot.addProperty("id", Registries.ITEM.getId(stack.getItem()).toString());
            slot.addProperty("count", stack.isEmpty() ? 0 : stack.getCount());
            hotbar.add(slot);
        }

        player.add("hotbar", hotbar);
        this.refreshInventoryMetadata();
        player.add("equipment", this.cachedEquipmentMetadata.deepCopy());
        player.add("inventory", this.cachedInventoryMetadata.deepCopy());
        return player;
    }

    void refreshInventoryMetadata() {
        long now = System.currentTimeMillis();
        if (now - this.lastInventoryMetadataAt >= 500L || this.cachedInventoryMetadata.size() != 36 || this.cachedEquipmentMetadata.size() != 6) {
            this.lastInventoryMetadataAt = now;
            JsonArray equipment = new JsonArray();

            for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
            }) {
                equipment.add(this.serializeStack(this.mc.player.getEquippedStack(slot)));
            }

            JsonArray inventory = new JsonArray();

            for (int i = 0; i < 36; i++) {
                inventory.add(this.serializeStack(this.mc.player.getInventory().getStack(i)));
            }

            this.cachedEquipmentMetadata = equipment;
            this.cachedInventoryMetadata = inventory;
        }
    }

    JsonObject serializeStack(ItemStack stack) {
        JsonObject slot = new JsonObject();
        slot.addProperty("id", Registries.ITEM.getId(stack.getItem()).toString());
        slot.addProperty("count", stack.isEmpty() ? 0 : stack.getCount());
        if (!stack.isEmpty()) {
            JsonArray tooltip = new JsonArray();
            TooltipContext tooltipContext = this.mc.world == null ? TooltipContext.DEFAULT : TooltipContext.create(this.mc.world);

            for (Text line : stack.getTooltip(tooltipContext, this.mc.player, TooltipType.BASIC)) {
                tooltip.add(line.getString());
                if (tooltip.size() >= 24) {
                    break;
                }
            }

            slot.add("tooltip", tooltip);
        }

        return slot;
    }

    void encodeAndSend(NativeImage image, JsonObject playerInfo, FriendScreen$StreamProfile profile, FriendScreenClient$StreamView view, AtomicInteger pending) {
        ENCODER.execute(
            () -> {
                try {
                    int width = image.getWidth();
                    int height = image.getHeight();
                    BufferedImage buffered = new BufferedImage(width, height, 1);

                    try {
                        int[] pixels = ((DataBufferInt)buffered.getRaster().getDataBuffer()).getData();

                        for (int y = 0; y < height; y++) {
                            for (int x = 0; x < width; x++) {
                                pixels[y * width + x] = image.getColorArgb(x, y) & 16777215;
                            }
                        }
                    } finally {
                        image.close();
                    }

                    float var23 = profile.jpegQuality;

                    byte[] var24;
                    for (var24 = encodeJpeg(buffered, var23, profile.initialBuffer);
                        var24.length > profile.maxFrameBytes && var23 > 0.52F;
                        var24 = encodeJpeg(buffered, var23, profile.initialBuffer)
                    ) {
                        var23 = Math.max(0.52F, var23 - 0.08F);
                    }

                    this.hub().sendFrame(var24, width, height, playerInfo, view);
                } catch (Exception var21) {
                } finally {
                    pending.decrementAndGet();
                }
            }
        );
    }

    static byte[] encodeJpeg(BufferedImage image, float quality, int initialBuffer) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream(initialBuffer);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            return new byte[0];
        }

        ImageWriter writer = writers.next();

        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(2);
            params.setCompressionQuality(quality);
            if (params instanceof JPEGImageWriteParam jpegParams) {
                jpegParams.setOptimizeHuffmanTables(true);
            }

            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }

        return output.toByteArray();
    }

    void handleChatEditing(DrawContext context) {
        if (!(this.mc.currentScreen instanceof ChatScreen)) {
            this.chatMouseDown = false;
            this.chatRightDown = false;
            this.chatDragging = false;
            this.popupOpen = false;
        } else if (this.visiblePanelCount() != 0) {
            double mouseX = this.mc.mouse.getX() * context.getScaledWindowWidth() / this.mc.getWindow().getWidth();
            double mouseY = this.mc.mouse.getY() * context.getScaledWindowHeight() / this.mc.getWindow().getHeight();
            long window = this.mc.getWindow().getHandle();
            boolean down = GLFW.glfwGetMouseButton(window, 0) == 1;
            boolean rightDown = GLFW.glfwGetMouseButton(window, 1) == 1;
            FriendScreen$PanelHit hit = this.panelAt(mouseX, mouseY, context.getScaledWindowWidth());
            boolean inPopup = this.isInPopup(mouseX, mouseY);
            if (down && !this.chatMouseDown) {
                if (inPopup && this.popupFriend != null && this.popupOpen) {
                    if (mouseY >= this.popupY + 34 && mouseY <= this.popupY + 55) {
                        String key = this.popupFriend.toLowerCase(Locale.ROOT);
                        if (!this.compactScreens.add(key)) {
                            this.compactScreens.remove(key);
                        }
                    } else if (this.compactToggleAnim < 0.5F && mouseY >= this.popupY + 72 && mouseY <= this.popupY + 93) {
                        int option = (int)((mouseX - this.popupRenderX - 9.0) / 46.0);

                        FriendScreenClient$StreamQuality quality = switch (Math.max(0, Math.min(2, option))) {
                            case 1 -> FriendScreenClient$StreamQuality.MEDIUM;
                            case 2 -> FriendScreenClient$StreamQuality.HIGH;
                            default -> FriendScreenClient$StreamQuality.LOW;
                        };
                        this.setQuality(this.popupFriend, quality);
                    } else {
                        float shift = 38.0F * this.compactToggleAnim;
                        if (mouseY >= this.popupY + 99 - shift && mouseY <= this.popupY + 120 - shift) {
                            this.spawnWorldPanel(this.popupFriend);
                            this.popupOpen = false;
                        } else if (mouseY >= this.popupY + 123 - shift && mouseY <= this.popupY + 144 - shift) {
                            this.openFriendInventory(this.popupFriend);
                        }
                    }
                } else if (hit != null) {
                    this.popupOpen = false;
                    this.chatDragging = true;
                    this.chatGrabX = mouseX - this.getPanelX(context.getScaledWindowWidth());
                    this.chatGrabY = mouseY - this.panelY.getIntValue();
                } else {
                    this.popupOpen = false;
                }
            }

            if (rightDown && !this.chatRightDown && hit != null) {
                this.popupFriend = hit.friend.login;
                this.popupOpen = true;
                this.popupY = Math.max(4, Math.min(context.getScaledWindowHeight() - 152 - 4, (int)mouseY - 18));
                int panelLeft = this.getPanelX(context.getScaledWindowWidth());
                this.popupTargetX = Math.max(4, panelLeft - 160 - 10);
                this.popupStartX = panelLeft - 6;
                if (this.popupAnimation <= 0.05F) {
                    this.popupRenderX = this.popupStartX;
                }

                this.chatDragging = false;
            }

            if (down && this.chatDragging) {
                int nextX = (int)Math.round(mouseX - this.chatGrabX);
                int nextY = (int)Math.round(mouseY - this.chatGrabY);
                nextX = Math.max(0, Math.min(context.getScaledWindowWidth() - this.displayGroupWidth(), nextX));
                int groupHeight = this.groupHeight();
                nextY = Math.max(16, Math.min(context.getScaledWindowHeight() - groupHeight + 16, nextY));
                this.setPanelPosition(nextX, nextY);
            }

            if (!down) {
                this.chatDragging = false;
            }

            this.chatMouseDown = down;
            this.chatRightDown = rightDown;
        }
    }

    void renderChatEditorHandle(DrawContext context) {
        if (!(this.mc.currentScreen instanceof ChatScreen)) {
            this.popupOpen = false;
        }

        this.popupAnimation = AnimationUtil.animate(this.popupOpen ? 1.0F : 0.0F, this.popupAnimation, 0.18F);
        if (this.popupAnimation <= 0.005F) {
            if (!this.popupOpen) {
                this.popupFriend = null;
            }
        } else {
            if (this.popupFriend != null) {
                this.drawSettingsPopup(context);
            }
        }
    }

    void drawSettingsPopup(DrawContext context) {
        float eased = MathHelper.clamp(this.popupAnimation, 0.0F, 1.0F);
        float progress = 1.0F - (1.0F - eased) * (1.0F - eased);
        this.popupRenderX = this.popupStartX + (this.popupTargetX - this.popupStartX) * progress;
        boolean enabled = this.compactScreens.contains(this.popupFriend.toLowerCase(Locale.ROOT));
        this.compactToggleAnim = AnimationUtil.animate(enabled ? 1.0F : 0.0F, this.compactToggleAnim, 0.2F);
        float currentHeight = 152.0F - 38.0F * this.compactToggleAnim;
        float centerX = this.popupRenderX + 80.0F;
        float centerY = this.popupY + currentHeight / 2.0F;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(centerX, centerY);
        float scale = 0.85F + 0.15F * progress;
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(-centerX, -centerY);
        this.drawTargetPanel(context, Math.round(this.popupRenderX), this.popupY, 160, Math.round(currentHeight), this.targetRadius(), eased);
        FontManager.drawCustomText(
            context, "FriendScreen", this.popupRenderX + 10.0F, this.popupY + 9, applyAlpha(ThemeManager.INSTANCE.getHudTitleColor(), eased)
        );
        context.fill(
            Math.round(this.popupRenderX) + 9, this.popupY + 29, Math.round(this.popupRenderX) + 160 - 9, this.popupY + 30, applyAlpha(620756991, eased)
        );
        FontManager.drawCustomText(context, "Компактное", this.popupRenderX + 10.0F, this.popupY + 41, applyAlpha(-1, eased));
        int switchX = Math.round(this.popupRenderX) + 160 - 36;
        int switchY = this.popupY + 38;
        int switchColor = lerpColor(-12236720, -10296710, this.compactToggleAnim);
        RenderUtil.drawRoundedRect(context, switchX, switchY, 26.0F, 14.0F, 7.0F, applyAlpha(switchColor, eased));
        float knobX = switchX + 2.0F + 12.0F * this.compactToggleAnim;
        RenderUtil.drawRoundedRect(context, knobX, switchY + 2, 10.0F, 10.0F, 5.0F, applyAlpha(-1, eased));
        float qualityAlpha = MathHelper.clamp(1.0F - this.compactToggleAnim, 0.0F, 1.0F);
        if (qualityAlpha > 0.01F) {
            float qAlpha = eased * qualityAlpha;
            FontManager.drawCustomText(context, "Качество", this.popupRenderX + 10.0F, this.popupY + 61, applyAlpha(-2565408, qAlpha));
            FriendScreenClient$StreamQuality quality = this.qualityFor(this.popupFriend);
            float targetCapX = Math.round(this.popupRenderX) + 9 + quality.ordinal() * 46;
            this.qualityCapsuleX = this.qualityCapsuleX == 0.0F ? targetCapX : AnimationUtil.animate(targetCapX, this.qualityCapsuleX, 0.25F);
            String[] names = new String[]{"Low", "Medium", "High"};

            for (int i = 0; i < names.length; i++) {
                int bx = Math.round(this.popupRenderX) + 9 + i * 46;
                RenderUtil.drawRoundedRect(context, bx, this.popupY + 73, 42.0F, 19.0F, 5.0F, applyAlpha(1430340944, qAlpha));
            }

            RenderUtil.drawRoundedRect(context, this.qualityCapsuleX, this.popupY + 73, 42.0F, 19.0F, 5.0F, applyAlpha(-867014488, qAlpha));

            for (int var22 = 0; var22 < names.length; var22++) {
                int bx = Math.round(this.popupRenderX) + 9 + var22 * 46;
                FontManager.drawCustomText(
                    context, names[var22], bx + (42 - FontManager.getWidth(names[var22])) / 2.0F, this.popupY + 76, applyAlpha(-1, qAlpha)
                );
            }
        }

        float shift = 38.0F * this.compactToggleAnim;
        this.drawPopupButton(context, "Отобразить в мире", Math.round(this.popupY + 99 - shift), eased);
        this.drawPopupButton(context, "Открыть инвентарь", Math.round(this.popupY + 123 - shift), eased);
        context.getMatrices().popMatrix();
    }

    void drawPopupButton(DrawContext context, String text, int y, float alpha) {
        int x = Math.round(this.popupRenderX) + 9;
        RenderUtil.drawRoundedRect(context, x, y, 142.0F, 20.0F, 5.0F, applyAlpha(-2008988336, alpha));
        FontManager.drawCustomText(context, text, x + (142 - FontManager.getWidth(text)) / 2.0F, y + 5, applyAlpha(-1710100, alpha));
    }

    FriendScreenClient$StreamQuality qualityFor(String login) {
        return this.screenQualities.getOrDefault(login == null ? "" : login.toLowerCase(Locale.ROOT), FriendScreenClient$StreamQuality.MEDIUM);
    }

    void setQuality(String login, FriendScreenClient$StreamQuality quality) {
        if (login != null && !login.isBlank()) {
            this.screenQualities.put(login.toLowerCase(Locale.ROOT), quality);
            this.hub().requestQuality(login, quality);
        }
    }

    FriendScreenClient$StreamView viewFor(String login) {
        String key = login == null ? "" : login.toLowerCase(Locale.ROOT);
        return this.worldPanels.containsKey(key) ? FriendScreenClient$StreamView.HUD : FriendScreenClient$StreamView.WORLD;
    }

    void spawnWorldPanel(String login) {
        if (login != null && !login.isBlank() && this.mc.player != null) {
            Vec3d camera = this.mc.player.getCameraPosVec(1.0F);
            Vec3d look = this.mc.player.getRotationVec(1.0F).normalize();
            Vec3d center = camera.add(look.multiply(3.0));
            Vec3d normal = horizontalNormal(center, camera, this.mc.player.getYaw());
            this.worldPanels.put(login.toLowerCase(Locale.ROOT), new FriendScreen$WorldPanel(center, normal, 3.2F));
            this.hub().requestView(login, FriendScreenClient$StreamView.HUD);
        }
    }

    public boolean handleWorldMouseButton(int button, int action) {
        if (!this.isEnabled() || this.mc.currentScreen != null || this.mc.player == null || this.worldPanels.isEmpty()) {
            return false;
        }

        if (action != 0) {
            if (action != 1) {
                return false;
            }

            FriendScreen$WorldHit hit = this.hitWorldPanel();
            if (hit == null) {
                return false;
            }

            if (button == 2) {
                this.worldPanels.remove(hit.login);
                this.hub().requestView(hit.login, FriendScreenClient$StreamView.WORLD);
                if (hit.login.equals(this.draggedWorldFriend)) {
                    this.draggedWorldFriend = null;
                }

                return true;
            } else {
                if (button != 0 && button != 1) {
                    return false;
                }

                this.draggedWorldFriend = hit.login;
                this.draggedWorldDistance = hit.distance;
                this.draggedLocalX = hit.localX;
                this.draggedLocalY = hit.localY;
                return true;
            }
        } else if ((button == 0 || button == 1) && this.draggedWorldFriend != null) {
            this.draggedWorldFriend = null;
            return true;
        } else {
            return false;
        }
    }

    public boolean handleWorldScroll(double amount) {
        if (this.isEnabled() && amount != 0.0 && this.mc.currentScreen == null && !this.worldPanels.isEmpty()) {
            FriendScreen$WorldHit hit = this.hitWorldPanel();
            if (hit == null) {
                return false;
            }

            FriendScreen$WorldPanel panel = this.worldPanels.get(hit.login);
            if (panel == null) {
                return false;
            }

            panel.width = MathHelper.clamp(panel.width * (float)(1.0 + amount * 0.1), 0.8F, 10.0F);
            return true;
        } else {
            return false;
        }
    }

    void updateWorldDrag(Vec3d camera, float yaw, float pitch) {
        if (this.draggedWorldFriend != null && this.mc.player != null && this.mc.currentScreen == null) {
            FriendScreen$WorldPanel panel = this.worldPanels.get(this.draggedWorldFriend);
            if (panel == null) {
                this.draggedWorldFriend = null;
            } else {
                long window = this.mc.getWindow().getHandle();
                boolean held = GLFW.glfwGetMouseButton(window, 0) == 1 || GLFW.glfwGetMouseButton(window, 1) == 1;
                if (!held) {
                    this.draggedWorldFriend = null;
                } else {
                    double yawRadians = Math.toRadians(yaw);
                    double pitchRadians = Math.toRadians(pitch);
                    double pitchCos = Math.cos(pitchRadians);
                    Vec3d look = new Vec3d(-Math.sin(yawRadians) * pitchCos, -Math.sin(pitchRadians), Math.cos(yawRadians) * pitchCos).normalize();
                    Vec3d rayPoint = camera.add(look.multiply(MathHelper.clamp(this.draggedWorldDistance, 1.0, 12.0)));
                    Vec3d normal = horizontalNormal(rayPoint, camera, this.mc.player.getYaw());
                    Vec3d right = new Vec3d(-normal.z, 0.0, normal.x);
                    panel.targetNormal = normal;
                    panel.targetCenter = rayPoint.subtract(right.multiply(this.draggedLocalX)).subtract(0.0, this.draggedLocalY, 0.0);
                }
            }
        }
    }

    FriendScreen$WorldHit hitWorldPanel() {
        if (this.mc.player == null) {
            return null;
        }

        Vec3d origin = this.mc.player.getCameraPosVec(1.0F);
        Vec3d direction = this.mc.player.getRotationVec(1.0F).normalize();
        FriendScreen$WorldHit closest = null;

        for (Entry<String, FriendScreen$WorldPanel> entry : this.worldPanels.entrySet()) {
            FriendScreen$WorldPanel panel = entry.getValue();
            double denominator = direction.dotProduct(panel.normal);
            double distance;
            if (!(Math.abs(denominator) < 1.0E-5)
                && !((distance = panel.center.subtract(origin).dotProduct(panel.normal) / denominator) <= 0.05)
                && !(distance > 64.0)) {
                Vec3d point = origin.add(direction.multiply(distance));
                Vec3d delta = point.subtract(panel.center);
                double localX = delta.dotProduct(panel.right());
                double localY = delta.y;
                if (!(Math.abs(localX) > panel.width * 0.5) && !(Math.abs(localY) > panel.height() * 0.5) && (closest == null || distance < closest.distance)) {
                    closest = new FriendScreen$WorldHit(entry.getKey(), distance, localX, localY);
                }
            }
        }

        return closest;
    }

    static Vec3d horizontalNormal(Vec3d center, Vec3d camera, float fallbackYaw) {
        Vec3d normal = new Vec3d(camera.x - center.x, 0.0, camera.z - center.z);
        if (normal.lengthSquared() < 1.0E-6) {
            double yaw = Math.toRadians(fallbackYaw);
            normal = new Vec3d(Math.sin(yaw), 0.0, -Math.cos(yaw));
        }

        return normal.normalize();
    }

    public boolean handleChatScroll(double mouseX, double mouseY, double amount) {
        if (this.mc.currentScreen instanceof ChatScreen && amount != 0.0 && this.isEnabled()) {
            FriendScreen$PanelHit hit = this.panelAt(mouseX, mouseY, this.mc.getWindow().getScaledWidth());
            if (hit == null && !this.isInPopup(mouseX, mouseY)) {
                return false;
            }

            if (hit != null && this.isCompact(hit.friend)) {
                this.compactScale.setValue(this.compactScale.getValue() + amount * 0.05);
            } else {
                this.resizePanel(amount * 10.0);
            }

            return true;
        } else {
            return false;
        }
    }

    FriendScreen$PanelHit panelAt(double mouseX, double mouseY, int screenWidth) {
        int width = this.panelWidth.getIntValue();
        int videoHeight = Math.max(80, Math.round(width * 9.0F / 16.0F));
        int infoHeight = this.getInfoHeight(width);
        int x = this.getPanelX(screenWidth);
        int cursorY = this.panelY.getIntValue();

        for (FriendScreenClient$Entry friend : this.hub().getFriends()) {
            if (this.isHudVisible(friend)) {
                int hitWidth = this.isCompact(friend) ? this.compactWidth(friend) : width;
                int hitHeight = this.isCompact(friend) ? this.compactHeight(friend) : 16 + videoHeight + infoHeight;
                int top = cursorY - 16;
                if (mouseX >= x - 2 && mouseX <= x + hitWidth + 2 && mouseY >= top && mouseY <= top + hitHeight) {
                    return new FriendScreen$PanelHit(friend);
                }

                cursorY += this.panelAdvance(friend, videoHeight);
            }
        }

        return null;
    }

    boolean isInPopup(double mouseX, double mouseY) {
        return this.popupOpen
            && this.popupFriend != null
            && this.popupAnimation > 0.25F
            && mouseX >= this.popupRenderX
            && mouseX <= this.popupRenderX + 160.0F
            && mouseY >= this.popupY
            && mouseY <= this.popupY + (152.0F - 38.0F * this.compactToggleAnim);
    }

    boolean isCompact(FriendScreenClient$Entry friend) {
        return this.compactScreens.contains(friend.login.toLowerCase(Locale.ROOT));
    }

    boolean isHudVisible(FriendScreenClient$Entry friend) {
        String key = friend.login.toLowerCase(Locale.ROOT);
        return !this.hiddenScreens.contains(key) && !this.worldPanels.containsKey(key);
    }

    int panelAdvance(FriendScreenClient$Entry friend, int videoHeight) {
        int infoHeight = this.getInfoHeight(this.panelWidth.getIntValue());
        return this.isCompact(friend) ? this.compactHeight(friend) + 8 : 16 + videoHeight + infoHeight + 8;
    }

    int displayGroupWidth() {
        int result = 0;
        boolean hasNormal = false;

        for (FriendScreenClient$Entry friend : this.hub().getFriends()) {
            if (this.isHudVisible(friend)) {
                if (this.isCompact(friend)) {
                    result = Math.max(result, this.compactWidth(friend));
                } else {
                    hasNormal = true;
                }
            }
        }

        if (hasNormal) {
            result = Math.max(result, this.panelWidth.getIntValue());
        }

        return result == 0 ? this.panelWidth.getIntValue() : result;
    }

    int groupHeight() {
        int width = this.panelWidth.getIntValue();
        int videoHeight = Math.max(80, Math.round(width * 9.0F / 16.0F));
        int result = 0;

        for (FriendScreenClient$Entry friend : this.hub().getFriends()) {
            if (this.isHudVisible(friend)) {
                result += this.panelAdvance(friend, videoHeight);
            }
        }

        return Math.max(0, result - 8);
    }

    int compactWidth(FriendScreenClient$Entry friend) {
        return Math.max(1, Math.round(this.compactBaseSize(friend)[0] * this.compactScale.getFloatValue()));
    }

    int compactHeight(FriendScreenClient$Entry friend) {
        return Math.max(1, Math.round(this.compactBaseSize(friend)[1] * this.compactScale.getFloatValue()));
    }

    void openFriendInventory(String login) {
        FriendScreenClient$Entry selected = null;

        for (FriendScreenClient$Entry friend : this.hub().getFriends()) {
            if (friend.login.equalsIgnoreCase(login)) {
                selected = friend;
                break;
            }
        }

        if (selected != null) {
            Screen parent = this.mc.currentScreen;
            this.mc.setScreen(new FriendInventoryScreen(parent, selected.login, selected.name));
        }
    }

    int visiblePanelCount() {
        int count = 0;

        for (FriendScreenClient$Entry friend : this.hub().getFriends()) {
            if (this.isHudVisible(friend)) {
                count++;
            }
        }

        return count;
    }

    public int getPanelX(int screenWidth) {
        return this.panelX.getIntValue() < 0 ? screenWidth - this.displayGroupWidth() - 8 : this.panelX.getIntValue();
    }

    public int getPanelY() {
        return this.panelY.getIntValue();
    }

    public int getPanelWidth() {
        return this.panelWidth.getIntValue();
    }

    public void setPanelPosition(int x, int y) {
        this.panelX.setValue((double)Math.max(0, x));
        this.panelY.setValue((double)Math.max(0, y));
    }

    public void resizePanel(double amount) {
        this.panelWidth.setValue(this.panelWidth.getValue() + amount);
    }
}
