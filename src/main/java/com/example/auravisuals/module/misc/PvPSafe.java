package com.example.auravisuals.module.misc;

import com.example.auravisuals.mixin.accessor.BossBarHudAccessor;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.hud.ClientBossBar;

public class PvPSafe extends Module {
    private static final Pattern PVP_TIME_PATTERN = Pattern.compile("(\\d{1,4})");
    private boolean bypassing = false;
    private static final String[] BLOCKED_COMMANDS = new String[]{"hub", "lobby", "spawn", "anarchy", "grief", "server", "call"};

    public PvPSafe() {
        super("PvPSafe", "Защита от выхода и команд в PVP", Category.MISC);
    }

    public boolean isPvpActive() {
        return this.findPvpBossBar() != null;
    }

    public String getPvpStatusText() {
        ClientBossBar bossBar = this.findPvpBossBar();
        if (bossBar == null) {
            return "";
        } else {
            String normalized = bossBar.getName().getString().replaceAll("\\s+", " ").trim();
            Matcher matcher = PVP_TIME_PATTERN.matcher(normalized);
            if (matcher.find()) {
                return "PvP " + matcher.group(1) + "s";
            } else {
                return normalized.isEmpty() ? "PvP active" : normalized;
            }
        }
    }

    private ClientBossBar findPvpBossBar() {
        if (this.mc.inGameHud != null && this.mc.inGameHud.getBossBarHud() != null) {
            Map<UUID, ClientBossBar> bossBars = ((BossBarHudAccessor)this.mc.inGameHud.getBossBarHud()).getBossBars();

            for (ClientBossBar bossBar : bossBars.values()) {
                String bossBarText = bossBar.getName().getString().toLowerCase(Locale.ROOT);
                if (bossBarText.contains("pvp")) {
                    return bossBar;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public boolean shouldBlockCommand(String command) {
        if (this.isEnabled() && this.isPvpActive()) {
            String lower = command.toLowerCase().trim();
            if (lower.startsWith("/")) {
                lower = lower.substring(1).trim();
            }

            if (lower.isEmpty()) {
                return false;
            }

            String root = lower.split("\\s+")[0];

            for (String blocked : BLOCKED_COMMANDS) {
                if (root.equals(blocked) || root.endsWith(":" + blocked)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public boolean isBypassing() {
        return this.bypassing;
    }

    public void setBypass(boolean bypassing) {
        this.bypassing = bypassing;
    }
}
