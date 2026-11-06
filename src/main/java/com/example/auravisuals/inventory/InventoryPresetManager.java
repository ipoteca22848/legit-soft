package com.example.auravisuals.inventory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;

public final class InventoryPresetManager {
    public static final InventoryPresetManager INSTANCE = new InventoryPresetManager();
    static final int MAIN_SIZE = 36;
    static final int ARMOR_SIZE = 4;
    final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    final File presetDir = new File(MinecraftClient.getInstance().runDirectory, "anvil/inventories");

    InventoryPresetManager() {
        if (!this.presetDir.exists()) {
            this.presetDir.mkdirs();
        }
    }

    public List<InventoryPresetManager$Preset> listPresets() {
        List<InventoryPresetManager$Preset> presets = new ArrayList<>();
        File[] files = this.presetDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return presets;
        }

        for (File file : files) {
            InventoryPresetManager$Preset preset = this.readPreset(file);
            if (preset != null) {
                presets.add(preset);
            }
        }

        presets.sort(Comparator.comparing(InventoryPresetManager$Preset::name, String.CASE_INSENSITIVE_ORDER));
        return presets;
    }

    public InventoryPresetManager$Preset findPreset(String name) {
        return name != null && !name.isBlank() ? this.readPreset(this.fileFor(name)) : null;
    }

    public InventoryPresetManager$Preset getFallbackPreset() {
        List<InventoryPresetManager$Preset> presets = this.listPresets();
        return presets.isEmpty() ? null : presets.get(0);
    }

    public InventoryPresetManager$Preset saveCurrent(String rawName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return null;
        }

        String name = sanitizeName(rawName);
        if (name.isEmpty()) {
            name = "inventory";
        }

        PlayerInventory inventory = client.player.getInventory();
        InventoryPresetManager$SavedStack[] main = new InventoryPresetManager$SavedStack[36];

        for (int i = 0; i < 36; i++) {
            main[i] = InventoryPresetManager$SavedStack.from(inventory.getStack(i));
        }

        InventoryPresetManager$SavedStack[] armor = new InventoryPresetManager$SavedStack[4];

        for (int i = 0; i < 4; i++) {
            armor[i] = InventoryPresetManager$SavedStack.from(inventory.getStack(36 + i));
        }

        InventoryPresetManager$SavedStack offhand = InventoryPresetManager$SavedStack.from(inventory.getStack(40));
        InventoryPresetManager$Preset preset = new InventoryPresetManager$Preset(name, System.currentTimeMillis(), main, armor, offhand);
        this.writePreset(preset);
        return preset;
    }

    public boolean deletePreset(String name) {
        File file = this.fileFor(name);
        return file.exists() && file.delete();
    }

    public List<InventoryPresetManager$MissingEntry> getMissingEntries(InventoryPresetManager$Preset preset) {
        if (preset == null) {
            return List.of();
        }

        Map<String, InventoryPresetManager$MissingCounter> required = new LinkedHashMap<>();
        this.addRequired(required, preset.main());
        this.addRequired(required, preset.armor());
        this.addRequired(required, preset.offhand());
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            PlayerInventory inventory = client.player.getInventory();

            for (int i = 0; i < inventory.size(); i++) {
                this.subtractAvailable(required, InventoryPresetManager$SavedStack.from(inventory.getStack(i)));
            }
        }

        List<InventoryPresetManager$MissingEntry> entries = new ArrayList<>();

        for (InventoryPresetManager$MissingCounter counter : required.values()) {
            if (counter.count > 0) {
                entries.add(
                    new InventoryPresetManager$MissingEntry(new InventoryPresetManager$SavedStack(counter.stack.itemId(), counter.count), counter.count)
                );
            }
        }

        return entries;
    }

    void addRequired(Map<String, InventoryPresetManager$MissingCounter> required, InventoryPresetManager$SavedStack[] stacks) {
        for (InventoryPresetManager$SavedStack stack : stacks) {
            this.addRequired(required, stack);
        }
    }

    void addRequired(Map<String, InventoryPresetManager$MissingCounter> required, InventoryPresetManager$SavedStack stack) {
        if (stack != null && !stack.isEmpty()) {
            InventoryPresetManager$MissingCounter counter = required.computeIfAbsent(stack.itemId(), id -> new InventoryPresetManager$MissingCounter(stack));
            counter.count = counter.count + stack.count();
        }
    }

    void subtractAvailable(Map<String, InventoryPresetManager$MissingCounter> required, InventoryPresetManager$SavedStack stack) {
        if (stack != null && !stack.isEmpty()) {
            InventoryPresetManager$MissingCounter counter = required.get(stack.itemId());
            if (counter != null) {
                counter.count = counter.count - stack.count();
            }
        }
    }

    InventoryPresetManager$Preset readPreset(File file) {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String name = json.has("name") ? json.get("name").getAsString() : file.getName().replace(".json", "");
            long createdAt = json.has("createdAt") ? json.get("createdAt").getAsLong() : file.lastModified();
            InventoryPresetManager$SavedStack[] main = this.readStackArray(json.getAsJsonArray("main"), 36);
            InventoryPresetManager$SavedStack[] armor = this.readStackArray(json.getAsJsonArray("armor"), 4);
            InventoryPresetManager$SavedStack offhand = json.has("offhand") && json.get("offhand").isJsonObject()
                ? InventoryPresetManager$SavedStack.fromJson(json.getAsJsonObject("offhand"))
                : InventoryPresetManager$SavedStack.empty();
            return new InventoryPresetManager$Preset(name, createdAt, main, armor, offhand);
        } catch (Exception var13) {
            return null;
        }
    }

    void writePreset(InventoryPresetManager$Preset preset) {
        JsonObject json = new JsonObject();
        json.addProperty("name", preset.name());
        json.addProperty("createdAt", preset.createdAt());
        json.add("main", this.writeStackArray(preset.main()));
        json.add("armor", this.writeStackArray(preset.armor()));
        json.add("offhand", preset.offhand().toJson());

        try (FileWriter writer = new FileWriter(this.fileFor(preset.name()))) {
            this.gson.toJson(json, writer);
        } catch (IOException var8) {
            var8.printStackTrace();
        }
    }

    JsonArray writeStackArray(InventoryPresetManager$SavedStack[] stacks) {
        JsonArray array = new JsonArray();

        for (InventoryPresetManager$SavedStack stack : stacks) {
            array.add((stack == null ? InventoryPresetManager$SavedStack.empty() : stack).toJson());
        }

        return array;
    }

    InventoryPresetManager$SavedStack[] readStackArray(JsonArray array, int size) {
        InventoryPresetManager$SavedStack[] stacks = new InventoryPresetManager$SavedStack[size];

        for (int i = 0; i < size; i++) {
            stacks[i] = InventoryPresetManager$SavedStack.empty();
        }

        if (array == null) {
            return stacks;
        }

        for (int i = 0; i < size && i < array.size(); i++) {
            if (array.get(i).isJsonObject()) {
                stacks[i] = InventoryPresetManager$SavedStack.fromJson(array.get(i).getAsJsonObject());
            }
        }

        return stacks;
    }

    File fileFor(String name) {
        return new File(this.presetDir, sanitizeName(name) + ".json");
    }

    public static String sanitizeName(String raw) {
        return raw == null ? "" : raw.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
