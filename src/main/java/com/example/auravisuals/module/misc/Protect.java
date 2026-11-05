package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;

public class Protect extends Module {
    public static final StringSetting customName = new StringSetting("Новое имя", "ProtectedUser");
    public static final StringSetting anarchyName = new StringSetting("Анархия", "1488");
    private Text originalHeader = null;
    private static volatile String cachedUsername = null;
    private static volatile String cachedCustomName = null;
    private static final ThreadLocal<Boolean> isReplacing = ThreadLocal.withInitial(() -> false);

    public Protect() {
        super("Protect", "Скрывает никнейм игрока", Category.MISC);
        this.addSettings(customName, anarchyName);
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
            Protect module;
            if (cachedUsername != null && cachedCustomName != null && (module = ModuleManager.INSTANCE.getModule(Protect.class)) != null && module.isEnabled()) {
                if (!cachedUsername.isEmpty() && original.contains(cachedUsername)) {
                    return original.replace(cachedUsername, cachedCustomName);
                }

                return original;
            }

            module = ModuleManager.INSTANCE.getModule(Protect.class);
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

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.getSession() != null) {
            cachedUsername = this.mc.getSession().getUsername();
            cachedCustomName = customName.getValue();
        }

        if (this.mc.world != null && this.mc.player != null) {
            Scoreboard scoreboard = this.mc.world.getScoreboard();
            if (scoreboard != null) {
                ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
                if (objective != null) {
                    Text header = objective.getDisplayName();
                    String headerString = header.getString();
                    if (headerString.contains("Анархия") && !headerString.contains("⚡")) {
                        if (this.originalHeader == null) {
                            this.originalHeader = header;
                        }

                        String modifiedHeader = headerString.replaceAll("Анархия[- ]?\\w*", "⚡ Анархия-" + anarchyName.getValue());
                        objective.setDisplayName(Text.of(modifiedHeader));
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        ScoreboardObjective objective;
        Scoreboard scoreboard;
        if (this.mc.world != null
            && this.mc.player != null
            && this.originalHeader != null
            && (scoreboard = this.mc.world.getScoreboard()) != null
            && (objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR)) != null) {
            objective.setDisplayName(this.originalHeader);
        }

        this.originalHeader = null;
    }
}
