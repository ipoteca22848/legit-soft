package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.StringSetting;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public class AutoLogin extends Module {
    private static final long LOGIN_DELAY_MS = 1000L;
    private static final long REGISTER_DELAY_MS = 3000L;
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH.mm.ss");
    private final StringSetting password = new StringSetting("Пароль", "");
    private String pendingCommand;
    private long pendingAtMs;
    private String lastPromptKey = "";
    private long lastPromptMs;

    public AutoLogin() {
        super("AutoLogin", "Автоматически регистрируется и логинится", Category.MISC);
        this.addSettings(this.password);
    }

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.getNetworkHandler() != null && this.pendingCommand != null) {
            if (System.currentTimeMillis() >= this.pendingAtMs) {
                this.mc.getNetworkHandler().sendChatCommand(this.pendingCommand);
                this.pendingCommand = null;
            }
        }
    }

    public void onGameMessage(String rawMessage) {
        if (this.mc.player != null && rawMessage != null && !rawMessage.isBlank()) {
            String message = rawMessage.toLowerCase(Locale.ROOT);
            boolean registerPrompt = message.contains("/reg") || message.contains("/register") || message.contains("зарегистр");
            boolean loginPrompt = message.contains("/login") || message.contains("логин") || message.contains("авториз") || message.contains("войдите");
            if (registerPrompt || loginPrompt) {
                String pass = this.resolvePassword();
                if (!pass.isBlank()) {
                    boolean repeatForLogin = message.contains("РїРѕРІС‚РѕСЂ") || message.contains("повтор") || message.contains("repeat");
                    String command;
                    if (registerPrompt && !loginPrompt) {
                        boolean repeatPassword = message.contains("повторите") || message.contains("repeat");
                        command = repeatPassword ? "reg " + pass + " " + pass : "reg " + pass;
                    } else {
                        command = repeatForLogin ? "login " + pass + " " + pass : "login " + pass;
                    }

                    String promptKey = command + "|" + this.currentServerKey();
                    long now = System.currentTimeMillis();
                    if (!promptKey.equals(this.lastPromptKey) || now - this.lastPromptMs >= 10000L) {
                        this.lastPromptKey = promptKey;
                        this.lastPromptMs = now;
                        this.pendingCommand = command;
                        this.pendingAtMs = now + (command.startsWith("login ") ? 1000L : 3000L);
                        this.ensurePasswordFile(pass);
                    }
                }
            }
        }
    }

    private String resolvePassword() {
        String fromFile = this.findPasswordFileValue();
        if (!fromFile.isBlank()) {
            return fromFile;
        }

        String value = this.password.getValue();
        return value == null ? "" : value.trim();
    }

    private String findPasswordFileValue() {
        Path dir = this.passwordsDir();
        String nick = this.currentNick().toLowerCase(Locale.ROOT);
        String server = this.currentServerKey().toLowerCase(Locale.ROOT);

        try {
            Files.createDirectories(dir);

            try (Stream<Path> stream = Files.list(dir)) {
                return stream.filter(path -> path.getFileName().toString().endsWith(".txt"))
                    .map(path -> this.parsePasswordFile(path, nick, server))
                    .filter(s -> s != null && !s.isBlank())
                    .findFirst()
                    .orElse("");
            }
        } catch (Exception ignored) {
            return "";
        }
    }

    private String parsePasswordFile(Path path, String nick, String server) {
        String fileName = path.getFileName().toString();
        String name = fileName.substring(0, fileName.length() - 4);
        String[] parts = this.readPasswordEntry(path, name).split("_", 3);
        if (parts.length < 3) {
            return "";
        } else if (!parts[0].equalsIgnoreCase(nick)) {
            return "";
        } else {
            return !parts[2].equalsIgnoreCase(server) ? "" : parts[1];
        }
    }

    private String readPasswordEntry(Path path, String fallback) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8).trim();
            return content.isBlank() ? fallback : content;
        } catch (IOException ignored) {
            return fallback;
        }
    }

    private void ensurePasswordFile(String pass) {
        if (!pass.isBlank()) {
            if (this.findPasswordFileValue().isBlank()) {
                Path file = this.nextPasswordFilePath();

                try {
                    Files.createDirectories(file.getParent());
                    if (!Files.exists(file)) {
                        Files.writeString(file, this.currentNick() + "_" + pass + "_" + this.currentServerKey(), StandardCharsets.UTF_8);
                    }
                } catch (IOException var4) {
                }
            }
        }
    }

    private Path nextPasswordFilePath() {
        String base = LocalTime.now().format(FILE_TIME_FORMAT) + "_" + this.currentNick();
        Path dir = this.passwordsDir();
        Path file = dir.resolve(base + ".txt");

        for (int suffix = 2; Files.exists(file); suffix++) {
            file = dir.resolve(base + "_" + suffix + ".txt");
        }

        return file;
    }

    private Path passwordsDir() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.runDirectory.toPath().resolve("anvil visual").resolve("passwords");
    }

    private String currentNick() {
        return this.sanitize(this.mc.getSession().getUsername());
    }

    private String currentServerKey() {
        ServerInfo entry = this.mc.getCurrentServerEntry();
        String address = entry != null && entry.address != null ? entry.address : "singleplayer";
        return this.sanitize(address.toLowerCase(Locale.ROOT));
    }

    private String sanitize(String value) {
        return value != null && !value.isBlank() ? value.trim().replaceAll("[^A-Za-z0-9А-Яа-я.-]+", "-") : "unknown";
    }
}
