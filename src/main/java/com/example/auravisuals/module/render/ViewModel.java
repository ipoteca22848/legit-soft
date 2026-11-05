package com.example.auravisuals.module.render;

import com.example.auravisuals.client.gui.hud.HudEditor;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.LabelSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.render.HandFxPipeline;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterInit;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.Remove;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents.BeforeMouseDrag;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents.BeforeMouseRelease;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;

public class ViewModel extends Module {
    static final int NO_HAND = 0;
    static final int LEFT_HAND = -1;
    static final int RIGHT_HAND = 1;
    static boolean editorHooksInstalled;
    static int draggingHand = 0;
    static int hoveredHand = 0;
    static double rawDragX;
    static double rawDragY;
    static long lastSmoothNanos;
    static double renderedRightX = Double.NaN;
    static double renderedRightY = Double.NaN;
    static double renderedRightZ = Double.NaN;
    static double renderedRightScale = Double.NaN;
    static double renderedLeftX = Double.NaN;
    static double renderedLeftY = Double.NaN;
    static double renderedLeftZ = Double.NaN;
    static double renderedLeftScale = Double.NaN;
    static int outlinedHand = 0;
    static float outlineOpacity;
    static float outlineExpansion;
    static long lastOutlineNanos;
    static final double SCALE_SCROLL_STEP = 0.04;
    public static final LabelSetting rightLabel = new LabelSetting("Правая рука").centered();
    public static final NumberSetting rightScale = new NumberSetting("Right Scale", 1.0, 0.1, 2.0, 0.01);
    public static final NumberSetting rightX = new NumberSetting("Right X", 0.0, -1.0, 1.0, 0.01);
    public static final NumberSetting rightY = new NumberSetting("Right Y", 0.0, -1.0, 1.0, 0.01);
    public static final NumberSetting rightZ = new NumberSetting("Right Z", 0.0, -1.0, 1.0, 0.1);
    public static final LabelSetting leftLabel = new LabelSetting("Левая рука").centered();
    public static final NumberSetting leftScale = new NumberSetting("Left Scale", 1.0, 0.1, 2.0, 0.01);
    public static final NumberSetting leftX = new NumberSetting("Left X", 0.0, -1.0, 1.0, 0.01);
    public static final NumberSetting leftY = new NumberSetting("Left Y", 0.0, -1.0, 1.0, 0.01);
    public static final NumberSetting leftZ = new NumberSetting("Left Z", 0.0, -1.0, 1.0, 0.1);

    public ViewModel() {
        super("ViewModel", "Настройка отображения рук", Category.VISUALS);
        rightScale.setDisplayName("Размер");
        rightX.setDisplayName("Координата X");
        rightY.setDisplayName("Координата Y");
        rightZ.setDisplayName("Координата Z");
        leftScale.setDisplayName("Размер");
        leftX.setDisplayName("Координата X");
        leftY.setDisplayName("Координата Y");
        leftZ.setDisplayName("Координата Z");
        this.addSettings(rightLabel, rightScale, rightX, rightY, rightZ, leftLabel, leftScale, leftX, leftY, leftZ);
        installEditorHooks();
    }

    @Override
    public void onTick() {
        if (!(this.mc.currentScreen instanceof ChatScreen)) {
            resetEditorState();
        }
    }

    @Override
    public void onDisable() {
        resetEditorState();
    }

    public static boolean handleEditorClick(Click click) {
        if (click.button() != 0 || !canEdit()) {
            return false;
        }

        if (!HudEditor.isDragging() && HudEditor.hovered() == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            int hand = pixelHandAt(click.x(), click.y(), client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
            if (hand == 0) {
                return false;
            }

            draggingHand = hand;
            hoveredHand = hand;
            rawDragX = xSetting(hand).getValue();
            rawDragY = ySetting(hand).getValue();
            return true;
        } else {
            return false;
        }
    }

    public static boolean handleEditorScroll(double mouseX, double mouseY, double amount) {
        if (amount != 0.0 && canEdit() && HudEditor.hovered() == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            int hand = draggingHand;
            if (hand == 0 && (hand = pixelHandAt(mouseX, mouseY, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight())) == 0) {
                return false;
            }

            if (client.isShiftPressed()) {
                NumberSetting z = zSetting(hand);
                z.setValue(z.getValue() + Math.copySign(z.getIncrement(), amount));
            } else {
                NumberSetting scale = scaleSetting(hand);
                scale.setValue(scale.getValue() + Math.copySign(0.04, amount));
            }

            return true;
        } else {
            return false;
        }
    }

    public static boolean shouldRenderEditorOutline() {
        return activeEditorHand() != 0;
    }

    public static boolean isEditorSessionActive() {
        return canEdit();
    }

    public static ViewModel$EditorSelection editorOutlineSelection() {
        int activeHand = activeEditorHand();
        long now = System.nanoTime();
        if (lastOutlineNanos == 0L) {
            lastOutlineNanos = now;
        }

        double seconds = Math.min(0.05, Math.max(0.0, (now - lastOutlineNanos) / 1.0E9));
        lastOutlineNanos = now;
        if (activeHand != 0) {
            outlinedHand = activeHand;
        }

        float opacityTarget = activeHand == 0 ? 0.0F : 1.0F;
        float expansionTarget = draggingHand == 0 ? 0.0F : 1.0F;
        outlineOpacity = animate(outlineOpacity, opacityTarget, seconds, opacityTarget > outlineOpacity ? 15.0F : 9.0F);
        outlineExpansion = animate(outlineExpansion, expansionTarget, seconds, 11.0F);
        if (outlinedHand == 0 || outlineOpacity < 0.01F && activeHand == 0) {
            if (activeHand == 0) {
                outlinedHand = 0;
            }

            return null;
        } else {
            int hand = outlinedHand;
            MinecraftClient client = MinecraftClient.getInstance();
            int screenWidth = Math.max(1, client.getWindow().getScaledWidth());
            int screenHeight;
            ViewModel$HandRegion region = regionFor(hand, screenWidth, screenHeight = Math.max(1, client.getWindow().getScaledHeight()));
            if (region == null) {
                return null;
            }

            ViewModel$HandRegion other = regionFor(-hand, screenWidth, screenHeight);
            float otherCenterX = other == null ? -10.0F : other.centerX() / screenWidth;
            float otherCenterY = other == null ? -10.0F : other.centerY() / screenHeight;
            return new ViewModel$EditorSelection(
                region.centerX() / screenWidth, region.centerY() / screenHeight, otherCenterX, otherCenterY, outlineOpacity, outlineExpansion, isMainHand(hand)
            );
        }
    }

    static float animate(float current, float target, double seconds, float speed) {
        float factor = (float)(1.0 - Math.exp(-seconds * speed));
        float value = current + (target - current) * factor;
        return Math.abs(target - value) < 0.002F ? target : value;
    }

    static int activeEditorHand() {
        if (!canEdit()) {
            hoveredHand = 0;
            return 0;
        } else if (draggingHand != 0) {
            return draggingHand;
        } else if (!HudEditor.isDragging() && HudEditor.hovered() == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            double mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
            double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
            hoveredHand = pixelHandAt(mouseX, mouseY, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
            return hoveredHand;
        } else {
            return 0;
        }
    }

    public static int editorOutlineColor() {
        return Hud.accentColor.getValue();
    }

    static void installEditorHooks() {
        if (!editorHooksInstalled) {
            editorHooksInstalled = true;
            ScreenEvents.AFTER_INIT.register((AfterInit)(client, screen, width, height) -> {
                if (screen instanceof ChatScreen) {
                    ScreenMouseEvents.beforeMouseDrag(screen).register((BeforeMouseDrag)(s, click, deltaX, deltaY) -> dragEditor(click, deltaX, deltaY));
                    ScreenMouseEvents.beforeMouseRelease(screen).register((BeforeMouseRelease)(s, click) -> {
                        if (click.button() == 0) {
                            draggingHand = 0;
                        }
                    });
                    ScreenEvents.remove(screen).register((Remove)s -> resetEditorState());
                }
            });
        }
    }

    static void dragEditor(Click click, double deltaX, double deltaY) {
        if (draggingHand != 0 && click.button() == 0 && canEdit()) {
            MinecraftClient client = MinecraftClient.getInstance();
            double xPixelsPerUnit = xPixelsPerUnit(client.getWindow().getScaledWidth());
            double yPixelsPerUnit = yPixelsPerUnit(client.getWindow().getScaledHeight());
            xSetting(draggingHand).setValue(rawDragX += deltaX / xPixelsPerUnit);
            ySetting(draggingHand).setValue(rawDragY -= deltaY / yPixelsPerUnit);
        }
    }

    static boolean canEdit() {
        MinecraftClient client = MinecraftClient.getInstance();
        ViewModel module = ModuleManager.INSTANCE.getModule(ViewModel.class);
        return module != null
            && module.isEnabled()
            && client.currentScreen instanceof ChatScreen
            && client.player != null
            && client.options != null
            && client.options.getPerspective().isFirstPerson();
    }

    static int handAt(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        ViewModel$HandRegion right = regionFor(1, screenWidth, screenHeight);
        ViewModel$HandRegion left = regionFor(-1, screenWidth, screenHeight);
        if (right == null) {
            return left == null ? 0 : -1;
        } else if (left == null) {
            return 1;
        } else {
            return right.distanceSquared(mouseX, mouseY) <= left.distanceSquared(mouseX, mouseY) ? 1 : -1;
        }
    }

    static int pixelHandAt(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        ViewModel$HandRegion right = regionFor(1, screenWidth, screenHeight);
        ViewModel$HandRegion left = regionFor(-1, screenWidth, screenHeight);
        double rightHit = right == null ? Double.POSITIVE_INFINITY : HandFxPipeline.editorHandHitDistance(isMainHand(1), mouseX, mouseY);
        double leftHit = left == null ? Double.POSITIVE_INFINITY : HandFxPipeline.editorHandHitDistance(isMainHand(-1), mouseX, mouseY);
        if (!Double.isFinite(rightHit) && !Double.isFinite(leftHit)) {
            return 0;
        } else if (rightHit == leftHit) {
            return isMainHand(1) ? -1 : 1;
        } else {
            return rightHit < leftHit ? 1 : -1;
        }
    }

    static ViewModel$HandRegion regionFor(int hand, int screenWidth, int screenHeight) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (hand != 0 && client.player != null && !heldStack(hand).isEmpty()) {
            updateRenderedTransforms();
            float scale = (float)renderedScale(hand);
            float depthScale = 1.0F + (float)renderedZ(hand) * 0.055F;
            float visualScale = MathHelper.clamp(scale * depthScale, 0.55F, 1.65F);
            float width = MathHelper.clamp(300.0F * visualScale, 210.0F, Math.min(460.0F, screenWidth - 10.0F));
            float height = MathHelper.clamp(420.0F * visualScale, 280.0F, Math.min(560.0F, screenHeight - 10.0F));
            float xUnit = (float)xPixelsPerUnit(screenWidth);
            float yUnit = (float)yPixelsPerUnit(screenHeight);
            float centerX = screenWidth * (hand == 1 ? 0.76F : 0.24F) + (float)renderedX(hand) * xUnit;
            float centerY = screenHeight * 0.61F - (float)renderedY(hand) * yUnit;
            float x = centerX - width / 2.0F;
            float y = centerY - height / 2.0F;
            return new ViewModel$HandRegion(x, y, width, height);
        } else {
            return null;
        }
    }

    static double xPixelsPerUnit(int screenWidth) {
        return Math.max(320.0, Math.min(540.0, screenWidth * 0.46));
    }

    static double yPixelsPerUnit(int screenHeight) {
        return Math.max(300.0, Math.min(520.0, screenHeight * 0.68));
    }

    static void updateRenderedTransforms() {
        long now = System.nanoTime();
        if (Double.isFinite(renderedRightX) && Double.isFinite(renderedLeftX)) {
            double seconds = Math.min(0.05, Math.max(0.0, (now - lastSmoothNanos) / 1.0E9));
            lastSmoothNanos = now;
            double factor = 1.0 - Math.exp(-seconds * 24.0);
            renderedRightX = lerp(renderedRightX, rightX.getValue(), factor);
            renderedRightY = lerp(renderedRightY, rightY.getValue(), factor);
            renderedRightZ = lerp(renderedRightZ, rightZ.getValue(), factor);
            renderedRightScale = lerp(renderedRightScale, rightScale.getValue(), factor);
            renderedLeftX = lerp(renderedLeftX, leftX.getValue(), factor);
            renderedLeftY = lerp(renderedLeftY, leftY.getValue(), factor);
            renderedLeftZ = lerp(renderedLeftZ, leftZ.getValue(), factor);
            renderedLeftScale = lerp(renderedLeftScale, leftScale.getValue(), factor);
        } else {
            snapRenderedTransforms();
            lastSmoothNanos = now;
        }
    }

    static void snapRenderedTransforms() {
        renderedRightX = rightX.getValue();
        renderedRightY = rightY.getValue();
        renderedRightZ = rightZ.getValue();
        renderedRightScale = rightScale.getValue();
        renderedLeftX = leftX.getValue();
        renderedLeftY = leftY.getValue();
        renderedLeftZ = leftZ.getValue();
        renderedLeftScale = leftScale.getValue();
    }

    static double lerp(double current, double target, double factor) {
        return Math.abs(target - current) < 1.0E-4 ? target : current + (target - current) * factor;
    }

    public static double renderedX(boolean right) {
        updateRenderedTransforms();
        return renderedX(right ? 1 : -1);
    }

    public static double renderedY(boolean right) {
        updateRenderedTransforms();
        return renderedY(right ? 1 : -1);
    }

    public static double renderedZ(boolean right) {
        updateRenderedTransforms();
        return renderedZ(right ? 1 : -1);
    }

    public static float renderedScale(boolean right) {
        updateRenderedTransforms();
        return (float)renderedScale(right ? 1 : -1);
    }

    static double renderedX(int hand) {
        return hand == 1 ? renderedRightX : renderedLeftX;
    }

    static double renderedY(int hand) {
        return hand == 1 ? renderedRightY : renderedLeftY;
    }

    static double renderedZ(int hand) {
        return hand == 1 ? renderedRightZ : renderedLeftZ;
    }

    static double renderedScale(int hand) {
        return hand == 1 ? renderedRightScale : renderedLeftScale;
    }

    static ItemStack heldStack(int hand) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return ItemStack.EMPTY;
        } else {
            return isMainHand(hand) ? client.player.getMainHandStack() : client.player.getOffHandStack();
        }
    }

    static boolean isMainHand(int hand) {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && client.player.getMainArm() == (hand == 1 ? Arm.RIGHT : Arm.LEFT);
    }

    static NumberSetting xSetting(int hand) {
        return hand == 1 ? rightX : leftX;
    }

    static NumberSetting ySetting(int hand) {
        return hand == 1 ? rightY : leftY;
    }

    static NumberSetting zSetting(int hand) {
        return hand == 1 ? rightZ : leftZ;
    }

    static NumberSetting scaleSetting(int hand) {
        return hand == 1 ? rightScale : leftScale;
    }

    static void resetEditorState() {
        draggingHand = 0;
        hoveredHand = 0;
        outlinedHand = 0;
        outlineOpacity = 0.0F;
        outlineExpansion = 0.0F;
        lastOutlineNanos = 0L;
        lastSmoothNanos = 0L;
        snapRenderedTransforms();
    }
}
