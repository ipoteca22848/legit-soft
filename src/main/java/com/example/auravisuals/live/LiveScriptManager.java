package com.example.auravisuals.live;

import com.example.auravisuals.command.CommandManager;
import com.example.auravisuals.compat.WorldRenderContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class LiveScriptManager {
    public static final LiveScriptManager INSTANCE = new LiveScriptManager();
    private final List<LiveVisual> activeVisuals = new CopyOnWriteArrayList<>();
    private final Map<String, Long> fileLastModified = new ConcurrentHashMap<>();
    private File scriptsDirectory;
    private boolean autoWatchEnabled = false;
    private long lastWatchCheckTime = 0L;
    private long lastErrorLogTime = 0L;

    private LiveScriptManager() {
    }

    public void init() {
        File gameDir = MinecraftClient.getInstance().runDirectory;
        if (gameDir == null) {
            gameDir = new File(".");
        }

        this.scriptsDirectory = new File(gameDir, "auravisuals/scripts");
        if (!this.scriptsDirectory.exists()) {
            this.scriptsDirectory.mkdirs();
        }

        this.createDefaultExampleIfEmpty();
    }

    public File getScriptsDirectory() {
        if (this.scriptsDirectory == null) {
            this.init();
        }

        return this.scriptsDirectory;
    }

    public boolean isAutoWatchEnabled() {
        return this.autoWatchEnabled;
    }

    public void setAutoWatchEnabled(boolean autoWatchEnabled) {
        this.autoWatchEnabled = autoWatchEnabled;
    }

    private void createDefaultExampleIfEmpty() {
        File exampleFile = new File(this.scriptsDirectory, "ExampleVisual.java");
        if (!exampleFile.exists()) {
            String template = "package auravisuals.scripts;\n\nimport com.example.auravisuals.live.LiveVisual;\nimport com.example.auravisuals.client.gui.render.RoundedRenderer;\nimport com.example.auravisuals.client.gui.render.TextRender;\nimport com.example.auravisuals.client.gui.font.FontManager;\nimport com.example.auravisuals.compat.WorldRenderContext;\nimport net.minecraft.client.MinecraftClient;\nimport net.minecraft.client.gui.DrawContext;\nimport net.minecraft.util.math.MathHelper;\nimport net.minecraft.util.math.Vec3d;\n\npublic class ExampleVisual implements LiveVisual {\n\n    @Override\n    public void onInit() {\n        System.out.println(\"[LiveCode] ExampleVisual initialized!\");\n    }\n\n    @Override\n    public void onTick() {\n        // Run code every tick (20 times per second)\n    }\n\n    @Override\n    public void onRender2D(DrawContext context, float tickDelta) {\n        MinecraftClient mc = MinecraftClient.getInstance();\n        if (mc.player == null) return;\n\n        // Example: Draw a custom live HUD panel on top right\n        float x = 20.0f;\n        float y = 20.0f;\n        float width = 180.0f;\n        float height = 45.0f;\n        float radius = 8.0f;\n\n        // Modern background with glow\n        RoundedRenderer.drawRoundedRect(context, x, y, width, height, radius, 0xCC12121A);\n        RoundedRenderer.drawOutline(context, x, y, width, height, radius, 1.5f, 0xFFFF4E5A);\n\n        // Text\n        FontManager.drawCustomText(context, \"§c★ §fLiveCode §7[Active]\", x + 10, y + 8, 0xFFFFFFFF, true);\n\n        String info = \"FPS: §c\" + mc.getCurrentFps() + \" §7| §fHP: §c\" + (int) mc.player.getHealth();\n        FontManager.drawCustomText(context, info, x + 10, y + 24, 0xFFE0E0E0, true);\n    }\n\n    @Override\n    public void onRender3D(WorldRenderContext context) {\n        // You can render 3D visuals here using WorldRenderContext / matrixStack\n    }\n\n    @Override\n    public void onCleanup() {\n        System.out.println(\"[LiveCode] ExampleVisual cleaned up!\");\n    }\n}\n";

            try {
                Files.writeString(exampleFile.toPath(), template);
            } catch (IOException var4) {
                var4.printStackTrace();
            }
        }
    }

    public synchronized void reloadScripts(boolean notifyChat) {
        this.init();
        File[] files = this.scriptsDirectory.listFiles((dir, name) -> name.endsWith(".java"));
        if (files == null || files.length == 0) {
            this.createDefaultExampleIfEmpty();
            files = this.scriptsDirectory.listFiles((dir, name) -> name.endsWith(".java"));
        }

        if (files != null && files.length != 0) {
            this.clearVisuals();
            int successCount = 0;
            int failCount = 0;

            for (File file : files) {
                try {
                    this.fileLastModified.put(file.getAbsolutePath(), file.lastModified());
                    String code = Files.readString(file.toPath());
                    String className = extractFullClassName(file.getName(), code);
                    LiveScriptCompiler$CompileResult result = LiveScriptCompiler.compileSource(className, code);
                    if (result.isSuccess() && result.getCompiledClass() != null) {
                        Class<?> clazz = result.getCompiledClass();
                        if (LiveVisual.class.isAssignableFrom(clazz)) {
                            LiveVisual visual = (LiveVisual)clazz.getDeclaredConstructor().newInstance();
                            visual.onInit();
                            this.activeVisuals.add(visual);
                            successCount++;
                        } else {
                            successCount++;
                            if (notifyChat) {
                                CommandManager.addChatMessage("§a[LiveCode] §fCompiled §e" + file.getName() + " §7(non-visual class loaded).");
                            }
                        }
                    } else {
                        failCount++;
                        if (notifyChat) {
                            CommandManager.addChatMessage("§c[LiveCode] Error in §e" + file.getName() + "§c:");

                            for (String err : result.getErrors()) {
                                CommandManager.addChatMessage("§c ✖ " + err);
                            }
                        }
                    }
                } catch (Throwable var14) {
                    failCount++;
                    if (notifyChat) {
                        CommandManager.addChatMessage("§c[LiveCode] Exception compiling §e" + file.getName() + "§c: " + var14.getMessage());
                    }
                }
            }

            if (notifyChat) {
                if (failCount == 0) {
                    CommandManager.addChatMessage("§a[LiveCode] Successfully loaded §e" + successCount + " §ascript(s)!");
                } else {
                    CommandManager.addChatMessage("§e[LiveCode] Loaded §a" + successCount + " §escripts with §c" + failCount + " §eerrors.");
                }
            }
        } else if (notifyChat) {
            CommandManager.addChatMessage("§7No .java script files found in: §e" + this.scriptsDirectory.getAbsolutePath());
        }
    }

    public synchronized void evalSnippet(String snippet) {
        long timestamp = System.currentTimeMillis();
        String className = "auravisuals.scripts.DynamicSnippet_" + timestamp;
        String fullCode = "package anvil visual.scripts;\n\nimport com.example.anvil visual.live.LiveVisual;\nimport com.example.anvil visual.client.gui.render.RoundedRenderer;\nimport com.example.anvil visual.client.gui.render.TextRender;\nimport com.example.anvil visual.client.gui.font.FontManager;\nimport com.example.anvil visual.compat.WorldRenderContext;\nimport com.example.anvil visual.command.CommandManager;\nimport net.minecraft.client.MinecraftClient;\nimport net.minecraft.client.gui.DrawContext;\nimport net.minecraft.util.math.MathHelper;\nimport net.minecraft.util.math.Vec3d;\n\npublic class DynamicSnippet_%d implements Runnable {\n    @Override\n    public void run() {\n        try {\n            %s;\n        } catch (Throwable t) {\n            CommandManager.addChatMessage(\"§c[Eval Error] \" + t.getMessage());\n            t.printStackTrace();\n        }\n    }\n}\n"
            .formatted(timestamp, snippet);
        LiveScriptCompiler$CompileResult result = LiveScriptCompiler.compileSource(className, fullCode);
        if (result.isSuccess() && result.getCompiledClass() != null) {
            try {
                Runnable runnable = (Runnable)result.getCompiledClass().getDeclaredConstructor().newInstance();
                MinecraftClient.getInstance().execute(runnable);
                CommandManager.addChatMessage("§a[LiveCode] Code executed successfully!");
            } catch (Throwable var9) {
                CommandManager.addChatMessage("§c[LiveCode] Runtime error: " + var9.getMessage());
            }
        } else {
            CommandManager.addChatMessage("§c[LiveCode] Eval compile error:");

            for (String err : result.getErrors()) {
                CommandManager.addChatMessage("§c ✖ " + err);
            }
        }
    }

    public synchronized void clearVisuals() {
        for (LiveVisual visual : this.activeVisuals) {
            try {
                visual.onCleanup();
            } catch (Throwable var4) {
            }
        }

        this.activeVisuals.clear();
    }

    public List<LiveVisual> getActiveVisuals() {
        return this.activeVisuals;
    }

    public void tick() {
        if (this.autoWatchEnabled) {
            long now = System.currentTimeMillis();
            if (now - this.lastWatchCheckTime > 1000L) {
                this.lastWatchCheckTime = now;
                this.checkFileModifications();
            }
        }

        for (LiveVisual visual : this.activeVisuals) {
            try {
                visual.onTick();
            } catch (Throwable var4) {
                this.logRuntimeError("tick", var4);
            }
        }
    }

    public void render2D(DrawContext context, float tickDelta) {
        for (LiveVisual visual : this.activeVisuals) {
            try {
                visual.onRender2D(context, tickDelta);
            } catch (Throwable var6) {
                this.logRuntimeError("render2D", var6);
            }
        }
    }

    public void render3D(WorldRenderContext context) {
        for (LiveVisual visual : this.activeVisuals) {
            try {
                visual.onRender3D(context);
            } catch (Throwable var5) {
                this.logRuntimeError("render3D", var5);
            }
        }
    }

    private void checkFileModifications() {
        if (this.scriptsDirectory != null && this.scriptsDirectory.exists()) {
            File[] files = this.scriptsDirectory.listFiles((dir, name) -> name.endsWith(".java"));
            if (files != null) {
                boolean modified = false;

                for (File file : files) {
                    Long lastMod = this.fileLastModified.get(file.getAbsolutePath());
                    if (lastMod == null || file.lastModified() > lastMod) {
                        modified = true;
                        break;
                    }
                }

                if (modified) {
                    this.reloadScripts(true);
                }
            }
        }
    }

    private void logRuntimeError(String stage, Throwable t) {
        long now = System.currentTimeMillis();
        if (now - this.lastErrorLogTime > 3000L) {
            this.lastErrorLogTime = now;
            CommandManager.addChatMessage("§c[LiveCode Runtime Error in " + stage + "] " + t.getMessage());
        }
    }

    private static String extractFullClassName(String fileName, String code) {
        String baseName = fileName.endsWith(".java") ? fileName.substring(0, fileName.length() - 5) : fileName;
        String pkg = "";

        for (String line : code.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("package ") && trimmed.endsWith(";")) {
                pkg = trimmed.substring(8, trimmed.length() - 1).trim();
                break;
            }
        }

        return pkg.isEmpty() ? baseName : pkg + "." + baseName;
    }
}
