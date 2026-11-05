package com.example.auravisuals.inventory;

import java.util.Objects;

public final class InventoryPresetManager$MissingEntry {
    InventoryPresetManager$SavedStack stack;
    int missingCount;

    public InventoryPresetManager$MissingEntry(InventoryPresetManager$SavedStack stack, int missingCount) {
        this.stack = stack;
        this.missingCount = missingCount;
    }

    public InventoryPresetManager$SavedStack stack() {
        return this.stack;
    }

    public int missingCount() {
        return this.missingCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof InventoryPresetManager$MissingEntry other)
                ? false
                : Objects.equals(this.stack, other.stack) && Objects.equals(this.missingCount, other.missingCount);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.stack, this.missingCount);
    }

    @Override
    public String toString() {
        return "InventoryPresetManager$MissingEntry[stack=" + this.stack + ", missingCount=" + this.missingCount + "]";
    }
}
