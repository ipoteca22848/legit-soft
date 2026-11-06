package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.StringSetting;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public class StreamerMode extends Module {
    public static final int MAX_NAME_LENGTH = 12;
    public static final StringSetting customName = new StringSetting("Новое имя", "anvil");
    public static final StringSetting anarchyName = new StringSetting("Анархия", "777");
    private Text originalHeader = null;
    private final Map<String, Text> originalTeamPrefixes = new HashMap<>();
    private final Map<String, Text> originalTeamSuffixes = new HashMap<>();
    private static volatile String cachedUsername = null;
    private static volatile String cachedCustomName = null;
    private static final ThreadLocal<Boolean> isReplacing = ThreadLocal.withInitial(() -> false);

    public StreamerMode() {
        super("StreamerMode", "Скрывает никнейм игрока", Category.MISC);
        this.addSettings(customName, anarchyName);
    }

    public static String limitStreamerValue(String value) {
        if (value == null) {
            return "";
        } else {
            return value.length() > 12 ? value.substring(0, 12) : value;
        }
    }

    public static String getReplacedName(String original) {
        if (original == null) {
            return null;
        }

        if (isReplacing.get()) {
            return original;
        }

        if (ModuleManager.INSTANCE == null) {
            return original;
        }

        try {
            isReplacing.set(true);
            StreamerMode module;
            if (cachedUsername != null && (module = ModuleManager.INSTANCE.getModule(StreamerMode.class)) != null && module.isEnabled()) {
                if (!cachedUsername.isEmpty() && original.contains(cachedUsername)) {
                    return original.replace(cachedUsername, customName.getValue());
                }

                return original;
            }

            module = ModuleManager.INSTANCE.getModule(StreamerMode.class);
            if (module == null || !module.isEnabled()) {
                return original;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getSession() == null) {
                return original;
            }

            String username = client.getSession().getUsername();
            if (username != null && !username.isEmpty() && original.contains(username)) {
                return original.replace(username, customName.getValue());
            }
        } catch (Exception e) {
            return original;
        } finally {
            isReplacing.set(false);
        }

        return original;
    }

    public static Text replaceInText(Text original) {
        if (original == null) {
            return null;
        }

        if (ModuleManager.INSTANCE == null) {
            return original;
        }

        StreamerMode module = ModuleManager.INSTANCE.getModule(StreamerMode.class);
        if (module != null && module.isEnabled()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getSession() != null) {
                String username = client.getSession().getUsername();
                if (username != null && !username.isEmpty()) {
                    String fullText = original.getString();
                    return !fullText.contains(username) ? original : deepReplace(original, username, customName.getValue());
                } else {
                    return original;
                }
            } else {
                return original;
            }
        } else {
            return original;
        }
    }

    public static Text replaceInChatText(Text original) {
        if (original != null && ModuleManager.INSTANCE != null) {
            StreamerMode module = ModuleManager.INSTANCE.getModule(StreamerMode.class);
            if (module != null && module.isEnabled()) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.getSession() != null) {
                    String username = client.getSession().getUsername();
                    String replacement = customName.getValue();
                    if (username != null && !username.isEmpty() && replacement != null) {
                        String fullText = original.getString();
                        if (fullText.contains(username)) {
                            return deepReplaceChat(original, username, replacement);
                        } else {
                            return !replacement.isEmpty() && fullText.contains(replacement) ? deepReplaceChat(original, replacement, replacement) : original;
                        }
                    } else {
                        return original;
                    }
                } else {
                    return original;
                }
            } else {
                return original;
            }
        } else {
            return original;
        }
    }

    private static MutableText deepReplace(Text text, String target, String replacement) {
        String content = text.copyContentOnly().getString();
        MutableText result = !content.isEmpty() && content.contains(target)
            ? Text.literal(content.replace(target, replacement)).setStyle(text.getStyle())
            : text.copyContentOnly().setStyle(text.getStyle());

        for (Text sibling : text.getSiblings()) {
            result.append(deepReplace(sibling, target, replacement));
        }

        return result;
    }

    private static MutableText deepReplaceChat(Text text, String target, String replacement) {
        String content = text.copyContentOnly().getString();
        MutableText result;
        if (!content.isEmpty() && content.contains(target)) {
            Style originalStyle = text.getStyle();
            Style ordinaryChatStyle = originalStyle.withColor((TextColor)null);
            result = Text.empty().setStyle(ordinaryChatStyle);
            int from = 0;

            int match;
            while ((match = content.indexOf(target, from)) >= 0) {
                if (match > from) {
                    result.append(Text.literal(content.substring(from, match)).setStyle(originalStyle));
                }

                result.append(Text.literal(replacement).setStyle(ordinaryChatStyle));
                from = match + target.length();
            }

            if (from < content.length()) {
                result.append(Text.literal(content.substring(from)).setStyle(originalStyle));
            }
        } else {
            result = text.copyContentOnly().setStyle(text.getStyle());
        }

        for (Text sibling : text.getSiblings()) {
            result.append(deepReplaceChat(sibling, target, replacement));
        }

        return result;
    }

    @Override
    public void onTick() {
        String limitedCustom = limitStreamerValue(customName.getValue());
        if (!limitedCustom.equals(customName.getValue())) {
            customName.setValue(limitedCustom);
        }

        String limitedAnarchy;
        if (!(limitedAnarchy = limitStreamerValue(anarchyName.getValue())).equals(anarchyName.getValue())) {
            anarchyName.setValue(limitedAnarchy);
        }

        if (this.mc.player != null && this.mc.getSession() != null) {
            cachedUsername = this.mc.getSession().getUsername();
            cachedCustomName = customName.getValue();
        }

        if (this.mc.world != null && this.mc.player != null) {
            Scoreboard scoreboard = this.mc.world.getScoreboard();
            if (scoreboard != null) {
                ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
                if (objective != null) {
                    Text header = this.originalHeader != null ? this.originalHeader : objective.getDisplayName();
                    String headerString = header.getString();
                    if (headerString.contains("Анархия")) {
                        if (this.originalHeader == null) {
                            this.originalHeader = header;
                        }

                        String numberPart = anarchyName.getValue();
                        MutableText lightning = Text.literal("⚡ ").setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true));
                        MutableText anarchyText = Text.literal("Анархия-" + numberPart).setStyle(Style.EMPTY.withColor(Formatting.DARK_RED).withBold(true));
                        MutableText styled = Text.empty().append(lightning).append(anarchyText);
                        objective.setDisplayName(styled);
                    }

                    String username = cachedUsername;
                    String replacement = customName.getValue();
                    if (username != null && !username.isEmpty() && replacement != null) {
                        for (Team team : scoreboard.getTeams()) {
                            String teamName = team.getName();
                            Text prefix = this.originalTeamPrefixes.getOrDefault(teamName, team.getPrefix());
                            Text suffix = this.originalTeamSuffixes.getOrDefault(teamName, team.getSuffix());
                            if (prefix.getString().contains(username)) {
                                this.originalTeamPrefixes.putIfAbsent(teamName, prefix);
                                team.setPrefix(deepReplace(prefix, username, replacement));
                            }

                            if (suffix.getString().contains(username)) {
                                this.originalTeamSuffixes.putIfAbsent(teamName, suffix);
                                team.setSuffix(deepReplace(suffix, username, replacement));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        Scoreboard scoreboard;
        if (this.mc.world != null && this.mc.player != null && (scoreboard = this.mc.world.getScoreboard()) != null) {
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (objective != null && this.originalHeader != null) {
                objective.setDisplayName(this.originalHeader);
            }

            for (Team team : scoreboard.getTeams()) {
                String teamName = team.getName();
                Text prefix = this.originalTeamPrefixes.get(teamName);
                Text suffix = this.originalTeamSuffixes.get(teamName);
                if (prefix != null) {
                    team.setPrefix(prefix);
                }

                if (suffix != null) {
                    team.setSuffix(suffix);
                }
            }
        }

        this.originalHeader = null;
        this.originalTeamPrefixes.clear();
        this.originalTeamSuffixes.clear();
        cachedCustomName = null;
    }
}
