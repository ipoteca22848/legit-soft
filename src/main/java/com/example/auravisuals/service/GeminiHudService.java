package com.example.auravisuals.service;

import com.example.auravisuals.config.HudPresetManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.loader.api.FabricLoader;

public final class GeminiHudService {
    static final String MODEL = "gemini-3.6-flash";
    static final URI ENDPOINT = URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent");
    static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(12L)).version(Version.HTTP_2).build();
    static final String SYSTEM_PROMPT = "Ты — безопасный редактор HUD для Minecraft-клиента bamber client. Отвечай по-русски.\nПользователь разрешает только оформление уже существующих HUD-элементов. Никогда не предлагай\nкод, Lua, команды Minecraft, новые игровые функции, ESP, анализ сущностей или доступ к файлам.\nВозвращай краткое поле message и только те поля changes, которые действительно нужны запросу.\n\nЭкран имеет координаты 1920x1080. changes.layout — массив объектов с id, x, y, scale.\nx и y — смещение от текущей базовой позиции,\nа не абсолютная координата. Допустимый scale: 0.60..1.60. CustomHotbar нельзя перемещать и\nмасштабировать. Доступные элементы: watermark (ватермарка), notifications (уведомления),\ntarget (цель), hotkeys (хоткеи), potions (зелья), cooldowns (кулдауны), mediaPlayer\n(медиаплеер), inventory (инвентарь), armorHud (броня), customHotbar (кастомный хотбар).\nhudStyle: Classic или Liquid Glass. Цвета строго #RRGGBB. glassOpacity: 0..255,\nglowStrength: 0.25..2.0. Если запрос непонятен, не меняй HUD и попроси уточнить в message.\n\nДля индивидуального оформления используй массив changes.elementStyles. Каждый объект массива\nсодержит id элемента и только нужные группы: material; colors (panel, panel2, text, accent);\npanel (opacity, radius); icon (name, scale); shadow, blur, glow, outline;\nstripe (enabled, color, width, side); label (text, color).\nДопустимые icon: logo, search, list, star, command, command_large, grid, diamond,\ncommand_small, person, monitor, ping, keyboard, flask, clock. Не добавляй свойства вне схемы.\nОбязательно кодируй в changes каждое изменение, о котором сообщаешь в message. Не утверждай,\nчто изменение сделано, если соответствующего поля нет в changes.\n";

    GeminiHudService() {
    }

    public static CompletableFuture<GeminiHudService$Result> request(String prompt) {
        String apiKey = loadApiKey();
        if (apiKey.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException("API-ключ Gemini не найден в локальном конфиге"));
        }

        JsonObject payload = new JsonObject();
        payload.add(
            "systemInstruction",
            content(
                "Ты — безопасный редактор HUD для Minecraft-клиента bamber client. Отвечай по-русски.\nПользователь разрешает только оформление уже существующих HUD-элементов. Никогда не предлагай\nкод, Lua, команды Minecraft, новые игровые функции, ESP, анализ сущностей или доступ к файлам.\nВозвращай краткое поле message и только те поля changes, которые действительно нужны запросу.\n\nЭкран имеет координаты 1920x1080. changes.layout — массив объектов с id, x, y, scale.\nx и y — смещение от текущей базовой позиции,\nа не абсолютная координата. Допустимый scale: 0.60..1.60. CustomHotbar нельзя перемещать и\nмасштабировать. Доступные элементы: watermark (ватермарка), notifications (уведомления),\ntarget (цель), hotkeys (хоткеи), potions (зелья), cooldowns (кулдауны), mediaPlayer\n(медиаплеер), inventory (инвентарь), armorHud (броня), customHotbar (кастомный хотбар).\nhudStyle: Classic или Liquid Glass. Цвета строго #RRGGBB. glassOpacity: 0..255,\nglowStrength: 0.25..2.0. Если запрос непонятен, не меняй HUD и попроси уточнить в message.\n\nДля индивидуального оформления используй массив changes.elementStyles. Каждый объект массива\nсодержит id элемента и только нужные группы: material; colors (panel, panel2, text, accent);\npanel (opacity, radius); icon (name, scale); shadow, blur, glow, outline;\nstripe (enabled, color, width, side); label (text, color).\nДопустимые icon: logo, search, list, star, command, command_large, grid, diamond,\ncommand_small, person, monitor, ping, keyboard, flask, clock. Не добавляй свойства вне схемы.\nОбязательно кодируй в changes каждое изменение, о котором сообщаешь в message. Не утверждай,\nчто изменение сделано, если соответствующего поля нет в changes.\n"
            )
        );
        JsonArray contents = new JsonArray();
        JsonObject user = content("Текущее состояние HUD:\n" + HudPresetManager.currentState() + "\n\nЗапрос пользователя:\n" + prompt.strip());
        user.addProperty("role", "user");
        contents.add(user);
        payload.add("contents", contents);
        JsonObject generation = new JsonObject();
        generation.addProperty("temperature", 0.35);
        generation.addProperty("responseMimeType", "application/json");
        generation.add("responseJsonSchema", responseSchema());
        payload.add("generationConfig", generation);
        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
            .timeout(Duration.ofSeconds(45L))
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("x-goog-api-key", apiKey)
            .POST(BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
            .build();
        return HTTP.sendAsync(request, BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApply(GeminiHudService::parseResponse);
    }

    static GeminiHudService$Result parseResponse(HttpResponse<String> response) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            try {
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray candidates = root.getAsJsonArray("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    JsonArray parts = candidates.get(0).getAsJsonObject().getAsJsonObject("content").getAsJsonArray("parts");
                    StringBuilder text = new StringBuilder();

                    for (JsonElement part : parts) {
                        if (part.isJsonObject() && part.getAsJsonObject().has("text")) {
                            text.append(part.getAsJsonObject().get("text").getAsString());
                        }
                    }

                    JsonObject result = JsonParser.parseString(text.toString()).getAsJsonObject();
                    String message = result.has("message") ? result.get("message").getAsString() : "Оформление HUD обновлено.";
                    JsonObject changes = result.has("changes") && result.get("changes").isJsonObject() ? result.getAsJsonObject("changes") : new JsonObject();
                    return new GeminiHudService$Result(message, changes);
                } else {
                    throw new IllegalStateException("Gemini не вернул ответ");
                }
            } catch (IllegalStateException var8) {
                throw var8;
            } catch (Exception var9) {
                throw new IllegalStateException("Не удалось прочитать ответ Gemini");
            }
        } else {
            String message = switch (response.statusCode()) {
                case 400 -> "Gemini отклонил запрос";
                case 401, 403 -> "Gemini не принял API-ключ или у ключа нет доступа";
                case 429 -> "Лимит запросов Gemini исчерпан. Попробуйте немного позже";
                default -> "Ошибка Gemini: HTTP " + response.statusCode();
            };
            throw new IllegalStateException(message);
        }
    }

    static JsonObject content(String text) {
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        parts.add(part);
        content.add("parts", parts);
        return content;
    }

    static JsonObject responseSchema() {
        JsonObject root = objectSchema();
        JsonObject properties = new JsonObject();
        properties.add("message", stringSchema());
        JsonObject changes = objectSchema();
        JsonObject changeProperties = new JsonObject();
        changeProperties.add("hudStyle", enumSchema("Classic", "Liquid Glass"));
        changeProperties.add("accentColor", stringSchema());
        changeProperties.add("glassColor", stringSchema());
        changeProperties.add("glassOpacity", numberSchema(0.0, 255.0));
        changeProperties.add("glassGlow", booleanSchema());
        changeProperties.add("glowStrength", numberSchema(0.25, 2.0));
        changeProperties.add("shadow", booleanSchema());
        changeProperties.add("blur", booleanSchema());
        JsonObject elements = objectSchema();
        JsonObject elementProperties = new JsonObject();

        for (String id : new String[]{
            "watermark", "notifications", "target", "hotkeys", "potions", "cooldowns", "mediaPlayer", "inventory", "armorHud", "customHotbar"
        }) {
            elementProperties.add(id, booleanSchema());
        }

        elements.add("properties", elementProperties);
        changeProperties.add("elements", elements);
        JsonObject layout = new JsonObject();
        layout.addProperty("type", "array");
        JsonObject placement = objectSchema();
        JsonObject placementProperties = new JsonObject();
        placementProperties.add(
            "id", enumSchema("watermark", "notifications", "target", "hotkeys", "potions", "cooldowns", "mediaPlayer", "inventory", "armorHud")
        );
        placementProperties.add("x", numberSchema(-1920.0, 1920.0));
        placementProperties.add("y", numberSchema(-1080.0, 1080.0));
        placementProperties.add("scale", numberSchema(0.6, 1.6));
        placement.add("properties", placementProperties);
        JsonArray placementRequired = new JsonArray();
        placementRequired.add("id");
        placement.add("required", placementRequired);
        layout.add("items", placement);
        changeProperties.add("layout", layout);
        JsonObject elementStyles = new JsonObject();
        elementStyles.addProperty("type", "array");
        elementStyles.add("items", elementStyleSchema());
        changeProperties.add("elementStyles", elementStyles);
        changes.add("properties", changeProperties);
        properties.add("changes", changes);
        root.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("message");
        root.add("required", required);
        return root;
    }

    static JsonObject elementStyleSchema() {
        JsonObject style = objectSchema();
        JsonObject properties = new JsonObject();
        properties.add(
            "id", enumSchema("watermark", "notifications", "target", "hotkeys", "potions", "cooldowns", "mediaPlayer", "inventory", "armorHud", "customHotbar")
        );
        properties.add("material", enumSchema("Inherit", "Classic", "Liquid Glass"));
        properties.add("colors", groupSchema(new String[]{"panel", "panel2", "text", "accent"}, "color"));
        JsonObject panel = objectSchema();
        JsonObject panelProperties = new JsonObject();
        panelProperties.add("opacity", numberSchema(0.0, 255.0));
        panelProperties.add("radius", numberSchema(0.0, 28.0));
        panel.add("properties", panelProperties);
        properties.add("panel", panel);
        JsonObject icon = objectSchema();
        JsonObject iconProperties = new JsonObject();
        iconProperties.add(
            "name",
            enumSchema(
                "logo",
                "search",
                "list",
                "star",
                "command",
                "command_large",
                "grid",
                "diamond",
                "command_small",
                "person",
                "monitor",
                "ping",
                "keyboard",
                "flask",
                "clock"
            )
        );
        iconProperties.add("scale", numberSchema(0.55, 1.8));
        icon.add("properties", iconProperties);
        properties.add("icon", icon);
        properties.add("shadow", effectSchema(true, 0.25, 2.5));
        properties.add("blur", effectSchema(false, 0.25, 2.5));
        properties.add("glow", effectSchema(true, 0.25, 3.0));
        properties.add("outline", effectSchema(true, 0.5, 3.0));
        JsonObject stripe = effectSchema(true, 1.0, 8.0);
        stripe.getAsJsonObject("properties").add("side", enumSchema("left", "right", "top", "bottom"));
        properties.add("stripe", stripe);
        JsonObject label = objectSchema();
        JsonObject labelProperties = new JsonObject();
        labelProperties.add("text", stringSchema());
        labelProperties.add("color", stringSchema());
        label.add("properties", labelProperties);
        properties.add("label", label);
        style.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("id");
        style.add("required", required);
        return style;
    }

    static JsonObject groupSchema(String[] names, String kind) {
        JsonObject group = objectSchema();
        JsonObject properties = new JsonObject();

        for (String name : names) {
            properties.add(name, stringSchema());
        }

        group.add("properties", properties);
        return group;
    }

    static JsonObject effectSchema(boolean color, double minimum, double maximum) {
        JsonObject effect = objectSchema();
        JsonObject properties = new JsonObject();
        properties.add("enabled", booleanSchema());
        if (color) {
            properties.add("color", stringSchema());
        }

        properties.add(color && minimum >= 0.5 ? "width" : "strength", numberSchema(minimum, maximum));
        effect.add("properties", properties);
        return effect;
    }

    static JsonObject objectSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    static JsonObject stringSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "string");
        return schema;
    }

    static JsonObject booleanSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "boolean");
        return schema;
    }

    static JsonObject numberSchema(double minimum, double maximum) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "number");
        schema.addProperty("minimum", minimum);
        schema.addProperty("maximum", maximum);
        return schema;
    }

    static JsonObject enumSchema(String... values) {
        JsonObject schema = stringSchema();
        JsonArray options = new JsonArray();

        for (String value : values) {
            options.add(value);
        }

        schema.add("enum", options);
        return schema;
    }

    static String loadApiKey() {
        String key = System.getProperty("auravisuals.geminiApiKey", "").strip();
        if (!key.isEmpty()) {
            return key;
        }

        key = System.getenv().getOrDefault("AURA_GEMINI_API_KEY", "").strip();
        if (!key.isEmpty()) {
            return key;
        }

        try {
            File file = FabricLoader.getInstance().getGameDir().resolve("bamber client").resolve("gemini-api-key.txt").toFile();
            if (file.isFile()) {
                return Files.readString(file.toPath(), StandardCharsets.UTF_8).strip();
            }
        } catch (Exception var2) {
        }

        return "";
    }
}
