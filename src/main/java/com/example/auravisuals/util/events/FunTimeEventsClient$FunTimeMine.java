package com.example.auravisuals.util.events;

import com.google.gson.JsonObject;
import java.util.regex.Matcher;

public final class FunTimeEventsClient$FunTimeMine {
    public final String server;
    public final String id;
    public final int timeSecondsLeft;
    public final String phase;
    public final String name;
    public final int anarchyId;
    public final FunTimeEventsClient$MineRarityKind mineRarity;
    public final FunTimeEventsClient$MineRarityKind nextMineRarity;
    public final String serverDisplayName;
    final long capturedAtMillis;

    FunTimeEventsClient$FunTimeMine(
        String server,
        String id,
        int timeSecondsLeft,
        String phase,
        String name,
        int anarchyId,
        FunTimeEventsClient$MineRarityKind mineRarity,
        FunTimeEventsClient$MineRarityKind nextMineRarity,
        String serverDisplayName
    ) {
        this.server = server;
        this.id = id;
        this.timeSecondsLeft = timeSecondsLeft;
        this.phase = phase;
        this.name = name;
        this.anarchyId = anarchyId;
        this.mineRarity = mineRarity;
        this.nextMineRarity = nextMineRarity;
        this.serverDisplayName = serverDisplayName;
        this.capturedAtMillis = System.currentTimeMillis();
    }

    FunTimeEventsClient$FunTimeMine(
        String server, String id, int timeSecondsLeft, String phase, String name, int anarchyId, FunTimeEventsClient$MineRarityKind mineRarity
    ) {
        this(server, id, timeSecondsLeft, phase, name, anarchyId, mineRarity, mineRarity, null);
    }

    static FunTimeEventsClient$FunTimeMine fromApi(String server, JsonObject o) {
        String id = FunTimeEventsClient.getString(o, "mine-name");
        if (id == null) {
            id = FunTimeEventsClient.getString(o, "id");
        }

        if (id == null) {
            id = FunTimeEventsClient.getString(o, "mine-id");
        }

        if (id == null) {
            id = FunTimeEventsClient.getString(o, "mine-type");
        }

        int t = FunTimeEventsClient.getInt(o, "reset-seconds-left", -1);
        if (t < 0) {
            t = FunTimeEventsClient.getInt(o, "time-seconds-left", 0);
        }

        String ph = FunTimeEventsClient.getString(o, "phase");
        FunTimeEventsClient$MineRarityKind rarity = FunTimeEventsClient$MineRarityKind.fromApiString(FunTimeEventsClient.getString(o, "mine-rarity"));
        FunTimeEventsClient$MineRarityKind nextRarity = FunTimeEventsClient$MineRarityKind.fromApiString(FunTimeEventsClient.getString(o, "next-mine-rarity"));
        String serverDisplayName = FunTimeEventsClient.getString(o, "server-ru-name");
        String explicit = FunTimeEventsClient.getString(o, "name");
        String title;
        if (explicit != null && !explicit.trim().isEmpty()) {
            title = explicit.trim();
        } else if (id != null && !id.trim().isEmpty() && !"null".equalsIgnoreCase(id.trim())) {
            String tr = FunTimeEventsClient.translateEventId(id);
            if (!tr.equals(id)) {
                title = tr;
            } else {
                String mineTitle = titleFromMineId(id);
                title = mineTitle != null ? mineTitle : "Шахта · " + id;
            }
        } else {
            title = "Шахта";
        }

        int aid = FunTimeEventsClient.parseAnarchyNumber(server);
        return new FunTimeEventsClient$FunTimeMine(server, id != null ? id : "", t, ph, title, aid, rarity, nextRarity, serverDisplayName);
    }

    static String titleFromMineId(String rawId) {
        if (rawId == null) {
            return null;
        }

        Matcher m = FunTimeEventsClient.MINE_ID_NUM.matcher(rawId.trim());
        return m.find() ? "Шахта №" + m.group(1) : null;
    }

    public String getDisplayServer() {
        return this.serverDisplayName != null && !this.serverDisplayName.isBlank()
            ? this.serverDisplayName
            : FunTimeEventsClient.formatServerDisplay(this.server);
    }

    public String getDisplayTime() {
        return FunTimeEventsClient.formatDurationHms(this.getRemainingSeconds());
    }

    public int getRemainingSeconds() {
        long elapsed = Math.max(0L, (System.currentTimeMillis() - this.capturedAtMillis) / 1000L);
        return Math.max(0, this.timeSecondsLeft - (int)elapsed);
    }

    public String getDisplayPhaseLabel() {
        return this.phase != null && !this.phase.isEmpty() ? FunTimeEventsClient.translatePhase(this.phase) : "";
    }

    public boolean isDoIEvintaTitle() {
        if (this.name == null) {
            return false;
        }

        String s = this.name.trim();
        return s.startsWith("До ивента") || s.startsWith("До начала");
    }

    public int anarchyNumberDigitCount() {
        return this.anarchyId <= 0 ? 0 : String.valueOf(this.anarchyId).length();
    }
}
