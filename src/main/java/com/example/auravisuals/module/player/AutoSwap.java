package com.example.auravisuals.module.player;

import com.example.auravisuals.client.gui.hud.Notifications;
import com.example.auravisuals.client.gui.render.RoundedRenderer;
import com.example.auravisuals.compat.Gui;
import com.example.auravisuals.mixin.accessor.HandledScreenInvoker;
import com.example.auravisuals.mixin.accessor.InputAccessor;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.Hud;
import com.example.auravisuals.setting.BindSetting;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.util.item.TotemUtil;
import com.example.auravisuals.util.language.LanguageManager;
import com.example.auravisuals.util.policy.HolyServerPolicy;
import com.example.auravisuals.util.render.MirageGlassPipeline;
import com.example.auravisuals.util.render.RenderUtil;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class AutoSwap extends Module {
    private static final int PACKET_SWAP_DELAY_TICKS = 4;
    private static final float TRIPLE_WHEEL_OUTER_RADIUS = 112.0F;
    private static final float TRIPLE_WHEEL_INNER_RADIUS = 38.0F;
    private static final float TRIPLE_WHEEL_ICON_RADIUS = 75.0F;
    private static final String MODE_DOUBLE = "Двойной";
    private static final String MODE_TRIPLE = "Тройной";
    private final ModeSetting swapType = new ModeSetting("Режим", "Двойной", "Двойной", "Тройной");
    private final ModeSetting item1 = new ModeSetting("Предмет 1", "Тотем", () -> this.swapType.is("Двойной"), "Тотем", "Щит", "Яблоко", "Сфера");
    private final ModeSetting item2 = new ModeSetting("Предмет 2", "Щит", () -> this.swapType.is("Двойной"), "Тотем", "Щит", "Яблоко", "Сфера");
    private final BooleanSetting priorityEnchanted = new BooleanSetting("Только зачар. тотемы", false, () -> this.swapType.is("Двойной"));
    private final BindSetting swapKey = new BindSetting("Кнопка свапа", -1);
    private boolean wasPressed = false;
    private int pendingSwapSlot = -1;
    private int pendingSwapTicks = -1;
    private final ItemStack[] tripleSlots = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private int pendingPickSlotIndex = -1;
    private boolean wasPickInventoryOpen = false;
    private boolean tripleKeyWasDown = false;
    private boolean wasLeftMouseDown = false;
    private boolean wasRightMouseDown = false;
    private int tripleScaledPtrX;
    private int tripleScaledPtrY;
    private boolean unlockedCursorForTripleOverlay = false;
    private boolean triplePickInventoryClosing = false;
    private boolean tripleSetupActive = false;
    private boolean tripleSuppressSwapUntilRelease = false;
    private int tripleLastHudHoveredSlot = -1;
    private final float[] tripleSlotHover = new float[3];
    private float tripleWheelAnim;
    private long tripleLastAnimMs;
    private boolean tripleWheelOpen;
    private boolean tripleWheelClosing;

    public AutoSwap() {
        super("AutoSwap", "Свапает предметы", Category.PLAYER);
        this.addSettings(this.swapType, this.item1, this.item2, this.priorityEnchanted, this.swapKey);
    }

    public boolean shouldHideAutoSwapInventoryScreen() {
        return false;
    }

    public boolean shouldSuppressPacketInventoryMovement() {
        return this.pendingSwapSlot >= 0;
    }

    public boolean shouldKeepCursorLockedForHiddenAutoSwap() {
        return false;
    }

    @Override
    public void onDisable() {
        this.cancelPendingSwap();
        this.forceReleaseTripleOverlayCursor();
    }

    private Slot findPlayerInventorySlot(ScreenHandler handler, int inventoryIndex) {
        for (Slot s : handler.slots) {
            if (s.inventory == this.mc.player.getInventory() && s.getIndex() == inventoryIndex) {
                return s;
            }
        }

        return null;
    }

    private boolean beginLegitSwap(int slot) {
        if (this.mc.player == null || this.mc.interactionManager == null || this.mc.getNetworkHandler() == null) {
            return false;
        }

        if (slot < 0 || slot > 35) {
            return false;
        }

        if (this.pendingSwapSlot >= 0) {
            return false;
        }

        if (this.findPlayerInventorySlot(this.mc.player.playerScreenHandler, slot) == null) {
            return false;
        }

        if (slot < 9) {
            return this.performInstantHotbarSwap(slot);
        }

        this.pendingSwapSlot = slot;
        this.pendingSwapTicks = 4;
        this.mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(this.mc.player, Mode.OPEN_INVENTORY));
        this.stopPlayerMovementInput();
        return true;
    }

    private boolean performInstantHotbarSwap(int hotbarSlot) {
        if (this.mc.player != null && this.mc.getNetworkHandler() != null) {
            int previousSlot = this.mc.player.getInventory().getSelectedSlot();
            this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
            this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
            this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
            return true;
        } else {
            return false;
        }
    }

    private void tickPendingSwap() {
        if (this.pendingSwapSlot >= 0) {
            if (this.mc.player != null && this.mc.interactionManager != null && this.mc.getNetworkHandler() != null) {
                this.stopPlayerMovementInput();
                if (--this.pendingSwapTicks <= 0) {
                    PlayerScreenHandler handler = this.mc.player.playerScreenHandler;
                    int slot = this.pendingSwapSlot;
                    Slot targetSlot = this.findPlayerInventorySlot(handler, slot);
                    if (targetSlot != null) {
                        if (slot < 9) {
                            this.mc.interactionManager.clickSlot(handler.syncId, 45, slot, SlotActionType.SWAP, this.mc.player);
                        } else {
                            this.mc.interactionManager.clickSlot(handler.syncId, targetSlot.id, 40, SlotActionType.SWAP, this.mc.player);
                        }

                        this.mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(handler.syncId));
                        this.cancelPendingSwap();
                        this.mc.player.input.tick();
                    } else {
                        this.cancelPendingSwap();
                        this.mc.player.input.tick();
                    }
                }
            } else {
                this.cancelPendingSwap();
            }
        }
    }

    private void cancelPendingSwap() {
        this.pendingSwapSlot = -1;
        this.pendingSwapTicks = -1;
    }

    private void stopPlayerMovementInput() {
        if (this.mc.player != null && this.mc.player.input != null) {
            InputAccessor input = (InputAccessor)this.mc.player.input;
            input.auravisuals$setPlayerInput(new PlayerInput(false, false, false, false, false, false, false));
            input.auravisuals$setMovementVector(Vec2f.ZERO);
        }
    }

    private void forceReleaseTripleOverlayCursor() {
        this.triplePickInventoryClosing = false;
        this.tripleSetupActive = false;
        this.tripleSuppressSwapUntilRelease = false;
        this.tripleWheelAnim = 0.0F;
        this.tripleWheelOpen = false;
        this.tripleWheelClosing = false;
        this.tripleLastAnimMs = 0L;
        if (this.unlockedCursorForTripleOverlay && this.mc != null && this.mc.mouse != null) {
            this.mc.mouse.lockCursor();
            this.unlockedCursorForTripleOverlay = false;
        }
    }

    private void syncTripleOverlayCursor() {
        if (!this.isEnabled() || this.mc == null || this.mc.mouse == null || this.mc.player == null) {
            this.forceReleaseTripleOverlayCursor();
        } else if (!this.isTripleMode()) {
            this.forceReleaseTripleOverlayCursor();
        } else if (this.swapKey.getValue() == -1) {
            this.forceReleaseTripleOverlayCursor();
        } else if (this.mc.currentScreen instanceof ChatScreen) {
            if (this.unlockedCursorForTripleOverlay) {
                this.mc.mouse.lockCursor();
                this.unlockedCursorForTripleOverlay = false;
            }
        } else {
            boolean needCursor = this.tripleOverlayNeedsFreeCursor();
            if (needCursor) {
                if (!this.unlockedCursorForTripleOverlay) {
                    this.mc.mouse.unlockCursor();
                    this.unlockedCursorForTripleOverlay = true;
                }
            } else if (this.unlockedCursorForTripleOverlay) {
                this.mc.mouse.lockCursor();
                this.unlockedCursorForTripleOverlay = false;
            }
        }
    }

    private boolean tripleOverlayNeedsFreeCursor() {
        boolean invPick = this.mc.currentScreen instanceof InventoryScreen && (this.pendingPickSlotIndex >= 0 || this.triplePickInventoryClosing);
        boolean configUi = this.tripleSetupActive && !this.tripleSlotsFilled() && this.isSwapKeyDown();
        boolean swapUi = this.tripleSlotsFilled() && this.isSwapKeyDown();
        return invPick || configUi || swapUi;
    }

    private void reassertTripleOverlayCursorUnlockedIfNeeded() {
        if (this.isEnabled() && this.mc != null && this.mc.mouse != null && this.mc.player != null) {
            if (this.isTripleMode() && this.swapKey.getValue() != -1) {
                if (!(this.mc.currentScreen instanceof ChatScreen)) {
                    if (this.tripleOverlayNeedsFreeCursor()) {
                        if (this.mc.mouse.isCursorLocked()) {
                            this.mc.mouse.unlockCursor();
                        }

                        this.unlockedCursorForTripleOverlay = true;
                    }
                }
            }
        }
    }

    public boolean isTripleMode() {
        return this.swapType.is("Тройной");
    }

    public boolean isPickingTripleSlot() {
        return this.pendingPickSlotIndex >= 0;
    }

    public boolean shouldSuppressCursorLockWhileTripleOverlay() {
        if (!this.isEnabled() || !this.isTripleMode() || this.swapKey.getValue() == -1 || this.mc == null || this.mc.player == null) {
            return false;
        } else if (!this.tripleOverlayNeedsFreeCursor()) {
            return false;
        } else if (this.mc.currentScreen instanceof ChatScreen) {
            return false;
        } else {
            return this.mc.currentScreen == null
                ? true
                : this.mc.currentScreen instanceof InventoryScreen && (this.pendingPickSlotIndex >= 0 || this.triplePickInventoryClosing);
        }
    }

    public boolean shouldConsumeTripleOverlayMouseButton(int button, int action) {
        if (!this.isEnabled() || !this.isTripleMode() || this.mc == null || this.mc.player == null || this.mc.currentScreen != null) {
            return false;
        } else {
            return button != 0 && button != 1 ? false : this.tripleWheelOpen && this.isSwapKeyDown();
        }
    }

    @Nullable
    public JsonObject serializeTripleSlot(int index) {
        if (index >= 0 && index <= 2) {
            ItemStack s = this.tripleSlots[index];
            if (s.isEmpty()) {
                return null;
            }

            JsonObject o = new JsonObject();
            o.addProperty("id", Registries.ITEM.getId(s.getItem()).toString());
            o.addProperty("count", Math.min(s.getMaxCount(), Math.max(1, s.getCount())));
            return o;
        } else {
            return null;
        }
    }

    public void deserializeTripleSlot(int index, JsonObject o) {
        if (index >= 0 && index <= 2) {
            if (o != null && o.has("id")) {
                Identifier id = Identifier.tryParse(o.get("id").getAsString());
                if (id == null) {
                    this.tripleSlots[index] = ItemStack.EMPTY;
                } else {
                    Item item = Registries.ITEM.get(id);
                    if (item == Items.AIR) {
                        this.tripleSlots[index] = ItemStack.EMPTY;
                    } else {
                        int c = o.has("count") ? o.get("count").getAsInt() : 1;
                        this.tripleSlots[index] = new ItemStack(item, Math.min(item.getMaxCount(), Math.max(1, c)));
                    }
                }
            } else {
                this.tripleSlots[index] = ItemStack.EMPTY;
            }
        }
    }

    public void clearTripleSlot(int index) {
        if (index >= 0 && index < 3) {
            this.tripleSlots[index] = ItemStack.EMPTY;
        }
    }

    public boolean tryConsumeTriplePickClick(HandledScreen<?> screen, double mouseX, double mouseY, int button) {
        if (!this.isEnabled() || HolyServerPolicy.isAutoSwapBlockedOnCurrentServer() || !this.isTripleMode() || this.pendingPickSlotIndex < 0) {
            return false;
        }

        if (button != 0) {
            return false;
        }

        if (!(screen.getScreenHandler() instanceof PlayerScreenHandler)) {
            return false;
        }

        Slot slot = ((HandledScreenInvoker)screen).auravisuals$getSlotAt(mouseX, mouseY);
        if (slot != null && slot.hasStack()) {
            ItemStack copy = slot.getStack().copy();
            copy.setCount(Math.min(copy.getMaxCount(), Math.max(1, copy.getCount())));
            this.tripleSlots[this.pendingPickSlotIndex] = copy;
            this.pendingPickSlotIndex = -1;
            this.wasPickInventoryOpen = false;
            this.triplePickInventoryClosing = true;
            this.tripleSuppressSwapUntilRelease = true;
            this.tripleSetupActive = !this.tripleSlotsFilled();
            if (this.mc.player != null) {
                this.mc.player.closeHandledScreen();
            }

            this.syncTripleMouseButtonStateFromGlfw();
            this.syncTripleOverlayCursor();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void toggle() {
        if (!HolyServerPolicy.isAutoSwapBlockedOnCurrentServer() || this.isEnabled()) {
            super.toggle();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!HolyServerPolicy.isAutoSwapBlockedOnCurrentServer() || !enabled) {
            super.setEnabled(enabled);
        }
    }

    private void performDoubleSwap() {
        if (this.mc.player != null) {
            ItemStack currentOffhand = this.mc.player.getOffHandStack();
            String targetName = this.item1.getValue();
            if (this.isDoubleItem(currentOffhand, this.item1.getValue())) {
                targetName = this.item2.getValue();
            }

            int slot;
            if ((slot = this.findDoubleItemSlot(targetName)) != -1) {
                boolean started = this.beginLegitSwap(slot);
                if (started) {
                    Notifications.postMessage(Hud.text("Свапнул на: ", "Swapped to: ") + notificationItemName(targetName));
                } else {
                    Notifications.postLocalized("Свап не выполнен", "Swap failed");
                }
            } else {
                Notifications.postMessage(Hud.text("Предмет ", "Item ") + notificationItemName(targetName) + Hud.text(" не найден!", " not found!"));
            }
        }
    }

    private static String notificationItemName(String name) {
        String repaired = LanguageManager.repairIrcMojibake(name == null ? "" : name);
        if (!Hud.isEnglish()) {
            return repaired;
        }

        return switch (repaired) {
            case "Тотем" -> "Totem";
            case "Щит" -> "Shield";
            case "Яблоко" -> "Golden Apple";
            case "Сфера" -> "Sphere";
            default -> repaired;
        };
    }

    private boolean isDoubleItem(ItemStack stack, String name) {
        if (stack.isEmpty()) {
            return false;
        }

        switch (name) {
            case "Тотем":
                if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                    if (this.priorityEnchanted.getValue()) {
                        return TotemUtil.isEnchantedTotem(stack);
                    }

                    return true;
                }

                return false;
            case "Щит":
                return stack.getItem() == Items.SHIELD;
            case "Яблоко":
                return stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
            case "Сфера":
                if (stack.getItem() == Items.PLAYER_HEAD) {
                    return true;
                }

                String stackName = stack.getName().getString().toLowerCase();
                return stackName.contains("сфер") || stackName.contains("шар") || stackName.contains("head");
            default:
                return false;
        }
    }

    private int findDoubleItemSlot(String name) {
        if (this.mc.player == null) {
            return -1;
        }

        for (int i = 0; i < 36; i++) {
            ItemStack stack = this.mc.player.getInventory().getStack(i);
            if (this.isDoubleItem(stack, name)) {
                return i;
            }
        }

        return -1;
    }

    private boolean swapItem(int slot) {
        return this.beginLegitSwap(slot);
    }

    private int findSlotForTripleTemplate(ItemStack template) {
        if (!template.isEmpty() && this.mc.player != null) {
            for (int i = 0; i < 36; i++) {
                ItemStack inv = this.mc.player.getInventory().getStack(i);
                if (!inv.isEmpty() && ItemStack.areItemsAndComponentsEqual(inv, template)) {
                    return i;
                }
            }

            for (int var5 = 0; var5 < 36; var5++) {
                ItemStack inv = this.mc.player.getInventory().getStack(var5);
                if (!inv.isEmpty() && inv.getItem() == template.getItem()) {
                    return var5;
                }
            }

            return -1;
        } else {
            return -1;
        }
    }

    private void performTripleSwap(int choiceIndex) {
        if (choiceIndex >= 0 && choiceIndex <= 2) {
            ItemStack template = this.tripleSlots[choiceIndex];
            if (!template.isEmpty()) {
                int slot = this.findSlotForTripleTemplate(template);
                if (slot != -1) {
                    boolean started = this.swapItem(slot);
                    if (started) {
                        Notifications.postMessage(
                            Hud.text("Свапнул на: ", "Swapped to: ") + LanguageManager.translateHudCooldownItemName(template.getItem(), Hud.isEnglish())
                        );
                    } else {
                        Notifications.postLocalized("Свап не выполнен", "Swap failed");
                    }
                } else {
                    Notifications.postLocalized("Предмет не найден в инвентаре!", "Item was not found in the inventory!");
                }
            }
        }
    }

    private boolean tripleSlotsFilled() {
        return !this.tripleSlots[0].isEmpty() && !this.tripleSlots[1].isEmpty() && !this.tripleSlots[2].isEmpty();
    }

    private boolean isSwapKeyDown() {
        int v = this.swapKey.getValue();
        if (v == -1) {
            return false;
        } else {
            return BindSetting.shouldBlockKeyInput() ? false : BindSetting.isPressed(this.mc.getWindow().getHandle(), v);
        }
    }

    private void syncTripleMouseButtonStateFromGlfw() {
        if (this.mc.getWindow() != null) {
            long win = this.mc.getWindow().getHandle();
            this.wasLeftMouseDown = GLFW.glfwGetMouseButton(win, 0) == 1;
            this.wasRightMouseDown = GLFW.glfwGetMouseButton(win, 1) == 1;
        }
    }

    private void updatePickInventoryState() {
        if (this.mc.currentScreen == null) {
            this.triplePickInventoryClosing = false;
        }

        if (this.pendingPickSlotIndex >= 0) {
            if (this.mc.currentScreen instanceof InventoryScreen) {
                this.wasPickInventoryOpen = true;
            } else if (this.wasPickInventoryOpen && this.mc.currentScreen == null) {
                this.wasPickInventoryOpen = false;
                this.pendingPickSlotIndex = -1;
                this.tripleSetupActive = false;
                this.syncTripleMouseButtonStateFromGlfw();
            }
        } else if (!(this.mc.currentScreen instanceof InventoryScreen)) {
            this.wasPickInventoryOpen = false;
        }
    }

    private int hitTestTripleWheel(int mouseX, int mouseY) {
        int sw = this.mc.getWindow().getScaledWidth();
        int sh = this.mc.getWindow().getScaledHeight();
        if (sw > 0 && sh > 0) {
            double dx = mouseX - sw * 0.5;
            double dy = mouseY - sh * 0.5;
            if (Math.hypot(dx, dy) < 38.0) {
                return -1;
            }

            double angle = Math.toDegrees(Math.atan2(dy, dx));
            double shifted = (angle + 150.0) % 360.0;
            if (shifted < 0.0) {
                shifted += 360.0;
            }

            return MathHelper.clamp((int)Math.floor(shifted / 120.0), 0, 2);
        } else {
            return -1;
        }
    }

    private void updateTripleScaledPointerFromMouse() {
        if (this.mc.getWindow() == null) {
            this.tripleScaledPtrX = 0;
            this.tripleScaledPtrY = 0;
        } else {
            int sw = this.mc.getWindow().getScaledWidth();
            int sh = this.mc.getWindow().getScaledHeight();
            int windowWidth = this.mc.getWindow().getWidth();
            int windowHeight = this.mc.getWindow().getHeight();
            if (windowWidth > 0 && windowHeight > 0) {
                this.tripleScaledPtrX = (int)Math.round(this.mc.mouse.getX() * sw / windowWidth);
                this.tripleScaledPtrY = (int)Math.round(this.mc.mouse.getY() * sh / windowHeight);
            } else {
                this.tripleScaledPtrX = 0;
                this.tripleScaledPtrY = 0;
            }
        }
    }

    private int hoveredTripleChoiceIndex() {
        if (this.tripleSlotsFilled() && this.mc.currentScreen == null) {
            this.updateTripleScaledPointerFromMouse();
            return this.hitTestTripleWheel(this.tripleScaledPtrX, this.tripleScaledPtrY);
        } else {
            return -1;
        }
    }

    private int hoveredTripleCellIndex() {
        if (this.mc.currentScreen != null) {
            return -1;
        }

        this.updateTripleScaledPointerFromMouse();
        return this.hitTestTripleWheel(this.tripleScaledPtrX, this.tripleScaledPtrY);
    }

    private int tripleHitForOverlayClick(int mx, int my) {
        return !this.mc.options.hudHidden && this.tripleLastHudHoveredSlot >= 0 && this.tripleLastHudHoveredSlot <= 2
            ? this.tripleLastHudHoveredSlot
            : this.hitTestTripleWheel(mx, my);
    }

    private void handleTripleOverlayMouse() {
        if (this.mc.currentScreen == null) {
            this.updateTripleScaledPointerFromMouse();
            int mx = this.tripleScaledPtrX;
            int my = this.tripleScaledPtrY;
            long win = this.mc.getWindow().getHandle();
            boolean leftDown = GLFW.glfwGetMouseButton(win, 0) == 1;
            boolean rightDown = GLFW.glfwGetMouseButton(win, 1) == 1;
            int hit;
            if (rightDown && !this.wasRightMouseDown && (hit = this.tripleHitForOverlayClick(mx, my)) >= 0 && !this.tripleSlots[hit].isEmpty()) {
                this.tripleSlots[hit] = ItemStack.EMPTY;
                this.tripleSetupActive = true;
                this.tripleSuppressSwapUntilRelease = true;
                this.wasRightMouseDown = false;
                this.wasLeftMouseDown = leftDown;
            } else if (leftDown && !this.wasLeftMouseDown && (hit = this.tripleHitForOverlayClick(mx, my)) >= 0) {
                this.pendingPickSlotIndex = hit;
                this.wasPickInventoryOpen = false;
                this.tripleSetupActive = true;
                this.tripleSuppressSwapUntilRelease = true;
                this.tripleWheelOpen = false;
                this.tripleWheelClosing = false;
                this.tripleWheelAnim = 0.0F;
                this.mc.setScreen(new InventoryScreen(this.mc.player));
                this.wasLeftMouseDown = false;
                this.wasRightMouseDown = rightDown;
            } else {
                this.wasLeftMouseDown = leftDown;
                this.wasRightMouseDown = rightDown;
            }
        }
    }

    @Override
    public void onTick() {
        if (HolyServerPolicy.isAutoSwapBlockedOnCurrentServer()) {
            if (this.isEnabled()) {
                this.setEnabledSilently(false);
                this.onDisable();
            } else {
                this.forceReleaseTripleOverlayCursor();
            }
        } else {
            try {
                if (this.mc.player == null) {
                    return;
                }

                boolean packetSwapWasActive = this.pendingSwapSlot >= 0;
                this.tickPendingSwap();
                if (packetSwapWasActive) {
                    return;
                }

                if (this.mc.currentScreen instanceof ChatScreen) {
                    return;
                }

                this.updatePickInventoryState();
                if (this.isTripleMode()) {
                    if (this.swapKey.getValue() != -1) {
                        boolean keyDown = this.isSwapKeyDown();
                        if (!keyDown && !this.tripleKeyWasDown) {
                            this.tripleSuppressSwapUntilRelease = false;
                        }

                        if (keyDown && !this.tripleKeyWasDown && this.mc.currentScreen == null) {
                            if (this.tripleWheelAnim < 0.002F) {
                                this.tripleWheelAnim = 0.0F;
                            }

                            this.tripleWheelOpen = true;
                            this.tripleWheelClosing = false;
                            this.tripleLastAnimMs = System.currentTimeMillis();
                            this.mc.mouse.unlockCursor();
                            this.unlockedCursorForTripleOverlay = true;
                            GLFW.glfwSetCursorPos(this.mc.getWindow().getHandle(), this.mc.getWindow().getWidth() * 0.5, this.mc.getWindow().getHeight() * 0.5);
                            this.syncTripleMouseButtonStateFromGlfw();
                        }

                        if (this.tripleSlotsFilled()) {
                            this.tripleSetupActive = false;
                        } else if (keyDown && !this.tripleKeyWasDown) {
                            this.tripleSetupActive = true;
                        }

                        boolean overlayInteractive = keyDown && (this.tripleSetupActive && !this.tripleSlotsFilled() || this.tripleSlotsFilled());
                        if (overlayInteractive) {
                            this.syncTripleOverlayCursor();
                            this.handleTripleOverlayMouse();
                        } else {
                            this.wasLeftMouseDown = false;
                            this.wasRightMouseDown = false;
                        }

                        if (!keyDown && this.tripleKeyWasDown && this.mc.currentScreen == null) {
                            int h;
                            if (!this.tripleSuppressSwapUntilRelease && this.tripleSlotsFilled() && (h = this.hoveredTripleChoiceIndex()) >= 0) {
                                this.performTripleSwap(h);
                            }

                            this.tripleSuppressSwapUntilRelease = false;
                            this.tripleWheelOpen = false;
                            this.tripleWheelClosing = this.tripleWheelAnim > 0.002F;
                        }

                        this.tripleKeyWasDown = keyDown;
                        return;
                    }

                    this.tripleKeyWasDown = false;
                    this.wasLeftMouseDown = false;
                    this.wasRightMouseDown = false;
                    return;
                }

                this.wasLeftMouseDown = false;
                this.wasRightMouseDown = false;
                this.tripleKeyWasDown = false;
                if (this.swapKey.getValue() != -1) {
                    boolean isPressed = BindSetting.isPressed(this.mc.getWindow().getHandle(), this.swapKey.getValue());
                    if (isPressed && !this.wasPressed && !BindSetting.shouldBlockKeyInput()) {
                        this.performDoubleSwap();
                    }

                    this.wasPressed = isPressed;
                    return;
                }

                this.wasPressed = false;
            } finally {
                this.syncTripleOverlayCursor();
            }
        }
    }

    @Override
    public void onHudRender(DrawContext context) {
        this.tripleLastHudHoveredSlot = -1;
        if (this.isEnabled() && this.isTripleMode() && this.swapKey.getValue() != -1 && this.mc.player != null) {
            if (!(this.mc.currentScreen instanceof ChatScreen)) {
                if (this.mc.currentScreen == null && !this.triplePickInventoryClosing) {
                    boolean keyDown = this.isSwapKeyDown();
                    if (this.tripleWheelOpen && keyDown) {
                        this.reassertTripleOverlayCursorUnlockedIfNeeded();
                    }

                    boolean showConfig = this.tripleSetupActive && !this.tripleSlotsFilled();
                    boolean showSwap = this.tripleSlotsFilled();
                    boolean opening = this.tripleWheelOpen && keyDown && (showConfig || showSwap);
                    if (opening || this.tripleWheelClosing || !(this.tripleWheelAnim <= 0.002F)) {
                        long now = System.currentTimeMillis();
                        float dt = this.tripleLastAnimMs == 0L ? 0.016F : MathHelper.clamp((float)(now - this.tripleLastAnimMs) / 1000.0F, 0.001F, 0.05F);
                        this.tripleLastAnimMs = now;
                        this.tripleWheelAnim = approach(this.tripleWheelAnim, opening ? 1.0F : 0.0F, dt, opening ? 11.0F : 14.0F);
                        if (!opening && this.tripleWheelAnim <= 0.002F) {
                            this.tripleWheelAnim = 0.0F;
                            this.tripleWheelClosing = false;
                            this.tripleLastAnimMs = 0L;
                        } else {
                            float rawAnim = MathHelper.clamp(this.tripleWheelAnim, 0.0F, 1.0F);
                            float anim = opening ? easeOutCubic(rawAnim) : smoothStep(rawAnim);
                            float scale = opening ? 0.74F + 0.26F * anim : 0.08F + 0.92F * anim;
                            float visualAlpha = anim;
                            int sw = context.getScaledWindowWidth();
                            int sh = context.getScaledWindowHeight();
                            float cx = sw * 0.5F;
                            float cy = sh * 0.5F;
                            float outer = 112.0F * scale;
                            float inner = 38.0F * scale;
                            float iconRing = 75.0F * scale;
                            int hovered;
                            this.tripleLastHudHoveredSlot = hovered = opening
                                ? (showSwap ? this.hoveredTripleChoiceIndex() : this.hoveredTripleCellIndex())
                                : -1;
                            context.fill(0, 0, sw, sh, withAlpha(0, Math.round(72.0F * visualAlpha)));
                            MirageGlassPipeline.captureFrame();
                            MirageGlassPipeline.drawBlurAt(context, cx - outer, cy - outer, outer * 2.0F, outer * 2.0F, outer, 20.0F, visualAlpha * 0.9F);

                            for (int i = 0; i < 3; i++) {
                                float target = hovered == i ? 1.0F : 0.0F;
                                this.tripleSlotHover[i] = approach(this.tripleSlotHover[i], target, dt, 15.0F);
                                float start = -150.0F + i * 120.0F;
                                double centerAngle = Math.toRadians(-90.0 + i * 120.0);
                                int sectorColor = lerpColor(592397, 2237739, this.tripleSlotHover[i]);
                                RoundedRenderer.drawArc(
                                    context,
                                    cx,
                                    cy,
                                    inner,
                                    outer,
                                    start,
                                    120.0F,
                                    withAlpha(sectorColor, Math.round((228.0F + 15.0F * this.tripleSlotHover[i]) * visualAlpha))
                                );
                                double edgeAngle = Math.toRadians(start);
                                drawLine(
                                    context,
                                    cx + (float)Math.cos(edgeAngle) * (inner + 1.0F),
                                    cy + (float)Math.sin(edgeAngle) * (inner + 1.0F),
                                    cx + (float)Math.cos(edgeAngle) * (outer - 1.0F),
                                    cy + (float)Math.sin(edgeAngle) * (outer - 1.0F),
                                    0.72F,
                                    withAlpha(6909043, Math.round(78.0F * visualAlpha))
                                );
                                int iconX = Math.round(cx + (float)Math.cos(centerAngle) * iconRing);
                                int iconY = Math.round(cy + (float)Math.sin(centerAngle) * iconRing);
                                ItemStack stack = this.tripleSlots[i];
                                if (stack.isEmpty()) {
                                    float plusHalf = (7.0F + this.tripleSlotHover[i] * 1.5F) * scale;
                                    int plusColor = withAlpha(15001065, Math.round(235.0F * visualAlpha));
                                    drawLine(context, iconX - plusHalf, iconY, iconX + plusHalf, iconY, 1.65F, plusColor);
                                    drawLine(context, iconX, iconY - plusHalf, iconX, iconY + plusHalf, 1.65F, plusColor);
                                } else {
                                    float iconVisibility = opening ? 1.0F : smoothStep(rawAnim);
                                    float itemScale = (1.65F + this.tripleSlotHover[i] * 0.16F) * scale * iconVisibility;
                                    float drawn = 16.0F * itemScale;
                                    context.getMatrices().pushMatrix();
                                    context.getMatrices().translate(iconX - drawn * 0.5F, iconY - drawn * 0.5F);
                                    context.getMatrices().scale(itemScale, itemScale);
                                    context.drawItem(stack, 0, 0);
                                    context.drawStackOverlay(this.mc.textRenderer, stack, 0, 0);
                                    context.getMatrices().popMatrix();
                                }
                            }

                            RenderUtil.drawCircle(context, cx, cy, inner - 1.0F, withAlpha(395018, Math.round(247.0F * visualAlpha)));
                            RoundedRenderer.drawArc(
                                context, cx, cy, inner - 1.65F, inner - 0.35F, 0.0F, 360.0F, withAlpha(6448752, Math.round(92.0F * visualAlpha))
                            );
                            float crossHalf = 5.5F * scale;
                            int crossColor = withAlpha(hovered < 0 ? 14211550 : 7830147, Math.round(215.0F * visualAlpha));
                            drawLine(context, cx - crossHalf, cy - crossHalf, cx + crossHalf, cy + crossHalf, 1.35F, crossColor);
                            drawLine(context, cx + crossHalf, cy - crossHalf, cx - crossHalf, cy + crossHalf, 1.35F, crossColor);
                        }
                    }
                } else {
                    this.tripleWheelOpen = false;
                    this.tripleWheelClosing = false;
                    this.tripleWheelAnim = 0.0F;
                    this.tripleLastAnimMs = 0L;
                }
            }
        }
    }

    private static void drawLine(DrawContext context, float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = Math.max(0.001F, (float)Math.sqrt(dx * dx + dy * dy));
        float nx = -dy / length * thickness * 0.5F;
        float ny = dx / length * thickness * 0.5F;
        Gui.color(context).quad(x1 + nx, y1 + ny, x2 + nx, y2 + ny, x2 - nx, y2 - ny, x1 - nx, y1 - ny, color).submit(context);
    }

    private static float approach(float current, float target, float dt, float speed) {
        float factor = 1.0F - (float)Math.exp(-speed * dt);
        return current + (target - current) * factor;
    }

    private static float easeOutCubic(float value) {
        float inverse = 1.0F - value;
        return 1.0F - inverse * inverse * inverse;
    }

    private static float smoothStep(float value) {
        float t = MathHelper.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static int withAlpha(int rgb, int alpha) {
        return MathHelper.clamp(alpha, 0, 255) << 24 | rgb & 16777215;
    }

    private static int lerpColor(int from, int to, float progress) {
        float t = MathHelper.clamp(progress, 0.0F, 1.0F);
        int fr = from >> 16 & 0xFF;
        int fg = from >> 8 & 0xFF;
        int fb = from & 0xFF;
        int tr = to >> 16 & 0xFF;
        int tg = to >> 8 & 0xFF;
        int tb = to & 0xFF;
        int r = Math.round(fr + (tr - fr) * t);
        int g = Math.round(fg + (tg - fg) * t);
        int b = Math.round(fb + (tb - fb) * t);
        return r << 16 | g << 8 | b;
    }

    static {
        ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
            AutoSwap m = ModuleManager.INSTANCE.getModule(AutoSwap.class);
            if (m != null && m.isEnabled() && m.isTripleMode()) {
                m.reassertTripleOverlayCursorUnlockedIfNeeded();
            }
        });
    }
}
