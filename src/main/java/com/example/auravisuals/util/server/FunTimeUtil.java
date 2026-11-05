package com.example.auravisuals.util.server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class FunTimeUtil {
    private static final Pattern BALANCE_PATTERN = Pattern.compile(
        "(?:Balance|Баланс|Money|Деньги|Coins|Монет[а-я]*|Purse|Кошелек|\\$)\\D*(\\d[\\d\\s,.]*[kKmM]?)", 2
    );

    private FunTimeUtil() {
    }

    public static boolean isFunTimeServer(MinecraftClient mc) {
        if (mc != null && mc.getCurrentServerEntry() != null) {
            String address = mc.getCurrentServerEntry().address;
            return address != null && address.toLowerCase().contains("funtime");
        } else {
            return false;
        }
    }

    public static double getPlayerBalance(MinecraftClient mc) {
        if (mc != null && mc.world != null) {
            Scoreboard scoreboard = mc.world.getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (objective == null) {
                return -1.0;
            }

            for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
                Team team = scoreboard.getScoreHolderTeam(entry.owner());
                Text name = Team.decorateName(team, Text.literal(entry.owner()));
                String text = Formatting.strip(name.getString());
                if (text != null) {
                    Matcher matcher = BALANCE_PATTERN.matcher(text);
                    if (matcher.find()) {
                        return parseBalanceString(matcher.group(1));
                    }
                }
            }

            return -1.0;
        } else {
            return -1.0;
        }
    }

    public static long parseSuffixedValue(String input) {
        if (input != null && !input.isEmpty()) {
            String clean = input.trim().toLowerCase().replaceAll("[,\\s]", "");
            long multiplier = 1L;
            if (clean.endsWith("кк") || clean.endsWith("kk") || clean.endsWith("m") || clean.endsWith("м")) {
                multiplier = 1000000L;
                clean = !clean.endsWith("кк") && !clean.endsWith("kk") ? clean.substring(0, clean.length() - 1) : clean.substring(0, clean.length() - 2);
            } else if (clean.endsWith("к") || clean.endsWith("k")) {
                multiplier = 1000L;
                clean = clean.substring(0, clean.length() - 1);
            }

            try {
                return (long)(Double.parseDouble(clean) * multiplier);
            } catch (NumberFormatException var5) {
                return 0L;
            }
        } else {
            return 0L;
        }
    }

    private static double parseBalanceString(String text) {
        String clean = text.replaceAll("[,\\s]", "");
        double multiplier = 1.0;
        String lower = clean.toLowerCase();
        if (lower.endsWith("k")) {
            multiplier = 1000.0;
            clean = clean.substring(0, clean.length() - 1);
        } else if (lower.endsWith("m")) {
            multiplier = 1000000.0;
            clean = clean.substring(0, clean.length() - 1);
        }

        try {
            return Double.parseDouble(clean) * multiplier;
        } catch (NumberFormatException var6) {
            return -1.0;
        }
    }
}
