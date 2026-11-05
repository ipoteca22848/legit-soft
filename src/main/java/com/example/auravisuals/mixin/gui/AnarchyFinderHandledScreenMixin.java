package com.example.auravisuals.mixin.gui;

import com.example.auravisuals.client.gui.font.icon.IconTtfBridge;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.misc.AnarchyFinder;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class AnarchyFinderHandledScreenMixin extends Screen {
    @Unique
    private static final Identifier SEARCH_ICON = Identifier.of("auravisuals", "textures/search.png");
    @Shadow
    protected int x;
    @Shadow
    protected int y;
    @Shadow
    protected int backgroundWidth;
    @Unique
    private ButtonWidget anarchyFinderModeButton;
    @Unique
    private ButtonWidget anarchyFinderConnectLeftButton;
    @Unique
    private ButtonWidget anarchyFinderConnectButton;

    protected AnarchyFinderHandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void auravisuals$onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        AnarchyFinder module = ModuleManager.INSTANCE.getModule(AnarchyFinder.class);
        if (module != null && module.isEnabled()) {
            boolean shouldShowButton = AnarchyFinder.isModeContainerTitle(this.getTitle());
            if (shouldShowButton) {
                int buttonWidth = 12;
                int buttonHeight = 12;
                int buttonX = this.x + this.backgroundWidth - buttonWidth - 6;
                int buttonY = this.y + 5;
                if (this.anarchyFinderModeButton == null) {
                    this.anarchyFinderModeButton = ButtonWidget.builder(Text.of(""), button -> module.startScan())
                        .dimensions(buttonX, buttonY, buttonWidth, buttonHeight)
                        .build();
                    this.addDrawableChild(this.anarchyFinderModeButton);
                } else {
                    this.anarchyFinderModeButton.setX(buttonX);
                    this.anarchyFinderModeButton.setY(buttonY);
                    this.anarchyFinderModeButton.active = !module.isScanInProgress();
                }

                this.anarchyFinderModeButton.visible = true;
            } else if (this.anarchyFinderModeButton != null) {
                this.anarchyFinderModeButton.visible = false;
            }

            if (module.isResultReady()) {
                int centerX = this.x + this.backgroundWidth / 2;
                int titleY = this.y - 28;
                int valuesY = this.y - 12;
                int titleColor = -6684775;
                int verLeftColor = -32640;
                int verRightColor = -5103070;
                int onlineColor = -171;
                int cmdColor = -1;
                context.drawCenteredTextWithShadow(this.textRenderer, "Минимальный онлайн:", centerX, titleY, titleColor);
                int first = module.getFirstMinOnline();
                int second = module.getSecondMinOnline();
                String firstId = module.getFirstAnarchyId();
                String secondId = module.getSecondAnarchyId();
                String leftOnline = first > 0 ? String.valueOf(first) : "не найден";
                String rightOnline = second > 0 ? String.valueOf(second) : "не найден";
                Object leftCmd = firstId != null ? "(/an" + firstId + ")" : "(/an?)";
                Object rightCmd = secondId != null ? "(/an" + secondId + ")" : "(/an?)";
                int leftX = this.x - 58;
                int rightX = this.x + this.backgroundWidth - 85;
                String leftPrefix = "1.16.5 - ";
                context.drawTextWithShadow(this.textRenderer, leftPrefix, leftX, valuesY, verLeftColor);
                int leftAfterPrefix = leftX + this.textRenderer.getWidth(leftPrefix);
                context.drawTextWithShadow(this.textRenderer, leftOnline, leftAfterPrefix, valuesY, onlineColor);
                int leftAfterOnline = leftAfterPrefix + this.textRenderer.getWidth(leftOnline);
                context.drawTextWithShadow(this.textRenderer, " " + (String)leftCmd, leftAfterOnline, valuesY, cmdColor);
                String rightPrefix = "1.21.8 - ";
                context.drawTextWithShadow(this.textRenderer, rightPrefix, rightX, valuesY, verRightColor);
                int rightAfterPrefix = rightX + this.textRenderer.getWidth(rightPrefix);
                context.drawTextWithShadow(this.textRenderer, rightOnline, rightAfterPrefix, valuesY, onlineColor);
                int rightAfterOnline = rightAfterPrefix + this.textRenderer.getWidth(rightOnline);
                context.drawTextWithShadow(this.textRenderer, " " + (String)rightCmd, rightAfterOnline, valuesY, cmdColor);
                int btnW = 14;
                int btnH = 12;
                int btnY = valuesY - 2;
                int leftBtnX = leftAfterOnline + this.textRenderer.getWidth(" " + (String)leftCmd) + 4;
                if (this.anarchyFinderConnectLeftButton == null) {
                    this.anarchyFinderConnectLeftButton = ButtonWidget.builder(Text.of("->"), button -> {
                        if (this.client != null && this.client.player != null) {
                            String id = module.getFirstAnarchyId();
                            if (id != null && !id.isEmpty()) {
                                this.client.player.closeHandledScreen();
                                this.client.setScreen(null);
                                if (this.client.player.networkHandler != null) {
                                    this.client.player.networkHandler.sendChatMessage("/an" + id);
                                }
                            }
                        }
                    }).dimensions(leftBtnX, btnY, btnW, btnH).build();
                    this.addDrawableChild(this.anarchyFinderConnectLeftButton);
                } else {
                    this.anarchyFinderConnectLeftButton.setX(leftBtnX);
                    this.anarchyFinderConnectLeftButton.setY(btnY);
                }

                this.anarchyFinderConnectLeftButton.active = this.anarchyFinderConnectLeftButton.visible = firstId != null && !firstId.isEmpty();
                int btnX = rightAfterOnline + this.textRenderer.getWidth(" " + (String)rightCmd) + 4;
                if (this.anarchyFinderConnectButton == null) {
                    this.anarchyFinderConnectButton = ButtonWidget.builder(Text.of("->"), button -> {
                        if (this.client != null && this.client.player != null) {
                            String id = module.getSecondAnarchyId();
                            if (id != null && !id.isEmpty()) {
                                this.client.player.closeHandledScreen();
                                this.client.setScreen(null);
                                if (this.client.player.networkHandler != null) {
                                    this.client.player.networkHandler.sendChatMessage("/an" + id);
                                }
                            }
                        }
                    }).dimensions(btnX, btnY, btnW, btnH).build();
                    this.addDrawableChild(this.anarchyFinderConnectButton);
                } else {
                    this.anarchyFinderConnectButton.setX(btnX);
                    this.anarchyFinderConnectButton.setY(btnY);
                }

                this.anarchyFinderConnectButton.active = this.anarchyFinderConnectButton.visible = secondId != null && !secondId.isEmpty();
            } else {
                if (this.anarchyFinderConnectButton != null) {
                    this.anarchyFinderConnectButton.visible = false;
                    this.anarchyFinderConnectButton.active = false;
                }

                if (this.anarchyFinderConnectLeftButton != null) {
                    this.anarchyFinderConnectLeftButton.visible = false;
                    this.anarchyFinderConnectLeftButton.active = false;
                }
            }
        } else {
            this.auravisuals$hideFinderWidgets();
        }
    }

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void auravisuals$drawSearchIconOnTop(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        AnarchyFinder module = ModuleManager.INSTANCE.getModule(AnarchyFinder.class);
        if (module != null && module.isEnabled()) {
            if (this.anarchyFinderModeButton != null) {
                if (this.anarchyFinderModeButton.visible) {
                    int iconX = this.anarchyFinderModeButton.getX() + 1;
                    int iconY = this.anarchyFinderModeButton.getY() + 2;
                    int iconW = Math.max(8, this.anarchyFinderModeButton.getWidth() - 2);
                    int iconH = Math.max(8, this.anarchyFinderModeButton.getHeight() - 2);
                    int iconSz = Math.max(4, Math.round(Math.min(iconW, iconH) * 0.52F));
                    int iconCol = -1;
                    int off = (Math.min(iconW, iconH) - iconSz) / 2;
                    if (!IconTtfBridge.tryDrawIconsFontChar(context, 'e', iconX + off, iconY + off, iconSz, iconCol)) {
                        context.drawTexture(RenderPipelines.GUI_TEXTURED, SEARCH_ICON, iconX + off, iconY + off, 0.0F, 0.0F, iconSz, iconSz, iconSz, iconSz);
                    }
                }
            }
        }
    }

    @Unique
    private void auravisuals$hideFinderWidgets() {
        if (this.anarchyFinderModeButton != null) {
            this.anarchyFinderModeButton.visible = false;
        }

        if (this.anarchyFinderConnectButton != null) {
            this.anarchyFinderConnectButton.visible = false;
            this.anarchyFinderConnectButton.active = false;
        }

        if (this.anarchyFinderConnectLeftButton != null) {
            this.anarchyFinderConnectLeftButton.visible = false;
            this.anarchyFinderConnectLeftButton.active = false;
        }
    }
}
