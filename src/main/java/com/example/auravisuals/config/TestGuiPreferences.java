package com.example.auravisuals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;

public final class TestGuiPreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String OLD_HITBOX = "HitBoxCustomizer";
    private static final String NEW_HITBOX = "CustomHitBox";
    private static final Set<String> favorites = new LinkedHashSet<>();
    private static final Set<String> csOpenSettings = new LinkedHashSet<>();
    private static final Map<String, Float> testScroll = new HashMap<>();
    private static final Map<String, Float> csScroll = new HashMap<>();
    private static boolean favoritesOnly = false;
    private static String testCategory = "PLAYER";
    private static String csCategory = "PLAYER";
    private static boolean loaded = false;

    private static File prefsFile() {
        return new File(MinecraftClient.getInstance().runDirectory, "anvil/test_gui_prefs.json");
    }

    public static void ensureLoaded() {
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    public static boolean isFavorite(String moduleName) {
        return moduleName == null ? false : favorites.contains(moduleName);
    }

    public static void setFavorite(String moduleName, boolean on) {
        if (moduleName != null) {
            if (on) {
                favorites.remove(moduleName);
                favorites.add(moduleName);
            } else {
                favorites.remove(moduleName);
            }

            save();
        }
    }

    public static void toggleFavorite(String moduleName) {
        setFavorite(moduleName, !isFavorite(moduleName));
    }

    public static boolean isCsModuleSettingsOpen(String moduleName) {
        ensureLoaded();
        return moduleName != null && csOpenSettings.contains(moduleName);
    }

    public static void setCsModuleSettingsOpen(String moduleName, boolean open) {
        if (moduleName != null) {
            ensureLoaded();
            if (open) {
                csOpenSettings.remove(moduleName);
                csOpenSettings.add(moduleName);
            } else {
                csOpenSettings.remove(moduleName);
            }

            save();
        }
    }

    public static boolean isFavoritesOnly() {
        return favoritesOnly;
    }

    public static void setFavoritesOnly(boolean v) {
        favoritesOnly = v;
        save();
    }

    public static void toggleFavoritesOnly() {
        setFavoritesOnly(!favoritesOnly);
    }

    public static String getTestCategory() {
        ensureLoaded();
        return testCategory;
    }

    public static void setTestCategory(String category) {
        if (category != null && !category.isBlank()) {
            testCategory = category;
            save();
        }
    }

    public static float getTestScroll(String category) {
        ensureLoaded();
        return testScroll.getOrDefault(category, 0.0F);
    }

    public static void setTestScroll(String category, float value) {
        if (category != null) {
            testScroll.put(category, value);
            save();
        }
    }

    public static String getCsCategory() {
        ensureLoaded();
        return csCategory;
    }

    public static void setCsCategory(String category) {
        if (category != null && !category.isBlank()) {
            csCategory = category;
            save();
        }
    }

    public static float getCsScroll(String category) {
        ensureLoaded();
        return csScroll.getOrDefault(category, 0.0F);
    }

    public static void setCsScroll(String category, float value) {
        if (category != null) {
            csScroll.put(category, value);
            save();
        }
    }

    private static void load() {
        File f = prefsFile();
        if (f.exists()) {
            try (FileReader r = new FileReader(f)) {
                JsonObject o = (JsonObject)GSON.fromJson(r, JsonObject.class);
                if (o != null) {
                    favorites.clear();
                    if (o.has("favorites") && o.get("favorites").isJsonArray()) {
                        JsonArray arr = o.getAsJsonArray("favorites");

                        for (int i = 0; i < arr.size(); i++) {
                            if (arr.get(i).isJsonPrimitive()) {
                                String name = arr.get(i).getAsString();
                                if ("HitBoxCustomizer".equals(name)) {
                                    name = "CustomHitBox";
                                }

                                favorites.add(name);
                            }
                        }
                    }

                    if (o.has("favoritesOnly") && o.get("favoritesOnly").isJsonPrimitive()) {
                        favoritesOnly = o.get("favoritesOnly").getAsBoolean();
                    }

                    csOpenSettings.clear();
                    if (o.has("csOpenSettings") && o.get("csOpenSettings").isJsonArray()) {
                        JsonArray arr = o.getAsJsonArray("csOpenSettings");

                        for (int ix = 0; ix < arr.size(); ix++) {
                            if (arr.get(ix).isJsonPrimitive()) {
                                csOpenSettings.add(arr.get(ix).getAsString());
                            }
                        }
                    }

                    if (o.has("testCategory") && o.get("testCategory").isJsonPrimitive()) {
                        testCategory = o.get("testCategory").getAsString();
                    }

                    if (o.has("csCategory") && o.get("csCategory").isJsonPrimitive()) {
                        csCategory = o.get("csCategory").getAsString();
                    }

                    readScrollMap(o, "testScroll", testScroll);
                    readScrollMap(o, "csScroll", csScroll);
                    return;
                }

                return;
            } catch (IOException var8) {
            }
        }
    }

    private static void readScrollMap(JsonObject root, String key, Map<String, Float> target) {
        target.clear();
        if (root.has(key) && root.get(key).isJsonObject()) {
            JsonObject o = root.getAsJsonObject(key);

            for (String name : o.keySet()) {
                if (o.get(name).isJsonPrimitive()) {
                    target.put(name, o.get(name).getAsFloat());
                }
            }
        }
    }

    private static void save() {
        File f = prefsFile();
        File dir = f.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }

        JsonObject o = new JsonObject();
        o.addProperty("favoritesOnly", favoritesOnly);
        o.addProperty("testCategory", testCategory);
        o.addProperty("csCategory", csCategory);
        JsonArray arr = new JsonArray();

        for (String s : favorites) {
            arr.add(s);
        }

        o.add("favorites", arr);
        JsonArray openSettings = new JsonArray();

        for (String s : csOpenSettings) {
            openSettings.add(s);
        }

        o.add("csOpenSettings", openSettings);
        o.add("testScroll", writeScrollMap(testScroll));
        o.add("csScroll", writeScrollMap(csScroll));

        try (FileWriter w = new FileWriter(f)) {
            GSON.toJson(o, w);
        } catch (IOException var10) {
        }
    }

    private static JsonObject writeScrollMap(Map<String, Float> source) {
        JsonObject o = new JsonObject();

        for (Entry<String, Float> entry : source.entrySet()) {
            o.addProperty(entry.getKey(), entry.getValue());
        }

        return o;
    }
}
