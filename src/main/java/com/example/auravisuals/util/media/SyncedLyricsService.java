package com.example.auravisuals.util.media;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SyncedLyricsService {
    static final SyncedLyricsService INSTANCE = new SyncedLyricsService();
    static final Pattern TIMESTAMP = Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]");
    static final Pattern WORD_TIMESTAMP = Pattern.compile("<(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?>");
    static final Pattern OFFSET = Pattern.compile("\\[offset:([+-]?\\d+)]", 2);
    static final Pattern SEARCH_RESULT = Pattern.compile("<a\\s+href=\"([^\"]+)\"[^>]*>[\\s\\S]*?<span\\s+class=\"result-name\">([\\s\\S]*?)</span>", 2);
    static final Pattern LYRICS_BLOCK = Pattern.compile("<p\\s+class=\"lyrics\"[^>]*>([\\s\\S]*?)</p>", 2);
    static final SyncedLyricsService$LyricsTrack LOADING = new SyncedLyricsService$LyricsTrack(List.of(), SyncedLyricsService$State.LOADING, false);
    static final SyncedLyricsService$LyricsTrack NOT_FOUND = new SyncedLyricsService$LyricsTrack(List.of(), SyncedLyricsService$State.NOT_FOUND, false);
    final Map<String, SyncedLyricsService$LyricsTrack> cache = new ConcurrentHashMap<>();
    final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "bamber client-Lyrics");
        thread.setDaemon(true);
        thread.setPriority(1);
        return thread;
    });
    final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4L)).followRedirects(Redirect.NORMAL).build();
    volatile String activeKey = "";
    volatile SyncedLyricsService$LyricsTrack activeTrack = NOT_FOUND;

    SyncedLyricsService() {
    }

    public static SyncedLyricsService getInstance() {
        return INSTANCE;
    }

    public SyncedLyricsService$LyricsTrack request(String title, String artist, long durationMs) {
        String cleanTitle = clean(title);
        String cleanArtist = clean(artist);
        if (cleanTitle.isEmpty()) {
            this.activeKey = "";
            this.activeTrack = NOT_FOUND;
            return this.activeTrack;
        } else {
            String key = normalize(cleanArtist) + "|" + normalize(cleanTitle) + "|" + Math.max(0L, durationMs / 1000L);
            if (key.equals(this.activeKey)) {
                return this.activeTrack;
            } else {
                this.activeKey = key;
                SyncedLyricsService$LyricsTrack cached = this.cache.get(key);
                if (cached != null) {
                    this.activeTrack = cached;
                    return cached;
                } else {
                    this.activeTrack = LOADING;
                    this.executor.execute(() -> this.load(key, cleanTitle, cleanArtist, durationMs));
                    return this.activeTrack;
                }
            }
        }
    }

    public SyncedLyricsService$LyricsTrack getActiveTrack() {
        return this.activeTrack;
    }

    void load(String key, String title, String artist, long durationMs) {
        SyncedLyricsService$LyricsTrack result = NOT_FOUND;

        try {
            JsonObject exact = this.fetchExact(title, artist, durationMs);
            if (exact != null) {
                result = this.parseResponse(exact);
            }

            if (result.lines().isEmpty() || result.estimated()) {
                JsonObject searched = this.fetchSearch(title, artist, durationMs);
                if (searched != null) {
                    SyncedLyricsService$LyricsTrack searchedTrack = this.parseResponse(searched);
                    if (!searchedTrack.lines().isEmpty() && (!searchedTrack.estimated() || result.lines().isEmpty())) {
                        result = searchedTrack;
                    }
                }
            }

            if (result.lines().isEmpty()) {
                result = this.fetchPlainLyricsFallback(title, artist, durationMs);
            }
        } catch (Exception var10) {
            result = NOT_FOUND;
        }

        if (this.cache.size() >= 64) {
            this.cache.clear();
        }

        this.cache.put(key, result);
        if (key.equals(this.activeKey)) {
            this.activeTrack = result;
        }
    }

    JsonObject fetchExact(String title, String artist, long durationMs) throws Exception {
        if (!artist.isEmpty() && durationMs > 0L) {
            String url = "https://lrclib.net/api/get?track_name="
                + encode(title)
                + "&artist_name="
                + encode(artist)
                + "&duration="
                + Math.max(1L, Math.round(durationMs / 1000.0));
            JsonElement root = this.fetch(url);
            return root != null && root.isJsonObject() ? root.getAsJsonObject() : null;
        } else {
            return null;
        }
    }

    JsonObject fetchSearch(String title, String artist, long durationMs) throws Exception {
        String url = "https://lrclib.net/api/search?track_name=" + encode(title) + (artist.isEmpty() ? "" : "&artist_name=" + encode(artist));
        JsonElement root = this.fetch(url);
        if (root != null && root.isJsonArray()) {
            double targetDuration = durationMs / 1000.0;
            return root.getAsJsonArray()
                .asList()
                .stream()
                .filter(JsonElement::isJsonObject)
                .<JsonObject>map(JsonElement::getAsJsonObject)
                .filter(SyncedLyricsService::hasAnyLyrics)
                .min(Comparator.comparingDouble(object -> responseScore(object, targetDuration)))
                .orElse(null);
        } else {
            return null;
        }
    }

    JsonElement fetch(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(7L))
            .header("Accept", "application/json")
            .header("User-Agent", "bamber/1.0.2 (WorldLyrics)")
            .header("Lrclib-Client", "bamber/1.0.2")
            .GET()
            .build();
        HttpResponse<String> response = this.client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        return response.statusCode() >= 200 && response.statusCode() < 300 && !response.body().isBlank() ? JsonParser.parseString(response.body()) : null;
    }

    SyncedLyricsService$LyricsTrack parseResponse(JsonObject object) {
        if (hasSyncedLyrics(object)) {
            List<SyncedLyricsService$LyricLine> lines = parseLrc(object.get("syncedLyrics").getAsString());
            if (!lines.isEmpty()) {
                return new SyncedLyricsService$LyricsTrack(List.copyOf(lines), SyncedLyricsService$State.READY);
            }
        }

        if (object.has("plainLyrics") && !object.get("plainLyrics").isJsonNull()) {
            double seconds = object.has("duration") ? object.get("duration").getAsDouble() : 180.0;
            List<SyncedLyricsService$LyricLine> lines = parsePlainLyrics(object.get("plainLyrics").getAsString(), Math.round(seconds * 1000.0));
            return lines.isEmpty() ? NOT_FOUND : new SyncedLyricsService$LyricsTrack(List.copyOf(lines), SyncedLyricsService$State.READY, true);
        } else {
            return NOT_FOUND;
        }
    }

    SyncedLyricsService$LyricsTrack fetchPlainLyricsFallback(String title, String artist, long durationMs) throws Exception {
        HttpRequest searchRequest = HttpRequest.newBuilder(URI.create("https://lyrics.ws/api/search"))
            .timeout(Duration.ofSeconds(7L))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("User-Agent", "bamber/1.0.2 (WorldLyrics)")
            .POST(BodyPublishers.ofString("query=" + encode(title), StandardCharsets.UTF_8))
            .build();
        String searchHtml = this.sendText(searchRequest);
        if (searchHtml == null) {
            return NOT_FOUND;
        }

        String normalizedTitle = normalize(title);
        String normalizedArtist = normalize(artist);
        Matcher resultMatcher = SEARCH_RESULT.matcher(searchHtml);
        String bestPath = null;
        int bestScore = Integer.MIN_VALUE;

        while (resultMatcher.find()) {
            String path = decodeHtml(resultMatcher.group(1)).strip();
            String label = normalize(decodeHtml(stripTags(resultMatcher.group(2))));
            int score = label.contains(normalizedTitle) ? 100 : 0;

            for (String token : normalizedArtist.split("[^\\p{L}\\p{N}]+")) {
                if (token.length() >= 3 && label.contains(token)) {
                    score += 12;
                }
            }

            if (score > bestScore && path.startsWith("/")) {
                bestScore = score;
                bestPath = path;
            }
        }

        if (bestPath != null && bestScore >= 100) {
            HttpRequest pageRequest = HttpRequest.newBuilder(URI.create("https://lyrics.ws" + bestPath))
                .timeout(Duration.ofSeconds(7L))
                .header("User-Agent", "bamber/1.0.2 (WorldLyrics)")
                .GET()
                .build();
            String page = this.sendText(pageRequest);
            if (page == null) {
                return NOT_FOUND;
            }

            Matcher lyricsMatcher = LYRICS_BLOCK.matcher(page);
            if (!lyricsMatcher.find()) {
                return NOT_FOUND;
            }

            String plainLyrics = decodeHtml(stripTags(lyricsMatcher.group(1).replaceAll("(?i)<br\\s*/?>", "\\n")));
            List<SyncedLyricsService$LyricLine> lines = parsePlainLyrics(plainLyrics, durationMs);
            return lines.isEmpty() ? NOT_FOUND : new SyncedLyricsService$LyricsTrack(List.copyOf(lines), SyncedLyricsService$State.READY, true);
        } else {
            return NOT_FOUND;
        }
    }

    String sendText(HttpRequest request) throws Exception {
        HttpResponse<String> response = this.client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            String body = response.body();
            return body != null && !body.isBlank() && body.length() <= 2000000 ? body : null;
        } else {
            return null;
        }
    }

    static List<SyncedLyricsService$LyricLine> parseLrc(String lrc) {
        List<SyncedLyricsService$LyricLine> lines = new ArrayList<>();
        if (lrc != null && !lrc.isBlank()) {
            long globalOffset = 0L;
            Matcher offsetMatcher = OFFSET.matcher(lrc);
            if (offsetMatcher.find()) {
                try {
                    globalOffset = Long.parseLong(offsetMatcher.group(1));
                } catch (NumberFormatException var21) {
                }
            }

            for (String rawLine : lrc.split("\\R")) {
                Matcher matcher = TIMESTAMP.matcher(rawLine);
                List<Long> times = new ArrayList<>(2);

                int textStart;
                for (textStart = 0; matcher.find(); textStart = matcher.end()) {
                    long minutes = Long.parseLong(matcher.group(1));
                    long seconds = Long.parseLong(matcher.group(2));
                    String fraction = matcher.group(3);
                    long var10000;
                    if (fraction == null) {
                        var10000 = 0L;
                    } else {
                        switch (fraction.length()) {
                            case 1:
                                var10000 = Long.parseLong(fraction) * 100L;
                                break;
                            case 2:
                                var10000 = Long.parseLong(fraction) * 10L;
                                break;
                            default:
                                var10000 = Long.parseLong(fraction.substring(0, 3));
                        }
                    }

                    long millis = var10000;
                    times.add((minutes * 60L + seconds) * 1000L + millis);
                }

                if (!times.isEmpty()) {
                    String payload = rawLine.substring(Math.min(textStart, rawLine.length()));
                    List<SyncedLyricsService$LyricWord> words = parseEnhancedWords(payload, globalOffset);
                    String text = WORD_TIMESTAMP.matcher(payload).replaceAll("").strip();
                    if (!text.isEmpty()) {
                        for (long time : times) {
                            lines.add(new SyncedLyricsService$LyricLine(Math.max(0L, time + globalOffset), text, words));
                        }
                    }
                }
            }

            lines.sort(Comparator.comparingLong(SyncedLyricsService$LyricLine::timeMs));
            return lines;
        } else {
            return lines;
        }
    }

    static List<SyncedLyricsService$LyricWord> parseEnhancedWords(String payload, long globalOffset) {
        Matcher matcher = WORD_TIMESTAMP.matcher(payload);
        List<Long> times = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();

        while (matcher.find()) {
            times.add(parseTimestamp(matcher.group(1), matcher.group(2), matcher.group(3)) + globalOffset);
            starts.add(matcher.end());
        }

        if (times.isEmpty()) {
            return List.of();
        }

        List<SyncedLyricsService$LyricWord> words = new ArrayList<>(times.size());

        for (int index = 0; index < times.size(); index++) {
            int end = index + 1 < times.size() ? payload.lastIndexOf(60, starts.get(index + 1) - 1) : payload.length();
            String text = payload.substring(starts.get(index), Math.max(starts.get(index), end));
            if (!text.isEmpty()) {
                words.add(new SyncedLyricsService$LyricWord(Math.max(0L, times.get(index)), text));
            }
        }

        return List.copyOf(words);
    }

    static long parseTimestamp(String minutesText, String secondsText, String fraction) {
        long minutes = Long.parseLong(minutesText);
        long seconds = Long.parseLong(secondsText);
        long var10000;
        if (fraction == null) {
            var10000 = 0L;
        } else {
            switch (fraction.length()) {
                case 1:
                    var10000 = Long.parseLong(fraction) * 100L;
                    break;
                case 2:
                    var10000 = Long.parseLong(fraction) * 10L;
                    break;
                default:
                    var10000 = Long.parseLong(fraction.substring(0, 3));
            }
        }

        long millis = var10000;
        return (minutes * 60L + seconds) * 1000L + millis;
    }

    static List<SyncedLyricsService$LyricLine> parsePlainLyrics(String plain, long durationMs) {
        List<String> textLines = new ArrayList<>();
        List<Boolean> sectionStarts = new ArrayList<>();
        if (plain != null && !plain.isBlank()) {
            boolean sectionStart = false;

            for (String raw : plain.split("\\R")) {
                String line = raw.replaceAll("\\s+", " ").strip();
                if (line.isEmpty()) {
                    sectionStart = !textLines.isEmpty();
                } else if (line.matches("^\\[[^]]+]$")) {
                    sectionStart = !textLines.isEmpty();
                } else {
                    textLines.add(line);
                    sectionStarts.add(sectionStart);
                    sectionStart = false;
                }
            }

            if (textLines.isEmpty()) {
                return List.of();
            }

            long duration = durationMs > 20000L ? durationMs : 180000L;
            long start = Math.min(1500L, duration / 50L);
            long usable = Math.max(10000L, duration - start - 2000L);
            double totalWeight = 0.0;

            for (int i = 0; i < textLines.size(); i++) {
                totalWeight += lineWeight(textLines.get(i));
                if (sectionStarts.get(i)) {
                    totalWeight += 0.85;
                }
            }

            double elapsedWeight = 0.0;
            List<SyncedLyricsService$LyricLine> result = new ArrayList<>(textLines.size());

            for (int ix = 0; ix < textLines.size(); ix++) {
                String line = textLines.get(ix);
                if (sectionStarts.get(ix)) {
                    elapsedWeight += 0.85;
                }

                long timestamp = start + Math.round(usable * elapsedWeight / Math.max(1.0, totalWeight));
                result.add(new SyncedLyricsService$LyricLine(timestamp, line));
                elapsedWeight += lineWeight(line);
            }

            return result;
        } else {
            return List.of();
        }
    }

    static boolean hasSyncedLyrics(JsonObject object) {
        return object != null && object.has("syncedLyrics") && !object.get("syncedLyrics").isJsonNull() && !object.get("syncedLyrics").getAsString().isBlank();
    }

    static boolean hasAnyLyrics(JsonObject object) {
        return hasSyncedLyrics(object)
            || object != null && object.has("plainLyrics") && !object.get("plainLyrics").isJsonNull() && !object.get("plainLyrics").getAsString().isBlank();
    }

    static double responseScore(JsonObject object, double targetDuration) {
        return durationDifference(object, targetDuration) + (hasSyncedLyrics(object) ? 0.0 : 120.0);
    }

    static double durationDifference(JsonObject object, double target) {
        if (!(target <= 0.0) && object.has("duration")) {
            try {
                return Math.abs(object.get("duration").getAsDouble() - target);
            } catch (RuntimeException var4) {
                return Double.MAX_VALUE;
            }
        } else {
            return 0.0;
        }
    }

    static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    static double lineWeight(String line) {
        return Math.max(0.75, Math.min(3.0, line.codePointCount(0, line.length()) / 22.0));
    }

    static String stripTags(String html) {
        return html == null ? "" : html.replaceAll("<[^>]+>", "");
    }

    static String decodeHtml(String value) {
        if (value == null) {
            return "";
        }

        String result = value.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&ndash;", "-")
            .replace("&mdash;", "-");
        Matcher decimal = Pattern.compile("&#(\\d+);").matcher(result);
        StringBuffer decoded = new StringBuffer();

        while (decimal.find()) {
            int codePoint = Integer.parseInt(decimal.group(1));
            decimal.appendReplacement(decoded, Matcher.quoteReplacement(new String(Character.toChars(codePoint))));
        }

        decimal.appendTail(decoded);
        return decoded.toString();
    }
}
