package com.example.auravisuals.util.events;

import java.util.Locale;

public final class FunTimeEventsClient$FunTimeEvent {
    public final String server;
    public final String eventType;
    public final String id;
    public final int timeSecondsLeft;
    public final String phase;
    public final String loot;
    public final boolean locationAnnounced;
    public final Integer x;
    public final Integer y;
    public final Integer z;
    final long capturedAtMillis;
    public final String name;
    public final String rarity;
    public final int anarchyId;
    public final String status;

    FunTimeEventsClient$FunTimeEvent(
        String server,
        String eventType,
        String id,
        int timeSecondsLeft,
        String phase,
        String loot,
        boolean locationAnnounced,
        Integer x,
        Integer y,
        Integer z,
        String name,
        String rarity,
        int anarchyId,
        String status
    ) {
        this.server = server;
        this.eventType = eventType;
        this.id = id;
        this.timeSecondsLeft = timeSecondsLeft;
        this.phase = phase;
        this.loot = loot;
        this.locationAnnounced = locationAnnounced;
        this.x = x;
        this.y = y;
        this.z = z;
        this.capturedAtMillis = System.currentTimeMillis();
        this.name = name;
        this.rarity = rarity;
        this.anarchyId = anarchyId;
        this.status = status;
    }

    FunTimeEventsClient$FunTimeEvent(
        String server, String eventType, String id, int timeSecondsLeft, String phase, String loot, String name, String rarity, int anarchyId, String status
    ) {
        this(server, eventType, id, timeSecondsLeft, phase, loot, false, null, null, null, name, rarity, anarchyId, status);
    }

    static FunTimeEventsClient$FunTimeEvent fromApi(
        String server, String eventType, String id, int timeSecondsLeft, String phase, String loot, boolean locationAnnounced, Integer x, Integer y, Integer z
    ) {
        String title = titleFrom(eventType, id);
        int aid = FunTimeEventsClient.parseAnarchyNumber(server);
        boolean running = id != null && !id.isBlank();
        String st = running ? "active" : "upcoming";
        String detailRarity = FunTimeEventsClient.translateLoot(loot);
        return new FunTimeEventsClient$FunTimeEvent(
            server, eventType, id, timeSecondsLeft, phase, loot, locationAnnounced, x, y, z, title, detailRarity, aid, st
        );
    }

    static String titleFrom(String eventType, String id) {
        String c = FunTimeEventsClient.canonicalEventIdKey(id);
        if (c != null && FunTimeEventsClient.PRIORITY_GAMEPLAY_EVENT_IDS.contains(c)) {
            return FunTimeEventsClient.translateEventId(id);
        } else if ("system".equalsIgnoreCase(eventType) && c == null) {
            return "До ивента: ";
        } else {
            return c != null ? FunTimeEventsClient.translateEventId(id) : "Следующий ивент";
        }
    }

    public String getSubtitle() {
        return FunTimeEventsClient.formatServerDisplay(this.server) + " · " + this.rarity;
    }

    public String getDisplayServer() {
        return FunTimeEventsClient.formatServerDisplay(this.server);
    }

    public String getDisplayTime() {
        return FunTimeEventsClient.formatDurationHms(this.getRemainingSeconds());
    }

    public int getRemainingSeconds() {
        long elapsed = Math.max(0L, (System.currentTimeMillis() - this.capturedAtMillis) / 1000L);
        return Math.max(0, this.timeSecondsLeft - (int)elapsed);
    }

    public boolean isActiveEvent() {
        return this.id != null && !this.id.isBlank() && !"null".equalsIgnoreCase(this.id);
    }

    public boolean shouldDisplay() {
        if (FunTimeEventsClient.isHiddenEventId(this.id)) {
            return false;
        }

        String normalizedPhase = this.phase == null ? "" : this.phase.trim().toUpperCase(Locale.ROOT);
        return !"CLOSED".equals(normalizedPhase) && !"FINISHED".equals(normalizedPhase)
            ? this.getRemainingSeconds() > 0 || "LOOTING".equals(normalizedPhase)
            : false;
    }

    public String getTimerLabel() {
        return this.getRemainingSeconds() <= 0 && this.phase != null && "LOOTING".equalsIgnoreCase(this.phase.trim())
            ? "Награду можно забирать"
            : (this.isActiveEvent() ? "До окончания: " : "До начала: ") + this.getDisplayTime();
    }

    public boolean hasLocation() {
        return this.locationAnnounced && this.x != null && this.y != null && this.z != null;
    }

    public String getCoordinatesDisplay() {
        return this.hasLocation() ? this.x + " " + this.y + " " + this.z : "Координаты ещё скрыты";
    }

    public String getDisplayPhaseLabel() {
        return this.phase != null && !this.phase.isEmpty() ? FunTimeEventsClient.translatePhase(this.phase) : "";
    }

    public boolean isRunningPhase() {
        return this.phase != null && "RUNNING".equalsIgnoreCase(this.phase.trim());
    }

    public boolean isOngoingPhase() {
        if (this.phase == null) {
            return false;
        }

        String u = this.phase.trim().toUpperCase(Locale.ROOT);

        return switch (u) {
            case "RUNNING", "ACTIVATING", "ACTIVE", "STARTED", "STARTING" -> true;
            case "VOTING" -> true;
            case "CLOSED", "OPENED" -> true;
            default -> false;
        };
    }

    public boolean isDoIEvintaTitle() {
        if (this.name == null) {
            return false;
        }

        String t = this.name.trim();
        return t.startsWith("До ивента") || t.startsWith("До начала");
    }

    public boolean matchesActiveListFilter() {
        if (this.isDoIEvintaTitle()) {
            return false;
        }

        if (this.isOngoingPhase()) {
            return true;
        }

        if (this.phase != null && !this.phase.trim().isEmpty()) {
            return false;
        }

        String c = FunTimeEventsClient.canonicalEventIdKey(this.id);
        return c != null && !"myst_beacon".equals(c);
    }

    public int anarchyNumberDigitCount() {
        return this.anarchyId <= 0 ? 0 : String.valueOf(this.anarchyId).length();
    }
}
