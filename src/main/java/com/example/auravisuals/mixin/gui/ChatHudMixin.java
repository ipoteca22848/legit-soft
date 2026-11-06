package com.example.auravisuals.mixin.gui;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.misc.ChatHelper;
import com.example.auravisuals.module.misc.Globals;
import com.example.auravisuals.module.misc.StreamerMode;
import com.example.auravisuals.module.render.Animations;
import com.example.auravisuals.module.render.SpeechBubbles;
import com.example.auravisuals.util.registry.GlobalsIconText;
import com.example.auravisuals.util.registry.GlobalsRegistry;
import com.example.auravisuals.util.render.RenderUtil;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Shadow
    private List<Visible> visibleMessages;
    @Shadow
    private List<ChatHudLine> messages;
    @Unique
    private String lastMessageString = null;
    @Unique
    private int spamCount = 1;
    @Unique
    private final Map<Integer, Long> auravisuals$chatAnimationStarts = new HashMap<>();
    @Unique
    private final IdentityHashMap<OrderedText, Integer> auravisuals$visibleLineTimes = new IdentityHashMap<>();
    @Unique
    private long auravisuals$chatAnimationNow = 0L;
    @Unique
    private boolean auravisuals$chatAnimationMatrixPushed = false;
    @Unique
    private static final Identifier GLOBALS_LOGO = Identifier.of("auravisuals", "textures/logoaura_globals.png");
    @Unique
    private static final int CHAT_TEX_SIZE = 128;
    @Unique
    private static final int CHAT_DISP = 12;
    @Unique
    private static final int CHAT_GAP = 2;
    @Unique
    private int auravisuals$chatIconX = Integer.MIN_VALUE;
    @Unique
    private int auravisuals$chatIconY = Integer.MIN_VALUE;

    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Text modifyMessage(Text message) {
        return this.auravisuals$gradientPrefix(StreamerMode.replaceInChatText(message));
    }

    @Inject(method = "clear", at = @At("HEAD"), cancellable = true, require = 0)
    private void onClear(boolean clearHistory, CallbackInfo ci) {
        ChatHelper module = ModuleManager.INSTANCE.getModule(ChatHelper.class);
        if (module != null && module.isEnabled() && ChatHelper.keepHistory.getValue()) {
            ci.cancel();
        }
    }

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Text onAddMessageModify(Text message) {
        message = this.auravisuals$gradientPrefix(message);
        message = this.auravisuals$globalsPrefix(message);
        ChatHelper module = ModuleManager.INSTANCE.getModule(ChatHelper.class);
        if (module != null && module.isEnabled() && ChatHelper.antiSpam.getValue()) {
            String currentString = message.getString();
            if (this.lastMessageString != null && currentString.equals(this.lastMessageString)) {
                this.spamCount++;
                return message.copy().append(Text.literal(" (x" + this.spamCount + ")").formatted(Formatting.DARK_GRAY));
            }

            this.lastMessageString = currentString;
            this.spamCount = 1;
        }

        return message;
    }

    @Unique
    private Text auravisuals$globalsPrefix(Text message) {
        if (message != null && Globals.shouldShowIcon()) {
            String content = message.getString().toLowerCase(Locale.ROOT);

            for (String player : GlobalsRegistry.getPlayers()) {
                if (!player.isEmpty() && content.contains(player)) {
                    return GlobalsIconText.prefix(message);
                }
            }

            return message;
        } else {
            return message;
        }
    }

    @Unique
    private Text auravisuals$gradientPrefix(Text message) {
        if (message == null) {
            return message;
        }

        String raw = message.getString();
        String prefix = "[anvil visual]";
        int index = raw.indexOf("[anvil visual]");
        if (index < 0) {
            return message;
        }

        MutableText result = Text.empty();
        if (index > 0) {
            result.append(Text.literal(raw.substring(0, index)));
        }

        int[] colors = new int[]{16726832, 16730678, 16734780, 16740420, 16747085, 16754268, 16742504, 16732754, 15021108, 12918568, 11015199, 8326679};

        for (int i = 0; i < prefix.length(); i++) {
            int color = colors[Math.min(colors.length - 1, i)];
            result.append(Text.literal(String.valueOf(prefix.charAt(i))).styled(style -> style.withColor(color).withBold(true)));
        }

        int restStart = index + prefix.length();
        if (restStart < raw.length()) {
            this.auravisuals$appendAuraRest(result, raw.substring(restStart));
        }

        return result;
    }

    @Unique
    private void auravisuals$appendAuraRest(MutableText result, String rest) {
        String stripped = Formatting.strip(rest);
        if (stripped == null) {
            stripped = rest;
        }

        int firstText = 0;

        while (firstText < stripped.length() && Character.isWhitespace(stripped.charAt(firstText))) {
            firstText++;
        }

        if (firstText > 0) {
            result.append(Text.literal(stripped.substring(0, firstText)).formatted(Formatting.GRAY));
        }

        String body;
        int separator;
        if ((separator = (body = stripped.substring(firstText)).indexOf(" - ")) > 0 && body.indexOf(32) == separator) {
            result.append(Text.literal(body.substring(0, separator)).styled(style -> style.withColor(16726832)));
            result.append(Text.literal(body.substring(separator)).formatted(Formatting.GRAY));
        } else {
            result.append(Text.literal(body).formatted(Formatting.GRAY));
        }
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"))
    private void onAddMessage(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        if (message != null) {
            SpeechBubbles.ingest(message.getString());
        }

        ChatHelper module;
        if ((module = ModuleManager.INSTANCE.getModule(ChatHelper.class)) != null && module.isEnabled() && ChatHelper.antiSpam.getValue() && this.spamCount > 1
            )
         {
            if (!this.messages.isEmpty()) {
                this.messages.remove(0);
            }

            if (!this.visibleMessages.isEmpty()) {
                int addedTime = this.visibleMessages.get(0).addedTime();
                this.visibleMessages.removeIf(line -> line.addedTime() == addedTime);
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void auravisuals$prepareChatAnimation(
        DrawContext context, TextRenderer textRenderer, int currentTick, int mouseX, int mouseY, boolean focused, boolean bl, CallbackInfo ci
    ) {
        this.auravisuals$chatAnimationMatrixPushed = false;
        this.auravisuals$visibleLineTimes.clear();
        this.auravisuals$chatAnimationNow = System.currentTimeMillis();
        if (!this.auravisuals$isChatAnimationEnabled()) {
            this.auravisuals$chatAnimationStarts.clear();
        } else {
            for (Visible line : this.visibleMessages) {
                this.auravisuals$visibleLineTimes.put(line.content(), line.addedTime());
                int ageTicks = currentTick - line.addedTime();
                if (ageTicks >= 0 && ageTicks <= 2 && !this.auravisuals$chatAnimationStarts.containsKey(line.addedTime())) {
                    this.auravisuals$chatAnimationStarts.put(line.addedTime(), this.auravisuals$chatAnimationNow);
                }
            }

            Iterator<Entry<Integer, Long>> iterator = this.auravisuals$chatAnimationStarts.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry<Integer, Long> entry = iterator.next();
                if (this.auravisuals$chatAnimationNow - entry.getValue() > 260L) {
                    iterator.remove();
                }
            }

            long newestStart = 0L;

            for (long started : this.auravisuals$chatAnimationStarts.values()) {
                newestStart = Math.max(newestStart, started);
            }

            float progress;
            if (newestStart > 0L && (progress = MathHelper.clamp((float)(this.auravisuals$chatAnimationNow - newestStart) / 170.0F, 0.0F, 1.0F)) < 1.0F) {
                float eased = 1.0F - (float)Math.pow(1.0F - progress, 3.0);
                context.getMatrices().pushMatrix();
                context.getMatrices().translate(-Math.round((1.0F - eased) * 34.0F), 0.0F);
                this.auravisuals$chatAnimationMatrixPushed = true;
            }
        }
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void auravisuals$finishChatAnimation(
        DrawContext context, TextRenderer textRenderer, int currentTick, int mouseX, int mouseY, boolean focused, boolean bl, CallbackInfo ci
    ) {
        if (this.auravisuals$chatAnimationMatrixPushed) {
            context.getMatrices().popMatrix();
            this.auravisuals$chatAnimationMatrixPushed = false;
        }
    }

    @Unique
    private boolean auravisuals$isChatAnimationEnabled() {
        Animations animations = Animations.INSTANCE;
        return animations != null && animations.isEnabled() && animations.isModeEnabled("Анимация чата");
    }

    @ModifyArgs(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I",
            ordinal = 0
        ),
        require = 0
    )
    private void shiftGlobalsChatText(Args args) {
        this.auravisuals$chatIconX = Integer.MIN_VALUE;
        if (Globals.shouldShowIcon()) {
            Set<String> globalsPlayers = GlobalsRegistry.getPlayers();
            if (!globalsPlayers.isEmpty()) {
                OrderedText orderedText = (OrderedText)args.get(1);
                if (orderedText != null) {
                    StringBuilder sb = new StringBuilder(80);
                    orderedText.accept((index, style, codePoint) -> {
                        sb.appendCodePoint(codePoint);
                        return sb.length() < 80;
                    });
                    String content = sb.toString();
                    if (content.indexOf(57344) < 0) {
                        String contentLower = content.toLowerCase(Locale.ROOT);
                        boolean found = false;
                        int lt = content.indexOf(60);
                        int gt = lt >= 0 ? content.indexOf(62, lt) : -1;
                        Object senderName;
                        if (lt >= 0
                            && gt > lt
                            && !((String)(senderName = content.substring(lt + 1, gt).trim())).isEmpty()
                            && GlobalsRegistry.isGlobalsPlayer((String)senderName)) {
                            found = true;
                        }

                        if (!found) {
                            for (String name : globalsPlayers) {
                                if (contentLower.contains(name)) {
                                    found = true;
                                    break;
                                }
                            }
                        }

                        if (found) {
                            int x = (Integer)args.get(2);
                            int y = (Integer)args.get(3);
                            this.auravisuals$chatIconX = x;
                            this.auravisuals$chatIconY = y + -1 + 2;
                            args.set(2, x + 12 + 2);
                        }
                    }
                }
            }
        }
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I",
            ordinal = 0,
            shift = Shift.AFTER
        ),
        require = 0
    )
    private void drawGlobalsChatIcon(
        DrawContext context, TextRenderer textRenderer, int currentTick, int mouseX, int mouseY, boolean focused, boolean bl, CallbackInfo ci
    ) {
        if (this.auravisuals$chatIconX != Integer.MIN_VALUE) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(this.auravisuals$chatIconX, this.auravisuals$chatIconY);
            RenderUtil.shaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderUtil.drawTexture(context, GLOBALS_LOGO, 0.0F, 0.0F, 12.0F, 12.0F, 0.0F, 0.0F, 128.0F, 128.0F, 128.0F, 128.0F, true);
            RenderUtil.shaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            context.getMatrices().popMatrix();
            this.auravisuals$chatIconX = Integer.MIN_VALUE;
        }
    }
}
