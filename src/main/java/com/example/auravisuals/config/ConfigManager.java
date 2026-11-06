package com.example.auravisuals.config;

import com.example.auravisuals.client.gui.hud.HudEditor;
import com.example.auravisuals.cosmetics.CosmeticItem;
import com.example.auravisuals.cosmetics.CosmeticsManager;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.player.AutoSwap;
import com.example.auravisuals.module.render.Crosshair;
import com.example.auravisuals.module.render.Markers;
import com.example.auravisuals.module.render.Markers$MarkerData;
import com.example.auravisuals.module.render.TrapTimer;
import com.example.auravisuals.setting.BindSetting;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.ItemHighlightSetting;
import com.example.auravisuals.setting.ItemScaleSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.MultiBooleanSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.setting.Setting;
import com.example.auravisuals.setting.StringSetting;
import com.example.auravisuals.theme.Theme;
import com.example.auravisuals.theme.ThemeManager;
import com.example.auravisuals.util.language.LanguageManager;
import com.example.auravisuals.util.policy.HolyServerPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.client.MinecraftClient;

public class ConfigManager {
    public static final ConfigManager INSTANCE = new ConfigManager();
    public static final String DEFAULT_CONFIG_NAME = "default";
    private final File configDir;
    private final Gson gson;
    private String activeConfigName = "default";

    private ConfigManager() {
        this.configDir = new File(MinecraftClient.getInstance().runDirectory, "anvil/configs");
        if (!this.configDir.exists()) {
            this.configDir.mkdirs();
        }

        this.migrateLegacyConfigs();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    private void migrateLegacyConfigs() {
        File legacyDir = new File(MinecraftClient.getInstance().runDirectory, "config/AuraVisuals/configs");
        if (legacyDir.isDirectory()) {
            File[] files = legacyDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File oldFile : files) {
                    File newFile = new File(this.configDir, oldFile.getName());
                    if (!newFile.exists()) {
                        oldFile.renameTo(newFile);
                    }
                }
            }
        }
    }

    public String getActiveConfigName() {
        return this.activeConfigName != null ? this.activeConfigName : "default";
    }

    public void setActiveConfigName(String name) {
        this.activeConfigName = name;
    }

    public JsonObject buildCurrentConfigJson() {
        JsonObject json = new JsonObject();
        Theme currentTheme = ThemeManager.INSTANCE.getCurrentTheme();
        if (currentTheme != null) {
            json.addProperty("__selectedTheme", currentTheme.getName());
        }

        json.add("__hudLayout", HudEditor.saveLayout());
        JsonObject cosmeticsJson = new JsonObject();

        for (Entry<String, CosmeticItem> entry : CosmeticsManager.INSTANCE.getEquippedMap().entrySet()) {
            CosmeticItem item = entry.getValue();
            if (item != null) {
                cosmeticsJson.addProperty(entry.getKey(), item.getId());
            }
        }

        json.add("__equippedCosmetics", cosmeticsJson);

        for (Module module : ModuleManager.INSTANCE.getModules()) {
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("enabled", module.isEnabled());
            moduleJson.addProperty("key", module.getKey());
            JsonObject settingsJson = new JsonObject();
            Map<String, Integer> settingKeyOccurrences = new LinkedHashMap<>();

            for (Setting<?> setting : this.getPersistedSettings(module)) {
                String storageKey = nextSettingStorageKey(setting, settingKeyOccurrences);
                if (setting instanceof ItemHighlightSetting itemSetting) {
                    JsonObject itemJson = new JsonObject();
                    itemJson.addProperty("enabled", itemSetting.getValue());
                    itemJson.addProperty("color", itemSetting.getColor());
                    settingsJson.add(storageKey, itemJson);
                } else if (setting instanceof ItemScaleSetting scaleSetting) {
                    settingsJson.addProperty(storageKey, scaleSetting.getValue());
                } else if (setting instanceof ColorSetting) {
                    settingsJson.addProperty(storageKey, ((ColorSetting)setting).getValue());
                } else if (setting instanceof BooleanSetting) {
                    settingsJson.addProperty(storageKey, ((BooleanSetting)setting).getValue());
                } else if (setting instanceof NumberSetting) {
                    settingsJson.addProperty(storageKey, ((NumberSetting)setting).getValue());
                } else if (setting instanceof ModeSetting) {
                    settingsJson.addProperty(storageKey, ((ModeSetting)setting).getValue());
                } else if (setting instanceof StringSetting) {
                    settingsJson.addProperty(storageKey, ((StringSetting)setting).getValue());
                } else if (setting instanceof BindSetting) {
                    settingsJson.addProperty(storageKey, ((BindSetting)setting).getValue());
                } else if (!(setting instanceof MultiBooleanSetting multiSetting)) {
                    if (setting.getValue() instanceof String value) {
                        settingsJson.addProperty(storageKey, value);
                    } else if (setting.getValue() instanceof Number value) {
                        settingsJson.addProperty(storageKey, value);
                    } else if (setting.getValue() instanceof Boolean value) {
                        settingsJson.addProperty(storageKey, value);
                    }
                } else {
                    JsonObject multiJson = new JsonObject();

                    for (String key : multiSetting.getValues()) {
                        multiJson.addProperty(key, multiSetting.isEnabled(key));
                    }

                    settingsJson.add(storageKey, multiJson);
                }
            }

            if (module instanceof TrapTimer) {
                settingsJson.addProperty("TrapTimer X", TrapTimer.x.getValue());
                settingsJson.addProperty("TrapTimer Y", TrapTimer.y.getValue());
                settingsJson.addProperty("TrapTimer Scale", TrapTimer.scale.getValue());
            }

            if (module instanceof Markers markers) {
                JsonArray markersJson = new JsonArray();

                for (Markers$MarkerData marker : markers.getMarkers()) {
                    JsonObject markerJson = new JsonObject();
                    markerJson.addProperty("name", marker.getName());
                    markerJson.addProperty("x", marker.getX());
                    markerJson.addProperty("y", marker.getY());
                    markerJson.addProperty("z", marker.getZ());
                    markerJson.addProperty("icon", String.valueOf(marker.getIconChar()));
                    markerJson.addProperty("background", marker.getBackgroundColor());
                    markerJson.addProperty("text", marker.getTextColor());
                    markerJson.addProperty("iconColor", marker.getIconColor());
                    markerJson.addProperty("iconBackgroundColor", marker.getIconBackgroundColor());
                    markerJson.addProperty("size", marker.getSize());
                    markersJson.add(markerJson);
                }

                settingsJson.add("markers", markersJson);
            }

            if (module instanceof AutoSwap autoSwap) {
                JsonArray arr = new JsonArray();

                for (int i = 0; i < 3; i++) {
                    JsonObject slotJson = autoSwap.serializeTripleSlot(i);
                    arr.add((JsonElement)(slotJson != null ? slotJson : JsonNull.INSTANCE));
                }

                settingsJson.add("tripleSwapSlots", arr);
            }

            moduleJson.add("settings", settingsJson);
            json.add(module.getConfigKey(), moduleJson);
        }

        return json;
    }

    public void saveConfig(String name) {
        this.activeConfigName = name;
        File file = new File(this.configDir, name + ".json");
        JsonObject json = this.buildCurrentConfigJson();
        File temporary = new File(this.configDir, name + ".json.tmp");

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(temporary), StandardCharsets.UTF_8)) {
            this.gson.toJson(json, writer);
            writer.flush();
        } catch (IOException var12) {
            var12.printStackTrace();
            temporary.delete();
            return;
        }

        try {
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException var9) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException var10) {
            var10.printStackTrace();
            temporary.delete();
        }
    }

    public void applyConfigJson(JsonObject json) {
        if (json != null) {
            try {
                if (json.has("__selectedTheme") && json.get("__selectedTheme").isJsonPrimitive()) {
                    ThemeManager.INSTANCE.setCurrentThemeByName(json.get("__selectedTheme").getAsString());
                }

                if (json.has("__hudLayout") && json.get("__hudLayout").isJsonObject()) {
                    HudEditor.loadLayout(json.getAsJsonObject("__hudLayout"));
                }

                if (json.has("__equippedCosmetics") && json.get("__equippedCosmetics").isJsonObject()) {
                    JsonObject stored = json.getAsJsonObject("__equippedCosmetics");
                    Map<String, String> configured = new LinkedHashMap<>();

                    for (String category : stored.keySet()) {
                        JsonElement value = stored.get(category);
                        if (value != null && value.isJsonPrimitive()) {
                            configured.put(category, value.getAsString());
                        }
                    }

                    CosmeticsManager.INSTANCE.applyEquippedConfig(configured);
                }

                for (Module module : ModuleManager.INSTANCE.getModules()) {
                    JsonObject moduleJson = this.getModuleJson(json, module);
                    if (moduleJson != null) {
                        Boolean enabledState = moduleJson.has("enabled") ? moduleJson.get("enabled").getAsBoolean() : null;
                        if (moduleJson.has("key")) {
                            module.setKey(moduleJson.get("key").getAsInt());
                        }

                        if (moduleJson.has("settings")) {
                            JsonObject settingsJson = moduleJson.getAsJsonObject("settings");
                            Map<String, Integer> settingKeyOccurrences = new LinkedHashMap<>();

                            for (Setting<?> setting : this.getPersistedSettings(module)) {
                                String storageKey = nextSettingStorageKey(setting, settingKeyOccurrences);
                                JsonElement element = this.getSettingElement(settingsJson, setting, storageKey);
                                if (element == null && module instanceof Crosshair && setting == Crosshair.pixelScale) {
                                    element = settingsJson.get("Scale");
                                }

                                if (element != null) {
                                    if (setting instanceof ItemHighlightSetting itemSetting && element.isJsonObject()) {
                                        JsonObject itemJson = element.getAsJsonObject();
                                        if (itemJson.has("enabled")) {
                                            itemSetting.setValue(itemJson.get("enabled").getAsBoolean());
                                        }

                                        if (itemJson.has("color")) {
                                            itemSetting.setColor(itemJson.get("color").getAsInt());
                                        }
                                    } else if (setting instanceof ItemScaleSetting && element.isJsonPrimitive()) {
                                        ((ItemScaleSetting)setting).setValue(element.getAsBoolean());
                                    } else if (setting instanceof ColorSetting && element.isJsonPrimitive()) {
                                        ((ColorSetting)setting).setValue(element.getAsInt());
                                    } else if (setting instanceof BooleanSetting && element.isJsonPrimitive()) {
                                        ((BooleanSetting)setting).setValue(element.getAsBoolean());
                                    } else if (setting instanceof NumberSetting && element.isJsonPrimitive()) {
                                        ((NumberSetting)setting).setValue(element.getAsDouble());
                                    } else if (setting instanceof ModeSetting && element.isJsonPrimitive()) {
                                        ((ModeSetting)setting).setValue(LanguageManager.repairIrcMojibake(element.getAsString()));
                                    } else if (setting instanceof StringSetting && element.isJsonPrimitive()) {
                                        ((StringSetting)setting).setValue(element.getAsString());
                                    } else if (setting instanceof BindSetting && element.isJsonPrimitive()) {
                                        ((BindSetting)setting).setValue(element.getAsInt());
                                    } else if (setting instanceof MultiBooleanSetting multiSetting && element.isJsonObject()) {
                                        JsonObject multiJson = element.getAsJsonObject();

                                        for (String key : multiSetting.getValues()) {
                                            if (multiJson.has(key)) {
                                                multiSetting.set(key, multiJson.get(key).getAsBoolean());
                                            }
                                        }
                                    } else if (element.isJsonPrimitive() && setting.getValue() instanceof String) {
                                        applyGenericSetting(setting, element.getAsString());
                                    } else if (element.isJsonPrimitive() && setting.getValue() instanceof Boolean) {
                                        applyGenericSetting(setting, element.getAsBoolean());
                                    } else if (element.isJsonPrimitive() && setting.getValue() instanceof Number) {
                                        applyGenericSetting(setting, element.getAsDouble());
                                    }
                                }
                            }

                            if (module instanceof Crosshair) {
                                ((Crosshair)module).loadPattern();
                            }

                            if (module instanceof TrapTimer) {
                                if (settingsJson.has("TrapTimer X")) {
                                    TrapTimer.x.setValue(settingsJson.get("TrapTimer X").getAsDouble());
                                }

                                if (settingsJson.has("TrapTimer Y")) {
                                    TrapTimer.y.setValue(settingsJson.get("TrapTimer Y").getAsDouble());
                                }

                                if (settingsJson.has("TrapTimer Scale")) {
                                    TrapTimer.scale.setValue(settingsJson.get("TrapTimer Scale").getAsDouble());
                                }
                            }

                            if (module instanceof Markers markers && settingsJson.has("markers") && settingsJson.get("markers").isJsonArray()) {
                                JsonArray markersJson = settingsJson.getAsJsonArray("markers");
                                List<Markers$MarkerData> list = new ArrayList<>();

                                for (JsonElement elementx : markersJson) {
                                    if (elementx.isJsonObject()) {
                                        JsonObject markerJson = elementx.getAsJsonObject();
                                        String markerName = markerJson.has("name") ? markerJson.get("name").getAsString() : "Метка";
                                        double x = markerJson.has("x") ? markerJson.get("x").getAsDouble() : 0.0;
                                        double y = markerJson.has("y") ? markerJson.get("y").getAsDouble() : 0.0;
                                        double z = markerJson.has("z") ? markerJson.get("z").getAsDouble() : 0.0;
                                        String iconStored = markerJson.has("icon") ? markerJson.get("icon").getAsString() : "A";
                                        char iconChar = Markers.migrateIconFromStored(iconStored);
                                        int background = markerJson.has("background") ? markerJson.get("background").getAsInt() : -14408146;
                                        int text = markerJson.has("text") ? markerJson.get("text").getAsInt() : -1;
                                        int iconColor = markerJson.has("iconColor") ? markerJson.get("iconColor").getAsInt() : -1;
                                        int iconBackgroundColor = markerJson.has("iconBackgroundColor")
                                            ? markerJson.get("iconBackgroundColor").getAsInt()
                                            : Markers.getIconBackgroundColor();
                                        int opacity = 255;
                                        float size = markerJson.has("size") ? markerJson.get("size").getAsFloat() : 1.0F;
                                        list.add(
                                            new Markers$MarkerData(
                                                markerName, x, y, z, iconChar, background, text, iconColor, opacity, size, iconBackgroundColor
                                            )
                                        );
                                    }
                                }

                                markers.setMarkers(list);
                            }

                            if (module instanceof AutoSwap autoSwap && settingsJson.has("tripleSwapSlots") && settingsJson.get("tripleSwapSlots").isJsonArray()
                                )
                             {
                                JsonArray arr = settingsJson.getAsJsonArray("tripleSwapSlots");

                                for (int i = 0; i < 3 && i < arr.size(); i++) {
                                    JsonElement el = arr.get(i);
                                    if (el == null || el.isJsonNull()) {
                                        autoSwap.clearTripleSlot(i);
                                    } else if (el.isJsonObject()) {
                                        autoSwap.deserializeTripleSlot(i, el.getAsJsonObject());
                                    }
                                }
                            }
                        }

                        if (enabledState != null && module.isEnabled() != enabledState) {
                            module.toggle();
                        }
                    }
                }

                HolyServerPolicy.enforceDisabledModulesForCurrentServer();
            } catch (Exception var29) {
                var29.printStackTrace();
            }
        }
    }

    public void loadConfig(String name) {
        File file = new File(this.configDir, name + ".json");
        if (file.exists()) {
            this.activeConfigName = name;

            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                this.applyConfigJson(json);
            } catch (IOException var8) {
                var8.printStackTrace();
            }
        }
    }

    public String exportToCode() {
        return exportJsonToCode(this.buildCurrentConfigJson());
    }

    public String exportConfigToCode(String name) {
        File file = new File(this.configDir, name + ".json");
        if (!file.exists()) {
            return this.exportToCode();
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return exportJsonToCode(json);
        } catch (Exception var8) {
            return this.exportToCode();
        }
    }

    public static String exportJsonToCode(JsonObject json) {
        try {
            String jsonStr = json.toString();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(jsonStr.getBytes(StandardCharsets.UTF_8));
            }

            return "AURA-" + Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray());
        } catch (Exception var8) {
            var8.printStackTrace();
            return "";
        }
    }

    public boolean importFromCode(String codeStr) {
        if (codeStr == null) {
            return false;
        }

        String raw = codeStr.trim();
        if (raw.startsWith("AURA-") || raw.startsWith("aura-")) {
            raw = raw.substring(5);
        }

        try {
            byte[] compressed = Base64.getUrlDecoder().decode(raw);
            ByteArrayInputStream bais = new ByteArrayInputStream(compressed);

            try (
                GZIPInputStream gzis = new GZIPInputStream(bais);
                InputStreamReader isr = new InputStreamReader(gzis, StandardCharsets.UTF_8);
            ) {
                JsonObject json = JsonParser.parseReader(isr).getAsJsonObject();
                this.applyConfigJson(json);
                return true;
            }
        } catch (Exception var15) {
            try {
                byte[] decoded = Base64.getDecoder().decode(raw);
                JsonObject json = JsonParser.parseString(new String(decoded, StandardCharsets.UTF_8)).getAsJsonObject();
                this.applyConfigJson(json);
                return true;
            } catch (Exception var10) {
                try {
                    JsonObject jsonx = JsonParser.parseString(raw).getAsJsonObject();
                    this.applyConfigJson(jsonx);
                    return true;
                } catch (Exception var9) {
                    return false;
                }
            }
        }
    }

    public List<String> listConfigs() {
        List<String> configs = new ArrayList<>();
        File[] files = this.configDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                configs.add(file.getName().replace(".json", ""));
            }
        }

        return configs;
    }

    public Path getConfigDirectory() {
        return this.configDir.toPath();
    }

    public boolean deleteConfig(String name) {
        File file = new File(this.configDir, name + ".json");
        return file.exists() ? file.delete() : false;
    }

    private JsonObject getModuleJson(JsonObject json, Module module) {
        for (String key : this.getModuleConfigKeys(module)) {
            JsonElement element = json.get(key);
            if (element != null && element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        }

        return null;
    }

    private List<String> getModuleConfigKeys(Module module) {
        List<String> keys = new ArrayList<>();
        this.addConfigKey(keys, module.getConfigKey());
        this.addConfigKey(keys, module.getName());
        String stableKey = module.getConfigKey();
        if ("CustomHitBox".equals(stableKey)) {
            this.addConfigKey(keys, "HitBoxCustomizer");
        } else if ("CustomWorld".equals(stableKey)) {
            this.addConfigKey(keys, "WorldCustomizer");
        } else if ("AucSearch".equals(stableKey)) {
            this.addConfigKey(keys, "AucHelper");
        }

        return keys;
    }

    private JsonElement getSettingElement(JsonObject settingsJson, Setting<?> setting, String storageKey) {
        JsonElement element = settingsJson.get(storageKey);
        if (element == null) {
            element = settingsJson.get(setting.getConfigKey());
        }

        if (element == null) {
            element = settingsJson.get(setting.getName());
        }

        if (element == null) {
            String configKey = setting.getConfigKey();
            String settingName = setting.getName();

            for (String storedKey : settingsJson.keySet()) {
                String repairedKey = LanguageManager.repairIrcMojibake(storedKey);
                if (repairedKey.equals(storageKey) || repairedKey.equals(configKey) || repairedKey.equals(settingName)) {
                    return settingsJson.get(storedKey);
                }
            }
        }

        return element;
    }

    private List<Setting<?>> getPersistedSettings(Module module) {
        List<Setting<?>> settings = new ArrayList<>();
        Set<Setting<?>> seen = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Setting<?> setting : module.getSettings()) {
            if (setting != null && seen.add(setting)) {
                settings.add(setting);
            }
        }

        for (Class<?> type = module.getClass(); type != null && Module.class.isAssignableFrom(type); type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Setting.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        Object owner = Modifier.isStatic(field.getModifiers()) ? null : module;
                        if (field.get(owner) instanceof Setting<?> settingx && seen.add(settingx)) {
                            settings.add(settingx);
                        }
                    } catch (RuntimeException | ReflectiveOperationException var12) {
                    }
                }
            }
        }

        return settings;
    }

    private static String nextSettingStorageKey(Setting<?> setting, Map<String, Integer> occurrences) {
        String baseKey = setting.getConfigKey();
        int occurrence = occurrences.merge(baseKey, 1, Integer::sum);
        return occurrence == 1 ? baseKey : baseKey + "#" + occurrence;
    }

    private static void applyGenericSetting(Setting setting, Object value) {
        setting.setValue(value);
    }

    private void addConfigKey(List<String> keys, String key) {
        if (key != null && !key.isEmpty() && !keys.contains(key)) {
            keys.add(key);
        }
    }
}
