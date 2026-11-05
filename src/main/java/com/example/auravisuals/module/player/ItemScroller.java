package com.example.auravisuals.module.player;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class ItemScroller extends Module {
    private static final Set<Integer> processedSlots = new HashSet<>();
    private static boolean dragging = false;

    public ItemScroller() {
        super("ItemScroller", "Быстрое перекладывание предметов", Category.PLAYER);
    }

    public static void startDrag(Slot initialSlot) {
        dragging = true;
        processedSlots.clear();
        if (initialSlot != null && initialSlot.hasStack()) {
            processedSlots.add(initialSlot.id);
        }
    }

    public static void onDragOver(Slot slot, ScreenHandler handler) {
        if (dragging && slot != null && !slot.getStack().isEmpty()) {
            if (!processedSlots.contains(slot.id)) {
                moveSlot(slot, handler);
            }
        }
    }

    public static void stopDrag() {
        dragging = false;
        processedSlots.clear();
    }

    public static boolean isDragging() {
        return dragging;
    }

    private static void moveSlot(Slot slot, ScreenHandler handler) {
        if (slot != null && !slot.getStack().isEmpty()) {
            processedSlots.add(slot.id);
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.interactionManager != null && mc.player != null) {
                mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
            }
        }
    }
}
