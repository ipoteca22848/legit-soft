package com.example.auravisuals.config;

import com.example.auravisuals.client.gui.hud.HudEditor;
import com.example.auravisuals.client.gui.hud.HudElementStyleManager;
import com.example.auravisuals.module.render.Hud;
import com.example.auravisuals.setting.BooleanSetting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

public final class HudPresetManager {
    public static final String DEFAULT_PRESET = "Основной";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, JsonObject> PRESETS = new LinkedHashMap<>();
    private static JsonObject mainProfile;
    private static boolean loaded;

    private HudPresetManager() {
    }

    public static synchronized List<String> modes() {
        ensureLoaded();
        List<String> modes = new ArrayList<>(PRESETS.size() + 1);
        modes.add("Основной");
        modes.addAll(PRESETS.keySet());
        return modes;
    }

    public static synchronized String create(String requestedName) {
        ensureLoaded();
        String name = sanitize(requestedName);
        if (!name.isEmpty() && !name.equalsIgnoreCase("Основной")) {
            boolean duplicate = PRESETS.keySet().stream().anyMatch(existing -> existing.equalsIgnoreCase(name));
            if (duplicate) {
                return null;
            }

            PRESETS.put(name, captureCurrent());
            save();
            return name;
        } else {
            return null;
        }
    }

    public static synchronized void saveCurrent(String name) {
        ensureLoaded();
        if (name != null && !name.isBlank()) {
            if (name.equalsIgnoreCase("Основной")) {
                mainProfile = captureCurrent();
            } else {
                String stored = findName(name);
                if (stored != null) {
                    PRESETS.put(stored, captureCurrent());
                }
            }

            save();
        }
    }

    public static synchronized void apply(String name) {
        ensureLoaded();
        String storedName = findName(name);
        JsonObject profile = name != null && name.equalsIgnoreCase("Основной") ? mainProfile : (storedName == null ? null : PRESETS.get(storedName));
        if (profile != null) {
            applyProfile(profile);
        }
    }

    public static synchronized JsonObject currentState() {
        return captureCurrent();
    }

    public static synchronized void applyAiChanges(String presetName, JsonObject changes) {
        if (changes != null) {
            applyStyle(changes);
            applyElements(changes.has("elements") && changes.get("elements").isJsonObject() ? changes.getAsJsonObject("elements") : null);
            JsonObject layoutPatch = null;
            if (changes.has("layout") && changes.get("layout").isJsonArray()) {
                layoutPatch = new JsonObject();

                for (JsonElement element : changes.getAsJsonArray("layout")) {
                    if (element.isJsonObject()) {
                        JsonObject update = element.getAsJsonObject();
                        if (update.has("id") && update.get("id").isJsonPrimitive()) {
                            String id = update.get("id").getAsString();
                            JsonObject placement = update.deepCopy();
                            placement.remove("id");
                            layoutPatch.add(id, placement);
                        }
                    }
                }
            }

            applyLayoutPatch(layoutPatch);
            if (changes.has("elementStyles") && changes.get("elementStyles").isJsonArray()) {
                JsonObject stylePatch = new JsonObject();

                for (JsonElement elementx : changes.getAsJsonArray("elementStyles")) {
                    if (elementx.isJsonObject()) {
                        JsonObject update = elementx.getAsJsonObject();
                        if (update.has("id") && update.get("id").isJsonPrimitive()) {
                            String id = update.get("id").getAsString();
                            if (HudElementStyleManager.ELEMENT_IDS.contains(id)) {
                                JsonObject style = flattenElementStyle(update);
                                stylePatch.add(id, style);
                            }
                        }
                    }
                }

                HudElementStyleManager.applyPatch(stylePatch);
            }

            saveCurrent(presetName);
        }
    }

    private static JsonObject flattenElementStyle(JsonObject update) {
        JsonObject style = new JsonObject();
        if (update.has("material")) {
            style.add("material", update.get("material"));
        }

        flattenGroup(update, style, "colors", Map.of("panel", "panelColor", "panel2", "panelColor2", "text", "textColor", "accent", "accentColor"));
        flattenGroup(update, style, "panel", Map.of("opacity", "panelOpacity", "radius", "radius"));
        flattenGroup(update, style, "icon", Map.of("name", "icon", "scale", "iconScale"));
        flattenGroup(update, style, "shadow", Map.of("enabled", "shadow", "color", "shadowColor", "strength", "shadowStrength"));
        flattenGroup(update, style, "blur", Map.of("enabled", "blur", "strength", "blurStrength"));
        flattenGroup(update, style, "glow", Map.of("enabled", "glow", "color", "glowColor", "strength", "glowStrength"));
        flattenGroup(update, style, "outline", Map.of("enabled", "outline", "color", "outlineColor", "width", "outlineWidth"));
        flattenGroup(update, style, "stripe", Map.of("enabled", "stripe", "color", "stripeColor", "width", "stripeWidth", "side", "stripeSide"));
        flattenGroup(update, style, "label", Map.of("text", "label", "color", "labelColor"));
        return style;
    }

    private static void flattenGroup(JsonObject source, JsonObject target, String group, Map<String, String> names) {
        if (source.has(group) && source.get(group).isJsonObject()) {
            JsonObject values = source.getAsJsonObject(group);
            names.forEach((from, to) -> {
                if (values.has(from)) {
                    target.add(to, values.get(from));
                }
            });
        }
    }

    private static JsonObject captureCurrent() {
        JsonObject profile = new JsonObject();
        profile.addProperty("hudStyle", Hud.isLiquidGlassStyle() ? "Liquid Glass" : "Classic");
        profile.addProperty("accentColor", rgb(Hud.accentColor.getColor()));
        profile.addProperty("glassColor", rgb(Hud.hudGlassColor.getColor()));
        profile.addProperty("glassOpacity", Hud.hudGlassColor.getAlpha());
        profile.addProperty("glassGlow", Hud.hudGlassGlow.getValue());
        profile.addProperty("glowStrength", Hud.hudGlowStrength.getValue());
        profile.addProperty("shadow", Hud.shadow.getValue());
        profile.addProperty("blur", Hud.blur.getValue());
        JsonObject elements = new JsonObject();
        elements.addProperty("watermark", Hud.watermark.getValue());
        elements.addProperty("notifications", Hud.notifications.getValue());
        elements.addProperty("target", Hud.target.getValue());
        elements.addProperty("hotkeys", Hud.hotkeys.getValue());
        elements.addProperty("potions", Hud.potions.getValue());
        elements.addProperty("cooldowns", Hud.cooldowns.getValue());
        elements.addProperty("inventory", Hud.inventory.getValue());
        elements.addProperty("mediaPlayer", Hud.mediaPlayer.getValue());
        elements.addProperty("armorHud", Hud.armorHud.getValue());
        elements.addProperty("customHotbar", Hud.customHotbar.getValue());
        profile.add("elements", elements);
        profile.add("layout", HudEditor.saveLayout());
        profile.add("elementStyles", HudElementStyleManager.snapshot());
        return profile;
    }

    private static void applyProfile(JsonObject profile) {
        applyStyle(profile);
        applyElements(profile.has("elements") && profile.get("elements").isJsonObject() ? profile.getAsJsonObject("elements") : null);
        if (profile.has("layout") && profile.get("layout").isJsonObject()) {
            HudEditor.loadLayout(profile.getAsJsonObject("layout"));
        }

        HudElementStyleManager.load(
            profile.has("elementStyles") && profile.get("elementStyles").isJsonObject() ? profile.getAsJsonObject("elementStyles") : new JsonObject()
        );
    }

    private static void applyStyle(JsonObject object) {
        if (primitive(object, "hudStyle")) {
            String style = object.get("hudStyle").getAsString();
            Hud.setHudStyle(style);
        }

        Color accent = parseColor(object, "accentColor", Hud.accentColor.getColor());
        Hud.accentColor.setColor(accent);
        Color oldGlass = Hud.hudGlassColor.getColor();
        Color glass = parseColor(object, "glassColor", oldGlass);
        int opacity = integer(object, "glassOpacity", glass.getAlpha(), 0, 255);
        Hud.hudGlassColor.setColor(new Color(glass.getRed(), glass.getGreen(), glass.getBlue(), opacity));
        setBoolean(object, "glassGlow", Hud.hudGlassGlow);
        setBoolean(object, "shadow", Hud.shadow);
        setBoolean(object, "blur", Hud.blur);
        if (primitive(object, "glowStrength")) {
            try {
                double value = MathHelper.clamp(object.get("glowStrength").getAsDouble(), 0.25, 2.0);
                Hud.hudGlowStrength.setValue(value);
            } catch (Exception var7) {
            }
        }
    }

    private static void applyElements(JsonObject elements) {
        if (elements != null) {
            setBoolean(elements, "watermark", Hud.watermark);
            setBoolean(elements, "notifications", Hud.notifications);
            setBoolean(elements, "target", Hud.target);
            setBoolean(elements, "hotkeys", Hud.hotkeys);
            setBoolean(elements, "potions", Hud.potions);
            setBoolean(elements, "cooldowns", Hud.cooldowns);
            setBoolean(elements, "inventory", Hud.inventory);
            setBoolean(elements, "mediaPlayer", Hud.mediaPlayer);
            setBoolean(elements, "armorHud", Hud.armorHud);
            setBoolean(elements, "customHotbar", Hud.customHotbar);
        }
    }

    private static void applyLayoutPatch(JsonObject patch) {
        if (patch != null) {
            JsonObject layout = HudEditor.saveLayout();

            for (String id : List.of("watermark", "notifications", "target", "hotkeys", "potions", "cooldowns", "mediaPlayer", "inventory", "armorHud")) {
                if (patch.has(id) && patch.get(id).isJsonObject()) {
                    JsonObject target = layout.has(id) && layout.get(id).isJsonObject() ? layout.getAsJsonObject(id) : new JsonObject();
                    JsonObject requested = patch.getAsJsonObject(id);
                    if (primitive(requested, "x")) {
                        target.addProperty("x", finite(requested.get("x"), 0.0F));
                    }

                    if (primitive(requested, "y")) {
                        target.addProperty("y", finite(requested.get("y"), 0.0F));
                    }

                    if (primitive(requested, "scale")) {
                        target.addProperty("scale", MathHelper.clamp(finite(requested.get("scale"), 1.0F), 0.6F, 1.6F));
                    }

                    layout.add(id, target);
                }
            }

            HudEditor.loadLayout(layout);
        }
    }

    private static void setBoolean(JsonObject object, String key, BooleanSetting setting) {
        try {
            if (primitive(object, key)) {
                setting.setValue(object.get(key).getAsBoolean());
            }
        } catch (Exception var4) {
        }
    }

    private static Color parseColor(JsonObject object, String key, Color fallback) {
        if (!primitive(object, key)) {
            return fallback;
        }

        try {
            String value = object.get(key).getAsString().strip();
            if (value.startsWith("#")) {
                value = value.substring(1);
            }

            if (value.length() != 6) {
                return fallback;
            }

            int rgb = Integer.parseInt(value, 16);
            return new Color(fallback.getAlpha() << 24 | rgb, true);
        } catch (Exception var5) {
            return fallback;
        }
    }

    private static int integer(JsonObject object, String key, int fallback, int min, int max) {
        try {
            return primitive(object, key) ? MathHelper.clamp(object.get(key).getAsInt(), min, max) : fallback;
        } catch (Exception var6) {
            return fallback;
        }
    }

    private static float finite(JsonElement element, float fallback) {
        try {
            float value = element.getAsFloat();
            return Float.isFinite(value) ? value : fallback;
        } catch (Exception var3) {
            return fallback;
        }
    }

    private static boolean primitive(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive();
    }

    private static String rgb(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static void ensureLoaded() {
        if (!loaded) {
            loaded = true;
            File file = file();
            if (file.isFile()) {
                try {
                    JsonElement parsed = JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8));
                    if (!parsed.isJsonObject()) {
                        return;
                    }

                    JsonObject root = parsed.getAsJsonObject();
                    if (root.has("mainProfile") && root.get("mainProfile").isJsonObject()) {
                        mainProfile = root.getAsJsonObject("mainProfile");
                    }

                    JsonArray array = root.has("presets") && root.get("presets").isJsonArray() ? root.getAsJsonArray("presets") : null;
                    if (array == null) {
                        return;
                    }

                    for (JsonElement element : array) {
                        String name;
                        JsonObject profile;
                        if (element.isJsonPrimitive()) {
                            name = sanitize(element.getAsString());
                            profile = null;
                        } else {
                            if (!element.isJsonObject()) {
                                continue;
                            }

                            JsonObject stored = element.getAsJsonObject();
                            name = stored.has("name") ? sanitize(stored.get("name").getAsString()) : "";
                            profile = stored.has("profile") && stored.get("profile").isJsonObject() ? stored.getAsJsonObject("profile") : null;
                        }

                        if (!name.isEmpty() && !name.equalsIgnoreCase("Основной")) {
                            PRESETS.put(name, profile);
                        }
                    }
                } catch (Exception var9) {
                }
            }
        }
    }

    private static void save() {
        try {
            File file = file();
            File parent = file.getParentFile();
            if (parent != null) {
                Files.createDirectories(parent.toPath());
            }

            JsonObject root = new JsonObject();
            if (mainProfile != null) {
                root.add("mainProfile", mainProfile);
            }

            JsonArray array = new JsonArray();
            PRESETS.forEach((name, profile) -> {
                JsonObject item = new JsonObject();
                item.addProperty("name", name);
                if (profile != null) {
                    item.add("profile", profile);
                }

                array.add(item);
            });
            root.add("presets", array);
            Files.writeString(file.toPath(), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception var4) {
        }
    }

    private static File file() {
        return FabricLoader.getInstance().getGameDir().resolve("bamber client").resolve("hud-presets.json").toFile();
    }

    private static String findName(String requested) {
        return requested == null ? null : PRESETS.keySet().stream().filter(name -> name.equalsIgnoreCase(requested)).findFirst().orElse(null);
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }

        String name = value.replaceAll("[\\p{Cntrl}\\r\\n\\t]", " ").replaceAll("\\s+", " ").strip();
        if (name.length() > 32) {
            name = name.substring(0, 32).strip();
        }

        return name.toLowerCase(Locale.ROOT).equals("Основной".toLowerCase(Locale.ROOT)) ? "Основной" : name;
    }
}
