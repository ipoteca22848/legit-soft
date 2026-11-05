package com.example.auravisuals.inventory;

import java.util.Objects;

public final class InventoryPresetManager$Preset {
    String name;
    long createdAt;
    InventoryPresetManager$SavedStack[] main;
    InventoryPresetManager$SavedStack[] armor;
    InventoryPresetManager$SavedStack offhand;

    public InventoryPresetManager$Preset(
        String name,
        long createdAt,
        InventoryPresetManager$SavedStack[] main,
        InventoryPresetManager$SavedStack[] armor,
        InventoryPresetManager$SavedStack offhand
    ) {
        this.name = name;
        this.createdAt = createdAt;
        this.main = main;
        this.armor = armor;
        this.offhand = offhand;
    }

    public String name() {
        return this.name;
    }

    public long createdAt() {
        return this.createdAt;
    }

    public InventoryPresetManager$SavedStack[] main() {
        return this.main;
    }

    public InventoryPresetManager$SavedStack[] armor() {
        return this.armor;
    }

    public InventoryPresetManager$SavedStack offhand() {
        return this.offhand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof InventoryPresetManager$Preset other)
                ? false
                : Objects.equals(this.name, other.name)
                    && Objects.equals(this.createdAt, other.createdAt)
                    && Objects.equals(this.main, other.main)
                    && Objects.equals(this.armor, other.armor)
                    && Objects.equals(this.offhand, other.offhand);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.createdAt, this.main, this.armor, this.offhand);
    }

    @Override
    public String toString() {
        return "InventoryPresetManager$Preset[name="
            + this.name
            + ", createdAt="
            + this.createdAt
            + ", main="
            + this.main
            + ", armor="
            + this.armor
            + ", offhand="
            + this.offhand
            + "]";
    }

    public int filledSlots() {
        int count = 0;

        for (InventoryPresetManager$SavedStack stack : this.main) {
            if (stack != null && !stack.isEmpty()) {
                count++;
            }
        }

        return count;
    }
}
