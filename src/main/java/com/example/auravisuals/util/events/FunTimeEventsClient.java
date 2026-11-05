package com.example.auravisuals.util.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;

public final class FunTimeEventsClient {
    static final String DEFAULT_API_TOKEN = "a71de3a.1d53638e29be5bd55b0852059514fbb3";
    static final String HARDCODED_API_ORIGIN = "https://api.funtime.su";
    static final String FIXED_API_BASE = normalizeApiBaseUrl("https://api.funtime.su");
    static final String EVENT_TYPE_FILTER = "all";
    static final Pattern ANARCHY_NUM = Pattern.compile("anarchy(\\d+)", 2);
    static final Pattern MINE_ID_NUM = Pattern.compile("mine[_-]?(\\d+)", 2);
    static final int MAX_SERVERS_PER_REQUEST = 30;
    static final HttpClient HTTP = HttpClient.newBuilder()
        .version(Version.HTTP_1_1)
        .followRedirects(Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(15L))
        .build();
    static final ExecutorService ASYNC = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "bamber client-FunTimeAPI");
        t.setDaemon(true);
        return t;
    });
    static final Object LOCK = new Object();
    static final AtomicBoolean FETCH_IN_FLIGHT = new AtomicBoolean(false);
    static final AtomicBoolean FETCH_MINES_IN_FLIGHT = new AtomicBoolean(false);
    static List<FunTimeEventsClient$FunTimeEvent> currentEvents = new ArrayList<>();
    static List<FunTimeEventsClient$FunTimeMine> currentMines = new ArrayList<>();
    static volatile String lastError = "";
    static volatile String lastMineError = "";
    static volatile long lastFetchTimeMs = System.currentTimeMillis();
    static volatile List<String> cachedServers = List.of();
    static volatile long cachedServersAtMs = 0L;
    public static final Set<String> PRIORITY_GAMEPLAY_EVENT_IDS = Set.of("beacon", "vote", "meteor_rain", "vulcan", "vulkan", "epstein");
    static final Set<String> HIDDEN_EVENT_IDS = Set.of("airdrop");
    static final Comparator<FunTimeEventsClient$FunTimeEvent> EVENT_SORT = (a, b) -> {
        int ga = sortGroup(a);
        int gb = sortGroup(b);
        return ga != gb ? Integer.compare(ga, gb) : Integer.compare(a.getRemainingSeconds(), b.getRemainingSeconds());
    };
    static final Comparator<FunTimeEventsClient$FunTimeMine> MINE_SORT = Comparator.<FunTimeEventsClient$FunTimeMine>comparingInt(
            mine -> mine.nextMineRarity.listGroupOrder()
        )
        .thenComparingInt(FunTimeEventsClient$FunTimeMine::getRemainingSeconds);

    FunTimeEventsClient() {
    }

    public static List<FunTimeEventsClient$FunTimeEvent> getEvents() {
        synchronized (LOCK) {
            return Collections.unmodifiableList(currentEvents.stream().filter(FunTimeEventsClient$FunTimeEvent::shouldDisplay).toList());
        }
    }

    public static String getLastError() {
        return lastError;
    }

    public static boolean isEventsLoading() {
        return FETCH_IN_FLIGHT.get();
    }

    public static long getLastFetchTimeMs() {
        return lastFetchTimeMs;
    }

    public static List<FunTimeEventsClient$FunTimeMine> getMines() {
        synchronized (LOCK) {
            return Collections.unmodifiableList(currentMines.stream().filter(mine -> mine.getRemainingSeconds() > 0).toList());
        }
    }

    public static String getLastMineError() {
        return lastMineError;
    }

    public static boolean isMinesLoading() {
        return FETCH_MINES_IN_FLIGHT.get();
    }

    public static String getApiToken() {
        return System.getProperty("auravisuals.funtimeApiToken", "a71de3a.1d53638e29be5bd55b0852059514fbb3").trim();
    }

    public static String getApiBaseUrl() {
        return FIXED_API_BASE;
    }

    static String normalizeApiBaseUrl(String raw) {
        if (raw != null && !raw.isEmpty()) {
            String u = raw.trim();

            while (u.endsWith("/")) {
                u = u.substring(0, u.length() - 1);
            }

            if (!u.toLowerCase(Locale.ROOT).endsWith("/method")) {
                u = u + "/method";
            }

            return u + "/";
        } else {
            return "https://api.funtime.su/method/";
        }
    }

    public static String getEventTypeFilter() {
        return "all";
    }

    public static void refreshAsync(Runnable onDone) {
        if (!FETCH_IN_FLIGHT.compareAndSet(false, true)) {
            finishOnClient(onDone);
        } else {
            ASYNC.submit(() -> {
                try {
                    String token = getApiToken();
                    String eventType = getEventTypeFilter();
                    List<String> servers = fetchServers(token);
                    if (servers.isEmpty()) {
                        throw new RuntimeException("Список серверов пуст");
                    }

                    List<FunTimeEventsClient$FunTimeEvent> merged = new ArrayList<>();

                    for (int i = 0; i < servers.size(); i += 30) {
                        int end = Math.min(i + 30, servers.size());
                        merged.addAll(fetchEventsForServers(token, eventType, String.join(",", servers.subList(i, end))));
                        List<FunTimeEventsClient$FunTimeEvent> partial = dedupeEvents(merged);
                        partial.sort(EVENT_SORT);
                        synchronized (LOCK) {
                            currentEvents = partial;
                            lastError = "";
                            lastFetchTimeMs = System.currentTimeMillis();
                        }
                    }

                    merged = dedupeEvents(merged);
                    merged.sort(EVENT_SORT);
                    synchronized (LOCK) {
                        currentEvents = merged;
                        lastError = "";
                        lastFetchTimeMs = System.currentTimeMillis();
                    }
                } catch (Exception var18) {
                    lastError = var18.getMessage() != null ? var18.getMessage() : var18.getClass().getSimpleName();
                } finally {
                    FETCH_IN_FLIGHT.set(false);
                    finishOnClient(onDone);
                }
            });
        }
    }

    static int sortGroup(FunTimeEventsClient$FunTimeEvent e) {
        return e.isDoIEvintaTitle() ? 1 : 0;
    }

    static String normalizeEventId(String id) {
        if (id == null) {
            return null;
        }

        String t = id.trim();
        return !t.isEmpty() && !"null".equalsIgnoreCase(t) ? t.toLowerCase(Locale.ROOT) : null;
    }

    static String canonicalEventIdKey(String id) {
        String n = normalizeEventId(id);
        if (n == null) {
            return null;
        }

        int colon = n.lastIndexOf(58);
        if (colon >= 0) {
            n = n.substring(colon + 1).trim();
        }

        if (n.isEmpty()) {
            return null;
        }

        n = n.replace('-', '_');
        return n.toLowerCase(Locale.ROOT);
    }

    static List<FunTimeEventsClient$FunTimeEvent> dedupeEvents(List<FunTimeEventsClient$FunTimeEvent> list) {
        Map<String, FunTimeEventsClient$FunTimeEvent> map = new LinkedHashMap<>();

        for (FunTimeEventsClient$FunTimeEvent e : list) {
            String key = dedupeKey(e);
            FunTimeEventsClient$FunTimeEvent prev = map.get(key);
            if (prev == null || e.timeSecondsLeft < prev.timeSecondsLeft) {
                map.put(key, e);
            }
        }

        return new ArrayList<>(map.values());
    }

    static String dedupeKey(FunTimeEventsClient$FunTimeEvent e) {
        String sid = e.server != null ? e.server : "";
        String et = e.eventType != null ? e.eventType.toLowerCase(Locale.ROOT) : "";
        String id = e.id != null ? e.id.trim().toLowerCase(Locale.ROOT) : "";
        String ph = e.phase != null ? e.phase.trim().toUpperCase(Locale.ROOT) : "";
        return sid + "\u0000" + et + "\u0000" + id + "\u0000" + ph + "\u0000" + e.x + "\u0000" + e.y + "\u0000" + e.z;
    }

    static int parseAnarchyNumber(String server) {
        Matcher m = ANARCHY_NUM.matcher(server == null ? "" : server);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException var3) {
                return -1;
            }
        } else {
            return -1;
        }
    }

    public static String formatServerDisplay(String server) {
        if (server != null && !server.isEmpty()) {
            Matcher m = ANARCHY_NUM.matcher(server);
            return m.find() ? "Анархия " + m.group(1) : server;
        } else {
            return "?";
        }
    }

    static String formatDurationHms(int sec) {
        if (sec < 0) {
            sec = 0;
        }

        int h = sec / 3600;
        int m = sec % 3600 / 60;
        int s = sec % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
    }

    static String translateEventId(String id) {
        if (id != null && !id.isEmpty() && !"null".equalsIgnoreCase(id)) {
            String k = canonicalEventIdKey(id);
            if (k == null) {
                return id;
            }

            return switch (k) {
                case "vulcan", "vulkan" -> "Вулкан";
                case "vote" -> "Голосование";
                case "meteor_rain" -> "Метеоритный дождь";
                case "myst_beacon", "mystic" -> "Призывной маяк";
                case "beacon" -> "Маяк";
                case "epstein" -> "Эпштейн";
                case "geyser" -> "Гейзер";
                case "helm", "hellm" -> "Адская резня";
                case "deathchest" -> "Сундук смерти";
                case "deathmatch" -> "Дезматч";
                case "airdrop" -> "Аирдроп";
                case "altarundead" -> "Алтарь нежити";
                default -> id;
            };
        } else {
            return id;
        }
    }

    public static boolean isHiddenEventId(String id) {
        String key = canonicalEventIdKey(id);
        return key != null && HIDDEN_EVENT_IDS.contains(key);
    }

    static String translatePhase(String phase) {
        if (phase != null && !phase.isEmpty()) {
            String u = phase.trim().toUpperCase(Locale.ROOT);

            return switch (u) {
                case "STARTING" -> "Скоро начнётся";
                case "RUNNING" -> "Активен";
                case "VOTING" -> "Голосование";
                case "OPENED" -> "Открыт";
                case "ACTIVATING" -> "Активируется";
                case "LOOTING" -> "Награда доступна";
                case "WAITING" -> "Ожидает запуска";
                case "ENDING" -> "Завершается";
                case "FINISHED" -> "Завершён";
                case "CLOSED" -> "Закрыт";
                default -> phase;
            };
        } else {
            return "";
        }
    }

    static String translateLoot(String loot) {
        if (loot != null && !loot.isBlank() && !"null".equalsIgnoreCase(loot)) {
            String var1 = loot.trim().toLowerCase(Locale.ROOT);

            return switch (var1) {
                case "default", "normal", "common" -> "Обычная";
                case "mythical", "mythic", "mystic" -> "Мифическая";
                case "legendary", "legend" -> "Легендарная";
                case "elite" -> "Элитная";
                case "rich" -> "Богатая";
                default -> loot;
            };
        } else {
            return "";
        }
    }

    static void finishOnClient(Runnable onDone) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c != null) {
            c.execute(() -> {
                if (onDone != null) {
                    onDone.run();
                }
            });
        } else if (onDone != null) {
            onDone.run();
        }
    }

    static List<String> fetchServers(String token) throws Exception {
        long now = System.currentTimeMillis();
        List<String> cached = cachedServers;
        if (!cached.isEmpty() && now - cachedServersAtMs < 600000L) {
            return cached;
        }

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(getApiBaseUrl() + "servers-info"))
            .header("Authorization-Token", token)
            .GET()
            .timeout(Duration.ofSeconds(30L))
            .build();
        HttpResponse<String> res = HTTP.send(req, BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() != 200) {
            throw new RuntimeException("servers-info: HTTP " + res.statusCode());
        }

        JsonObject root = JsonParser.parseString(res.body()).getAsJsonObject();
        JsonArray arr = root.getAsJsonArray("response");
        List<String> out = new ArrayList<>();
        if (arr != null) {
            for (JsonElement e : arr) {
                if (e != null && e.isJsonPrimitive()) {
                    out.add(e.getAsString());
                }
            }
        }

        cachedServers = List.copyOf(out);
        cachedServersAtMs = now;
        return out;
    }

    static List<FunTimeEventsClient$FunTimeEvent> fetchEventsForServers(String token, String eventType, String serverTypeCsv) throws Exception {
        String qEvent = URLEncoder.encode(eventType, StandardCharsets.UTF_8);
        String qServer = URLEncoder.encode(serverTypeCsv, StandardCharsets.UTF_8);
        String url = getApiBaseUrl() + "events-info?event-type=" + qEvent + "&server-type=" + qServer;
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).header("Authorization-Token", token).GET().timeout(Duration.ofSeconds(40L)).build();
        HttpResponse<String> res = HTTP.send(req, BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() == 401) {
            throw new RuntimeException("Неверный или просроченный токен (401)");
        } else if (res.statusCode() == 402) {
            throw new RuntimeException("Лимит запросов API (402)");
        } else if (res.statusCode() != 200) {
            throw new RuntimeException("events-info: HTTP " + res.statusCode());
        } else {
            return parseEventsResponse(res.body());
        }
    }

    public static void refreshMinesAsync(Runnable onDone) {
        if (!FETCH_MINES_IN_FLIGHT.compareAndSet(false, true)) {
            finishOnClient(onDone);
        } else {
            ASYNC.submit(() -> {
                try {
                    String token = getApiToken();
                    List<FunTimeEventsClient$FunTimeMine> merged = fetchMinesForServers(token, "all");
                    merged.sort(MINE_SORT);
                    synchronized (LOCK) {
                        currentMines = merged;
                        lastMineError = "";
                        lastFetchTimeMs = System.currentTimeMillis();
                    }
                } catch (Exception var13) {
                    Exception e = var13;
                    synchronized (LOCK) {
                        lastMineError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    }
                } finally {
                    FETCH_MINES_IN_FLIGHT.set(false);
                    finishOnClient(onDone);
                }
            });
        }
    }

    static List<FunTimeEventsClient$FunTimeMine> fetchMinesForServers(String token, String serverTypeCsv) throws Exception {
        String qServer = URLEncoder.encode(serverTypeCsv, StandardCharsets.UTF_8);
        String url = getApiBaseUrl() + "mines-info?server-types=" + qServer;
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).header("Authorization-Token", token).GET().timeout(Duration.ofSeconds(25L)).build();
        HttpResponse<String> res = HTTP.send(req, BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() == 401) {
            throw new RuntimeException("Неверный или просроченный токен (401)");
        } else if (res.statusCode() == 402) {
            throw new RuntimeException("Лимит запросов API (402)");
        } else if (res.statusCode() != 200) {
            throw new RuntimeException("mines-info: HTTP " + res.statusCode());
        } else {
            return parseMinesResponse(res.body());
        }
    }

    static List<FunTimeEventsClient$FunTimeMine> parseMinesResponse(String json) {
        List<FunTimeEventsClient$FunTimeMine> list = new ArrayList<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (root.has("success") && !root.get("success").isJsonNull() && root.get("success").isJsonPrimitive()) {
            try {
                if (!root.get("success").getAsBoolean()) {
                    String em = getString(root, "error-message");
                    throw new RuntimeException(em != null ? em : "mines-info: API вернул ошибку");
                }
            } catch (IllegalStateException | ClassCastException var12) {
            }
        }

        JsonObject serversObj = root.getAsJsonObject("servers");
        if (serversObj != null) {
            for (Entry<String, JsonElement> e : serversObj.entrySet()) {
                String server = e.getKey();
                if (server != null && !server.isEmpty()) {
                    JsonElement arrEl = e.getValue();
                    if (arrEl != null && arrEl.isJsonArray()) {
                        for (JsonElement mEl : arrEl.getAsJsonArray()) {
                            if (mEl.isJsonObject()) {
                                list.add(FunTimeEventsClient$FunTimeMine.fromApi(server, mEl.getAsJsonObject()));
                            }
                        }
                    }
                }
            }

            return list;
        } else {
            JsonArray response = root.getAsJsonArray("response");
            if (response == null) {
                return list;
            }

            for (JsonElement serverEl : response) {
                if (serverEl.isJsonObject()) {
                    JsonObject block = serverEl.getAsJsonObject();
                    String server = getString(block, "server");
                    if (server != null) {
                        JsonArray mines = block.getAsJsonArray("mines");
                        if (mines == null) {
                            mines = block.getAsJsonArray("events");
                        }

                        if (mines != null) {
                            for (JsonElement mElx : mines) {
                                if (mElx.isJsonObject()) {
                                    list.add(FunTimeEventsClient$FunTimeMine.fromApi(server, mElx.getAsJsonObject()));
                                }
                            }
                        }
                    }
                }
            }

            return list;
        }
    }

    static List<FunTimeEventsClient$FunTimeEvent> parseEventsResponse(String json) {
        List<FunTimeEventsClient$FunTimeEvent> list = new ArrayList<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray response = root.getAsJsonArray("response");
        if (response == null) {
            return list;
        }

        for (JsonElement serverEl : response) {
            if (serverEl.isJsonObject()) {
                JsonObject block = serverEl.getAsJsonObject();
                String server = getString(block, "server");
                if (server != null) {
                    JsonArray events = block.getAsJsonArray("events");
                    if (events != null) {
                        for (JsonElement evEl : events) {
                            if (evEl.isJsonObject()) {
                                JsonObject o = evEl.getAsJsonObject();
                                String et = getString(o, "event-type");
                                if (et == null) {
                                    et = "unknown";
                                }

                                int timeLeft = getInt(o, "time-seconds-left", 0);
                                String id = getString(o, "id");
                                if (!isHiddenEventId(id)) {
                                    String phase = getString(o, "phase");
                                    String loot = getString(o, "loot");
                                    boolean locationAnnounced = getBoolean(o, "location-announced", false);
                                    JsonObject location = o.has("location-event") && o.get("location-event").isJsonObject()
                                        ? o.getAsJsonObject("location-event")
                                        : null;
                                    Integer x = location != null ? getNullableInt(location, "x") : null;
                                    Integer y = location != null ? getNullableInt(location, "y") : null;
                                    Integer z = location != null ? getNullableInt(location, "z") : null;
                                    list.add(FunTimeEventsClient$FunTimeEvent.fromApi(server, et, id, timeLeft, phase, loot, locationAnnounced, x, y, z));
                                }
                            }
                        }
                    }
                }
            }
        }

        return list;
    }

    static String getString(JsonObject o, String key) {
        if (o.has(key) && !o.get(key).isJsonNull()) {
            try {
                return o.get(key).getAsString();
            } catch (Exception var3) {
                return null;
            }
        } else {
            return null;
        }
    }

    static String getMineRarityDisplayRaw(JsonObject o) {
        String r = getString(o, "next-mine-rarity");
        if (r != null && !r.isBlank()) {
            return r.trim();
        }

        r = getString(o, "nextMineRarity");
        if (r != null && !r.isBlank()) {
            return r.trim();
        }

        r = getString(o, "next_mine_rarity");
        if (r != null && !r.isBlank()) {
            return r.trim();
        }

        r = getString(o, "mine-rarity");
        if (r != null && !r.isBlank()) {
            return r.trim();
        }

        r = getString(o, "mineRarity");
        if (r != null && !r.isBlank()) {
            return r.trim();
        }

        r = getString(o, "mine_rarity");
        return r != null && !r.isBlank() ? r.trim() : null;
    }

    static int getInt(JsonObject o, String key, int def) {
        if (o.has(key) && !o.get(key).isJsonNull()) {
            try {
                return o.get(key).getAsInt();
            } catch (Exception var4) {
                return def;
            }
        } else {
            return def;
        }
    }

    static Integer getNullableInt(JsonObject o, String key) {
        if (o.has(key) && !o.get(key).isJsonNull()) {
            try {
                return o.get(key).getAsInt();
            } catch (Exception var3) {
                return null;
            }
        } else {
            return null;
        }
    }

    static boolean getBoolean(JsonObject o, String key, boolean def) {
        if (o.has(key) && !o.get(key).isJsonNull()) {
            try {
                return o.get(key).getAsBoolean();
            } catch (Exception var4) {
                return def;
            }
        } else {
            return def;
        }
    }

    static List<FunTimeEventsClient$FunTimeEvent> generateScheduleEvents() {
        List<FunTimeEventsClient$FunTimeEvent> list = new ArrayList<>();
        long nowSec = System.currentTimeMillis() / 1000L;
        int[][] data = new int[][]{
            {501, 1, 64, 1, 1},
            {210, 2, 85, 1, 2},
            {213, 3, 362, 0, 3},
            {106, 4, 49, 1, 4},
            {504, 3, 318, 0, 3},
            {103, 2, 92, 1, 2},
            {901, 1, 63, 1, 5},
            {901, 5, 77, 1, 2},
            {101, 6, 135, 1, 1},
            {102, 7, 270, 0, 1},
            {105, 8, 190, 0, 5},
            {204, 1, 74, 1, 1},
            {301, 2, 110, 1, 2},
            {401, 4, 145, 0, 4},
            {502, 3, 410, 0, 3},
            {601, 1, 95, 1, 1},
            {701, 2, 80, 1, 2},
            {801, 6, 215, 0, 1},
            {902, 5, 88, 1, 2}
        };
        String[] names = new String[]{"", "Метеоритный дождь", "Айрдроп", "Адская резня", "Вулкан", "Алтарь мертвецов", "Мистик", "Эпштейн", "Сундук смерти"};
        String[] rarities = new String[]{"", "Легенда", "Элитный", "Солидный", "Солидный", "Богатый"};

        for (int[] d : data) {
            int anarchyId = d[0];
            int nameIdx = d[1];
            int baseSec = d[2];
            boolean isRunning = d[3] == 1;
            int rarIdx = d[4];
            int timeCycle = baseSec + 600;
            int timeOffset = (int)((nowSec + anarchyId * 13L) % timeCycle);
            int timeLeft = Math.max(12, timeCycle - timeOffset);
            if (isRunning) {
                timeLeft = Math.min(timeLeft, 240);
            }

            String server = "anarchy" + anarchyId;
            String name = names[nameIdx];
            String rar = rarities[rarIdx];
            String phase = isRunning ? "RUNNING" : "WAITING";
            String status = isRunning ? "active" : "upcoming";
            list.add(new FunTimeEventsClient$FunTimeEvent(server, "gameplay", "ev_" + anarchyId, timeLeft, phase, "", name, rar, anarchyId, status));
        }

        return list;
    }

    static List<FunTimeEventsClient$FunTimeMine> generateScheduleMines() {
        List<FunTimeEventsClient$FunTimeMine> list = new ArrayList<>();
        long nowSec = System.currentTimeMillis() / 1000L;
        int[] anarchies = new int[]{101, 102, 103, 104, 105, 106, 201, 204, 210, 213, 301, 401, 501, 502, 504, 601, 701, 801, 901, 902};

        for (int i = 0; i < anarchies.length; i++) {
            int aid = anarchies[i];
            FunTimeEventsClient$MineRarityKind rar = i % 3 == 0
                ? FunTimeEventsClient$MineRarityKind.LEGENDARY
                : (i % 3 == 1 ? FunTimeEventsClient$MineRarityKind.MYTHICAL : FunTimeEventsClient$MineRarityKind.NORMAL);
            int cycle = 900;
            int timeOffset = (int)((nowSec + aid * 23L) % cycle);
            int timeLeft = Math.max(10, cycle - timeOffset);
            String title = rar == FunTimeEventsClient$MineRarityKind.LEGENDARY
                ? "Легендарная шахта"
                : (rar == FunTimeEventsClient$MineRarityKind.MYTHICAL ? "Мифическая шахта" : "Обычная шахта");
            list.add(new FunTimeEventsClient$FunTimeMine("anarchy" + aid, "mine_" + aid, timeLeft, "ACTIVE", title, aid, rar));
        }

        return list;
    }
}
