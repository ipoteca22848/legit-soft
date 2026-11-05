package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.render.RenderUtil;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class ChestSorter extends Module {
    final BooleanSetting autoSort = new BooleanSetting("Auto Sort", false);
    final ModeSetting order = new ModeSetting("Order", "Loadout", "Loadout", "Name", "Count");
    final NumberSetting delay = new NumberSetting("Delay", 2.0, 0.0, 10.0, 1.0);
    final Queue<ChestSorter$Swap> swaps = new ArrayDeque<>();
    int lastSyncId = -1;
    int delayTicks;

    public ChestSorter() {
        super("ChestSorter", "Sorts chest resources neatly", Category.MISC);
        this.addSettings(this.autoSort, this.order, this.delay);
    }

    @Override
    public void onEnable() {
        this.swaps.clear();
        this.lastSyncId = -1;
    }

    @Override
    public void onTick() {
        if (this.mc != null && this.mc.player != null && this.mc.interactionManager != null) {
            if (this.mc.currentScreen instanceof HandledScreen screen && !(this.mc.currentScreen instanceof InventoryScreen)) {
                ScreenHandler handler = screen.getScreenHandler();
                if (this.autoSort.getValue() && handler.syncId != this.lastSyncId && this.swaps.isEmpty()) {
                    this.requestSort();
                }

                if (!this.swaps.isEmpty()) {
                    if (this.delayTicks-- <= 0) {
                        this.delayTicks = this.delay.getIntValue();
                        this.runNextSwap(handler);
                    }
                }
            } else {
                this.swaps.clear();
                this.lastSyncId = -1;
            }
        }
    }

    public void requestSort() {
        this.swaps.clear();
        if (this.mc != null && this.mc.player != null) {
            if (this.mc.currentScreen instanceof HandledScreen screen && !(this.mc.currentScreen instanceof InventoryScreen)) {
                ScreenHandler handler = screen.getScreenHandler();
                if (handler.getCursorStack().isEmpty()) {
                    List<Slot> chestSlots = this.chestSlots(handler);
                    if (chestSlots.size() >= 2) {
                        this.lastSyncId = handler.syncId;
                        ArrayList<ItemStack> current = new ArrayList<>(chestSlots.size());

                        for (Slot slot : chestSlots) {
                            current.add(slot.getStack().copy());
                        }

                        ArrayList<ItemStack> sorted = new ArrayList<>(current);
                        sorted.sort(this.stackComparator());
                        boolean[] fixed = new boolean[current.size()];

                        for (int i = 0; i < current.size(); i++) {
                            if (!fixed[i] && !this.equivalentForSort(current.get(i), sorted.get(i))) {
                                int j = this.findMatching(current, sorted.get(i), fixed, i + 1);
                                if (j >= 0) {
                                    this.swaps.add(new ChestSorter$Swap(chestSlots.get(i).id, chestSlots.get(j).id));
                                    ItemStack tmp = current.get(i);
                                    current.set(i, current.get(j));
                                    current.set(j, tmp);
                                    fixed[i] = true;
                                }
                            } else {
                                fixed[i] = true;
                            }
                        }

                        this.delayTicks = 0;
                    }
                }
            }
        }
    }

    public static void renderSortButton(DrawContext context, HandledScreen<?> screen, int mouseX, int mouseY, int guiX, int guiY, int backgroundWidth) {
        ChestSorter module = ModuleManager.INSTANCE.getModule(ChestSorter.class);
        if (module != null && module.isEnabled() && module.canShowButton(screen)) {
            int bx = guiX + backgroundWidth - 24;
            int by = guiY + 5;
            boolean hover = mouseX >= bx && mouseX <= bx + 20 && mouseY >= by && mouseY <= by + 20;
            int fill = hover ? -399099064 : -400547028;
            int border = hover ? -10520321 : -12960182;
            RenderUtil.drawRoundedRect(context, bx, by, 20.0F, 20.0F, 4.0F, fill);
            RenderUtil.drawBorder(context, bx, by, 20, 20, border);
            context.drawItem(new ItemStack(Items.HOPPER), bx + 2, by + 2);
        }
    }

    public static boolean mouseClickedSortButton(HandledScreen<?> screen, double mouseX, double mouseY, int button, int guiX, int guiY, int backgroundWidth) {
        ChestSorter module = ModuleManager.INSTANCE.getModule(ChestSorter.class);
        if (button == 0 && module != null && module.isEnabled() && module.canShowButton(screen)) {
            int bx = guiX + backgroundWidth - 24;
            int by = guiY + 5;
            if (!(mouseX < bx) && !(mouseX > bx + 20) && !(mouseY < by) && !(mouseY > by + 20)) {
                module.requestSort();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    boolean canShowButton(HandledScreen<?> screen) {
        if (this.mc != null && this.mc.player != null && screen != null && !(screen instanceof InventoryScreen)) {
            ScreenHandler handler = screen.getScreenHandler();

            for (Slot slot : handler.slots) {
                if (slot.inventory != this.mc.player.getInventory()) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    void runNextSwap(ScreenHandler handler) {
        if (handler.getCursorStack() != null && !handler.getCursorStack().isEmpty()) {
            this.swaps.clear();
        } else {
            ChestSorter$Swap swap = this.swaps.poll();
            if (swap != null && swap.from != swap.to) {
                this.mc.interactionManager.clickSlot(handler.syncId, swap.from, 0, SlotActionType.PICKUP, this.mc.player);
                this.mc.interactionManager.clickSlot(handler.syncId, swap.to, 0, SlotActionType.PICKUP, this.mc.player);
                this.mc.interactionManager.clickSlot(handler.syncId, swap.from, 0, SlotActionType.PICKUP, this.mc.player);
            }
        }
    }

    List<Slot> chestSlots(ScreenHandler handler) {
        ArrayList<Slot> slots = new ArrayList<>();

        for (Slot slot : handler.slots) {
            if (slot.inventory != this.mc.player.getInventory()) {
                slots.add(slot);
            }
        }

        return slots;
    }

    Comparator<ItemStack> stackComparator() {
        return (a, b) -> {
            boolean ae = a.isEmpty();
            boolean be = b.isEmpty();
            if (!ae && !be) {
                int mode = switch (this.order.getValue().toLowerCase(Locale.ROOT)) {
                    case "name" -> this.compareName(a, b);
                    case "count" -> this.compareCount(a, b);
                    default -> this.compareLoadout(a, b);
                };
                if (mode != 0) {
                    return mode;
                }

                int count = Integer.compare(b.getCount(), a.getCount());
                return count != 0 ? count : this.compareName(a, b);
            } else {
                return ae == be ? 0 : (ae ? 1 : -1);
            }
        };
    }

    int compareLoadout(ItemStack a, ItemStack b) {
        int group = Integer.compare(this.group(a), this.group(b));
        if (group != 0) {
            return group;
        }

        int sub = Integer.compare(this.subGroup(a), this.subGroup(b));
        return sub != 0 ? sub : this.compareType(a, b);
    }

    int group(ItemStack stack) {
        if (stack.isEmpty()) {
            return 99;
        } else {
            String id = this.itemId(stack);
            if (this.isArmor(stack)) {
                return 0;
            } else if (this.isSword(stack, id)) {
                return 1;
            } else if (this.isTool(stack, id)) {
                return 2;
            } else if (this.isBuff(stack, id)) {
                return 3;
            } else if (this.isConsumable(stack, id)) {
                return 4;
            } else {
                return this.isBlockOrResource(id) ? 5 : 6;
            }
        }
    }

    int subGroup(ItemStack stack) {
        String id = this.itemId(stack);
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable != null) {
            EquipmentSlot slot = equippable.slot();
            if (slot == EquipmentSlot.HEAD) {
                return 0;
            }

            if (slot == EquipmentSlot.CHEST) {
                return 1;
            }

            if (slot == EquipmentSlot.LEGS) {
                return 2;
            }

            if (slot == EquipmentSlot.FEET) {
                return 3;
            }

            if (slot == EquipmentSlot.OFFHAND) {
                return 4;
            }
        }

        if (id.contains("netherite")) {
            return 0;
        } else if (id.contains("diamond")) {
            return 1;
        } else if (id.contains("iron")) {
            return 2;
        } else if (id.contains("gold")) {
            return 3;
        } else if (id.contains("stone")) {
            return 4;
        } else if (id.contains("wood")) {
            return 5;
        } else if (id.contains("pickaxe")) {
            return 0;
        } else if (id.contains("axe")) {
            return 1;
        } else if (id.contains("shovel")) {
            return 2;
        } else if (id.contains("hoe")) {
            return 3;
        } else if (id.contains("potion")) {
            return 0;
        } else if (id.contains("golden_apple")) {
            return 1;
        } else if (id.contains("totem")) {
            return 2;
        } else {
            return id.contains("ender_pearl") ? 3 : 10;
        }
    }

    boolean isArmor(ItemStack stack) {
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        return equippable != null && equippable.slot().isArmorSlot();
    }

    boolean isSword(ItemStack stack, String id) {
        return id.contains("_sword") || id.endsWith(":mace") || stack.contains(DataComponentTypes.WEAPON) && !this.isTool(stack, id);
    }

    boolean isTool(ItemStack stack, String id) {
        return id.contains("_pickaxe")
            || id.contains("_axe")
            || id.contains("_shovel")
            || id.contains("_hoe")
            || id.contains("trident")
            || stack.contains(DataComponentTypes.TOOL);
    }

    boolean isBuff(ItemStack stack, String id) {
        return stack.contains(DataComponentTypes.POTION_CONTENTS)
            || stack.isOf(Items.GOLDEN_APPLE)
            || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)
            || id.contains("potion")
            || id.contains("totem");
    }

    boolean isConsumable(ItemStack stack, String id) {
        return stack.contains(DataComponentTypes.FOOD)
            || stack.contains(DataComponentTypes.CONSUMABLE)
            || stack.isOf(Items.ENDER_PEARL)
            || id.contains("firework_rocket")
            || id.contains("experience_bottle");
    }

    boolean isBlockOrResource(String id) {
        return id.contains("_block")
            || id.contains("_ore")
            || id.contains("_ingot")
            || id.contains("_nugget")
            || id.contains("_gem")
            || id.contains("diamond")
            || id.contains("emerald")
            || id.contains("redstone")
            || id.contains("lapis")
            || id.contains("coal")
            || id.contains("quartz")
            || id.contains("netherite")
            || id.contains("cobblestone")
            || id.contains("stone")
            || id.contains("wood")
            || id.contains("log")
            || id.contains("planks");
    }

    int compareType(ItemStack a, ItemStack b) {
        return this.itemId(a).compareTo(this.itemId(b));
    }

    int compareName(ItemStack a, ItemStack b) {
        return a.getName().getString().compareToIgnoreCase(b.getName().getString());
    }

    int compareCount(ItemStack a, ItemStack b) {
        int count = Integer.compare(b.getCount(), a.getCount());
        return count != 0 ? count : this.compareLoadout(a, b);
    }

    String itemId(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).toString().toLowerCase(Locale.ROOT);
    }

    int findMatching(List<ItemStack> current, ItemStack desired, boolean[] fixed, int start) {
        for (int i = start; i < current.size(); i++) {
            if (!fixed[i] && this.equivalentForSort(current.get(i), desired)) {
                return i;
            }
        }

        return -1;
    }

    boolean equivalentForSort(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() ? ItemStack.areItemsAndComponentsEqual(a, b) && a.getCount() == b.getCount() : a.isEmpty() && b.isEmpty();
    }
}
