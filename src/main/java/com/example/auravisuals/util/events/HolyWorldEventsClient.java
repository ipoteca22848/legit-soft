package com.example.auravisuals.util.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;

public final class HolyWorldEventsClient {
    static final String API = "https://api.holyworld.me/v1/";
    static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15L)).followRedirects(Redirect.NORMAL).build();
    static final ExecutorService ASYNC = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "bamber client-HolyWorldAPI");
        thread.setDaemon(true);
        return thread;
    });
    static final AtomicBoolean LOADING = new AtomicBoolean(false);
    static final Object LOCK = new Object();
    static final Pattern SERVER_NUMBER = Pattern.compile("#(\\d+)$");
    static List<HolyWorldEventsClient$HolyWorldEvent> events = new ArrayList<>();
    static Map<String, String> servers = new HashMap<>();
    static long serversFetchedAt;
    static volatile String lastError = "";

    HolyWorldEventsClient() {
    }

    public static List<HolyWorldEventsClient$HolyWorldEvent> getEvents() {
        synchronized (LOCK) {
            return Collections.unmodifiableList(new ArrayList<>(events));
        }
    }

    public static boolean isLoading() {
        return LOADING.get();
    }

    public static String getLastError() {
        return lastError;
    }

    public static void refreshAsync(Runnable onDone) {
        if (LOADING.compareAndSet(false, true)) {
            ASYNC.submit(() -> {
                try {
                    Map<String, String> serverNames = getServers();
                    JsonObject payload = request("events");
                    List<HolyWorldEventsClient$HolyWorldEvent> loaded = new ArrayList<>();

                    for (Entry<String, JsonElement> entry : payload.entrySet()) {
                        if (entry.getValue().isJsonArray()) {
                            for (JsonElement element : entry.getValue().getAsJsonArray()) {
                                if (element.isJsonObject()) {
                                    loaded.add(parse(entry.getKey(), serverNames, element.getAsJsonObject()));
                                }
                            }
                        } else if (entry.getValue().isJsonObject()) {
                            loaded.add(parse(entry.getKey(), serverNames, entry.getValue().getAsJsonObject()));
                        }
                    }

                    loaded.sort(Comparator.comparingInt(HolyWorldEventsClient$HolyWorldEvent::serverNumber).thenComparing(event -> event.serverName));
                    synchronized (LOCK) {
                        events = loaded;
                        lastError = "";
                    }
                } catch (Exception var15) {
                    lastError = var15.getMessage() == null ? var15.getClass().getSimpleName() : var15.getMessage();
                } finally {
                    LOADING.set(false);
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (onDone != null) {
                        if (client != null) {
                            client.execute(onDone);
                        } else {
                            onDone.run();
                        }
                    }
                }
            });
        }
    }

    static Map<String, String> getServers() throws Exception {
        if (!servers.isEmpty() && System.currentTimeMillis() - serversFetchedAt < 600000L) {
            return servers;
        }

        JsonObject payload = request("servers");
        Map<String, String> loaded = new HashMap<>();

        for (Entry<String, JsonElement> entry : payload.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                loaded.put(entry.getKey(), entry.getValue().getAsString());
            }
        }

        servers = loaded;
        serversFetchedAt = System.currentTimeMillis();
        return loaded;
    }

    static JsonObject request(String method) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.holyworld.me/v1/" + method)).timeout(Duration.ofSeconds(25L)).GET().build();
        HttpResponse<String> response = HTTP.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException("HolyWorld API: HTTP " + response.statusCode());
        } else {
            return JsonParser.parseString(response.body()).getAsJsonObject();
        }
    }

    static HolyWorldEventsClient$HolyWorldEvent parse(String serverId, Map<String, String> names, JsonObject object) {
        String id = string(object, "id", "EVENT");
        JsonObject metadata = object.has("metadata") && object.get("metadata").isJsonObject() ? object.getAsJsonObject("metadata") : new JsonObject();
        String name = string(metadata, "displayName", translateEvent(id));
        String rarityRaw = string(metadata, "rare", "default");
        return new HolyWorldEventsClient$HolyWorldEvent(serverId, names.getOrDefault(serverId, serverId), id, name, translateRarity(rarityRaw), rarityRaw);
    }

    static String string(JsonObject object, String key, String fallback) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
        } catch (Exception var4) {
            return fallback;
        }
    }

    static String translateEvent(String id) {
        String var1 = id.toUpperCase(Locale.ROOT);

        return switch (var1) {
            case "CUBE" -> "Кубик";
            case "GOLDEN_FORTRESS" -> "Золотая лихорадка";
            case "CARGO" -> "Ценный груз";
            case "CONTAINER" -> "Контейнер";
            case "JAYCOB" -> "Опытный Тыпо";
            case "PARCELS" -> "Посылка";
            case "SHIP" -> "Таинственный корабль";
            case "MINE" -> "Смертельная шахта";
            case "SNOWQUARRY" -> "Цветочная поляна";
            default -> id.replace('_', ' ');
        };
    }

    static String translateRarity(String raw) {
        String value = raw == null ? "default" : raw.toLowerCase(Locale.ROOT);
        if (value.startsWith("legendary_")) {
            return "Легендарная";
        }

        if (value.startsWith("rare_")) {
            return "Редкая";
        }

        if (value.startsWith("normal_")) {
            return "Обычная";
        }

        return switch (value) {
            case "default", "normal", "обычный", "ship_default", "ship_default_f" -> "Обычная";
            case "epic", "эпический" -> "Эпическая";
            case "rare" -> "Редкая";
            case "legendary", "легендарный" -> "Легендарная";
            case "peaceful" -> "Мирная";
            case "deadly" -> "Смертельная";
            case "explosive" -> "Взрывная";
            case "ship_roskoshni_f" -> "Роскошная";
            case "ship_zajit_f" -> "Зажиточная";
            default -> raw == null ? "Обычная" : raw.replace('_', ' ');
        };
    }
}
