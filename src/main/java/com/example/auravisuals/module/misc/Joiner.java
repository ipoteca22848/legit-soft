package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import java.util.Locale;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class Joiner extends Module {
    private static final long COMPASS_USE_DELAY_MS = 180L;
    private static final int[] REALLY_WORLD_GRIEF_SLOTS = new int[]{
        0, 1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15, 16, 17, 18, 19, 20, 21, 23, 24, 25, 26, 27, 28, 29, 30, 32, 33, 34, 35
    };
    public static final ModeSetting serverMode = new ModeSetting("Режим", "SpookyTime", "SpookyTime", "ReallyWorld");
    public static final ModeSetting spookyMode = new ModeSetting("SpookyTime", "Дуели", () -> serverMode.is("SpookyTime"), "Дуели");
    public static final ModeSetting reallyWorldMode = new ModeSetting("ReallyWorld", "Grief 1.16.5", () -> serverMode.is("ReallyWorld"), "Grief 1.16.5");
    public static final NumberSetting reallyWorldGrief = new NumberSetting(
        "Гриф", 1.0, 1.0, 76.0, 1.0, () -> serverMode.is("ReallyWorld") && reallyWorldMode.is("Grief 1.16.5")
    );
    private long lastUseMs = 0L;
    private long lastClickMs = 0L;
    private long lastReallyWorldJoinMs = 0L;
    private long pendingCompassUseMs = 0L;
    private int pendingCompassHotbarSlot = -1;
    private boolean clickedCurrentContainer = false;

    public Joiner() {
        super("Joiner", "Автоматически заходит на режимы", Category.MISC);
        this.addSettings(serverMode, spookyMode, reallyWorldMode, reallyWorldGrief);
    }

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.world != null && this.mc.interactionManager != null) {
            long now = System.currentTimeMillis();
            if (!this.tryUsePendingCompass(now)) {
                if (serverMode.is("ReallyWorld")) {
                    this.tickReallyWorld();
                } else if (serverMode.is("SpookyTime") && spookyMode.is("Дуели") && this.isSpookyTime()) {
                    if (this.mc.currentScreen instanceof HandledScreen screen) {
                        this.clearPendingCompassUse();
                        if (!this.clickedCurrentContainer && now - this.lastClickMs >= 650L) {
                            int slot = 14;
                            if (14 < screen.getScreenHandler().slots.size()) {
                                this.mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, slot, 0, SlotActionType.PICKUP, this.mc.player);
                                this.clickedCurrentContainer = true;
                                this.lastClickMs = now;
                            }
                        }
                    } else {
                        this.clickedCurrentContainer = false;
                        if (now - this.lastUseMs >= 900L) {
                            int compassSlot = this.findChooserCompass();
                            if (compassSlot >= 0) {
                                this.prepareCompassUse(compassSlot, now);
                                this.lastUseMs = now;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isSpookyTime() {
        ServerInfo entry = this.mc.getCurrentServerEntry();
        return entry != null && entry.address != null ? entry.address.toLowerCase(Locale.ROOT).contains("spookytime.net") : false;
    }

    private void tickReallyWorld() {
        if (reallyWorldMode.is("Grief 1.16.5") && this.isReallyWorld()) {
            long now = System.currentTimeMillis();
            if (this.mc.currentScreen instanceof HandledScreen screen) {
                this.clearPendingCompassUse();
                String title = screen.getTitle().getString();
                if (title.contains("Выбор мира грифа")) {
                    this.clickReallyWorldGrief(screen, now);
                } else if (!this.clickedCurrentContainer && now - this.lastClickMs >= 650L) {
                    this.clickContainerSlot(screen, 21);
                    this.clickedCurrentContainer = true;
                    this.lastClickMs = now;
                }
            } else {
                this.clickedCurrentContainer = false;
                if (now - this.lastUseMs >= 900L) {
                    int compassSlot = this.findChooserCompass();
                    if (compassSlot >= 0) {
                        this.prepareCompassUse(compassSlot, now);
                        this.lastUseMs = now;
                    }
                }
            }
        }
    }

    private void clickReallyWorldGrief(HandledScreen<?> screen, long now) {
        int grief = reallyWorldGrief.getIntValue();
        int targetPage = (grief - 1) / REALLY_WORLD_GRIEF_SLOTS.length + 1;
        int currentPage = this.parseCurrentGriefPage(screen.getTitle().getString());
        if (currentPage < targetPage) {
            if (now - this.lastClickMs >= 650L) {
                this.clickContainerSlot(screen, 44);
                this.lastClickMs = now;
            }
        } else if (currentPage > targetPage) {
            if (now - this.lastClickMs >= 650L) {
                this.clickContainerSlot(screen, 36);
                this.lastClickMs = now;
            }
        } else if (now - this.lastReallyWorldJoinMs >= 5000L) {
            int index = (grief - 1) % REALLY_WORLD_GRIEF_SLOTS.length;
            this.clickContainerSlot(screen, REALLY_WORLD_GRIEF_SLOTS[index]);
            this.lastReallyWorldJoinMs = now;
            this.lastClickMs = now;
        }
    }

    private int parseCurrentGriefPage(String title) {
        try {
            int slash = title.indexOf(47);
            if (slash > 0) {
                int start = slash - 1;

                while (start > 0 && Character.isDigit(title.charAt(start - 1))) {
                    start--;
                }

                return Integer.parseInt(title.substring(start, slash).trim());
            }
        } catch (Exception var4) {
        }

        return 1;
    }

    private void clickContainerSlot(HandledScreen<?> screen, int slot) {
        if (slot >= 0 && slot < screen.getScreenHandler().slots.size()) {
            this.mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, slot, 0, SlotActionType.PICKUP, this.mc.player);
        }
    }

    private boolean isReallyWorld() {
        ServerInfo entry = this.mc.getCurrentServerEntry();
        return entry != null && entry.address != null ? entry.address.toLowerCase(Locale.ROOT).contains("reallyworld") : false;
    }

    private int findChooserCompass() {
        PlayerInventory inventory = this.mc.player.getInventory();

        for (int i = 0; i < inventory.getMainStacks().size(); i++) {
            ItemStack stack = inventory.getMainStacks().get(i);
            String name;
            if (!stack.isEmpty()
                && (name = stack.getName().getString()).contains("Выбор")
                && (stack.getItem() == Items.COMPASS || stack.getItem() == Items.RECOVERY_COMPASS || name.toLowerCase(Locale.ROOT).contains("выбор"))) {
                return i;
            }
        }

        return -1;
    }

    private boolean tryUsePendingCompass(long now) {
        if (this.pendingCompassUseMs <= 0L) {
            return false;
        }

        if (this.mc.currentScreen != null) {
            this.clearPendingCompassUse();
            return false;
        }

        if (now < this.pendingCompassUseMs) {
            return true;
        }

        int slot = this.pendingCompassHotbarSlot;
        this.clearPendingCompassUse();
        if (slot >= 0 && slot <= 8) {
            this.selectHotbarSlot(slot);
            ItemStack stack = this.mc.player.getInventory().getMainStacks().get(slot);
            if (!this.isChooserCompass(stack)) {
                int found = this.findChooserCompass();
                if (found < 0) {
                    return false;
                }

                this.prepareCompassUse(found, now);
                return true;
            } else {
                this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
                this.lastUseMs = now;
                return true;
            }
        } else {
            return false;
        }
    }

    private void prepareCompassUse(int compassSlot, long now) {
        int selected = this.mc.player.getInventory().getSelectedSlot();
        if (compassSlot >= 0 && compassSlot <= 8) {
            this.selectHotbarSlot(compassSlot);
            this.pendingCompassHotbarSlot = compassSlot;
        } else {
            this.mc
                .interactionManager
                .clickSlot(
                    this.mc.player.currentScreenHandler.syncId, this.inventoryIndexToHandlerSlot(compassSlot), selected, SlotActionType.SWAP, this.mc.player
                );
            this.selectHotbarSlot(selected);
            this.pendingCompassHotbarSlot = selected;
        }

        this.pendingCompassUseMs = now + 180L;
    }

    private boolean isChooserCompass(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            String name = stack.getName().getString();
            return name.contains("Выбор")
                && (stack.getItem() == Items.COMPASS || stack.getItem() == Items.RECOVERY_COMPASS || name.toLowerCase(Locale.ROOT).contains("выбор"));
        } else {
            return false;
        }
    }

    private void clearPendingCompassUse() {
        this.pendingCompassUseMs = 0L;
        this.pendingCompassHotbarSlot = -1;
    }

    private void selectHotbarSlot(int slot) {
        if (slot >= 0 && slot <= 8) {
            if (this.mc.player.getInventory().getSelectedSlot() != slot) {
                this.mc.player.getInventory().setSelectedSlot(slot);
                this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            }
        }
    }

    private int inventoryIndexToHandlerSlot(int inventoryIndex) {
        return inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
    }
}
