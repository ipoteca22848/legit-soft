package com.example.auravisuals.inventory;

import com.google.gson.JsonObject;
import java.util.Objects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class InventoryPresetManager$SavedStack {
    String itemId;
    int count;

    public InventoryPresetManager$SavedStack(String itemId, int count) {
        this.itemId = itemId;
        this.count = count;
    }

    public String itemId() {
        return this.itemId;
    }

    public int count() {
        return this.count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof InventoryPresetManager$SavedStack other)
                ? false
                : Objects.equals(this.itemId, other.itemId) && Objects.equals(this.count, other.count);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.itemId, this.count);
    }

    @Override
    public String toString() {
        return "InventoryPresetManager$SavedStack[itemId=" + this.itemId + ", count=" + this.count + "]";
    }

    public static InventoryPresetManager$SavedStack empty() {
        return new InventoryPresetManager$SavedStack("minecraft:air", 0);
    }

    public static InventoryPresetManager$SavedStack from(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            Identifier id = Registries.ITEM.getId(stack.getItem());
            return new InventoryPresetManager$SavedStack(id.toString(), stack.getCount());
        } else {
            return empty();
        }
    }

    public static InventoryPresetManager$SavedStack fromJson(JsonObject json) {
        String item = json.has("item") ? json.get("item").getAsString() : "minecraft:air";
        int count = json.has("count") ? json.get("count").getAsInt() : 0;
        return new InventoryPresetManager$SavedStack(item, count);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("item", this.itemId);
        json.addProperty("count", this.count);
        return json;
    }

    public boolean isEmpty() {
        return this.count <= 0 || "minecraft:air".equals(this.itemId);
    }

    public ItemStack toItemStack() {
        if (this.isEmpty()) {
            return ItemStack.EMPTY;
        }

        Identifier id = Identifier.tryParse(this.itemId);
        if (id == null) {
            return ItemStack.EMPTY;
        }

        Item item = Registries.ITEM.get(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item, Math.max(1, this.count));
    }
}
