package com.example.auravisuals.client.gui;

import com.example.auravisuals.client.gui.render.IconRender;
import com.example.auravisuals.client.gui.render.RoundedRenderer;
import com.example.auravisuals.client.gui.render.TextRender;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.ClickGui;
import com.example.auravisuals.module.render.Hud;
import com.example.auravisuals.module.render.Markers;
import com.example.auravisuals.module.render.Markers$MarkerData;
import com.example.auravisuals.util.ClientProfile;
import com.example.auravisuals.util.IrcIdentity;
import com.example.auravisuals.util.MessengerPreferences;
import com.example.auravisuals.util.chat.ChatHubClient;
import com.example.auravisuals.util.chat.ChatHubClient$State;
import com.example.auravisuals.util.chat.ChatHubMessage;
import com.example.auravisuals.util.friend.FriendHubClient;
import com.example.auravisuals.util.friend.FriendHubClient$DmMessage;
import com.example.auravisuals.util.friend.FriendHubClient$FriendEntry;
import com.example.auravisuals.util.friend.FriendHubClient$GroupData;
import com.example.auravisuals.util.friend.FriendHubClient$GroupInviteEntry;
import com.example.auravisuals.util.friend.FriendHubClient$GroupMessage;
import com.example.auravisuals.util.friend.FriendHubClient$State;
import com.example.auravisuals.util.render.MirageGlassPipeline;
import com.example.auravisuals.util.render.RenderUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public class IrcChatMessengerScreen extends Screen {
    public static IrcChatMessengerScreen INSTANCE;
    static final float DESIGN_WIDTH = 1920.0F;
    static final float DESIGN_HEIGHT = 1080.0F;
    static final float MODAL_WIDTH = 920.0F;
    static final float MODAL_HEIGHT = 540.0F;
    static final float MODAL_RADIUS = 16.0F;
    static final float SIDEBAR_WIDTH = 260.0F;
    static final float GAP = 12.0F;
    static final int ACCENT = -45478;
    static final int SELF_BUBBLE_COLOR = -564776850;
    static final int OTHER_BUBBLE_COLOR = -652798176;
    static final int PANEL_COLOR = -552792814;
    static final int CARD_COLOR = 1209933352;
    static final int INPUT_BG = 1712855074;
    static final int TEXT_COLOR = -1;
    static final int TEXT_SECOND = -1962934273;
    static final int TEXT_HINT = 1442840575;
    static final int ONLINE_GREEN = -13318311;
    static final int BUTTON_HOVER = 721420287;
    static final int GEO_BTN_BG = 872415231;
    static final long GLOBAL_CHAT_COOLDOWN_MS = 5000L;
    static final long DIRECT_CHAT_COOLDOWN_MS = 3000L;
    static final int MAX_CHAT_MESSAGE_LENGTH = 40;
    static final String[] SIDEBAR_SECTIONS = new String[]{"Чаты", "Группы", "Заявки"};
    static final float TAB_SEPARATOR_SPACING = 16.0F;
    static long lastGlobalMessageSentAt;
    static final Identifier FALLBACK_AVATAR = Identifier.of("auravisuals", "icons/avatar.png");
    static final Pattern GEO_PATTERN = Pattern.compile(
        "(?i)(?:\\[geo:\\s*|геопозиция[\\s:]*|координаты:[\\s:]*|geo:\\s*)(-?\\d+(?:\\.\\d+)?)[\\s,]+(-?\\d+(?:\\.\\d+)?)[\\s,]+(-?\\d+(?:\\.\\d+)?)"
    );
    float scale = 1.0F;
    float originX;
    float originY;
    float openProgress = 0.0F;
    float chatSwitchProgress = 1.0F;
    String chatInput = "";
    String addFriendInput = "";
    String groupNameInput = "";
    boolean isChatInputActive = true;
    boolean isAddFriendActive = false;
    float messageScroll = 0.0F;
    float targetMessageScroll = 0.0F;
    float sidebarScroll = 0.0F;
    float targetSidebarScroll = 0.0F;
    boolean showActionMenu = false;
    float actionMenuProgress;
    boolean headerMenuOpen;
    float headerMenuProgress;
    ChatHubMessage messageMenuMessage;
    boolean messageMenuOpen;
    boolean messageMenuAllowFriendRequest;
    float messageMenuProgress;
    float messageMenuX;
    float messageMenuY;
    float messageMenuScrollAtOpen;
    boolean memberMenuOpen;
    float memberMenuProgress;
    String memberMenuLogin;
    float memberMenuX;
    float memberMenuY;
    boolean groupMenuOpen;
    float groupMenuProgress;
    String groupMenuId;
    float groupMenuX;
    float groupMenuY;
    final String initialConversationType;
    final String initialConversationPeer;
    int selectedChatIndex = 0;
    int sidebarSection = 0;
    String selectedFriendLogin;
    String selectedLocalGroup;
    String pendingCreatedGroupName;
    float groupViewProgress = 1.0F;
    final Map<String, Float> rowHoverAnimations = new HashMap<>();
    final Map<String, Float> messageAppearAnimations = new HashMap<>();
    final Map<String, Long> lastDirectMessageSentAt = new HashMap<>();
    float sectionTransition = 1.0F;
    final float[] sectionTabAnimations = new float[]{1.0F, 0.0F, 0.0F};
    float addButtonAnimation;
    static final List<IrcChatMessengerScreen$MessengerToast> TOASTS = new CopyOnWriteArrayList<>();
    final List<IrcChatMessengerScreen$PanelNotice> panelNotices = new CopyOnWriteArrayList<>();
    static final long TOAST_DURATION_MS = 4000L;

    public IrcChatMessengerScreen() {
        this("global", null);
    }

    IrcChatMessengerScreen(String conversationType, String conversationPeer) {
        super(Text.empty());
        this.initialConversationType = conversationType == null ? "global" : conversationType;
        this.initialConversationPeer = conversationPeer;
        INSTANCE = this;
    }

    @Override
    protected void init() {
        super.init();
        ChatHubClient hub = ChatHubClient.INSTANCE;
        hub.ensureIdentityCurrent();
        hub.loadAvatar(ClientProfile.getAvatarUrl());
        FriendHubClient.INSTANCE.ensureIdentityCurrent();
        if (hub.getState() == ChatHubClient$State.DISCONNECTED) {
            hub.connect(ChatHubClient.DEFAULT_URL);
        }

        if (FriendHubClient.INSTANCE.getState() == FriendHubClient$State.DISCONNECTED) {
            FriendHubClient.INSTANCE.connect(FriendHubClient.DEFAULT_URL);
        }

        FriendHubClient.INSTANCE.setOnHubRequestResult(result -> {
            if ("friend_ok".equals(result)) {
                this.showPanelNotice("Заявка в друзья отправлена");
            } else if ("friend_not_found".equals(result)) {
                this.showPanelNotice("Не удалось отправить заявку");
            } else if ("group_ok".equals(result)) {
                this.showPanelNotice("Приглашение в группу отправлено");
            } else if ("group_not_found".equals(result)) {
                this.showPanelNotice("Не удалось пригласить участника");
            }
        });
        this.sidebarSection = "request".equalsIgnoreCase(this.initialConversationType) ? 2 : ("group".equalsIgnoreCase(this.initialConversationType) ? 1 : 0);
        this.selectedChatIndex = "dm".equalsIgnoreCase(this.initialConversationType) ? 1 : 0;
        this.selectedFriendLogin = "dm".equalsIgnoreCase(this.initialConversationType) ? this.initialConversationPeer : null;
        String string = this.selectedLocalGroup = "group".equalsIgnoreCase(this.initialConversationType) ? this.initialConversationPeer : null;
        if (this.selectedFriendLogin != null) {
            FriendHubClient.INSTANCE.requestDmHistory(this.selectedFriendLogin);
        }

        FriendHubClient.INSTANCE.sendPresenceFromMc();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        float alpha = 0.85F * this.openProgress;
        int edge = fade(-1207959552, alpha);
        int center = fade(-1879048192, alpha);
        RenderUtil.drawVerticalGradientRect(context, 0.0F, 0.0F, this.width, this.height / 2.0F, edge, center);
        RenderUtil.drawVerticalGradientRect(context, 0.0F, this.height / 2.0F, this.width, this.height, center, edge);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this.openProgress = Math.min(1.0F, this.openProgress + 0.09F);
        this.sectionTransition = MathHelper.lerp(0.22F, this.sectionTransition, 1.0F);
        this.groupViewProgress = MathHelper.lerp(0.18F, this.groupViewProgress, 1.0F);
        this.chatSwitchProgress = MathHelper.lerp(0.18F, this.chatSwitchProgress, 1.0F);
        this.actionMenuProgress = MathHelper.lerp(this.showActionMenu ? 0.22F : 0.16F, this.actionMenuProgress, this.showActionMenu ? 1.0F : 0.0F);
        if (!this.showActionMenu && this.actionMenuProgress < 0.01F) {
            this.actionMenuProgress = 0.0F;
        }

        this.headerMenuProgress = MathHelper.lerp(this.headerMenuOpen ? 0.22F : 0.16F, this.headerMenuProgress, this.headerMenuOpen ? 1.0F : 0.0F);
        this.messageMenuProgress = MathHelper.lerp(this.messageMenuOpen ? 0.22F : 0.16F, this.messageMenuProgress, this.messageMenuOpen ? 1.0F : 0.0F);
        this.memberMenuProgress = MathHelper.lerp(this.memberMenuOpen ? 0.22F : 0.16F, this.memberMenuProgress, this.memberMenuOpen ? 1.0F : 0.0F);
        this.groupMenuProgress = MathHelper.lerp(this.groupMenuOpen ? 0.22F : 0.16F, this.groupMenuProgress, this.groupMenuOpen ? 1.0F : 0.0F);
        if (!this.headerMenuOpen && this.headerMenuProgress < 0.01F) {
            this.headerMenuProgress = 0.0F;
        }

        if (!this.messageMenuOpen && this.messageMenuProgress < 0.01F) {
            this.messageMenuProgress = 0.0F;
        }

        if (!this.memberMenuOpen && this.memberMenuProgress < 0.01F) {
            this.memberMenuProgress = 0.0F;
        }

        if (!this.groupMenuOpen && this.groupMenuProgress < 0.01F) {
            this.groupMenuProgress = 0.0F;
        }

        this.addButtonAnimation = MathHelper.lerp(0.2F, this.addButtonAnimation, 0.0F);
        float easeOpen = 1.0F - (float)Math.pow(1.0F - this.openProgress, 3.0);
        float animScale = 0.94F + 0.06F * easeOpen;
        this.scale = Math.min(this.width / 1920.0F, this.height / 1080.0F) * animScale;
        this.originX = Math.round((this.width - 920.0F * this.scale) / 2.0F);
        this.originY = Math.round((this.height - 540.0F * this.scale) / 2.0F);
        float designMouseX = (mouseX - this.originX) / this.scale;
        float designMouseY = (mouseY - this.originY) / this.scale;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(this.originX, this.originY);
        context.getMatrices().scale(this.scale, this.scale);
        MirageGlassPipeline.captureFrame();
        this.renderSidebar(context, designMouseX, designMouseY);
        this.renderChatArea(context, designMouseX, designMouseY);
        this.renderPanelNotices(context);
        context.getMatrices().popMatrix();
    }

    void renderSidebar(DrawContext context, float mouseX, float mouseY) {
        this.resolvePendingCreatedGroup();
        float sideX = 0.0F;
        float sideY = 0.0F;
        float sideH = 540.0F;
        MirageGlassPipeline.drawBlurAt(context, sideX, sideY, 260.0F, sideH, 16.0F, 24.0F * this.scale, this.openProgress);
        RoundedRenderer.drawRoundedRect(context, sideX, sideY, 260.0F, sideH, 16.0F, fade(-552792814, this.openProgress));
        float topPad = 16.0F;
        boolean openedGroup = this.sidebarSection == 1 && this.selectedGroup() != null;
        String actionLabel = this.sidebarSection == 1 ? (openedGroup ? "Добавить участника" : "Создать группу") : "Добавить друга";
        TextRender.draw(context, actionLabel, sideX + 16.0F, sideY + topPad + 12.0F, 13.0F, -1962934273);
        float addBoxY = sideY + topPad + 20.0F;
        float addBoxW = 192.0F;
        float addBoxH = 34.0F;
        RoundedRenderer.drawRoundedRect(context, sideX + 16.0F, addBoxY, addBoxW, addBoxH, 8.0F, this.isAddFriendActive ? 1914379046 : 1712855074);
        String activeActionInput = this.sidebarSection == 1 ? this.groupNameInput : this.addFriendInput;
        String actionHint;
        if (this.sidebarSection == 1) {
            actionHint = openedGroup ? "Введите ник" : "Введите название группы";
        } else {
            actionHint = "Введите ник друга...";
        }

        String addPrompt = activeActionInput.isEmpty()
            ? (this.isAddFriendActive ? "|" : actionHint)
            : activeActionInput + (this.isAddFriendActive && System.currentTimeMillis() / 500L % 2L == 0L ? "|" : "");
        int addTextColor = activeActionInput.isEmpty() ? 1442840575 : -1;
        TextRender.draw(context, addPrompt, sideX + 24.0F, addBoxY + 21.0F, 11.5F, addTextColor);
        float addBtnX = sideX + 16.0F + addBoxW + 6.0F;
        float addBtnW = 30.0F;
        float addBtnH = 34.0F;
        boolean addBtnHov = inside(mouseX, mouseY, addBtnX, addBoxY, addBtnW, addBtnH);
        float addPress = this.addButtonAnimation;
        RoundedRenderer.drawRoundedRect(
            context, addBtnX, addBoxY + addPress * 1.5F, addBtnW, addBtnH - addPress * 2.0F, 8.0F, addBtnHov ? 2080374783 : 1209933352
        );
        drawPlusIcon(context, addBtnX + addBtnW / 2.0F, addBoxY + addBtnH / 2.0F, 12.0F, -1);
        float chatsLabelY = addBoxY + addBoxH + 10.0F;
        float sectionX = sidebarTabsStartX();
        int liveAccent = accentColor();

        for (int index = 0; index < SIDEBAR_SECTIONS.length; index++) {
            this.sectionTabAnimations[index] = MathHelper.lerp(0.11F, this.sectionTabAnimations[index], index == this.sidebarSection ? 1.0F : 0.0F);
            int color = blendColor(-1962934273, liveAccent, this.sectionTabAnimations[index]);
            TextRender.draw(context, SIDEBAR_SECTIONS[index], sectionX, chatsLabelY + 10.0F, 12.5F, color);
            sectionX += TextRender.width(SIDEBAR_SECTIONS[index], 12.5F);
            if (index < SIDEBAR_SECTIONS.length - 1) {
                TextRender.draw(context, "|", sectionX + 8.0F, chatsLabelY + 10.0F, 12.5F, -1962934273);
                sectionX += TextRender.width("|", 12.5F) + 16.0F;
            }
        }

        this.sidebarScroll = MathHelper.lerp(0.22F, this.sidebarScroll, this.targetSidebarScroll);
        float dividerY = chatsLabelY + 18.0F;
        RoundedRenderer.drawRoundedRect(context, sideX + 16.0F, dividerY, 228.0F, 1.0F, 0.5F, 620756991);
        float chatListClipY = dividerY + 7.0F;
        float chatListY = chatListClipY + (1.0F - this.sectionTransition) * 8.0F - this.sidebarScroll;
        context.enableScissor(0, (int)chatListClipY, 260, 468);
        if (this.sidebarSection == 0) {
            int itemBg = 0;
            boolean globalSelected = this.selectedChatIndex == 0;
            boolean globalHovered = inside(mouseX, mouseY, sideX + 12.0F, chatListY, 236.0F, 50.0F);
            if (globalSelected) {
                int n = 1209933352;
            } else {
                itemBg = globalHovered ? 1075715624 : 0;
            }

            if (itemBg != 0) {
                RoundedRenderer.drawRoundedRect(context, sideX + 12.0F, chatListY, 236.0F, 50.0F, 10.0F, itemBg);
            }

            float avSize = 34.0F;
            float avX = sideX + 20.0F;
            float avY = chatListY + (50.0F - avSize) / 2.0F;
            RenderUtil.drawRoundedTexture(
                context, FALLBACK_AVATAR, avX, avY, avSize, avSize, avSize / 2.0F, true, true, true, true, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F
            );
            TextRender.draw(context, "Общий", avX + avSize + 10.0F, chatListY + 22.0F, 13.5F, -1);
            List msgs = ChatHubClient.INSTANCE.getMessages();
            Object lastSnippet = "Нет сообщений";
            ChatHubMessage last;
            if (!msgs.isEmpty()
                && ((String)(
                            lastSnippet = ((last = (ChatHubMessage)msgs.get(msgs.size() - 1)).login().equalsIgnoreCase(getUsername()) ? "Вы" : last.login())
                                + ": "
                                + last.message()
                        ))
                        .length()
                    > 22) {
                lastSnippet = ((String)lastSnippet).substring(0, 20) + "...";
            }

            TextRender.draw(context, (String)lastSnippet, avX + avSize + 10.0F, chatListY + 38.0F, 11.0F, -1962934273);
            float friendY = chatListY + 56.0F;

            for (FriendHubClient$FriendEntry friend : sortedFriends()) {
                float hover = this.animateRow("friend:" + friend.login, inside(mouseX, mouseY, sideX + 12.0F, friendY, 236.0F, 48.0F));
                boolean selectedFriend = friend.login.equalsIgnoreCase(this.selectedFriendLogin == null ? "" : this.selectedFriendLogin);
                if (selectedFriend || hover > 0.02F) {
                    int alpha = Math.round((selectedFriend ? 0.42F : 0.2F * hover) * 255.0F);
                    RoundedRenderer.drawRoundedRect(context, sideX + 12.0F, friendY, 236.0F, 48.0F, 10.0F, alpha << 24 | 1973800);
                }

                this.drawFriendAvatar(context, friend, sideX + 20.0F, friendY + 8.0F, 32.0F);
                TextRender.draw(context, friend.login, sideX + 62.0F, friendY + 21.0F, 12.5F, -1);
                TextRender.draw(context, friend.online ? "В сети" : "Не в сети", sideX + 62.0F, friendY + 37.0F, 10.5F, friend.online ? -13318311 : -1962934273);
                friendY += 54.0F;
            }
        } else if (this.sidebarSection == 1) {
            FriendHubClient$GroupData group = this.selectedGroup();
            float slide = (1.0F - this.groupViewProgress) * 18.0F;
            if (group == null) {
                float groupY = chatListY + slide;
                List<FriendHubClient$GroupData> groups = FriendHubClient.INSTANCE.getGroups();

                for (FriendHubClient$GroupData entry : groups) {
                    float hover = this.animateRow("group:" + entry.id, inside(mouseX, mouseY, sideX + 12.0F, groupY, 236.0F, 48.0F));
                    if (hover > 0.02F) {
                        int alpha = Math.round(0.2F * hover * 255.0F);
                        RoundedRenderer.drawRoundedRect(context, sideX + 12.0F, groupY, 236.0F, 48.0F, 10.0F, alpha << 24 | 1973800);
                    }

                    this.drawGroupIcon(context, entry, sideX + 20.0F, groupY + 8.0F, 32.0F);
                    TextRender.draw(context, entry.name, sideX + 62.0F, groupY + 22.0F, 12.5F, fade(-1, this.groupViewProgress));
                    int memberCount = groupMemberCount(entry);
                    TextRender.draw(
                        context,
                        memberCount + " " + participantWord(memberCount),
                        sideX + 62.0F,
                        groupY + 38.0F,
                        10.5F,
                        fade(-1962934273, this.groupViewProgress)
                    );
                    groupY += 54.0F;
                }

                if (groups.isEmpty()) {
                    TextRender.draw(context, "Созданных групп пока нет", sideX + 20.0F, chatListY + 28.0F, 11.0F, -1962934273);
                }
            } else {
                float contentY = chatListY + slide;
                String membersTitle = "Участники группы " + group.name;
                float membersTitleWidth = TextRender.width(membersTitle, 10.5F);
                TextRender.draw(
                    context, membersTitle, sideX + (260.0F - membersTitleWidth) / 2.0F, contentY + 12.0F, 10.5F, fade(-1962934273, this.groupViewProgress)
                );
                float memberY = contentY + 25.0F;

                for (String member : groupMembers(group)) {
                    FriendHubClient$FriendEntry friend = findFriend(member);
                    if (friend != null) {
                        this.drawFriendAvatar(context, friend, sideX + 20.0F, memberY + 7.0F, 30.0F);
                    } else {
                        this.drawFallbackAvatar(context, sideX + 20.0F, memberY + 7.0F, 30.0F);
                    }

                    String suffix = member.equalsIgnoreCase(group.owner) ? " · Владелец" : "";
                    TextRender.draw(context, member + suffix, sideX + 60.0F, memberY + 21.0F, 11.5F, fade(-1, this.groupViewProgress));
                    boolean online = member.equalsIgnoreCase(getUsername()) || friend != null && friend.online;
                    TextRender.draw(
                        context,
                        online ? "В сети" : "Не в сети",
                        sideX + 60.0F,
                        memberY + 36.0F,
                        10.0F,
                        fade(online ? -13318311 : -1962934273, this.groupViewProgress)
                    );
                    memberY += 50.0F;
                }
            }
        } else {
            float requestY = chatListY;
            List<FriendHubClient$FriendEntry> requests = FriendHubClient.INSTANCE.getIncomingRequests();

            for (Object request : requests) {
                RoundedRenderer.drawRoundedRect(context, sideX + 12.0F, requestY, 236.0F, 54.0F, 10.0F, 1209933352);
                this.drawFriendAvatar(context, (FriendHubClient$FriendEntry)request, sideX + 20.0F, requestY + 10.0F, 32.0F);
                TextRender.draw(context, ((FriendHubClient$FriendEntry)request).login, sideX + 62.0F, requestY + 23.0F, 12.0F, -1);
                this.drawRequestButton(context, sideX + 62.0F, requestY + 30.0F, 52.0F, "Принять", true);
                this.drawRequestButton(context, sideX + 120.0F, requestY + 30.0F, 58.0F, "Отклонить", false);
                requestY += 60.0F;
            }

            List<FriendHubClient$GroupInviteEntry> groupInvites = FriendHubClient.INSTANCE.getIncomingGroupInvites();

            for (FriendHubClient$GroupInviteEntry invite : groupInvites) {
                RoundedRenderer.drawRoundedRect(context, sideX + 12.0F, requestY, 236.0F, 52.0F, 10.0F, 1209933352);
                this.drawGroupIcon(context, invite.avatarUrl, sideX + 20.0F, requestY + 10.0F, 30.0F);
                String inviteTitle = "Группа " + invite.groupName + "  |  " + invite.from;
                TextRender.draw(context, trimText(inviteTitle, 10.5F, 182.0F), sideX + 58.0F, requestY + 19.0F, 10.5F, -1);
                this.drawRequestButton(context, sideX + 58.0F, requestY + 29.0F, 52.0F, "Принять", true);
                this.drawRequestButton(context, sideX + 116.0F, requestY + 29.0F, 58.0F, "Отклонить", false);
                requestY += 58.0F;
            }

            if (requests.isEmpty() && groupInvites.isEmpty()) {
                TextRender.draw(context, "Новых заявок нет", sideX + 20.0F, chatListY + 28.0F, 11.0F, -1962934273);
            }
        }

        context.disableScissor();
        if (this.memberMenuOpen || this.memberMenuProgress > 0.0F) {
            this.renderMemberMenu(context, mouseX, mouseY);
        }

        if (this.groupMenuOpen || this.groupMenuProgress > 0.0F) {
            this.renderGroupMenu(context, mouseX, mouseY);
        }

        FriendHubClient$GroupData selectedGroup = this.selectedGroup();
        int itemCount = this.sidebarSection == 0
            ? 1 + FriendHubClient.INSTANCE.getFriends().size()
            : (
                this.sidebarSection == 1
                    ? (selectedGroup == null ? FriendHubClient.INSTANCE.getGroups().size() : groupMembers(selectedGroup).size() + 1)
                    : FriendHubClient.INSTANCE.getIncomingRequestCount()
            );
        float itemStep = this.sidebarSection == 2 ? 60.0F : 54.0F;
        float contentHeight = this.sidebarSection == 2
            ? FriendHubClient.INSTANCE.getIncomingRequests().size() * 60.0F + FriendHubClient.INSTANCE.getIncomingGroupInvites().size() * 58.0F + 8.0F
            : itemCount * itemStep + 8.0F;
        float visibleHeight = 468.0F - chatListClipY;
        this.targetSidebarScroll = MathHelper.clamp(this.targetSidebarScroll, 0.0F, Math.max(0.0F, contentHeight - visibleHeight));
        float profH = 52.0F;
        float profW = 236.0F;
        float profX = sideX + 12.0F;
        float profY = sideH - profH - 12.0F;
        RoundedRenderer.drawRoundedRect(context, profX, profY, profW, profH, 12.0F, 1209933352);
        float userAvSize = 32.0F;
        float userAvX = profX + 10.0F;
        float userAvY = profY + (profH - userAvSize) / 2.0F;
        MinecraftClient mc = MinecraftClient.getInstance();
        String profileAvatarUrl = ClientProfile.getAvatarUrl();
        Identifier profileAvatar = ChatHubClient.INSTANCE.getAvatarTexture(profileAvatarUrl);
        if (profileAvatar == null && profileAvatarUrl != null && !profileAvatarUrl.isBlank()) {
            ChatHubClient.INSTANCE.loadAvatar(profileAvatarUrl);
        }

        if (profileAvatar != null) {
            RenderUtil.drawRoundedTexture(
                context, profileAvatar, userAvX, userAvY, userAvSize, userAvSize, userAvSize / 2.0F, true, true, true, true, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F
            );
        } else {
            ClientPlayerEntity class_7462 = mc.player;
            if (class_7462 instanceof AbstractClientPlayerEntity) {
                ClientPlayerEntity playerEntity = class_7462;
                context.getMatrices().pushMatrix();
                context.getMatrices().translate(userAvX, userAvY);
                context.getMatrices().scale(userAvSize / 8.0F, userAvSize / 8.0F);
                PlayerSkinDrawer.draw(context, playerEntity.getSkin(), 0, 0, 8);
                context.getMatrices().popMatrix();
            } else {
                RenderUtil.drawRoundedTexture(
                    context,
                    FALLBACK_AVATAR,
                    userAvX,
                    userAvY,
                    userAvSize,
                    userAvSize,
                    userAvSize / 2.0F,
                    true,
                    true,
                    true,
                    true,
                    0.0F,
                    0.0F,
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
                );
            }
        }

        RoundedRenderer.drawRoundedRect(context, userAvX + userAvSize - 6.0F, userAvY + userAvSize - 6.0F, 7.0F, 7.0F, 3.5F, -13318311);
        String userName = getUsername();
        String activity = mc.isInSingleplayer()
            ? "В одиночной игре"
            : (mc.getCurrentServerEntry() != null ? "На сервере " + mc.getCurrentServerEntry().address : "В сети");
        TextRender.draw(context, userName, userAvX + userAvSize + 10.0F, profY + 22.0F, 13.0F, -1);
        TextRender.draw(context, activity, userAvX + userAvSize + 10.0F, profY + 38.0F, 10.5F, -1962934273);
    }

    void renderChatArea(DrawContext context, float mouseX, float mouseY) {
        float chatX = 272.0F;
        float chatY = 0.0F;
        float chatW = 920.0F - chatX;
        float chatH = 540.0F;
        MirageGlassPipeline.drawBlurAt(context, chatX, chatY, chatW, chatH, 16.0F, 24.0F * this.scale, this.openProgress);
        RoundedRenderer.drawRoundedRect(context, chatX, chatY, chatW, chatH, 16.0F, fade(-552792814, this.openProgress));
        float chatEase = easeOut(this.chatSwitchProgress);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate((1.0F - chatEase) * 14.0F, 0.0F);
        FriendHubClient$GroupData activeGroup = this.selectedGroup();
        if (this.sidebarSection == 1 && activeGroup == null) {
            String hint = "Выберите группу для общения";
            float hintWidth = TextRender.width(hint, 13.0F);
            float alpha = MathHelper.clamp(this.groupViewProgress, 0.0F, 1.0F);
            TextRender.draw(context, hint, chatX + (chatW - hintWidth) / 2.0F, chatH / 2.0F + (1.0F - alpha) * 10.0F, 13.0F, fade(-1962934273, alpha));
            context.getMatrices().popMatrix();
        } else {
            float headerH = 50.0F;
            RoundedRenderer.drawRoundedRect(context, chatX, chatY, chatW, headerH, 12.0F, 1209933352);
            float hAvSize = 32.0F;
            float hAvX = chatX + 14.0F;
            float hAvY = chatY + (headerH - hAvSize) / 2.0F;
            FriendHubClient$FriendEntry headerFriend = this.selectedFriendLogin == null ? null : findFriend(this.selectedFriendLogin);
            if (activeGroup != null) {
                this.drawGroupIcon(context, activeGroup, hAvX, hAvY, hAvSize);
            } else if (headerFriend != null) {
                this.drawFriendAvatar(context, headerFriend, hAvX, hAvY, hAvSize);
            } else {
                RenderUtil.drawRoundedTexture(
                    context, FALLBACK_AVATAR, hAvX, hAvY, hAvSize, hAvSize, hAvSize / 2.0F, true, true, true, true, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F
                );
            }

            String chatTitle = this.selectedFriendLogin != null ? this.selectedFriendLogin : (activeGroup != null ? activeGroup.name : "Общий");
            TextRender.draw(context, chatTitle, hAvX + hAvSize + 12.0F, chatY + 21.0F, 14.0F, -1);
            String onlineText;
            if (this.selectedFriendLogin != null) {
                onlineText = "Личные сообщения";
            } else {
                onlineText = activeGroup != null ? groupMemberCount(activeGroup) + " участников" : "В сети: " + ChatHubClient.INSTANCE.getOnlineCount();
            }

            if (!onlineText.isEmpty()) {
                TextRender.draw(context, onlineText, hAvX + hAvSize + 12.0F, chatY + 37.0F, 11.0F, -1962934273);
            }

            float dotsY;
            float dotsX;
            if (inside(mouseX, mouseY, dotsX = chatX + chatW - 34.0F, dotsY = chatY + 11.0F, 24.0F, 28.0F)) {
                RoundedRenderer.drawRoundedRect(context, dotsX, dotsY, 24.0F, 28.0F, 8.0F, 721420287);
            }

            for (int index = 0; index < 3; index++) {
                RoundedRenderer.drawRoundedRect(context, dotsX + 10.5F, dotsY + 7.0F + index * 6.0F, 3.0F, 3.0F, 1.5F, -1962934273);
            }

            float inputH = 46.0F;
            float inputY = chatH - inputH;
            RoundedRenderer.drawRoundedRect(context, chatX, inputY, chatW, inputH, 12.0F, 1209933352);
            float actionBtnX = chatX + 10.0F;
            float actionBtnY = inputY + (inputH - 28.0F) / 2.0F;
            float actionBtnSize = 28.0F;
            boolean actHov = inside(mouseX, mouseY, actionBtnX, actionBtnY, actionBtnSize, actionBtnSize);
            RoundedRenderer.drawRoundedRect(context, actionBtnX, actionBtnY, actionBtnSize, actionBtnSize, 14.0F, actHov ? 1291845631 : 1209933352);
            drawPlusIcon(context, actionBtnX + actionBtnSize / 2.0F, actionBtnY + actionBtnSize / 2.0F, 11.0F, -1);
            float textInputX = actionBtnX + actionBtnSize + 10.0F;
            String prompt = this.chatInput.isEmpty()
                ? (this.isChatInputActive ? "|" : "Сообщение...")
                : this.chatInput + (this.isChatInputActive && System.currentTimeMillis() / 500L % 2L == 0L ? "|" : "");
            int textColor = this.chatInput.isEmpty() ? 1442840575 : -1;
            TextRender.draw(context, prompt, textInputX, inputY + 28.0F, 13.0F, textColor);
            float feedY = headerH + 6.0F;
            float feedH = inputY - feedY - 6.0F;
            this.messageScroll = MathHelper.lerp(0.24F, this.messageScroll, this.targetMessageScroll);
            context.enableScissor((int)chatX, (int)feedY, (int)(chatX + chatW), (int)(feedY + feedH));
            boolean connected = this.selectedFriendLogin == null && activeGroup == null
                ? ChatHubClient.INSTANCE.getState() == ChatHubClient$State.CONNECTED
                : FriendHubClient.INSTANCE.getState() == FriendHubClient$State.CONNECTED;
            if (!connected) {
                String connectionError = this.selectedFriendLogin == null && activeGroup == null
                    ? ChatHubClient.INSTANCE.getLastConnectionError()
                    : FriendHubClient.INSTANCE.getLastConnectionError();
                String stateText = connectionError != null && !connectionError.isBlank() ? connectionError : "Подключение к серверу...";
                float stateWidth = TextRender.width(stateText, 13.0F);
                TextRender.draw(context, stateText, chatX + (chatW - stateWidth) / 2.0F, feedY + feedH / 2.0F, 13.0F, -1962934273);
            } else if (activeGroup != null) {
                this.renderGroupMessages(context, activeGroup, chatX, feedY, chatW, feedH);
            } else if (this.selectedFriendLogin != null) {
                this.renderDirectMessages(context, chatX, feedY, chatW, feedH);
            } else {
                this.renderMessages(context, chatX, feedY, chatW, feedH, mouseX, mouseY);
            }

            context.disableScissor();
            if (this.showActionMenu || this.actionMenuProgress > 0.0F) {
                this.renderActionMenu(context, actionBtnX, inputY - 48.0F, mouseX, mouseY, this.actionMenuProgress);
            }

            if (this.messageMenuOpen || this.messageMenuProgress > 0.0F) {
                this.renderMessageMenu(context, mouseX, mouseY);
            }

            if (this.headerMenuOpen || this.headerMenuProgress > 0.0F) {
                this.renderHeaderMenu(context, chatX, chatW, mouseX, mouseY);
            }

            context.getMatrices().popMatrix();
        }
    }

    void beginChatSwitch() {
        this.chatSwitchProgress = 0.0F;
        this.headerMenuOpen = false;
        this.messageMenuOpen = false;
        this.showActionMenu = false;
    }

    void renderMessages(DrawContext context, float feedX, float feedY, float feedW, float feedH, float mouseX, float mouseY) {
        List msgs = ChatHubClient.INSTANCE.getMessages();
        String myName = getUsername();
        float currentY = feedY + feedH - 10.0F + this.messageScroll;
        float maxBubbleWidth = feedW * 0.62F;

        for (int i = msgs.size() - 1; i >= 0; i--) {
            ChatHubMessage msg = (ChatHubMessage)msgs.get(i);
            boolean isSelf = msg.login().equalsIgnoreCase(myName);
            boolean isGeo = isGeoMessage(msg.message());
            String animationKey = "global:" + msg.login() + ":" + msg.time() + ":" + msg.message();
            float appear = MathHelper.lerp(0.2F, this.messageAppearAnimations.getOrDefault(animationKey, 0.0F), 1.0F);
            this.messageAppearAnimations.put(animationKey, appear);
            float bubbleW = Math.min(
                maxBubbleWidth, Math.max(150.0F, Math.max(TextRender.width(msg.message(), 12.5F) + 26.0F, TextRender.width(msg.time(), 8.5F) + 24.0F))
            );
            float bubbleH = isSelf ? 36.0F : 42.0F;
            if (isGeo) {
                bubbleW = 180.0F;
                bubbleH = 68.0F;
            }

            currentY -= bubbleH;
            float bubbleX = (isSelf ? feedX + feedW - bubbleW - 14.0F : feedX + 46.0F) + (isSelf ? 1.0F : -1.0F) * (1.0F - appear) * 12.0F;
            if (!isSelf) {
                float msgAvSize = 28.0F;
                float msgAvX = feedX + 10.0F;
                float msgAvY = currentY + bubbleH - msgAvSize;
                this.drawMessageAvatar(context, msg.avatarUrl(), false, msgAvX, msgAvY, msgAvSize);
            }

            int bubbleBg = fade(isSelf ? -564776850 : -652798176, appear);
            RoundedRenderer.drawRoundedRect(context, bubbleX, currentY, bubbleW, bubbleH, 12.0F, bubbleBg);
            if (isGeo) {
                IconRender.draw(context, "diamond", bubbleX + 10.0F, currentY + 8.0F, 12.0F, -45478);
                TextRender.draw(context, "Геопозиция", bubbleX + 24.0F, currentY + 18.0F, 12.0F, -1);
                String coordStr = extractCoords(msg.message());
                TextRender.draw(context, "Координаты: " + coordStr, bubbleX + 10.0F, currentY + 34.0F, 10.5F, -1962934273);
                float btnX = bubbleX + 10.0F;
                float btnY = currentY + 42.0F;
                float btnW = bubbleW - 20.0F;
                float btnH = 20.0F;
                boolean btnHov = inside(mouseX, mouseY, btnX, btnY, btnW, btnH);
                RoundedRenderer.drawRoundedRect(context, btnX, btnY, btnW, btnH, 6.0F, btnHov ? 872415231 : 872415231);
                TextRender.draw(context, "Создать метку", btnX + (btnW - TextRender.width("Создать метку", 10.5F)) / 2.0F, btnY + 14.0F, 10.5F, -1);
            } else {
                if (!isSelf) {
                    TextRender.draw(context, msg.login(), bubbleX + 10.0F, currentY + 14.0F, 11.0F, -45478);
                    TextRender.draw(context, msg.message(), bubbleX + 10.0F, currentY + 28.0F, 12.5F, -1);
                } else {
                    TextRender.draw(context, msg.message(), bubbleX + 12.0F, currentY + 18.0F, 12.5F, -1);
                }

                float timeWidth = TextRender.width(msg.time(), 8.5F);
                TextRender.draw(context, msg.time(), bubbleX + bubbleW - timeWidth - 9.0F, currentY + bubbleH - 5.0F, 8.5F, -1962934273);
            }

            currentY -= 8.0F;
        }

        float totalHeight = feedY + feedH - 10.0F - currentY + this.messageScroll;
        float maxScroll = Math.max(0.0F, totalHeight - feedH);
        this.targetMessageScroll = MathHelper.clamp(this.targetMessageScroll, 0.0F, maxScroll);
    }

    void renderDirectMessages(DrawContext context, float feedX, float feedY, float feedW, float feedH) {
        List messages = FriendHubClient.INSTANCE.getDmMessages(this.selectedFriendLogin);
        String myName = getUsername();
        float currentY = feedY + feedH - 10.0F + this.messageScroll;
        float maxBubbleWidth = feedW * 0.62F;

        for (int index = messages.size() - 1; index >= 0; index--) {
            FriendHubClient$DmMessage message = (FriendHubClient$DmMessage)messages.get(index);
            boolean self = message.from.equalsIgnoreCase(myName);
            String animationKey = "dm:" + message.from + ":" + message.ts + ":" + message.message;
            float appear = MathHelper.lerp(0.2F, this.messageAppearAnimations.getOrDefault(animationKey, 0.0F), 1.0F);
            this.messageAppearAnimations.put(animationKey, appear);
            float bubbleW = Math.min(
                maxBubbleWidth, Math.max(150.0F, Math.max(TextRender.width(message.message, 12.5F) + 26.0F, TextRender.width(message.time, 8.5F) + 24.0F))
            );
            float bubbleH = self ? 36.0F : 42.0F;
            currentY -= bubbleH;
            float bubbleX = (self ? feedX + feedW - bubbleW - 14.0F : feedX + 46.0F) + (self ? 1.0F : -1.0F) * (1.0F - appear) * 12.0F;
            if (!self) {
                float avatarSize = 28.0F;
                this.drawMessageAvatar(context, message.avatarUrl, true, feedX + 10.0F, currentY + bubbleH - avatarSize, avatarSize);
            }

            RoundedRenderer.drawRoundedRect(context, bubbleX, currentY, bubbleW, bubbleH, 12.0F, fade(self ? -564776850 : -652798176, appear));
            if (self) {
                TextRender.draw(context, message.message, bubbleX + 12.0F, currentY + 18.0F, 12.5F, -1);
            } else {
                TextRender.draw(context, message.from, bubbleX + 10.0F, currentY + 14.0F, 11.0F, accentColor());
                TextRender.draw(context, message.message, bubbleX + 10.0F, currentY + 28.0F, 12.5F, -1);
            }

            float timeWidth = TextRender.width(message.time, 8.5F);
            TextRender.draw(context, message.time, bubbleX + bubbleW - timeWidth - 9.0F, currentY + bubbleH - 5.0F, 8.5F, -1962934273);
            currentY -= 8.0F;
        }

        float totalHeight = feedY + feedH - 10.0F - currentY + this.messageScroll;
        this.targetMessageScroll = MathHelper.clamp(this.targetMessageScroll, 0.0F, Math.max(0.0F, totalHeight - feedH));
    }

    void renderGroupMessages(DrawContext context, FriendHubClient$GroupData group, float feedX, float feedY, float feedW, float feedH) {
        List messages = group.messages;
        String myName = getUsername();
        float currentY = feedY + feedH - 10.0F + this.messageScroll;
        float maxBubbleWidth = feedW * 0.62F;

        for (int index = messages.size() - 1; index >= 0; index--) {
            FriendHubClient$GroupMessage message = (FriendHubClient$GroupMessage)messages.get(index);
            boolean self = message.from.equalsIgnoreCase(myName);
            String animationKey = "group:" + group.id + ":" + message.from + ":" + message.time + ":" + message.message;
            float appear = MathHelper.lerp(0.2F, this.messageAppearAnimations.getOrDefault(animationKey, 0.0F), 1.0F);
            this.messageAppearAnimations.put(animationKey, appear);
            boolean geo = isGeoMessage(message.message);
            float bubbleW = Math.min(
                maxBubbleWidth, Math.max(150.0F, Math.max(TextRender.width(message.message, 12.5F) + 26.0F, TextRender.width(message.time, 8.5F) + 24.0F))
            );
            float bubbleH = self ? 36.0F : 42.0F;
            if (geo) {
                bubbleW = 180.0F;
                bubbleH = 68.0F;
            }

            currentY -= bubbleH;
            float bubbleX = (self ? feedX + feedW - bubbleW - 14.0F : feedX + 46.0F) + (self ? 1.0F : -1.0F) * (1.0F - appear) * 12.0F;
            if (!self) {
                this.drawMessageAvatar(context, message.avatarUrl, true, feedX + 10.0F, currentY + bubbleH - 28.0F, 28.0F);
            }

            RoundedRenderer.drawRoundedRect(context, bubbleX, currentY, bubbleW, bubbleH, 12.0F, fade(self ? -564776850 : -652798176, appear));
            if (geo) {
                IconRender.draw(context, "diamond", bubbleX + 10.0F, currentY + 8.0F, 12.0F, -45478);
                TextRender.draw(context, "Координаты", bubbleX + 24.0F, currentY + 18.0F, 12.0F, -1);
                TextRender.draw(context, extractCoords(message.message), bubbleX + 10.0F, currentY + 35.0F, 10.5F, -1962934273);
            } else if (self) {
                TextRender.draw(context, message.message, bubbleX + 12.0F, currentY + 18.0F, 12.5F, -1);
            } else {
                TextRender.draw(context, message.from, bubbleX + 10.0F, currentY + 14.0F, 11.0F, accentColor());
                TextRender.draw(context, message.message, bubbleX + 10.0F, currentY + 28.0F, 12.5F, -1);
            }

            float timeWidth = TextRender.width(message.time, 8.5F);
            TextRender.draw(context, message.time, bubbleX + bubbleW - timeWidth - 9.0F, currentY + bubbleH - 5.0F, 8.5F, -1962934273);
            currentY -= 8.0F;
        }

        float totalHeight = feedY + feedH - 10.0F - currentY + this.messageScroll;
        this.targetMessageScroll = MathHelper.clamp(this.targetMessageScroll, 0.0F, Math.max(0.0F, totalHeight - feedH));
    }

    void renderActionMenu(DrawContext context, float x, float y, float mouseX, float mouseY, float progress) {
        float eased = 1.0F - (float)Math.pow(1.0F - MathHelper.clamp(progress, 0.0F, 1.0F), 3.0);
        float menuW = 174.0F;
        float menuH = 38.0F;
        float animatedY = y + (1.0F - eased) * 24.0F;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, animatedY + menuH);
        context.getMatrices().scale(0.88F + 0.12F * eased, 0.76F + 0.24F * eased);
        context.getMatrices().translate(-x, -(animatedY + menuH));
        RoundedRenderer.drawRoundedRect(context, x, animatedY, menuW, menuH, 10.0F, fade(-552792814, eased));
        boolean actionHovered = eased > 0.9F && inside(mouseX, mouseY, x + 4.0F, animatedY + 4.0F, menuW - 8.0F, 30.0F);
        if (actionHovered) {
            RoundedRenderer.drawRoundedRect(context, x + 4.0F, animatedY + 4.0F, menuW - 8.0F, 30.0F, 6.0F, fade(721420287, eased));
        }

        IconRender.draw(context, "diamond", x + 10.0F, animatedY + 12.0F, 12.0F, fade(-45478, eased));
        TextRender.draw(context, "Поделиться координатами", x + 28.0F, animatedY + 23.0F, 11.5F, fade(-1, eased));
        context.getMatrices().popMatrix();
    }

    void renderPanelNotices(DrawContext context) {
        long now = System.currentTimeMillis();
        this.panelNotices.removeIf(notice -> now - notice.createdAt > 2600L);
        int visibleIndex = 0;

        for (int index = this.panelNotices.size() - 1; index >= 0; index--) {
            IrcChatMessengerScreen$PanelNotice notice2 = this.panelNotices.get(index);
            long age = now - notice2.createdAt;
            float enter = MathHelper.clamp((float)age / 180.0F, 0.0F, 1.0F);
            float exit = age < 2300L ? 1.0F : MathHelper.clamp((float)(2600L - age) / 300.0F, 0.0F, 1.0F);
            float alpha = easeOut(enter) * exit;
            float width = Math.max(150.0F, TextRender.width(notice2.text, 11.0F) + 28.0F);
            float x = 272.0F + (648.0F - width) / 2.0F;
            float targetY = -34.0F - visibleIndex * 31.0F;
            float y = targetY + (1.0F - easeOut(enter)) * 14.0F;
            RoundedRenderer.drawRoundedRect(context, x, y, width, 25.0F, 8.0F, fade(-317582823, alpha));
            RoundedRenderer.drawRoundedRect(context, x + 0.55F, y + 0.55F, width - 1.1F, 23.9F, 7.45F, fade(-384954860, alpha));
            float textWidth = TextRender.width(notice2.text, 11.0F);
            TextRender.draw(context, notice2.text, x + (width - textWidth) / 2.0F, y + 16.5F, 11.0F, fade(-1, alpha));
            visibleIndex++;
        }
    }

    void showPanelNotice(String text) {
        if (text != null && !text.isBlank()) {
            this.panelNotices.add(new IrcChatMessengerScreen$PanelNotice(text, System.currentTimeMillis()));

            while (this.panelNotices.size() > 3) {
                this.panelNotices.remove(0);
            }
        }
    }

    void renderMessageMenu(DrawContext context, float mouseX, float mouseY) {
        if (this.messageMenuMessage != null) {
            boolean canAdd = this.messageMenuAllowFriendRequest && !FriendHubClient.INSTANCE.isFriend(this.messageMenuMessage.login());
            float menuW = 140.0F;
            float itemH = 30.0F;
            float menuH = (canAdd ? 2.0F : 1.0F) * itemH + 8.0F;
            float eased = easeOut(this.messageMenuProgress);
            float anchoredY = this.messageMenuCurrentY();
            float y = anchoredY + (1.0F - eased) * 16.0F;
            if (!(anchoredY < 50.0F) && !(anchoredY > 492.0F)) {
                drawPopupBase(context, this.messageMenuX, y, menuW, menuH, eased);
                int row = 0;
                if (canAdd) {
                    drawPopupItem(
                        context,
                        this.messageMenuX + 4.0F,
                        y + 4.0F,
                        menuW - 8.0F,
                        itemH,
                        "Добавить в друзья",
                        inside(mouseX, mouseY, this.messageMenuX + 4.0F, y + 4.0F, menuW - 8.0F, itemH),
                        eased,
                        true
                    );
                    row++;
                }

                float copyY = y + 4.0F + row * itemH;
                drawPopupItem(
                    context,
                    this.messageMenuX + 4.0F,
                    copyY,
                    menuW - 8.0F,
                    itemH,
                    "Скопировать",
                    inside(mouseX, mouseY, this.messageMenuX + 4.0F, copyY, menuW - 8.0F, itemH),
                    eased,
                    false
                );
            }
        }
    }

    void renderHeaderMenu(DrawContext context, float chatX, float chatW, float mouseX, float mouseY) {
        boolean direct = this.selectedFriendLogin != null;
        FriendHubClient$GroupData group = this.selectedGroup();
        float menuW = 168.0F;
        float itemH = 30.0F;
        float menuH = (direct ? 2.0F : 1.0F) * itemH + 8.0F;
        float x = chatX + chatW - menuW - 10.0F;
        float baseY = 44.0F;
        float eased = easeOut(this.headerMenuProgress);
        float y = baseY - (1.0F - eased) * 12.0F;
        drawPopupBase(context, x, y, menuW, menuH, eased);
        Object key = group != null ? "group:" + group.id : (direct ? "dm:" + this.selectedFriendLogin : "global");
        String notificationLabel = MessengerPreferences.isEnabled((String)key) ? "Выкл. уведомления" : "Вкл. уведомления";
        drawPopupItem(
            context, x + 4.0F, y + 4.0F, menuW - 8.0F, itemH, notificationLabel, inside(mouseX, mouseY, x + 4.0F, y + 4.0F, menuW - 8.0F, itemH), eased, true
        );
        if (direct) {
            drawPopupItem(
                context,
                x + 4.0F,
                y + 4.0F + itemH,
                menuW - 8.0F,
                itemH,
                "Удалить из друзей",
                inside(mouseX, mouseY, x + 4.0F, y + 4.0F + itemH, menuW - 8.0F, itemH),
                eased,
                false
            );
        }
    }

    void renderMemberMenu(DrawContext context, float mouseX, float mouseY) {
        if (this.memberMenuLogin != null) {
            float width = 142.0F;
            float height = 38.0F;
            float eased = easeOut(this.memberMenuProgress);
            float y = this.memberMenuY + (1.0F - eased) * 12.0F;
            drawPopupBase(context, this.memberMenuX, y, width, height, eased);
            drawPopupItem(
                context,
                this.memberMenuX + 4.0F,
                y + 4.0F,
                width - 8.0F,
                30.0F,
                "Изгнать из группы",
                inside(mouseX, mouseY, this.memberMenuX + 4.0F, y + 4.0F, width - 8.0F, 30.0F),
                eased,
                false
            );
        }
    }

    void renderGroupMenu(DrawContext context, float mouseX, float mouseY) {
        if (this.groupMenuId != null) {
            float width = 148.0F;
            float itemH = 30.0F;
            float height = itemH * 2.0F + 8.0F;
            float eased = easeOut(this.groupMenuProgress);
            float y = this.groupMenuY + (1.0F - eased) * 12.0F;
            drawPopupBase(context, this.groupMenuX, y, width, height, eased);
            drawPopupItem(
                context,
                this.groupMenuX + 4.0F,
                y + 4.0F,
                width - 8.0F,
                itemH,
                "Загрузить аватар",
                inside(mouseX, mouseY, this.groupMenuX + 4.0F, y + 4.0F, width - 8.0F, itemH),
                eased,
                true
            );
            drawPopupItem(
                context,
                this.groupMenuX + 4.0F,
                y + 4.0F + itemH,
                width - 8.0F,
                itemH,
                "Удалить группу",
                inside(mouseX, mouseY, this.groupMenuX + 4.0F, y + 4.0F + itemH, width - 8.0F, itemH),
                eased,
                false
            );
        }
    }

    float messageMenuCurrentY() {
        return this.messageMenuY + (this.messageScroll - this.messageMenuScrollAtOpen);
    }

    static void drawPopupBase(DrawContext context, float x, float y, float width, float height, float alpha) {
        RoundedRenderer.drawRoundedRect(context, x, y, width, height, 10.0F, fade(620756991, alpha));
        RoundedRenderer.drawRoundedRect(context, x + 0.55F, y + 0.55F, width - 1.1F, height - 1.1F, 9.45F, fade(-552792814, alpha));
    }

    static void drawPopupItem(DrawContext context, float x, float y, float width, float height, String label, boolean hovered, float alpha, boolean accent) {
        if (hovered && alpha > 0.9F) {
            RoundedRenderer.drawRoundedRect(context, x, y, width, height, 7.0F, fade(385875967, alpha));
        }

        TextRender.draw(context, label, x + 10.0F, y + 20.0F, 11.5F, fade(accent ? -1 : -1962934273, alpha));
    }

    static float easeOut(float progress) {
        float value = MathHelper.clamp(progress, 0.0F, 1.0F);
        return 1.0F - (float)Math.pow(1.0F - value, 3.0);
    }

    static void drawPlusIcon(DrawContext context, float centerX, float centerY, float size, int color) {
        float half = size / 2.0F;
        float thickness = 1.5F;
        RoundedRenderer.drawRoundedRect(context, centerX - half, centerY - thickness / 2.0F, size, thickness, thickness / 2.0F, color);
        RoundedRenderer.drawRoundedRect(context, centerX - thickness / 2.0F, centerY - half, thickness, size, thickness / 2.0F, color);
    }

    void drawFriendAvatar(DrawContext context, FriendHubClient$FriendEntry friend, float x, float y, float size) {
        Identifier texture = FriendHubClient.INSTANCE.getAvatarTexture(friend.avatarUrl);
        if (texture == null) {
            texture = FALLBACK_AVATAR;
        }

        RenderUtil.drawRoundedTexture(context, texture, x, y, size, size, size / 2.0F, true, true, true, true, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
        if (friend.online) {
            RoundedRenderer.drawRoundedRect(context, x + size - 6.0F, y + size - 6.0F, 7.0F, 7.0F, 3.5F, -13318311);
        }
    }

    void drawFallbackAvatar(DrawContext context, float x, float y, float size) {
        RenderUtil.drawRoundedTexture(context, FALLBACK_AVATAR, x, y, size, size, size / 2.0F, true, true, true, true, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    void drawGroupIcon(DrawContext context, FriendHubClient$GroupData group, float x, float y, float size) {
        this.drawGroupIcon(context, group == null ? "" : group.avatarUrl, x, y, size);
    }

    void drawGroupIcon(DrawContext context, String avatarUrl, float x, float y, float size) {
        Identifier texture = FriendHubClient.INSTANCE.getAvatarTexture(avatarUrl);
        if (texture == null) {
            texture = FALLBACK_AVATAR;
        }

        RenderUtil.drawRoundedTexture(context, texture, x, y, size, size, size / 2.0F, true, true, true, true, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    void drawMessageAvatar(DrawContext context, String avatarUrl, boolean friendHub, float x, float y, float size) {
        Identifier texture = friendHub ? FriendHubClient.INSTANCE.getAvatarTexture(avatarUrl) : ChatHubClient.INSTANCE.getAvatarTexture(avatarUrl);
        if (texture == null) {
            texture = FALLBACK_AVATAR;
        }

        RenderUtil.drawRoundedTexture(context, texture, x, y, size, size, size / 2.0F, true, true, true, true, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    static FriendHubClient$FriendEntry findFriend(String login) {
        if (login == null) {
            return null;
        }

        for (FriendHubClient$FriendEntry friend : FriendHubClient.INSTANCE.getFriends()) {
            if (friend.login.equalsIgnoreCase(login)) {
                return friend;
            }
        }

        return null;
    }

    static List<FriendHubClient$FriendEntry> sortedFriends() {
        ArrayList<FriendHubClient$FriendEntry> friends = new ArrayList<>(FriendHubClient.INSTANCE.getFriends());
        friends.sort((left, right) -> {
            if (left.online != right.online) {
                return left.online ? -1 : 1;
            } else {
                return left.login.compareToIgnoreCase(right.login);
            }
        });
        return friends;
    }

    FriendHubClient$GroupData selectedGroup() {
        if (this.selectedLocalGroup == null) {
            return null;
        }

        for (FriendHubClient$GroupData group : FriendHubClient.INSTANCE.getGroups()) {
            if (group.id.equals(this.selectedLocalGroup)) {
                return group;
            }
        }

        return null;
    }

    void resolvePendingCreatedGroup() {
        if (this.pendingCreatedGroupName != null) {
            for (FriendHubClient$GroupData group : FriendHubClient.INSTANCE.getGroups()) {
                if (group.name.equalsIgnoreCase(this.pendingCreatedGroupName) && group.isOwner(getUsername())) {
                    this.selectedLocalGroup = group.id;
                    this.pendingCreatedGroupName = null;
                    this.groupViewProgress = 0.0F;
                    this.messageScroll = 0.0F;
                    this.targetMessageScroll = 0.0F;
                    break;
                }
            }
        }

        if (this.selectedLocalGroup != null
            && this.selectedGroup() == null
            && this.pendingCreatedGroupName == null
            && FriendHubClient.INSTANCE.isInitialDataLoaded()) {
            this.selectedLocalGroup = null;
        }
    }

    static List<String> groupMembers(FriendHubClient$GroupData group) {
        ArrayList<String> members = new ArrayList<>();
        if (group.owner != null && !group.owner.isBlank()) {
            members.add(group.owner);
        }

        for (String member : group.members) {
            if (member != null && !member.isBlank() && members.stream().noneMatch(existing -> existing.equalsIgnoreCase(member))) {
                members.add(member);
            }
        }

        members.sort((left, right) -> {
            boolean leftOwner = left.equalsIgnoreCase(group.owner);
            if (leftOwner != right.equalsIgnoreCase(group.owner)) {
                return leftOwner ? -1 : 1;
            } else {
                FriendHubClient$FriendEntry lf = findFriend(left);
                FriendHubClient$FriendEntry rf = findFriend(right);
                boolean lo = left.equalsIgnoreCase(getUsername()) || lf != null && lf.online;
                boolean ro = right.equalsIgnoreCase(getUsername()) || rf != null && rf.online;
                if (lo != ro) {
                    return lo ? -1 : 1;
                } else {
                    return left.compareToIgnoreCase(right);
                }
            }
        });
        return members;
    }

    static int groupMemberCount(FriendHubClient$GroupData group) {
        return groupMembers(group).size();
    }

    static String participantWord(int count) {
        int mod100 = Math.abs(count) % 100;
        int mod10 = Math.abs(count) % 10;
        if (mod10 == 1 && mod100 != 11) {
            return "участник";
        } else {
            return mod10 < 2 || mod10 > 4 || mod100 >= 12 && mod100 <= 14 ? "участников" : "участника";
        }
    }

    static String trimText(String text, float size, float maxWidth) {
        if (text != null && !text.isEmpty() && !(TextRender.width(text, size) <= maxWidth)) {
            String suffix = "...";
            int end = text.length();

            while (end > 0 && TextRender.width(text.substring(0, end) + suffix, size) > maxWidth) {
                end--;
            }

            return text.substring(0, end) + suffix;
        } else {
            return text;
        }
    }

    void drawRequestButton(DrawContext context, float x, float y, float width, String text, boolean accept) {
        int color = accept ? fade(accentColor(), 0.32F) : 620756991;
        RoundedRenderer.drawRoundedRect(context, x, y, width, 18.0F, 5.0F, color);
        float textWidth = TextRender.width(text, 8.5F);
        TextRender.draw(context, text, x + (width - textWidth) / 2.0F, y + 12.0F, 8.5F, accept ? -1 : -1962934273);
    }

    float animateRow(String key, boolean hovered) {
        float previous = this.rowHoverAnimations.getOrDefault(key, 0.0F);
        float value = MathHelper.lerp(0.22F, previous, hovered ? 1.0F : 0.0F);
        this.rowHoverAnimations.put(key, value);
        return value;
    }

    static int accentColor() {
        return ClickGui.getAccentColor(1.0F, 255);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        float mouseX = ((float)click.x() - this.originX) / this.scale;
        float mouseY = ((float)click.y() - this.originY) / this.scale;
        if (click.button() == 0) {
            if (this.groupMenuOpen && this.groupMenuId != null) {
                if (inside(mouseX, mouseY, this.groupMenuX + 4.0F, this.groupMenuY + 4.0F, 140.0F, 30.0F)) {
                    this.openGroupAvatarPicker(this.groupMenuId);
                    this.groupMenuOpen = false;
                    return true;
                }

                if (inside(mouseX, mouseY, this.groupMenuX + 4.0F, this.groupMenuY + 34.0F, 140.0F, 30.0F)) {
                    FriendHubClient.INSTANCE.deleteGroup(this.groupMenuId);
                    if (this.groupMenuId.equals(this.selectedLocalGroup)) {
                        this.selectedLocalGroup = null;
                    }

                    this.showPanelNotice("Группа удалена");
                    this.groupMenuOpen = false;
                    return true;
                }

                this.groupMenuOpen = false;
            }

            if (this.memberMenuOpen && this.memberMenuLogin != null) {
                if (inside(mouseX, mouseY, this.memberMenuX + 4.0F, this.memberMenuY + 4.0F, 134.0F, 30.0F)) {
                    FriendHubClient$GroupData group = this.selectedGroup();
                    if (group != null && group.isOwner(getUsername())) {
                        FriendHubClient.INSTANCE.kickGroupMember(group.id, this.memberMenuLogin);
                        this.showPanelNotice(this.memberMenuLogin + " исключён из группы");
                    }

                    this.memberMenuOpen = false;
                    return true;
                }

                this.memberMenuOpen = false;
            }

            float tabX = sidebarTabsStartX();

            for (int index = 0; index < 3; index++) {
                float tabW = TextRender.width(SIDEBAR_SECTIONS[index], 12.5F);
                if (inside(mouseX, mouseY, tabX, 78.0F, tabW, 22.0F)) {
                    if (index == 1 && this.sidebarSection == 1 && this.selectedGroup() != null) {
                        this.selectedLocalGroup = null;
                        this.groupNameInput = "";
                        this.sidebarScroll = 0.0F;
                        this.targetSidebarScroll = 0.0F;
                        this.messageScroll = 0.0F;
                        this.targetMessageScroll = 0.0F;
                        this.groupViewProgress = 0.0F;
                        this.memberMenuOpen = false;
                        this.beginChatSwitch();
                        return true;
                    }

                    this.sidebarSection = index;
                    this.sectionTransition = 0.0F;
                    this.sidebarScroll = 0.0F;
                    this.targetSidebarScroll = 0.0F;
                    this.isAddFriendActive = false;
                    this.beginChatSwitch();
                    return true;
                }

                tabX += tabW;
                if (index < 2) {
                    tabX += TextRender.width("|", 12.5F) + 16.0F;
                }
            }

            float listY = 105.0F + (1.0F - this.sectionTransition) * 8.0F - this.sidebarScroll;
            if (this.sidebarSection == 0) {
                if (inside(mouseX, mouseY, 12.0F, listY, 236.0F, 50.0F)) {
                    this.selectedChatIndex = 0;
                    this.selectedFriendLogin = null;
                    this.selectedLocalGroup = null;
                    this.messageScroll = 0.0F;
                    this.targetMessageScroll = 0.0F;
                    this.beginChatSwitch();
                    return true;
                }

                float friendY = listY + 56.0F;

                for (FriendHubClient$FriendEntry friend : sortedFriends()) {
                    if (inside(mouseX, mouseY, 12.0F, friendY, 236.0F, 48.0F)) {
                        this.selectedFriendLogin = friend.login;
                        this.selectedLocalGroup = null;
                        this.selectedChatIndex = 1;
                        FriendHubClient.INSTANCE.requestDmHistory(friend.login);
                        this.messageScroll = 0.0F;
                        this.targetMessageScroll = 0.0F;
                        this.beginChatSwitch();
                        return true;
                    }

                    friendY += 54.0F;
                }
            } else if (this.sidebarSection == 1) {
                FriendHubClient$GroupData opened = this.selectedGroup();
                if (opened == null) {
                    float groupY = listY;

                    for (FriendHubClient$GroupData group : FriendHubClient.INSTANCE.getGroups()) {
                        if (inside(mouseX, mouseY, 12.0F, groupY, 236.0F, 48.0F)) {
                            this.selectedLocalGroup = group.id;
                            this.selectedFriendLogin = null;
                            this.messageScroll = 0.0F;
                            this.targetMessageScroll = 0.0F;
                            this.groupViewProgress = 0.0F;
                            this.beginChatSwitch();
                            return true;
                        }

                        groupY += 54.0F;
                    }
                }
            } else {
                float requestY = listY;

                for (FriendHubClient$FriendEntry request : FriendHubClient.INSTANCE.getIncomingRequests()) {
                    if (inside(mouseX, mouseY, 62.0F, requestY + 30.0F, 52.0F, 18.0F)) {
                        FriendHubClient.INSTANCE.acceptFriendRequest(request.login);
                        this.addButtonAnimation = 1.0F;
                        return true;
                    }

                    if (inside(mouseX, mouseY, 120.0F, requestY + 30.0F, 58.0F, 18.0F)) {
                        FriendHubClient.INSTANCE.declineFriendRequest(request.login);
                        this.addButtonAnimation = 1.0F;
                        return true;
                    }

                    requestY += 60.0F;
                }

                for (FriendHubClient$GroupInviteEntry invite : FriendHubClient.INSTANCE.getIncomingGroupInvites()) {
                    if (inside(mouseX, mouseY, 58.0F, requestY + 29.0F, 52.0F, 18.0F)) {
                        FriendHubClient.INSTANCE.acceptGroupInvite(invite.groupId, invite.from);
                        this.addButtonAnimation = 1.0F;
                        this.showPanelNotice("Приглашение в группу принято");
                        return true;
                    }

                    if (inside(mouseX, mouseY, 116.0F, requestY + 29.0F, 58.0F, 18.0F)) {
                        FriendHubClient.INSTANCE.declineGroupInvite(invite.groupId, invite.from);
                        this.addButtonAnimation = 1.0F;
                        this.showPanelNotice("Приглашение отклонено");
                        return true;
                    }

                    requestY += 58.0F;
                }
            }

            float chatX = 272.0F;
            float inputY = 494.0F;
            float actionBtnX = chatX + 10.0F;
            float actionBtnY = inputY + 9.0F;
            float dotsX = 886.0F;
            if (inside(mouseX, mouseY, dotsX, 11.0F, 24.0F, 28.0F)) {
                this.headerMenuOpen = !this.headerMenuOpen;
                this.messageMenuOpen = false;
                this.showActionMenu = false;
                return true;
            }

            if (this.headerMenuOpen) {
                boolean direct = this.selectedFriendLogin != null;
                FriendHubClient$GroupData activeGroup = this.selectedGroup();
                float menuW = 168.0F;
                float menuX = 920.0F - menuW - 10.0F;
                float menuY = 44.0F;
                if (inside(mouseX, mouseY, menuX + 4.0F, menuY + 4.0F, menuW - 8.0F, 30.0F)) {
                    Object key = activeGroup != null ? "group:" + activeGroup.id : (direct ? "dm:" + this.selectedFriendLogin : "global");
                    boolean enabled = MessengerPreferences.toggle((String)key);
                    this.showPanelNotice(enabled ? "Уведомления включены" : "Уведомления выключены");
                    this.headerMenuOpen = false;
                    return true;
                }

                if (direct && activeGroup == null && inside(mouseX, mouseY, menuX + 4.0F, menuY + 34.0F, menuW - 8.0F, 30.0F)) {
                    FriendHubClient.INSTANCE.removeFriend(this.selectedFriendLogin);
                    this.showPanelNotice("Удалено из друзей: " + this.selectedFriendLogin);
                    this.selectedFriendLogin = null;
                    this.selectedChatIndex = 0;
                    this.headerMenuOpen = false;
                    return true;
                }

                this.headerMenuOpen = false;
            }

            if (this.messageMenuOpen && this.messageMenuMessage != null) {
                boolean canAdd = this.messageMenuAllowFriendRequest && !FriendHubClient.INSTANCE.isFriend(this.messageMenuMessage.login());
                float itemH = 30.0F;
                float anchoredMenuY = this.messageMenuCurrentY();
                if (canAdd && inside(mouseX, mouseY, this.messageMenuX + 4.0F, anchoredMenuY + 4.0F, 132.0F, itemH)) {
                    if (FriendHubClient.INSTANCE.getState() != FriendHubClient$State.CONNECTED) {
                        this.showPanelNotice("Нет подключения к серверу друзей");
                        this.messageMenuOpen = false;
                        return true;
                    }

                    FriendHubClient.INSTANCE.sendFriendRequest(this.messageMenuMessage.login());
                    this.messageMenuOpen = false;
                    return true;
                }

                float copyY = anchoredMenuY + 4.0F + (canAdd ? itemH : 0.0F);
                if (inside(mouseX, mouseY, this.messageMenuX + 4.0F, copyY, 132.0F, itemH)) {
                    MinecraftClient.getInstance().keyboard.setClipboard(this.messageMenuMessage.message());
                    this.showPanelNotice("Сообщение скопировано");
                    this.messageMenuOpen = false;
                    return true;
                }

                this.messageMenuOpen = false;
            }

            if (inside(mouseX, mouseY, actionBtnX, actionBtnY, 28.0F, 28.0F)) {
                this.showActionMenu = !this.showActionMenu;
                return true;
            }

            if (this.showActionMenu) {
                float menuY = 446.0F;
                if (inside(mouseX, mouseY, actionBtnX, menuY + 4.0F, 180.0F, 30.0F)) {
                    this.shareCurrentGeoposition();
                    this.showActionMenu = false;
                    return true;
                }

                this.showActionMenu = false;
            }

            if (inside(mouseX, mouseY, chatX, inputY, 920.0F - chatX, 46.0F)) {
                this.isChatInputActive = true;
                this.isAddFriendActive = false;
                return true;
            }

            if (inside(mouseX, mouseY, 16.0F, 36.0F, 192.0F, 34.0F)) {
                this.isAddFriendActive = true;
                this.isChatInputActive = false;
                return true;
            }

            if (inside(mouseX, mouseY, 210.0F, 36.0F, 34.0F, 34.0F)) {
                if (this.sidebarSection == 1) {
                    if (this.selectedGroup() != null) {
                        this.inviteGroupMember();
                    } else {
                        this.createServerGroup();
                    }
                } else {
                    this.sendFriendRequest();
                }

                return true;
            }

            List msgs = ChatHubClient.INSTANCE.getMessages();
            float feedY = 56.0F;
            float feedH = inputY - feedY - 6.0F;
            float currentY = feedY + feedH - 10.0F + this.messageScroll;
            float maxBubbleWidth = (920.0F - chatX) * 0.62F;
            if (this.selectedFriendLogin == null && this.selectedLocalGroup == null) {
                for (int i = msgs.size() - 1; i >= 0; i--) {
                    ChatHubMessage msg = (ChatHubMessage)msgs.get(i);
                    boolean isSelf = msg.login().equalsIgnoreCase(getUsername());
                    boolean isGeo = isGeoMessage(msg.message());
                    float bubbleW = isGeo ? 180.0F : Math.min(maxBubbleWidth, Math.max(140.0F, TextRender.width(msg.message(), 12.5F) + 26.0F));
                    float bubbleH = isGeo ? 68.0F : (isSelf ? 36.0F : 42.0F);
                    float bubbleX = isSelf ? chatX + (920.0F - chatX) - bubbleW - 14.0F : chatX + 46.0F;
                    if (isGeo) {
                        float btnX;
                        float var10002 = btnX = bubbleX + 10.0F;
                        float btnY = (currentY -= bubbleH) + 42.0F;
                        float btnW;
                        float var10004 = btnW = bubbleW - 20.0F;
                        float btnH = 20.0F;
                        if (inside(mouseX, mouseY, var10002, btnY, var10004, 20.0F)) {
                            this.createMarkerFromMessage(msg);
                            return true;
                        }
                    }

                    currentY -= 8.0F;
                }
            }
        }

        if (click.button() == 1 && this.openGroupListMenu(mouseX, mouseY)) {
            return true;
        } else if (click.button() == 1 && this.selectedFriendLogin == null && this.selectedLocalGroup == null && this.openGlobalMessageMenu(mouseX, mouseY)) {
            return true;
        } else if (click.button() == 1 && this.selectedFriendLogin != null && this.openDirectMessageMenu(mouseX, mouseY)) {
            return true;
        } else if (click.button() == 1 && this.openGroupMemberMenu(mouseX, mouseY)) {
            return true;
        } else {
            return click.button() == 1 && this.selectedGroup() != null && this.openGroupMessageMenu(mouseX, mouseY) ? true : super.mouseClicked(click, doubled);
        }
    }

    boolean openGlobalMessageMenu(float mouseX, float mouseY) {
        float chatX = 272.0F;
        float inputY = 494.0F;
        float feedY = 56.0F;
        float feedH = inputY - feedY - 6.0F;
        float feedW = 920.0F - chatX;
        float currentY = feedY + feedH - 10.0F + this.messageScroll;
        float maxBubbleWidth = feedW * 0.62F;
        List messages = ChatHubClient.INSTANCE.getMessages();

        for (int index = messages.size() - 1; index >= 0; index--) {
            ChatHubMessage message = (ChatHubMessage)messages.get(index);
            boolean self = message.login().equalsIgnoreCase(getUsername());
            boolean geo = isGeoMessage(message.message());
            float bubbleW = Math.min(
                maxBubbleWidth, Math.max(150.0F, Math.max(TextRender.width(message.message(), 12.5F) + 26.0F, TextRender.width(message.time(), 8.5F) + 24.0F))
            );
            float bubbleH = self ? 36.0F : 42.0F;
            if (geo) {
                bubbleW = 180.0F;
                bubbleH = 68.0F;
            }

            float bubbleX = self ? chatX + feedW - bubbleW - 14.0F : chatX + 46.0F;
            if (!self && inside(mouseX, mouseY, bubbleX, currentY -= bubbleH, bubbleW, bubbleH)) {
                this.messageMenuMessage = message;
                this.messageMenuAllowFriendRequest = true;
                this.messageMenuX = MathHelper.clamp(bubbleX + bubbleW + 8.0F, chatX + 8.0F, 756.0F);
                this.messageMenuY = MathHelper.clamp(currentY - 20.0F, 54.0F, inputY - (FriendHubClient.INSTANCE.isFriend(message.login()) ? 46.0F : 76.0F));
                this.messageMenuOpen = true;
                this.messageMenuProgress = 0.0F;
                this.messageMenuScrollAtOpen = this.messageScroll;
                this.headerMenuOpen = false;
                this.showActionMenu = false;
                return true;
            }

            currentY -= 8.0F;
        }

        this.messageMenuOpen = false;
        return false;
    }

    boolean openDirectMessageMenu(float mouseX, float mouseY) {
        float chatX = 272.0F;
        float inputY = 494.0F;
        float feedY = 56.0F;
        float feedH = inputY - feedY - 6.0F;
        float feedW = 920.0F - chatX;
        float currentY = feedY + feedH - 10.0F + this.messageScroll;
        float maxBubbleWidth = feedW * 0.62F;
        String myName = getUsername();
        List messages = FriendHubClient.INSTANCE.getDmMessages(this.selectedFriendLogin);

        for (int index = messages.size() - 1; index >= 0; index--) {
            FriendHubClient$DmMessage message = (FriendHubClient$DmMessage)messages.get(index);
            boolean self = message.from.equalsIgnoreCase(myName);
            float bubbleW = Math.min(
                maxBubbleWidth, Math.max(150.0F, Math.max(TextRender.width(message.message, 12.5F) + 26.0F, TextRender.width(message.time, 8.5F) + 24.0F))
            );
            float bubbleH = self ? 36.0F : 42.0F;
            float bubbleX = self ? chatX + feedW - bubbleW - 14.0F : chatX + 46.0F;
            if (inside(mouseX, mouseY, bubbleX, currentY = currentY - bubbleH, bubbleW, bubbleH)) {
                this.messageMenuMessage = new ChatHubMessage(message.from, "USER", message.avatarUrl, message.message, message.time);
                this.messageMenuAllowFriendRequest = false;
                this.messageMenuX = MathHelper.clamp(bubbleX + (self ? -162.0F : bubbleW + 8.0F), chatX + 8.0F, 756.0F);
                this.messageMenuY = MathHelper.clamp(currentY - 8.0F, 54.0F, inputY - 46.0F);
                this.messageMenuOpen = true;
                this.messageMenuProgress = 0.0F;
                this.messageMenuScrollAtOpen = this.messageScroll;
                this.headerMenuOpen = false;
                this.showActionMenu = false;
                return true;
            }

            currentY -= 8.0F;
        }

        this.messageMenuOpen = false;
        return false;
    }

    boolean openGroupMemberMenu(float mouseX, float mouseY) {
        FriendHubClient$GroupData group = this.selectedGroup();
        if (this.sidebarSection == 1 && group != null && group.isOwner(getUsername())) {
            float listY = 105.0F + (1.0F - this.sectionTransition) * 8.0F - this.sidebarScroll;
            float memberY = listY + (1.0F - this.groupViewProgress) * 18.0F + 25.0F;

            for (String member : groupMembers(group)) {
                if (!member.equalsIgnoreCase(group.owner) && inside(mouseX, mouseY, 12.0F, memberY, 236.0F, 46.0F)) {
                    this.memberMenuLogin = member;
                    this.memberMenuX = MathHelper.clamp(mouseX + 18.0F, 8.0F, 110.0F);
                    this.memberMenuY = MathHelper.clamp(memberY - 16.0F, 102.0F, 438.0F);
                    this.memberMenuOpen = true;
                    this.memberMenuProgress = 0.0F;
                    this.messageMenuOpen = false;
                    return true;
                }

                memberY += 50.0F;
            }

            return false;
        } else {
            return false;
        }
    }

    boolean openGroupMessageMenu(float mouseX, float mouseY) {
        FriendHubClient$GroupData group = this.selectedGroup();
        if (group == null) {
            return false;
        }

        float chatX = 272.0F;
        float inputY = 494.0F;
        float feedY = 56.0F;
        float feedH = inputY - feedY - 6.0F;
        float feedW = 920.0F - chatX;
        float currentY = feedY + feedH - 10.0F + this.messageScroll;
        float maxBubbleWidth = feedW * 0.62F;
        String myName = getUsername();
        List messages = group.messages;

        for (int index = messages.size() - 1; index >= 0; index--) {
            FriendHubClient$GroupMessage message = (FriendHubClient$GroupMessage)messages.get(index);
            boolean self = message.from.equalsIgnoreCase(myName);
            boolean geo = isGeoMessage(message.message);
            float bubbleW = Math.min(
                maxBubbleWidth, Math.max(150.0F, Math.max(TextRender.width(message.message, 12.5F) + 26.0F, TextRender.width(message.time, 8.5F) + 24.0F))
            );
            float bubbleH = self ? 36.0F : 42.0F;
            if (geo) {
                bubbleW = 180.0F;
                bubbleH = 68.0F;
            }

            float bubbleX = self ? chatX + feedW - bubbleW - 14.0F : chatX + 46.0F;
            if (inside(mouseX, mouseY, bubbleX, currentY = currentY - bubbleH, bubbleW, bubbleH)) {
                this.messageMenuMessage = new ChatHubMessage(message.from, "USER", message.avatarUrl, message.message, message.time);
                this.messageMenuAllowFriendRequest = !self;
                this.messageMenuX = MathHelper.clamp(bubbleX + (self ? -162.0F : bubbleW + 8.0F), chatX + 8.0F, 756.0F);
                this.messageMenuY = MathHelper.clamp(
                    currentY - 8.0F, 54.0F, inputY - (this.messageMenuAllowFriendRequest && !FriendHubClient.INSTANCE.isFriend(message.from) ? 76.0F : 46.0F)
                );
                this.messageMenuOpen = true;
                this.messageMenuProgress = 0.0F;
                this.messageMenuScrollAtOpen = this.messageScroll;
                this.memberMenuOpen = false;
                this.headerMenuOpen = false;
                this.showActionMenu = false;
                return true;
            }

            currentY -= 8.0F;
        }

        return false;
    }

    boolean openGroupListMenu(float mouseX, float mouseY) {
        if (this.sidebarSection == 1 && this.selectedGroup() == null) {
            float groupY = 105.0F + (1.0F - this.sectionTransition) * 8.0F - this.sidebarScroll;

            for (FriendHubClient$GroupData group : FriendHubClient.INSTANCE.getGroups()) {
                if (group.isOwner(getUsername()) && inside(mouseX, mouseY, 12.0F, groupY, 236.0F, 48.0F)) {
                    this.groupMenuId = group.id;
                    this.groupMenuX = MathHelper.clamp(mouseX + 10.0F, 8.0F, 104.0F);
                    this.groupMenuY = MathHelper.clamp(groupY - 18.0F, 102.0F, 412.0F);
                    this.groupMenuOpen = true;
                    this.groupMenuProgress = 0.0F;
                    this.memberMenuOpen = false;
                    this.messageMenuOpen = false;
                    return true;
                }

                groupY += 54.0F;
            }

            return false;
        } else {
            return false;
        }
    }

    void openGroupAvatarPicker(String groupId) {
        CompletableFuture.runAsync(
            () -> {
                MemoryStack stack = MemoryStack.stackPush();

                String path;
                try {
                    PointerBuffer filters = stack.mallocPointer(1);
                    filters.put(stack.UTF8("*.png"));
                    filters.flip();
                    path = TinyFileDialogs.tinyfd_openFileDialog("Выберите PNG-аватар группы", "", filters, "PNG (*.png)", false);
                } catch (Throwable var7) {
                    if (stack != null) {
                        try {
                            stack.close();
                        } catch (Throwable var6) {
                            var7.addSuppressed(var6);
                        }
                    }

                    throw var7;
                }

                if (stack != null) {
                    stack.close();
                }

                if (path != null && !path.isBlank()) {
                    try {
                        byte[] bytes = Files.readAllBytes(Path.of(path));
                        boolean png = bytes.length >= 8
                            && (bytes[0] & 255) == 137
                            && bytes[1] == 80
                            && bytes[2] == 78
                            && bytes[3] == 71
                            && bytes[4] == 13
                            && bytes[5] == 10
                            && bytes[6] == 26
                            && bytes[7] == 10;
                        MinecraftClient.getInstance().execute(() -> {
                            if (!png) {
                                this.showPanelNotice("Нужно выбрать PNG-изображение");
                            } else if (bytes.length > 550000) {
                                this.showPanelNotice("Аватар должен весить не больше 550 КБ");
                            } else {
                                FriendHubClient.INSTANCE.updateGroupAvatar(groupId, bytes);
                                this.showPanelNotice("Аватар группы обновляется...");
                            }
                        });
                    } catch (Exception ignored) {
                        MinecraftClient.getInstance().execute(() -> this.showPanelNotice("Не удалось загрузить аватар"));
                    }
                }
            }
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float designMouseX = ((float)mouseX - this.originX) / this.scale;
        if (designMouseX <= 260.0F) {
            this.targetSidebarScroll -= (float)verticalAmount * 35.0F;
        } else {
            this.targetMessageScroll += (float)verticalAmount * 35.0F;
        }

        return true;
    }

    @Override
    public boolean charTyped(CharInput input) {
        String text = input.asString();
        if (!text.isEmpty()) {
            if (this.isChatInputActive) {
                int remaining = 40 - this.chatInput.length();
                if (remaining > 0) {
                    this.chatInput = this.chatInput + text.substring(0, Math.min(remaining, text.length()));
                }

                return true;
            }

            if (this.isAddFriendActive) {
                if (this.sidebarSection == 1) {
                    this.groupNameInput = this.groupNameInput + text;
                } else {
                    this.addFriendInput = this.addFriendInput + text;
                }

                return true;
            }
        }

        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == 256) {
            if (this.showActionMenu) {
                this.showActionMenu = false;
                return true;
            } else {
                this.close();
                return true;
            }
        } else {
            if (input.key() == 257 || input.key() == 335) {
                if (this.isChatInputActive) {
                    this.sendChatMessage();
                    return true;
                }

                if (this.isAddFriendActive) {
                    if (this.sidebarSection == 1) {
                        if (this.selectedGroup() != null) {
                            this.inviteGroupMember();
                        } else {
                            this.createServerGroup();
                        }
                    } else {
                        this.sendFriendRequest();
                    }

                    return true;
                }
            }

            if (input.key() == 259) {
                if (this.isChatInputActive && !this.chatInput.isEmpty()) {
                    this.chatInput = this.chatInput.substring(0, this.chatInput.length() - 1);
                    return true;
                }

                if (this.isAddFriendActive) {
                    if (this.sidebarSection == 1 && !this.groupNameInput.isEmpty()) {
                        this.groupNameInput = this.groupNameInput.substring(0, this.groupNameInput.length() - 1);
                        return true;
                    }

                    if (this.sidebarSection != 1 && !this.addFriendInput.isEmpty()) {
                        this.addFriendInput = this.addFriendInput.substring(0, this.addFriendInput.length() - 1);
                        return true;
                    }
                }
            }

            return super.keyPressed(input);
        }
    }

    void sendChatMessage() {
        if (!this.chatInput.trim().isEmpty()) {
            String message = this.chatInput.trim();
            if (this.selectedLocalGroup != null) {
                FriendHubClient$GroupData group = this.selectedGroup();
                if (group == null || FriendHubClient.INSTANCE.getState() != FriendHubClient$State.CONNECTED) {
                    this.showPanelNotice("Нет подключения к группе");
                    return;
                }

                FriendHubClient.INSTANCE.sendGroupMessage(group.id, message);
            } else if (this.selectedFriendLogin != null) {
                if (FriendHubClient.INSTANCE.getState() != FriendHubClient$State.CONNECTED) {
                    this.showPanelNotice("Нет подключения к серверу друзей");
                    return;
                }

                String peerKey = this.selectedFriendLogin.toLowerCase(Locale.ROOT);
                long now = System.currentTimeMillis();
                long remaining = 3000L - (now - this.lastDirectMessageSentAt.getOrDefault(peerKey, 0L));
                if (remaining > 0L) {
                    this.showPanelNotice("Подождите " + Math.max(1L, (remaining + 999L) / 1000L) + " сек.");
                    return;
                }

                FriendHubClient.INSTANCE.sendDm(this.selectedFriendLogin, message);
                this.lastDirectMessageSentAt.put(peerKey, now);
            } else {
                long now = System.currentTimeMillis();
                long remaining = 5000L - (now - lastGlobalMessageSentAt);
                if (remaining > 0L) {
                    this.showPanelNotice("Подождите " + Math.max(1L, (remaining + 999L) / 1000L) + " сек.");
                    return;
                }

                if (!ChatHubClient.INSTANCE.sendMessage(message)) {
                    this.showPanelNotice("Нет подключения к общему чату");
                    return;
                }

                lastGlobalMessageSentAt = now;
            }

            this.chatInput = "";
            this.targetMessageScroll = 0.0F;
        }
    }

    void sendFriendRequest() {
        if (!this.addFriendInput.trim().isEmpty()) {
            if (FriendHubClient.INSTANCE.getState() != FriendHubClient$State.CONNECTED) {
                this.showPanelNotice("Нет подключения к серверу друзей");
            } else {
                FriendHubClient.INSTANCE.sendFriendRequest(this.addFriendInput.trim());
                this.addFriendInput = "";
                this.addButtonAnimation = 1.0F;
            }
        }
    }

    void createServerGroup() {
        String name = this.groupNameInput.trim();
        if (!name.isEmpty()) {
            if (FriendHubClient.INSTANCE.getState() != FriendHubClient$State.CONNECTED) {
                this.showPanelNotice("Нет подключения к серверу друзей");
            } else {
                if (name.length() > 28) {
                    name = name.substring(0, 28);
                }

                this.pendingCreatedGroupName = name;
                FriendHubClient.INSTANCE.createGroup(name);
                this.showPanelNotice("Создание группы: " + name);
                this.groupNameInput = "";
                this.addButtonAnimation = 1.0F;
                this.sectionTransition = 0.0F;
            }
        }
    }

    void inviteGroupMember() {
        FriendHubClient$GroupData group = this.selectedGroup();
        String login = this.groupNameInput.trim();
        if (group != null && !login.isEmpty()) {
            if (FriendHubClient.INSTANCE.getState() != FriendHubClient$State.CONNECTED) {
                this.showPanelNotice("Нет подключения к серверу друзей");
            } else {
                FriendHubClient.INSTANCE.inviteToGroup(group.id, login);
                this.showPanelNotice("Приглашение отправлено: " + login);
                this.groupNameInput = "";
                this.addButtonAnimation = 1.0F;
            }
        }
    }

    void shareCurrentGeoposition() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            int x = (int)Math.round(mc.player.getX());
            int y = (int)Math.round(mc.player.getY());
            int z = (int)Math.round(mc.player.getZ());
            String geoText = "Координаты: " + x + " " + y + " " + z;
            FriendHubClient$GroupData group = this.selectedGroup();
            if (group != null) {
                FriendHubClient.INSTANCE.sendGroupMessage(group.id, geoText);
            } else if (this.selectedFriendLogin != null) {
                FriendHubClient.INSTANCE.sendDm(this.selectedFriendLogin, geoText);
            } else {
                ChatHubClient.INSTANCE.sendMessage(geoText);
            }

            this.showPanelNotice("Геопозиция отправлена в чат");
        }
    }

    void createMarkerFromMessage(ChatHubMessage msg) {
        double[] coords = parseCoords(msg.message());
        if (coords != null) {
            Markers markers = ModuleManager.INSTANCE.getModule(Markers.class);
            if (markers != null) {
                markers.addMarker(
                    new Markers$MarkerData(
                        msg.login() + " Geo", coords[0], coords[1], coords[2], 'A', -14408146, -1, -1, 255, 1.0F, Markers.getIconBackgroundColor()
                    )
                );
            }

            Hud hud;
            if ((hud = ModuleManager.INSTANCE.getModule(Hud.class)) != null) {
                hud.setGpsTarget(coords[0], coords[2]);
            }

            this.showPanelNotice("Метка создана: " + (int)coords[0] + " " + (int)coords[1] + " " + (int)coords[2]);
        }
    }

    static boolean isGeoMessage(String text) {
        return text == null ? false : GEO_PATTERN.matcher(text).find();
    }

    static String extractCoords(String text) {
        if (text == null) {
            return "0 0 0";
        }

        Matcher m = GEO_PATTERN.matcher(text);
        return m.find() ? m.group(1) + " " + m.group(2) + " " + m.group(3) : text;
    }

    static double[] parseCoords(String text) {
        if (text == null) {
            return null;
        }

        Matcher m = GEO_PATTERN.matcher(text);
        if (m.find()) {
            try {
                return new double[]{Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3))};
            } catch (Exception var3) {
            }
        }

        return null;
    }

    static String getUsername() {
        return IrcIdentity.getLogin();
    }

    static float sidebarTabsStartX() {
        float width = 0.0F;

        for (int index = 0; index < SIDEBAR_SECTIONS.length; index++) {
            width += TextRender.width(SIDEBAR_SECTIONS[index], 12.5F);
            if (index < SIDEBAR_SECTIONS.length - 1) {
                width += TextRender.width("|", 12.5F) + 16.0F;
            }
        }

        return (260.0F - width) / 2.0F;
    }

    static boolean inside(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    static int fade(int color, float opacity) {
        int alpha = Math.round((color >>> 24) * opacity);
        return alpha << 24 | color & 16777215;
    }

    static int blendColor(int from, int to, float progress) {
        float t = MathHelper.clamp(progress, 0.0F, 1.0F);
        int a = Math.round((from >>> 24 & 0xFF) + ((to >>> 24 & 0xFF) - (from >>> 24 & 0xFF)) * t);
        int r = Math.round((from >>> 16 & 0xFF) + ((to >>> 16 & 0xFF) - (from >>> 16 & 0xFF)) * t);
        int g = Math.round((from >>> 8 & 0xFF) + ((to >>> 8 & 0xFF) - (from >>> 8 & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public static boolean handleToastClick(double x, double y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.currentScreen instanceof ChatScreen) {
            long now = System.currentTimeMillis();

            for (int index = TOASTS.size() - 1; index >= 0; index--) {
                IrcChatMessengerScreen$MessengerToast toast = TOASTS.get(index);
                if (now - toast.createdAt <= 4000L
                    && !(toast.drawW <= 0.0F)
                    && x >= toast.drawX
                    && x <= toast.drawX + toast.drawW
                    && y >= toast.drawY
                    && y <= toast.drawY + toast.drawH) {
                    TOASTS.remove(toast);
                    String targetType = "dm".equalsIgnoreCase(toast.type)
                        ? "dm"
                        : ("group".equalsIgnoreCase(toast.type) ? "group" : ("request".equalsIgnoreCase(toast.type) ? "request" : "global"));
                    String peer = !"dm".equalsIgnoreCase(targetType) && !"group".equalsIgnoreCase(targetType) ? null : toast.extra;
                    client.setScreen(new IrcChatMessengerScreen(targetType, peer));
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public static void renderToasts(DrawContext context, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getWindow() != null && !TOASTS.isEmpty()) {
            long now = System.currentTimeMillis();
            TOASTS.removeIf(toast -> now - toast.createdAt > 4000L);
            if (!TOASTS.isEmpty()) {
                float toastW = 119.0F;
                float toastH = 29.0F;
                float gap = 4.0F;
                float screenW = client.getWindow().getScaledWidth();
                float screenH = client.getWindow().getScaledHeight();
                float x = screenW - toastW - 7.0F;
                int count = TOASTS.size();

                for (int index = 0; index < count; index++) {
                    IrcChatMessengerScreen$MessengerToast toast2 = TOASTS.get(index);
                    long age = now - toast2.createdAt;
                    float enter = MathHelper.clamp((float)age / 260.0F, 0.0F, 1.0F);
                    float exit = age <= 3700L ? 1.0F : MathHelper.clamp((float)(4000L - age) / 300.0F, 0.0F, 1.0F);
                    float visibility = easeOut(enter) * exit;
                    float targetY = screenH - toastH - 7.0F - (count - 1 - index) * (toastH + gap);
                    if (Float.isNaN(toast2.currentY)) {
                        toast2.currentY = screenH + 3.0F;
                    }

                    toast2.currentY = MathHelper.lerp(0.18F, toast2.currentY, targetY);
                    float drawY = toast2.currentY + (1.0F - visibility) * 14.0F;
                    toast2.drawX = x;
                    toast2.drawY = drawY;
                    toast2.drawW = toastW;
                    toast2.drawH = toastH;
                    RoundedRenderer.drawRoundedRect(context, x, drawY, toastW, toastH, 6.0F, fade(-317714410, visibility));
                    RoundedRenderer.drawRoundedOutline(context, x, drawY, toastW, toastH, 6.0F, 0.5F, fade(721420287, visibility));
                    Identifier avatar = "global".equalsIgnoreCase(toast2.type)
                        ? ChatHubClient.INSTANCE.getAvatarTexture(toast2.avatarUrl)
                        : FriendHubClient.INSTANCE.getAvatarTexture(toast2.avatarUrl);
                    if (avatar == null) {
                        avatar = FALLBACK_AVATAR;
                    }

                    RenderUtil.drawRoundedTexture(
                        context, avatar, x + 5.0F, drawY + 5.0F, 19.0F, 19.0F, 9.5F, true, true, true, true, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, visibility
                    );
                    String heading = toast2.login + " · " + toast2.title;
                    TextRender.draw(context, fitToastText(heading, 27), x + 29.0F, drawY + 11.5F, 5.75F, fade(-1, visibility));
                    TextRender.draw(context, fitToastText(toast2.body, 31), x + 29.0F, drawY + 20.5F, 5.25F, fade(-1962934273, visibility));
                }
            }
        }
    }

    public static void pushRichNotification(String type, String avatarUrl, String login, String title, String body, String extra) {
        if ("request".equalsIgnoreCase(type)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                Screen class_4372 = client.currentScreen;
                if (client.currentScreen instanceof IrcChatMessengerScreen) {
                    IrcChatMessengerScreen screen = (IrcChatMessengerScreen)class_4372;
                    screen.showPanelNotice(title + ": " + body);
                    return;
                }
            }
        } else {
            String key = MessengerPreferences.keyFor(type, extra, login);
            if (!MessengerPreferences.isEnabled(key)) {
                return;
            }
        }

        if ("global".equalsIgnoreCase(type)) {
            ChatHubClient.INSTANCE.loadAvatar(avatarUrl);
        } else {
            FriendHubClient.INSTANCE.loadAvatar(avatarUrl);
        }

        TOASTS.add(new IrcChatMessengerScreen$MessengerToast(type, avatarUrl, login, title, body, extra, System.currentTimeMillis()));

        while (TOASTS.size() > 5) {
            TOASTS.remove(0);
        }
    }

    public static void pushRichNotification(String type, String avatarUrl, String login, String title, String body) {
        pushRichNotification(type, avatarUrl, login, title, body, login);
    }

    static String fitToastText(String text, int maxLength) {
        if (text == null) {
            return "";
        } else {
            return text.length() <= maxLength ? text : text.substring(0, Math.max(0, maxLength - 3)) + "...";
        }
    }
}
