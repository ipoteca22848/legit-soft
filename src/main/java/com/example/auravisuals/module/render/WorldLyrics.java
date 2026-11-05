package com.example.auravisuals.module.render;

import com.example.auravisuals.client.gui.font.CustomFontRenderer;
import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.client.gui.hud.MediaPlayer;
import com.example.auravisuals.client.gui.render.RoundedRenderer;
import com.example.auravisuals.client.gui.render.ShadowRender;
import com.example.auravisuals.client.gui.render.TextRender;
import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.ProjectionHolder;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.theme.ThemeManager;
import com.example.auravisuals.util.media.MediaSessionReader;
import com.example.auravisuals.util.media.SyncedLyricsService;
import com.example.auravisuals.util.media.SyncedLyricsService$LyricLine;
import com.example.auravisuals.util.media.SyncedLyricsService$LyricWord;
import com.example.auravisuals.util.media.SyncedLyricsService$LyricsTrack;
import com.example.auravisuals.util.media.SyncedLyricsService$State;
import com.example.auravisuals.util.render.RenderUtil;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

public class WorldLyrics extends Module {
    static final float MAX_LINE_WIDTH = 225.0F;
    static final float MAX_META_WIDTH = 175.0F;
    static final long LINE_APPEAR_LEAD_MS = 110L;
    static final long WORD_APPEAR_LEAD_MS = 140L;
    final ModeSetting display = new ModeSetting("Отображение", "Строчки", "Строчки", "Слова");
    final ModeSetting mode = new ModeSetting("Mode", "Billboard", "Billboard", "World Space");
    final NumberSetting distance = new NumberSetting("Distance", 4.5, 2.0, 12.0, 0.25);
    final NumberSetting minDistance = new NumberSetting("Минимальная дальность", 8.0, 4.0, 13.0, 0.5);
    final NumberSetting maxDistance = new NumberSetting("Максимальная дальность", 13.0, 7.0, 18.0, 0.5);
    final NumberSetting scale = new NumberSetting("Размер текста", 1.2, 1.0, 3.0, 0.1);
    final NumberSetting depth = new NumberSetting("3D Depth", 0.6, 0.0, 2.0, 0.1);
    final BooleanSetting glow = new BooleanSetting("Neon glow", true);
    final BooleanSetting karaoke = new BooleanSetting("Karaoke", true);
    final BooleanSetting adjacentLines = new BooleanSetting("Adjacent lines", false);
    final BooleanSetting playerCard = new BooleanSetting("Player card", false);
    final BooleanSetting customColor = new BooleanSetting("Custom color", true);
    final ColorSetting color = new ColorSetting("Color", new Color(255, 92, 112), () -> this.customColor.getValue());
    final ColorSetting activeTextColor = new ColorSetting("Активный текст", new Color(255, 255, 255));
    final ColorSetting inactiveTextColor = new ColorSetting("Неактивный текст", new Color(145, 145, 145));
    final MediaSessionReader media = MediaSessionReader.getInstance();
    final SyncedLyricsService lyrics = SyncedLyricsService.getInstance();
    final CustomFontRenderer karaokeFont = new CustomFontRenderer(72.0F, 0.125F, "karaoke_font_atlas");
    String trackKey = "";
    String placementMode = "";
    int renderedIndex = Integer.MIN_VALUE;
    long lineChangedAt;
    Vec3d worldAnchor;
    float worldYaw;
    boolean draggingWorldScreen;
    double dragDepth;
    double dragViewX;
    double dragViewY;
    String previousScreenLine = "";
    float lineOffsetX;
    float lineOffsetY;
    float lineAngle;
    float previousLineOffsetX;
    float previousLineOffsetY;
    float previousLineAngle;
    float lastLineScreenX = Float.NaN;
    float lastLineScreenY = Float.NaN;
    Vec3d previousWorldAnchor;
    Quaternionf worldRotation;
    Quaternionf previousWorldRotation;
    float worldRoll;
    float previousWorldRoll;
    float worldTiltX;
    float previousWorldTiltX;
    float worldTiltY;
    float previousWorldTiltY;
    float worldPhysicalScale = 0.025F;
    float previousWorldPhysicalScale = 0.025F;
    boolean lyricsRequestIssued;
    long trackDetectedAt;
    long requestedLyricsDuration;
    long lineAnimationElapsedNanos;
    long lineColorDurationNanos = 1L;
    long lineLifetimeDurationNanos = 1L;
    long lineAnimationUpdatedAt;
    float currentLineProgress;
    float currentLineOpacity;
    float previousLineProgress;
    float previousLineOpacity;
    long lineFadeDurationNanos = 1L;
    long previousFadeDurationNanos = 1L;
    int finishedLineIndex = -1;
    String activeDisplayMode = "";
    int renderedWordLine = -1;
    int renderedWordIndex = -1;
    WorldLyrics$WorldTextPlacement wordPlacement;
    int lastPresentedWordLine = -1;
    int lastPresentedWordIndex = -1;
    long wordClockPositionNanos = -1L;
    long wordClockUpdatedNanos;

    public WorldLyrics() {
        this("WorldLyrics", "Synchronized 3D lyrics for the currently playing track");
    }

    protected WorldLyrics(String name, String description) {
        super(name, description, Category.VISUALS);
        this.addSettings(this.display, this.minDistance, this.maxDistance, this.scale, this.activeTextColor, this.inactiveTextColor);
    }

    @Override
    public void onEnable() {
        this.media.start();
        this.trackKey = "";
        this.lyricsRequestIssued = false;
        this.trackDetectedAt = 0L;
        this.requestedLyricsDuration = 0L;
        this.resetPlacement();
    }

    @Override
    public void onDisable() {
        this.draggingWorldScreen = false;
        this.resetPlacement();
    }

    @Override
    public void onTick() {
        this.updateTrack();
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.mc.player != null && this.mc.world != null && this.media.hasMedia() && context.consumers() != null) {
            this.updateTrack();
            if (this.lyricsRequestIssued) {
                SyncedLyricsService$LyricsTrack track = this.lyrics.getActiveTrack();
                if (track.state() == SyncedLyricsService$State.READY && !track.lines().isEmpty()) {
                    long lyricsPosition = Math.max(0L, this.media.getImmediatePositionMs());
                    if (!this.display.getValue().equals(this.activeDisplayMode)) {
                        this.resetPlacement();
                        this.activeDisplayMode = this.display.getValue();
                    }

                    if (this.display.is("Слова")) {
                        this.renderWorldWords(context, track, this.monotonicWordPosition(lyricsPosition + 140L));
                    } else {
                        long selectionPosition = lyricsPosition + 110L;
                        int desiredIndex = this.visibleLineIndex(track, track.indexAt(selectionPosition), lyricsPosition);
                        int stableIndex = this.stabilizeLineIndex(track, desiredIndex, selectionPosition);
                        if (stableIndex != this.renderedIndex) {
                            this.changeWorldLine(context, track, stableIndex, lyricsPosition);
                        }

                        long renderNow = System.nanoTime();
                        float transition = MathHelper.clamp((float)(renderNow - this.lineChangedAt) / 1.1E8F, 0.0F, 1.0F);
                        float eased = transition * transition * (3.0F - 2.0F * transition);
                        Vec3d cameraPos = context.camera().getCameraPos();
                        float previousFade = MathHelper.clamp(
                            (float)(renderNow - this.lineChangedAt) / (float)Math.max(1L, this.previousFadeDurationNanos), 0.0F, 1.0F
                        );
                        previousFade = 1.0F - previousFade;
                        if (!this.previousScreenLine.isBlank()
                            && this.previousWorldAnchor != null
                            && this.previousLineOpacity > 0.001F
                            && previousFade > 0.001F) {
                            this.renderFixedWorldLine(
                                context,
                                cameraPos,
                                this.previousWorldAnchor,
                                this.previousWorldRotation,
                                this.previousWorldTiltX,
                                this.previousWorldTiltY,
                                this.previousWorldRoll,
                                this.previousWorldPhysicalScale,
                                this.previousScreenLine,
                                this.previousLineProgress,
                                this.previousLineOpacity * previousFade,
                                1.0F
                            );
                        }

                        if (this.renderedIndex >= 0 && this.renderedIndex < track.lines().size() && this.worldAnchor != null) {
                            SyncedLyricsService$LyricLine current = track.lines().get(this.renderedIndex);
                            this.updateLineAnimation();
                            float progress = MathHelper.clamp((float)this.lineAnimationElapsedNanos / (float)this.lineColorDurationNanos, 0.0F, 1.0F);
                            float exit = this.lineAnimationElapsedNanos <= this.lineColorDurationNanos
                                ? 1.0F
                                : 1.0F
                                    - MathHelper.clamp(
                                        (float)(this.lineAnimationElapsedNanos - this.lineColorDurationNanos)
                                            / (float)Math.max(1L, this.lineLifetimeDurationNanos - this.lineColorDurationNanos),
                                        0.0F,
                                        1.0F
                                    );
                            exit = exit * exit * (3.0F - 2.0F * exit);
                            this.currentLineProgress = progress;
                            this.currentLineOpacity = eased * exit;
                            this.renderFixedWorldLine(
                                context,
                                cameraPos,
                                this.worldAnchor,
                                this.worldRotation,
                                this.worldTiltX,
                                this.worldTiltY,
                                this.worldRoll,
                                this.worldPhysicalScale,
                                current.text(),
                                progress,
                                this.currentLineOpacity,
                                0.965F + eased * 0.035F
                            );
                        }
                    }
                }
            }
        }
    }

    void renderWorldWords(WorldRenderContext context, SyncedLyricsService$LyricsTrack track, long positionMs) {
        int lineIndex = this.visibleLineIndex(track, track.indexAt(positionMs), positionMs);
        if (lineIndex >= 0 && lineIndex < track.lines().size()) {
            SyncedLyricsService$LyricLine line = track.lines().get(lineIndex);
            List<WorldLyrics$TimedWord> words = this.timedWords(line, this.activeLineEnd(track, lineIndex));
            int wordIndex = -1;

            for (int index = 0; index < words.size(); index++) {
                WorldLyrics$TimedWord word = words.get(index);
                if (positionMs >= word.startMs && positionMs < word.endMs) {
                    wordIndex = index;
                    break;
                }
            }

            if (wordIndex < 0) {
                this.clearRenderedWord();
            } else {
                boolean sameActiveWord = lineIndex == this.renderedWordLine && wordIndex == this.renderedWordIndex && this.wordPlacement != null;
                boolean alreadyFinished = lineIndex < this.lastPresentedWordLine
                    || lineIndex == this.lastPresentedWordLine && wordIndex <= this.lastPresentedWordIndex && !sameActiveWord;
                if (!alreadyFinished) {
                    WorldLyrics$TimedWord word = words.get(wordIndex);
                    if (lineIndex != this.renderedWordLine || wordIndex != this.renderedWordIndex || this.wordPlacement == null) {
                        this.renderedWordLine = lineIndex;
                        this.renderedWordIndex = wordIndex;
                        this.lastPresentedWordLine = lineIndex;
                        this.lastPresentedWordIndex = wordIndex;
                        this.wordPlacement = this.randomWorldPlacement(context);
                    }

                    long duration = Math.max(1L, word.endMs - word.startMs);
                    long elapsed = MathHelper.clamp(positionMs - word.startMs, 0L, duration);
                    long appearDuration = Math.min(Math.max(1L, duration / 4L), Math.min(65L, Math.max(28L, Math.round((float)duration * 0.12F))));
                    long fadeDuration = Math.min(
                        Math.max(1L, Math.round((float)duration * 0.42F)), Math.min(380L, Math.max(90L, Math.round((float)duration * 0.32F)))
                    );
                    float appear = MathHelper.clamp((float)elapsed / (float)appearDuration, 0.0F, 1.0F);
                    float fade = MathHelper.clamp((float)(word.endMs - positionMs) / (float)fadeDuration, 0.0F, 1.0F);
                    appear = appear * appear * (3.0F - 2.0F * appear);
                    fade = fade * fade * (3.0F - 2.0F * fade);
                    float progress = MathHelper.clamp((float)elapsed / (float)Math.max(1L, duration - fadeDuration), 0.0F, 1.0F);
                    float appearanceScale = 0.965F + appear * 0.035F;
                    Vec3d cameraPos = context.camera().getCameraPos();
                    this.renderFixedWorldLine(
                        context,
                        cameraPos,
                        this.wordPlacement.anchor,
                        this.wordPlacement.rotation,
                        this.wordPlacement.tiltX,
                        this.wordPlacement.tiltY,
                        this.wordPlacement.roll,
                        this.wordPlacement.physicalScale,
                        word.text,
                        progress,
                        appear * fade,
                        appearanceScale
                    );
                }
            }
        } else {
            this.clearRenderedWord();
        }
    }

    long monotonicWordPosition(long positionMs) {
        long now = System.nanoTime();
        long candidate = Math.max(0L, positionMs) * 1000000L;
        if (this.wordClockPositionNanos < 0L) {
            this.wordClockPositionNanos = candidate;
            this.wordClockUpdatedNanos = now;
            return positionMs;
        }

        long elapsed = Math.max(0L, now - this.wordClockUpdatedNanos);
        this.wordClockUpdatedNanos = now;
        long expected = this.wordClockPositionNanos + (this.media.isPlaying() ? elapsed : 0L);
        long error = candidate - expected;
        if (this.media.isPlaying() && Math.abs(error) <= 4000000000L) {
            this.wordClockPositionNanos = Math.max(expected, candidate);
        } else {
            if (Math.abs(error) > 250000000L) {
                this.clearRenderedWord();
                this.lastPresentedWordLine = -1;
                this.lastPresentedWordIndex = -1;
            }

            this.wordClockPositionNanos = candidate;
        }

        return Math.max(0L, this.wordClockPositionNanos / 1000000L);
    }

    List<WorldLyrics$TimedWord> timedWords(SyncedLyricsService$LyricLine line, long lineEndMs) {
        ArrayList<WorldLyrics$TimedWord> result = new ArrayList<>();
        List enhanced = line.words();
        if (enhanced != null && !enhanced.isEmpty()) {
            this.appendEnhancedWords(result, line, enhanced, lineEndMs);
        } else {
            this.appendTimedWords(result, line.text(), line.timeMs(), lineEndMs);
        }

        return result;
    }

    void appendEnhancedWords(
        List<WorldLyrics$TimedWord> target, SyncedLyricsService$LyricLine line, List<SyncedLyricsService$LyricWord> enhanced, long lineEndMs
    ) {
        ArrayList glyphTimes = new ArrayList();

        for (SyncedLyricsService$LyricWord chunk : enhanced) {
            long time = Math.max(line.timeMs(), chunk.timeMs());
            if (chunk.text() != null) {
                chunk.text().codePoints().filter(codePoint -> !Character.isWhitespace(codePoint)).forEach(codePoint -> glyphTimes.add(time));
            }
        }

        if (glyphTimes.isEmpty()) {
            this.appendTimedWords(target, line.text(), line.timeMs(), lineEndMs);
        } else {
            ArrayList<WorldLyrics$WordStart> starts = new ArrayList<>();
            int glyphCursor = 0;

            for (String word : line.text().strip().split("\\s+")) {
                if (!word.isBlank()) {
                    long start = (Long)glyphTimes.get(Math.min(glyphCursor, glyphTimes.size() - 1));
                    starts.add(new WorldLyrics$WordStart(word, start));
                    glyphCursor += this.rawAnimationGlyphCount(word);
                }
            }

            int index = 0;

            while (index < starts.size()) {
                int endIndex = index + 1;

                while (endIndex < starts.size() && starts.get(endIndex).startMs == starts.get(index).startMs) {
                    endIndex++;
                }

                long end = endIndex < starts.size() ? starts.get(endIndex).startMs : lineEndMs;
                StringBuilder grouped = new StringBuilder();

                for (int groupedIndex = index; groupedIndex < endIndex; groupedIndex++) {
                    if (!grouped.isEmpty()) {
                        grouped.append(' ');
                    }

                    grouped.append(starts.get(groupedIndex).text);
                }

                this.appendTimedWords(target, grouped.toString(), starts.get(index).startMs, Math.max(starts.get(index).startMs + 1L, Math.min(lineEndMs, end)));
                index = endIndex;
            }
        }
    }

    void appendTimedWords(List<WorldLyrics$TimedWord> target, String text, long startMs, long endMs) {
        if (text != null && !text.isBlank() && endMs > startMs) {
            String[] parts = text.strip().split("\\s+");
            double totalWeight = 0.0;

            for (String part : parts) {
                totalWeight += this.wordWeight(part);
            }

            double consumedWeight = 0.0;

            for (String part : parts) {
                if (!part.isBlank()) {
                    long wordStart = startMs + Math.round((endMs - startMs) * consumedWeight / totalWeight);
                    long wordEnd = startMs + Math.round((endMs - startMs) * (consumedWeight += this.wordWeight(part)) / totalWeight);
                    target.add(new WorldLyrics$TimedWord(part, wordStart, Math.max(wordStart + 1L, wordEnd)));
                }
            }
        }
    }

    double wordWeight(String word) {
        return Math.max(1, this.rawAnimationGlyphCount(word)) + 1.5;
    }

    void clearRenderedWord() {
        this.renderedWordLine = -1;
        this.renderedWordIndex = -1;
        this.wordPlacement = null;
    }

    @Override
    public void onHudRender(DrawContext context) {
    }

    int stabilizeLineIndex(SyncedLyricsService$LyricsTrack track, int desiredIndex, long positionMs) {
        if (this.renderedIndex == Integer.MIN_VALUE) {
            return desiredIndex;
        }

        if (desiredIndex == this.renderedIndex) {
            return this.renderedIndex;
        }

        if (this.renderedIndex < 0 || this.renderedIndex >= track.lines().size()) {
            return desiredIndex == this.finishedLineIndex && desiredIndex >= 0 ? -1 : desiredIndex;
        }

        if (desiredIndex < 0) {
            return positionMs >= this.activeLineEnd(track, this.renderedIndex) ? -1 : this.renderedIndex;
        }

        if (desiredIndex > this.renderedIndex) {
            return positionMs >= track.lines().get(desiredIndex).timeMs() ? desiredIndex : this.renderedIndex;
        }

        long currentStart = track.lines().get(this.renderedIndex).timeMs();
        return positionMs < currentStart - 1400L ? desiredIndex : this.renderedIndex;
    }

    int visibleLineIndex(SyncedLyricsService$LyricsTrack track, int index, long positionMs) {
        if (index >= 0 && index < track.lines().size()) {
            return positionMs < this.activeLineEnd(track, index) ? index : -1;
        } else {
            return -1;
        }
    }

    long activeLineEnd(SyncedLyricsService$LyricsTrack track, int index) {
        SyncedLyricsService$LyricLine line = track.lines().get(index);
        long nextStart = index + 1 < track.lines().size() ? track.lines().get(index + 1).timeMs() : Math.max(line.timeMs() + 3500L, this.media.getDurationMs());
        if (track.estimated()) {
            return nextStart;
        }

        long naturalDuration = this.estimatedSpokenDuration(line.text());
        long interval = Math.max(0L, nextStart - line.timeMs());
        return interval > Math.max(6000L, naturalDuration + 1500L) ? Math.min(nextStart, line.timeMs() + naturalDuration) : nextStart;
    }

    long estimatedSpokenDuration(String text) {
        int glyphs = this.animationGlyphCount(text);
        int words = text != null && !text.isBlank() ? text.trim().split("\\s+").length : 0;
        return MathHelper.clamp(500L + glyphs * 70L + words * 180L, 1600L, 6500L);
    }

    void updateLineAnimation() {
        long now = System.nanoTime();
        if (this.lineAnimationUpdatedAt == 0L) {
            this.lineAnimationUpdatedAt = now;
        }

        long elapsed = Math.max(0L, now - this.lineAnimationUpdatedAt);
        this.lineAnimationUpdatedAt = now;
        this.lineAnimationElapsedNanos = Math.min(this.lineLifetimeDurationNanos, this.lineAnimationElapsedNanos + elapsed);
    }

    int animationGlyphCount(String text) {
        return Math.max(1, this.rawAnimationGlyphCount(text));
    }

    int rawAnimationGlyphCount(String text) {
        return text != null && !text.isEmpty() ? (int)text.codePoints().filter(codePoint -> !Character.isWhitespace(codePoint)).count() : 0;
    }

    void changeWorldLine(WorldRenderContext context, SyncedLyricsService$LyricsTrack track, int newIndex, long positionMs) {
        int oldIndex = this.renderedIndex;
        this.previousScreenLine = this.renderedIndex >= 0 && this.renderedIndex < track.lines().size() ? track.lines().get(this.renderedIndex).text() : "";
        this.previousWorldAnchor = this.worldAnchor;
        this.previousWorldRotation = this.worldRotation == null ? null : new Quaternionf(this.worldRotation);
        this.previousWorldRoll = this.worldRoll;
        this.previousWorldTiltX = this.worldTiltX;
        this.previousWorldTiltY = this.worldTiltY;
        this.previousWorldPhysicalScale = this.worldPhysicalScale;
        this.previousLineProgress = this.currentLineProgress;
        this.previousLineOpacity = this.currentLineOpacity;
        this.previousFadeDurationNanos = Math.max(1L, Math.round((float)this.lineFadeDurationNanos * Math.max(0.0F, this.currentLineOpacity)));
        this.renderedIndex = newIndex;
        this.lineAnimationUpdatedAt = this.lineChangedAt = System.nanoTime();
        if (newIndex >= 0 && newIndex < track.lines().size()) {
            SyncedLyricsService$LyricLine line = track.lines().get(newIndex);
            long earlyBy = Math.max(0L, line.timeMs() - positionMs);
            long timingStart = Math.max(positionMs, line.timeMs());
            long remaining = Math.max(300L, this.activeLineEnd(track, newIndex) - timingStart);
            long fadeDuration = MathHelper.clamp(Math.round(remaining * 0.16), 260L, 480L);
            this.lineLifetimeDurationNanos = remaining * 1000000L;
            this.lineFadeDurationNanos = fadeDuration * 1000000L;
            this.lineColorDurationNanos = Math.max(180L, remaining - fadeDuration) * 1000000L;
            this.lineAnimationElapsedNanos = -earlyBy * 1000000L;
            this.currentLineProgress = 0.0F;
            this.currentLineOpacity = 0.0F;
            if (newIndex != this.finishedLineIndex) {
                this.finishedLineIndex = -1;
            }

            this.spawnWorldLine(context);
        } else {
            if (oldIndex >= 0 && oldIndex < track.lines().size()) {
                this.finishedLineIndex = oldIndex;
            }

            this.lineAnimationElapsedNanos = 0L;
            this.lineColorDurationNanos = 1L;
            this.lineLifetimeDurationNanos = 1L;
            this.lineFadeDurationNanos = 1L;
            this.currentLineProgress = 0.0F;
            this.currentLineOpacity = 0.0F;
            this.worldAnchor = null;
        }
    }

    void spawnWorldLine(WorldRenderContext context) {
        WorldLyrics$WorldTextPlacement placement = this.randomWorldPlacement(context);
        this.worldAnchor = placement.anchor;
        this.worldRotation = placement.rotation;
        this.worldTiltX = placement.tiltX;
        this.worldTiltY = placement.tiltY;
        this.worldRoll = placement.roll;
        this.worldPhysicalScale = placement.physicalScale;
    }

    WorldLyrics$WorldTextPlacement randomWorldPlacement(WorldRenderContext context) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Camera camera = context.camera();
        Quaternionf cameraRotation = new Quaternionf(camera.getRotation());
        Vector3f forward = cameraRotation.transform(new Vector3f(0.0F, 0.0F, -1.0F));
        Vector3f right = cameraRotation.transform(new Vector3f(1.0F, 0.0F, 0.0F));
        Vector3f up = cameraRotation.transform(new Vector3f(0.0F, 1.0F, 0.0F));
        double minimum = Math.min(this.minDistance.getValue(), this.maxDistance.getValue());
        double maximum = Math.max(this.minDistance.getValue(), this.maxDistance.getValue());
        double spawnDistance = random.nextDouble(minimum, maximum + 1.0E-4);
        double horizontal = random.nextDouble(-0.48, 0.48) * spawnDistance;
        double vertical = random.nextDouble(-0.28, 0.28) * spawnDistance;
        Vec3d cameraPos = camera.getCameraPos();
        Vec3d anchor = cameraPos.add(forward.x * spawnDistance, forward.y * spawnDistance, forward.z * spawnDistance)
            .add(right.x * horizontal, right.y * horizontal, right.z * horizontal)
            .add(up.x * vertical, up.y * vertical, up.z * vertical);
        return new WorldLyrics$WorldTextPlacement(
            anchor,
            cameraRotation,
            (float)random.nextDouble(-7.0, 7.0),
            (float)random.nextDouble(-10.0, 10.0),
            (float)random.nextDouble(-9.0, 9.0),
            0.02F * (float)(spawnDistance / 8.0) * this.actualTextScale()
        );
    }

    void renderFixedWorldLine(
        WorldRenderContext context,
        Vec3d cameraPos,
        Vec3d anchor,
        Quaternionf rotation,
        float tiltX,
        float tiltY,
        float roll,
        float physicalScale,
        String text,
        float progress,
        float opacity,
        float appearanceScale
    ) {
        if (anchor != null && rotation != null && text != null && !text.isBlank() && !(opacity <= 0.001F)) {
            MatrixStack matrices = context.matrixStack();
            matrices.push();

            try {
                matrices.translate(anchor.x - cameraPos.x, anchor.y - cameraPos.y, anchor.z - cameraPos.z);
                matrices.multiply(rotation);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(tiltX));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(tiltY));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
                float renderScale = physicalScale * appearanceScale;
                matrices.scale(renderScale, -renderScale, renderScale);
                this.drawCharacterSweepWorld(matrices, context.consumers(), text, progress, opacity);
            } finally {
                matrices.pop();
            }
        }
    }

    void drawCharacterSweepWorld(MatrixStack matrices, VertexConsumerProvider consumers, String text, float progress, float opacity) {
        List<String> characters = text.codePoints().mapToObj(codePoint -> new String(Character.toChars(codePoint))).toList();
        if (!characters.isEmpty()) {
            float totalWidth = 0.0F;

            for (String character : characters) {
                totalWidth += this.textWidth(character);
            }

            float fitScale = Math.min(1.0F, 225.0F / Math.max(1.0F, totalWidth));
            float pen = -totalWidth * 0.5F;
            int animatedIndex = 0;
            float sweep = MathHelper.clamp(progress, 0.0F, 1.0F) * this.animationGlyphCount(text);

            for (int index = 0; index < characters.size(); index++) {
                String character = characters.get(index);
                float width = this.textWidth(character);
                boolean animatable = !character.isBlank();
                float active = animatable ? MathHelper.clamp(sweep - animatedIndex, 0.0F, 1.0F) : 0.0F;
                active = active * active * (3.0F - 2.0F * active);
                float characterScale = fitScale * (1.0F + active * 0.045F);
                int valueColor = interpolateTextColor(this.inactiveTextColor.getValue(), this.activeTextColor.getValue(), active, opacity);
                if (!character.isBlank()) {
                    float centerX = pen + width * 0.5F;
                    matrices.push();
                    matrices.translate(centerX, 0.0F, 0.0F);
                    matrices.scale(characterScale, characterScale, 1.0F);
                    matrices.translate(-centerX, 0.0F, 0.0F);
                    if (this.useKaraokeFont()) {
                        this.karaokeFont.drawStringExplicitAlpha(matrices, character, pen, 0.0F, valueColor, false);
                    } else {
                        this.mc
                            .textRenderer
                            .draw(character, pen, 0.0F, valueColor, false, matrices.peek().getPositionMatrix(), consumers, TextLayerType.NORMAL, 0, 15728880);
                    }

                    matrices.pop();
                }

                if (animatable) {
                    animatedIndex++;
                }

                pen += width;
            }
        }
    }

    void chooseRandomLinePlacement(DrawContext context) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        this.lineOffsetX = (float)random.nextDouble(-context.getScaledWindowWidth() * 0.24, context.getScaledWindowWidth() * 0.24);
        this.lineOffsetY = (float)random.nextDouble(-context.getScaledWindowHeight() * 0.18, context.getScaledWindowHeight() * 0.15);
        this.lineAngle = (float)random.nextDouble(-9.0, 9.0);
    }

    void renderAnimatedScreenLine(
        DrawContext context,
        WorldLyrics$ScreenAnchor projected,
        String text,
        float progress,
        float opacity,
        float offsetX,
        float offsetY,
        float angle,
        float appearanceScale,
        boolean rememberPosition
    ) {
        if (text != null && !text.isBlank() && !(opacity <= 0.001F)) {
            float screenWidth = context.getScaledWindowWidth();
            float screenHeight = context.getScaledWindowHeight();
            float size = fitScreenSize(text, 31.0F, screenWidth * 0.72F);
            float renderedWidth = TextRender.width(text, size) * projected.scale() * appearanceScale;
            float marginX = Math.min(screenWidth * 0.42F, renderedWidth * 0.5F + 12.0F);
            float targetX = MathHelper.clamp(projected.x() + offsetX, marginX, screenWidth - marginX);
            float targetY = MathHelper.clamp(projected.y() + offsetY, screenHeight * 0.18F, screenHeight * 0.78F);
            if (rememberPosition) {
                this.lastLineScreenX = targetX;
                this.lastLineScreenY = targetY;
            }

            context.getMatrices().pushMatrix();

            try {
                context.getMatrices().translate(targetX, targetY);
                context.getMatrices().rotate((float)Math.toRadians(angle));
                float totalScale = projected.scale() * appearanceScale;
                context.getMatrices().scale(totalScale, totalScale);
                this.drawCharacterSweep(context, text, size, progress, opacity);
            } finally {
                context.getMatrices().popMatrix();
            }
        }
    }

    void drawCharacterSweep(DrawContext context, String text, float size, float progress, float opacity) {
        List<String> characters = text.codePoints().mapToObj(codePoint -> new String(Character.toChars(codePoint))).toList();
        if (!characters.isEmpty()) {
            float totalWidth = 0.0F;

            for (String character : characters) {
                totalWidth += TextRender.width(character, size);
            }

            float pen = -totalWidth * 0.5F;
            float sweep = MathHelper.clamp(progress, 0.0F, 1.0F) * characters.size();

            for (int index = 0; index < characters.size(); index++) {
                String character = characters.get(index);
                float width = TextRender.width(character, size);
                float active = MathHelper.clamp(sweep - index, 0.0F, 1.0F);
                active = active * active * (3.0F - 2.0F * active);
                float characterScale = 1.0F + active * 0.045F;
                int color = interpolateTextColor(this.inactiveTextColor.getValue(), this.activeTextColor.getValue(), active, opacity);
                if (!character.isBlank()) {
                    float centerX = pen + width * 0.5F;
                    float centerY = -size * 0.32F;
                    context.getMatrices().pushMatrix();
                    context.getMatrices().translate(centerX, centerY);
                    context.getMatrices().scale(characterScale, characterScale);
                    context.getMatrices().translate(-centerX, -centerY);
                    TextRender.draw(context, character, pen, 0.0F, size, color);
                    context.getMatrices().popMatrix();
                }

                pen += width;
            }
        }
    }

    static int interpolateTextColor(int from, int to, float delta, float opacity) {
        int alpha = Math.round(MathHelper.lerp(delta, from >>> 24 & 0xFF, to >>> 24 & 0xFF) * opacity);
        int red = Math.round(MathHelper.lerp(delta, from >>> 16 & 0xFF, to >>> 16 & 0xFF));
        int green = Math.round(MathHelper.lerp(delta, from >>> 8 & 0xFF, to >>> 8 & 0xFF));
        int blue = Math.round(MathHelper.lerp(delta, from & 0xFF, to & 0xFF));
        return MathHelper.clamp(alpha, 0, 255) << 24 | red << 16 | green << 8 | blue;
    }

    public boolean handleWorldMouseButton(int button, int action) {
        return false;
    }

    public boolean handleWorldScroll(double amount) {
        return false;
    }

    void updateWorldScreenDrag() {
        if (this.draggingWorldScreen && this.worldAnchor != null) {
            long window = this.mc.getWindow().getHandle();
            if (GLFW.glfwGetMouseButton(window, 1) != 1) {
                this.draggingWorldScreen = false;
            } else {
                Camera camera = this.mc.gameRenderer.getCamera();
                if (camera != null) {
                    Quaternionf rotation = new Quaternionf(camera.getRotation());
                    Vector3f forward = rotation.transform(new Vector3f(0.0F, 0.0F, -1.0F));
                    Vector3f right = rotation.transform(new Vector3f(1.0F, 0.0F, 0.0F));
                    Vector3f up = rotation.transform(new Vector3f(0.0F, 1.0F, 0.0F));
                    Vec3d cameraPos = camera.getCameraPos();
                    this.worldAnchor = cameraPos.add(forward.x * this.dragDepth, forward.y * this.dragDepth, forward.z * this.dragDepth)
                        .add(right.x * this.dragViewX, right.y * this.dragViewX, right.z * this.dragViewX)
                        .add(up.x * this.dragViewY, up.y * this.dragViewY, up.z * this.dragViewY);
                }
            }
        }
    }

    boolean isPointerOverWorldScreen() {
        if (this.mc.getWindow() != null && this.worldAnchor != null && this.media.hasMedia()) {
            WorldLyrics$ScreenAnchor projected = this.projectWorldAnchor();
            if (projected == null) {
                return false;
            }

            double mouseX = this.mc.mouse.getX() * this.mc.getWindow().getScaledWidth() / this.mc.getWindow().getWidth();
            double mouseY = this.mc.mouse.getY() * this.mc.getWindow().getScaledHeight() / this.mc.getWindow().getHeight();
            float screenScale = projected.scale();
            float centerX = Float.isFinite(this.lastLineScreenX) ? this.lastLineScreenX : projected.x();
            float centerY = Float.isFinite(this.lastLineScreenY) ? this.lastLineScreenY : projected.y();
            return mouseX >= centerX - 165.0F * screenScale
                && mouseX <= centerX + 165.0F * screenScale
                && mouseY >= centerY - 45.0F * screenScale
                && mouseY <= centerY + 45.0F * screenScale;
        } else {
            return false;
        }
    }

    WorldLyrics$ScreenAnchor projectWorldAnchor() {
        Camera camera = this.mc.gameRenderer.getCamera();
        if (camera == null) {
            return null;
        }

        Vec3d cameraPos = camera.getCameraPos();
        Quaternionf rotation = new Quaternionf(camera.getRotation());
        Vector3f forward = rotation.transform(new Vector3f(0.0F, 0.0F, -1.0F));
        if (this.worldAnchor == null) {
            this.worldAnchor = cameraPos.add(forward.x * this.distance.getValue(), forward.y * this.distance.getValue(), forward.z * this.distance.getValue());
        }

        Vec3d relative = this.worldAnchor.subtract(cameraPos);
        Vector3f right = rotation.transform(new Vector3f(1.0F, 0.0F, 0.0F));
        Vector3f up = rotation.transform(new Vector3f(0.0F, 1.0F, 0.0F));
        float viewX = (float)(relative.x * right.x + relative.y * right.y + relative.z * right.z);
        float viewY = (float)(relative.x * up.x + relative.y * up.y + relative.z * up.z);
        float forwardDepth = (float)(relative.x * forward.x + relative.y * forward.y + relative.z * forward.z);
        if (forwardDepth <= 0.05F) {
            return null;
        } else {
            Matrix4f projection = ProjectionHolder.lastProjectionMatrix;
            if (projection == null) {
                return null;
            } else {
                Vector4f clip = projection.transform(new Vector4f(viewX, viewY, -forwardDepth, 1.0F));
                if (clip.w <= 1.0E-4F) {
                    return null;
                } else {
                    float ndcX = clip.x / clip.w;
                    float ndcY = clip.y / clip.w;
                    if (!(ndcX < -1.35F) && !(ndcX > 1.35F) && !(ndcY < -1.35F) && !(ndcY > 1.35F)) {
                        float screenWidth = this.mc.getWindow().getScaledWidth();
                        float screenHeight = this.mc.getWindow().getScaledHeight();
                        float x = (ndcX * 0.5F + 0.5F) * screenWidth;
                        float y = (0.5F - ndcY * 0.5F) * screenHeight;
                        float referenceDistance = Math.max(0.25F, this.distance.getFloatValue());
                        float projectedScale = MathHelper.clamp(referenceDistance / forwardDepth, 0.38F, 1.55F) * this.actualTextScale();
                        return new WorldLyrics$ScreenAnchor(x, y, projectedScale);
                    } else {
                        return null;
                    }
                }
            }
        }
    }

    float actualTextScale() {
        return 1.3F + (this.scale.getFloatValue() - 1.0F) * 1.1F;
    }

    void renderReferenceComposition(DrawContext context, SyncedLyricsService$LyricsTrack track, int index, long positionMs) {
        int screenWidth = this.mc.getWindow().getScaledWidth();
        int screenHeight = this.mc.getWindow().getScaledHeight();
        float centerX = screenWidth * 0.5F;
        float centerY = screenHeight * 0.5F;
        int accent = this.accentColor();
        List lines = track.lines();
        if (!lines.isEmpty() && index >= 0) {
            float transition = MathHelper.clamp((float)(System.nanoTime() - this.lineChangedAt) / 2.8E8F, 0.0F, 1.0F);
            float eased = 1.0F - (1.0F - transition) * (1.0F - transition);
            if (this.adjacentLines.getValue() && index > 0) {
                this.drawScreenLine(
                    context, ((SyncedLyricsService$LyricLine)lines.get(index - 1)).text(), centerX, centerY - 116.0F, 16.5F, withAlpha(11187396, 72), false
                );
            }

            String current = ((SyncedLyricsService$LyricLine)lines.get(index)).text();
            float currentY = centerY - 63.0F + (1.0F - eased) * 4.0F;
            float currentSize = fitScreenSize(current, 31.0F, screenWidth - 80.0F);
            this.drawScreenLine(context, current, centerX, currentY, currentSize, -525828, true);
            if (this.karaoke.getValue()) {
                long start = ((SyncedLyricsService$LyricLine)lines.get(index)).timeMs();
                long end = index + 1 < lines.size()
                    ? ((SyncedLyricsService$LyricLine)lines.get(index + 1)).timeMs()
                    : Math.max(start + 3500L, this.media.getDurationMs());
                float progress = MathHelper.clamp((float)(positionMs - start) / (float)Math.max(250L, end - start), 0.0F, 1.0F);
                this.drawScreenKaraoke(context, current, centerX, currentY, currentSize, progress, accent);
            }

            if (this.adjacentLines.getValue() && index + 1 < lines.size()) {
                this.drawScreenLine(
                    context, ((SyncedLyricsService$LyricLine)lines.get(index + 1)).text(), centerX, centerY + 18.0F, 17.5F, withAlpha(11187396, 88), false
                );
            }
        } else if (!this.media.getTitle().isBlank()) {
            this.drawScreenLine(
                context, this.media.getTitle(), centerX, centerY - 63.0F, fitScreenSize(this.media.getTitle(), 28.0F, screenWidth - 80.0F), -525828, true
            );
        }

        if (this.playerCard.getValue()) {
            this.renderReferenceCard(context, centerX, centerY + 47.0F, accent);
        }
    }

    void drawScreenLine(DrawContext context, String text, float centerX, float baselineY, float size, int textColor, boolean neon) {
        if (text != null && !text.isBlank()) {
            float x = centerX - TextRender.width(text, size) * 0.5F;
            if (neon && this.glow.getValue()) {
                int glowColor = withAlpha(this.accentColor(), 26);
                TextRender.draw(context, text, x - 1.2F, baselineY, size, glowColor);
                TextRender.draw(context, text, x + 1.2F, baselineY, size, glowColor);
                TextRender.draw(context, text, x, baselineY - 1.2F, size, glowColor);
                TextRender.draw(context, text, x, baselineY + 1.2F, size, glowColor);
                TextRender.draw(context, text, x - 0.55F, baselineY, size, withAlpha(this.accentColor(), 42));
                TextRender.draw(context, text, x + 0.55F, baselineY, size, withAlpha(this.accentColor(), 42));
            }

            TextRender.draw(context, text, x, baselineY, size, textColor);
        }
    }

    void drawScreenKaraoke(DrawContext context, String full, float centerX, float baselineY, float size, float progress, int accent) {
        int count = full.codePointCount(0, full.length());
        if (count != 0) {
            int visible = MathHelper.clamp(Math.round(count * progress), 0, count);
            String prefix = full.substring(0, full.offsetByCodePoints(0, visible));
            float x = centerX - TextRender.width(full, size) * 0.5F;
            if (!prefix.isEmpty()) {
                TextRender.draw(context, prefix, x, baselineY, size, withAlpha(accent, 255));
            }

            int activePoint = MathHelper.clamp((int)Math.floor(progress * count), 0, count - 1);
            int charIndex;
            int start = charIndex = full.offsetByCodePoints(0, activePoint);

            while (start > 0 && !Character.isWhitespace(full.charAt(start - 1))) {
                start--;
            }

            int end = charIndex;

            while (end < full.length() && !Character.isWhitespace(full.charAt(end))) {
                end++;
            }

            if (end > start) {
                String before = full.substring(0, start);
                String word = full.substring(start, end);
                float wordX = x + TextRender.width(before, size);
                int cyan = -9971457;
                TextRender.draw(context, word, wordX - 0.7F, baselineY, size, 677894399);
                TextRender.draw(context, word, wordX + 0.7F, baselineY, size, 677894399);
                TextRender.draw(context, word, wordX, baselineY, size, cyan);
            }
        }
    }

    void renderReferenceCard(DrawContext context, float centerX, float y, int accent) {
        MediaPlayer artwork = MediaPlayer.INSTANCE;
        artwork.prepareWorldArtwork();
        float width = 282.0F;
        float height = 66.0F;
        float x = centerX - width * 0.5F;
        ShadowRender.draw(context, x, y, width, height, 8.0F, 5.0F, 2013265920);
        RoundedRenderer.drawRoundedRect(context, x, y, width, height, 8.0F, -401533153);
        RoundedRenderer.drawRoundedOutline(context, x, y, width, height, 8.0F, 0.7F, 620756991);
        float artX = x + 7.0F;
        float artY = y + 6.0F;
        float artSize = 54.0F;
        if (artwork.hasWorldArtwork()) {
            float textureWidth = artwork.getWorldArtworkWidth();
            float textureHeight = artwork.getWorldArtworkHeight();
            float crop = Math.min(textureWidth, textureHeight);
            float u = (textureWidth - crop) * 0.5F;
            float v = (textureHeight - crop) * 0.5F;
            RenderUtil.drawRoundedTexture(
                context,
                artwork.getWorldArtworkTexture(),
                artX,
                artY,
                artSize,
                artSize,
                6.0F,
                true,
                true,
                true,
                true,
                u,
                v,
                crop,
                crop,
                textureWidth,
                textureHeight,
                1.0F
            );
            RoundedRenderer.drawRoundedOutline(context, artX, artY, artSize, artSize, 6.0F, 0.8F, 1348983039);
        } else {
            RoundedRenderer.drawRoundedRect(context, artX, artY, artSize, artSize, 6.0F, withAlpha(darken(accent, 0.28F), 255));
        }

        float textX = x + 73.0F;
        String title = fitScreen(this.media.getTitle(), 15.0F, 150.0F);
        String artist = fitScreen(this.media.getArtist(), 11.5F, 150.0F);
        TextRender.draw(context, title, textX, y + 21.0F, 15.0F, -591621);
        TextRender.draw(context, artist, textX, y + 36.5F, 11.5F, -5195065);
        long position = this.media.getPositionMs();
        long duration = this.media.getDurationMs();
        float progress = duration > 0L ? MathHelper.clamp((float)position / (float)duration, 0.0F, 1.0F) : 0.0F;
        float progressX = textX;
        float progressY = y + 51.0F;
        float timeSize = 9.5F;
        String time = formatTime(position) + " / " + formatTime(duration);
        float timeWidth = TextRender.width(time, timeSize);
        float timeX = x + width - 10.0F - timeWidth;
        float progressWidth = Math.max(24.0F, timeX - 10.0F - progressX);
        RoundedRenderer.drawRoundedRect(context, progressX, progressY, progressWidth, 2.0F, 1.0F, 1174405119);
        RoundedRenderer.drawRoundedRect(context, progressX, progressY, Math.max(1.0F, progressWidth * progress), 2.0F, 1.0F, withAlpha(accent, 255));
        float knobX = progressX + progressWidth * progress;
        ShadowRender.draw(context, knobX - 2.0F, progressY - 2.0F, 4.0F, 4.0F, 2.0F, 2.4F, -1335371521);
        RoundedRenderer.drawRoundedRect(context, knobX - 2.0F, progressY - 2.0F, 4.0F, 4.0F, 2.0F, -1);
        TextRender.draw(context, time, timeX, y + 54.0F, timeSize, -4274480);
    }

    static float fitScreenSize(String text, float preferred, float maxWidth) {
        float unitWidth = TextRender.width(text, 1.0F);
        return unitWidth <= 0.0F ? preferred : Math.min(preferred, maxWidth / unitWidth);
    }

    static String fitScreen(String text, float size, float maxWidth) {
        if (text != null && !text.isBlank() && !(TextRender.width(text, size) <= maxWidth)) {
            String suffix = "...";
            int end = text.length();

            while (end > 0) {
                String candidate = text.substring(0, end = text.offsetByCodePoints(end, -1)).stripTrailing() + suffix;
                if (TextRender.width(candidate, size) <= maxWidth) {
                    return candidate;
                }
            }

            return suffix;
        } else {
            return text == null ? "" : text;
        }
    }

    void updateTrack() {
        if (this.media.hasMedia()) {
            String newKey = this.media.getArtist() + "\n" + this.media.getTitle();
            if (!newKey.equals(this.trackKey)) {
                this.trackKey = newKey;
                this.lyricsRequestIssued = false;
                this.trackDetectedAt = System.currentTimeMillis();
                this.requestedLyricsDuration = 0L;
                this.resetPlacement();
            }

            long duration;
            if (!this.lyricsRequestIssued && ((duration = this.media.getDurationMs()) >= 20000L || System.currentTimeMillis() - this.trackDetectedAt >= 1800L)) {
                this.requestedLyricsDuration = duration;
                this.lyricsRequestIssued = true;
            }

            if (this.lyricsRequestIssued) {
                this.lyrics.request(this.media.getTitle(), this.media.getArtist(), this.requestedLyricsDuration);
            }
        }
    }

    Vec3d resolveAnchor(WorldRenderContext context, Vec3d cameraPos) {
        if (!this.mode.getValue().equals(this.placementMode)) {
            this.placementMode = this.mode.getValue();
            this.worldAnchor = null;
        }

        Vec3d forward = lookVector(context.camera().getYaw(), context.camera().getPitch());
        if (this.mode.is("Billboard")) {
            this.worldAnchor = null;
            return cameraPos.add(forward.multiply(this.distance.getValue()));
        }

        if (this.worldAnchor == null) {
            this.worldAnchor = cameraPos.add(forward.multiply(this.distance.getValue()));
            this.worldYaw = context.camera().getYaw();
        }

        return this.worldAnchor;
    }

    void renderLyrics(MatrixStack matrices, VertexConsumerProvider consumers, SyncedLyricsService$LyricsTrack track, int index, long positionMs) {
        if (consumers != null) {
            int accent = this.accentColor();
            if (this.playerCard.getValue()) {
                this.renderWorldCard(matrices, consumers, accent);
            }

            List lines;
            if ((lines = track.lines()).isEmpty()) {
                this.renderNowPlaying(matrices, consumers, track, accent);
            } else if (index < 0) {
                this.drawLine(
                    matrices, consumers, ((SyncedLyricsService$LyricLine)lines.getFirst()).text(), -3.0F, 0.76F, withAlpha(16777215, 120), true, false
                );
            } else {
                float transition = MathHelper.clamp((float)(System.nanoTime() - this.lineChangedAt) / 2.6E8F, 0.0F, 1.0F);
                float eased = 1.0F - (1.0F - transition) * (1.0F - transition);
                float currentY = -3.0F + (1.0F - eased) * 2.5F;
                if (this.adjacentLines.getValue() && index > 0) {
                    this.drawLine(
                        matrices, consumers, ((SyncedLyricsService$LyricLine)lines.get(index - 1)).text(), -31.0F, 0.58F, withAlpha(14476786, 62), false, false
                    );
                }

                String current = ((SyncedLyricsService$LyricLine)lines.get(index)).text();
                float currentScale = 1.16F + (1.0F - eased) * 0.055F;
                this.drawLine(matrices, consumers, current, currentY, currentScale, withAlpha(16317183, 255), true, false);
                if (this.karaoke.getValue()) {
                    long start = ((SyncedLyricsService$LyricLine)lines.get(index)).timeMs();
                    float progress = MathHelper.clamp(
                        (float)(positionMs - start)
                            / (float)Math.max(
                                250L,
                                (
                                        index + 1 < lines.size()
                                            ? ((SyncedLyricsService$LyricLine)lines.get(index + 1)).timeMs()
                                            : Math.max(start + 3500L, this.media.getDurationMs())
                                    )
                                    - start
                            ),
                        0.0F,
                        1.0F
                    );
                    String highlighted = prefixByProgress(current, progress);
                    if (!highlighted.isEmpty()) {
                        this.drawLineAlignedToFull(matrices, consumers, highlighted, current, currentY, currentScale, withAlpha(accent, 255));
                    }

                    this.drawActiveWord(matrices, consumers, current, progress, currentY, currentScale);
                }

                if (this.adjacentLines.getValue() && index + 1 < lines.size()) {
                    this.drawLine(
                        matrices, consumers, ((SyncedLyricsService$LyricLine)lines.get(index + 1)).text(), 24.0F, 0.58F, withAlpha(14476786, 72), false, false
                    );
                }
            }
        }
    }

    void renderNowPlaying(MatrixStack matrices, VertexConsumerProvider consumers, SyncedLyricsService$LyricsTrack track, int accent) {
        String title = this.fit(this.media.getTitle(), 225.0F);
        String artist = this.fit(this.media.getArtist(), 175.0F);
        this.drawLine(matrices, consumers, title, -8.0F, 0.78F, withAlpha(16777215, 245), true, true);
        if (!artist.isBlank()) {
            this.drawLine(matrices, consumers, artist, 7.0F, 0.46F, withAlpha(accent, 205), false, false);
        }

        if (track.state() == SyncedLyricsService$State.LOADING) {
            this.drawLine(matrices, consumers, "syncing", 18.0F, 0.34F, withAlpha(16777215, 58), false, false);
        }
    }

    void renderWorldCard(MatrixStack matrices, VertexConsumerProvider consumers, int accent) {
        MediaPlayer playerCardRenderer = MediaPlayer.INSTANCE;
        playerCardRenderer.prepareWorldArtwork();
        float x = -80.0F;
        float y = 43.0F;
        float width = 160.0F;
        float height = 40.0F;
        this.drawRoundedWorldRect(matrices, x - 2.0F, y - 2.0F, width + 4.0F, height + 4.0F, 7.0F, -0.65F, 637534208);
        this.drawRoundedWorldRect(matrices, x, y, width, height, 6.0F, -0.5F, -401599203);
        this.drawRoundedWorldRect(matrices, x, y, 2.0F, height, 1.0F, -0.25F, withAlpha(accent, 255));
        float artX = x + 6.0F;
        float artY = y + 5.0F;
        float artSize = 30.0F;
        if (playerCardRenderer.hasWorldArtwork()) {
            float imageWidth = playerCardRenderer.getWorldArtworkWidth();
            float imageHeight = playerCardRenderer.getWorldArtworkHeight();
            float u0 = 0.0F;
            float v0 = 0.0F;
            float u1 = 1.0F;
            float v1 = 1.0F;
            if (imageWidth > imageHeight && imageWidth > 0.0F) {
                float visible = imageHeight / imageWidth;
                u0 = (1.0F - visible) * 0.5F;
                u1 = 1.0F - u0;
            } else if (imageHeight > imageWidth && imageHeight > 0.0F) {
                float visible = imageWidth / imageHeight;
                v0 = (1.0F - visible) * 0.5F;
                v1 = 1.0F - v0;
            }

            this.drawWorldTexture(matrices, playerCardRenderer.getWorldArtworkTexture(), artX, artY, artSize, artSize, u0, v0, u1, v1);
        } else {
            this.drawRoundedWorldRect(matrices, artX, artY, artSize, artSize, 4.0F, -0.2F, withAlpha(darken(accent, 0.35F), 255));
            this.drawRoundedWorldRect(matrices, artX + 12.0F, artY + 12.0F, 12.0F, 12.0F, 6.0F, 0.0F, withAlpha(accent, 230));
        }

        float textX = x + 42.0F;
        String title = this.fit(this.media.getTitle(), 88.0F);
        String artist = this.fit(this.media.getArtist(), 88.0F);
        this.drawWorldTextAt(matrices, consumers, title, textX, y + 5.0F, 0.52F, -525828);
        this.drawWorldTextAt(matrices, consumers, artist, textX, y + 14.0F, 0.38F, -6313288);
        long position = this.media.getPositionMs();
        long duration = this.media.getDurationMs();
        float progress = duration > 0L ? MathHelper.clamp((float)position / (float)duration, 0.0F, 1.0F) : 0.0F;
        float progressX = textX;
        float progressY = y + 29.5F;
        float progressWidth = 73.0F;
        this.drawRoundedWorldRect(matrices, progressX, progressY, progressWidth, 1.5F, 0.75F, 0.05F, 956301311);
        this.drawRoundedWorldRect(matrices, progressX, progressY, Math.max(1.0F, progressWidth * progress), 1.5F, 0.75F, 0.08F, withAlpha(accent, 255));
        this.drawRoundedWorldRect(matrices, progressX + progressWidth * progress - 1.25F, progressY - 0.5F, 2.5F, 2.5F, 1.25F, 0.12F, -1);
        this.drawWorldTextAt(matrices, consumers, formatTime(position) + " / " + formatTime(duration), x + 119.0F, y + 26.5F, 0.3F, -4800564);
    }

    void drawActiveWord(MatrixStack matrices, VertexConsumerProvider consumers, String full, float progress, float y, float lineScale) {
        int codePoints = full.codePointCount(0, full.length());
        if (codePoints != 0) {
            int activePoint = MathHelper.clamp((int)Math.floor(progress * codePoints), 0, codePoints - 1);
            int charIndex;
            int start = charIndex = full.offsetByCodePoints(0, activePoint);

            while (start > 0 && !Character.isWhitespace(full.charAt(start - 1))) {
                start--;
            }

            int end = charIndex;

            while (end < full.length() && !Character.isWhitespace(full.charAt(end))) {
                end++;
            }

            if (end > start) {
                this.drawSegmentAligned(matrices, consumers, full, start, end, y, lineScale, -9971457);
            }
        }
    }

    void drawSegmentAligned(MatrixStack matrices, VertexConsumerProvider consumers, String full, int start, int end, float y, float lineScale, int segmentColor) {
        String before = full.substring(0, start);
        String segment = full.substring(start, end);
        int fullWidth = Math.max(1, this.textWidth(full));
        lineScale = Math.min(lineScale, 225.0F / fullWidth);
        float x = -fullWidth / 2.0F + this.textWidth(before);
        matrices.push();

        try {
            matrices.scale(lineScale, lineScale, 1.0F);
            matrices.translate(0.0F, 0.0F, 0.16F);
            if (FontManager.isUsingCustomFont()) {
                FontManager.customFontRenderer.drawString(matrices, segment, x, y / lineScale, segmentColor, false);
            } else {
                this.mc
                    .textRenderer
                    .draw(segment, x, y / lineScale, segmentColor, false, matrices.peek().getPositionMatrix(), consumers, TextLayerType.NORMAL, 0, 15728880);
            }
        } finally {
            matrices.pop();
        }
    }

    void drawWorldTextAt(MatrixStack matrices, VertexConsumerProvider consumers, String text, float x, float y, float textScale, int textColor) {
        if (text != null && !text.isBlank()) {
            matrices.push();

            try {
                matrices.translate(x, y, 0.18F);
                matrices.scale(textScale, textScale, 1.0F);
                if (FontManager.isUsingCustomFont()) {
                    FontManager.customFontRenderer.drawString(matrices, text, 0.0F, 0.0F, textColor, false);
                } else {
                    this.mc
                        .textRenderer
                        .draw(text, 0.0F, 0.0F, textColor, false, matrices.peek().getPositionMatrix(), consumers, TextLayerType.NORMAL, 0, 15728880);
                }
            } finally {
                matrices.pop();
            }
        }
    }

    void drawRoundedWorldRect(MatrixStack matrices, float x, float y, float width, float height, float radius, float z, int rectColor) {
        if (!(width <= 0.0F) && !(height <= 0.0F)) {
            float halfWidth = width / 2.0F;
            float halfHeight = height / 2.0F;
            float safeRadius = Math.min(radius, Math.min(halfWidth, halfHeight));
            float centerX = x + halfWidth;
            float centerY = y + halfHeight;
            int points = 32;
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, centerX, centerY, z).color(rectColor);

            for (int i = 0; i <= points; i++) {
                double angle = (Math.PI * 2) * i / points;
                float cos = (float)Math.cos(angle);
                float sin = (float)Math.sin(angle);
                float px = centerX + Math.copySign(halfWidth - safeRadius, cos) + cos * safeRadius;
                float py = centerY + Math.copySign(halfHeight - safeRadius, sin) + sin * safeRadius;
                buffer.vertex(matrix, px, py, z).color(rectColor);
            }

            AuraRender.drawColor(buffer, AuraRender.POS_COLOR_TRI_FAN_NODEPTH);
        }
    }

    void drawWorldTexture(MatrixStack matrices, Identifier texture, float x, float y, float width, float height, float u0, float v0, float u1, float v1) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, x, y + height, -0.1F).texture(u0, v1).color(-1);
        buffer.vertex(matrix, x + width, y + height, -0.1F).texture(u1, v1).color(-1);
        buffer.vertex(matrix, x + width, y, -0.1F).texture(u1, v0).color(-1);
        buffer.vertex(matrix, x, y, -0.1F).texture(u0, v0).color(-1);
        AuraRender.drawTexAuto(buffer, texture, false, false);
    }

    void drawLine(MatrixStack matrices, VertexConsumerProvider consumers, String text, float y, float lineScale, int frontColor, boolean neon, boolean extruded) {
        if (text != null && !text.isBlank()) {
            if (FontManager.isUsingCustomFont()) {
                this.drawCustomLine(matrices, text, y, lineScale, frontColor, neon, extruded);
            } else {
                TextRenderer renderer = this.mc.textRenderer;
                lineScale = Math.min(lineScale, 225.0F / Math.max(1, renderer.getWidth(text)));
                float x = -renderer.getWidth(text) * lineScale / 2.0F;
                Matrix4f base = new Matrix4f(matrices.peek().getPositionMatrix()).scale(lineScale, lineScale, 1.0F);
                if (neon && this.glow.getValue()) {
                    int glowColor = withAlpha(this.accentColor(), 34);
                    float radius = 1.35F;

                    for (int i = 0; i < 8; i++) {
                        double angle = (Math.PI * 2) * i / 8.0;
                        renderer.draw(
                            text,
                            x / lineScale + (float)Math.cos(angle) * radius,
                            y / lineScale + (float)Math.sin(angle) * radius,
                            glowColor,
                            false,
                            base,
                            consumers,
                            TextLayerType.SEE_THROUGH,
                            0,
                            15728880
                        );
                    }
                }

                for (int layer = extruded ? Math.max(0, Math.round(this.depth.getFloatValue() * 4.0F)) : 0; layer >= 1; layer--) {
                    Matrix4f sideMatrix = new Matrix4f(base).translate(0.0F, 0.0F, -layer * 0.42F);
                    renderer.draw(
                        text,
                        x / lineScale,
                        y / lineScale,
                        withAlpha(darken(this.accentColor(), 0.28F), 220),
                        false,
                        sideMatrix,
                        consumers,
                        TextLayerType.NORMAL,
                        0,
                        15728880
                    );
                }

                renderer.draw(text, x / lineScale, y / lineScale, frontColor, false, base, consumers, TextLayerType.NORMAL, 0, 15728880);
            }
        }
    }

    void drawCustomLine(MatrixStack matrices, String text, float y, float lineScale, int frontColor, boolean neon, boolean extruded) {
        int width = Math.max(1, FontManager.getWidth(text));
        lineScale = Math.min(lineScale, 225.0F / width);
        float x = -width / 2.0F;
        float localY = y / lineScale;
        matrices.push();

        try {
            matrices.scale(lineScale, lineScale, 1.0F);
            if (neon && this.glow.getValue()) {
                int glowColor = withAlpha(this.accentColor(), 22);
                FontManager.customFontRenderer.drawString(matrices, text, x - 0.45F, localY, glowColor, false);
                FontManager.customFontRenderer.drawString(matrices, text, x + 0.45F, localY, glowColor, false);
                FontManager.customFontRenderer.drawString(matrices, text, x, localY - 0.45F, glowColor, false);
                FontManager.customFontRenderer.drawString(matrices, text, x, localY + 0.45F, glowColor, false);
            }

            for (int layer = extruded ? Math.max(0, Math.round(this.depth.getFloatValue() * 3.0F)) : 0; layer >= 1; layer--) {
                matrices.push();
                matrices.translate(0.0F, 0.0F, -layer * 0.5F);
                FontManager.customFontRenderer.drawString(matrices, text, x, localY, withAlpha(darken(this.accentColor(), 0.28F), 220), false);
                matrices.pop();
            }

            matrices.translate(0.0F, 0.0F, 0.08F);
            FontManager.customFontRenderer.drawString(matrices, text, x, localY, frontColor, false);
        } finally {
            matrices.pop();
        }
    }

    void drawLineAlignedToFull(MatrixStack matrices, VertexConsumerProvider consumers, String prefix, String full, float y, float lineScale, int valueColor) {
        if (FontManager.isUsingCustomFont()) {
            int width = Math.max(1, FontManager.getWidth(full));
            lineScale = Math.min(lineScale, 225.0F / width);
            matrices.push();

            try {
                matrices.scale(lineScale, lineScale, 1.0F);
                matrices.translate(0.0F, 0.0F, 0.12F);
                FontManager.customFontRenderer.drawString(matrices, prefix, -width / 2.0F, y / lineScale, valueColor, false);
            } finally {
                matrices.pop();
            }
        } else {
            TextRenderer renderer = this.mc.textRenderer;
            lineScale = Math.min(lineScale, 225.0F / Math.max(1, renderer.getWidth(full)));
            float fullX = -renderer.getWidth(full) / 2.0F;
            Matrix4f matrix = new Matrix4f(matrices.peek().getPositionMatrix()).scale(lineScale, lineScale, 1.0F).translate(0.0F, 0.0F, 0.08F);
            renderer.draw(prefix, fullX, y / lineScale, valueColor, false, matrix, consumers, TextLayerType.NORMAL, 0, 15728880);
        }
    }

    int accentColor() {
        return (this.customColor.getValue() ? this.color.getValue() : ThemeManager.INSTANCE.getVisualModulesColor()) & 16777215;
    }

    void resetPlacement() {
        this.worldAnchor = null;
        this.draggingWorldScreen = false;
        this.placementMode = "";
        this.renderedIndex = Integer.MIN_VALUE;
        this.previousScreenLine = "";
        this.lineOffsetX = 0.0F;
        this.lineOffsetY = 0.0F;
        this.lineAngle = 0.0F;
        this.previousLineOffsetX = 0.0F;
        this.previousLineOffsetY = 0.0F;
        this.previousLineAngle = 0.0F;
        this.lastLineScreenX = Float.NaN;
        this.lastLineScreenY = Float.NaN;
        this.previousWorldAnchor = null;
        this.worldRotation = null;
        this.previousWorldRotation = null;
        this.worldRoll = 0.0F;
        this.previousWorldRoll = 0.0F;
        this.worldTiltX = 0.0F;
        this.previousWorldTiltX = 0.0F;
        this.worldTiltY = 0.0F;
        this.previousWorldTiltY = 0.0F;
        this.worldPhysicalScale = 0.025F;
        this.previousWorldPhysicalScale = 0.025F;
        this.lineAnimationElapsedNanos = 0L;
        this.lineColorDurationNanos = 1L;
        this.lineLifetimeDurationNanos = 1L;
        this.lineAnimationUpdatedAt = 0L;
        this.currentLineProgress = 0.0F;
        this.currentLineOpacity = 0.0F;
        this.previousLineProgress = 0.0F;
        this.previousLineOpacity = 0.0F;
        this.lineFadeDurationNanos = 1L;
        this.previousFadeDurationNanos = 1L;
        this.finishedLineIndex = -1;
        this.clearRenderedWord();
        this.lastPresentedWordLine = -1;
        this.lastPresentedWordIndex = -1;
        this.wordClockPositionNanos = -1L;
        this.wordClockUpdatedNanos = 0L;
        this.lineChangedAt = System.nanoTime();
    }

    static Vec3d lookVector(float yawDegrees, float pitchDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double cosPitch = Math.cos(pitch);
        return new Vec3d(-Math.sin(yaw) * cosPitch, -Math.sin(pitch), Math.cos(yaw) * cosPitch);
    }

    static String prefixByProgress(String text, float progress) {
        int count = text.codePointCount(0, text.length());
        int visible = MathHelper.clamp(Math.round(count * progress), 0, count);
        return text.substring(0, text.offsetByCodePoints(0, visible));
    }

    static String formatTime(long milliseconds) {
        long seconds = Math.max(0L, milliseconds) / 1000L;
        return "%d:%02d".formatted(seconds / 60L, seconds % 60L);
    }

    String fit(String text, float maxWidth) {
        if (text != null && !text.isBlank()) {
            if (this.textWidth(text) <= maxWidth) {
                return text;
            }

            String suffix = "...";
            int end = text.length();

            while (end > 0) {
                String candidate = text.substring(0, end = text.offsetByCodePoints(end, -1)).stripTrailing() + suffix;
                if (this.textWidth(candidate) <= maxWidth) {
                    return candidate;
                }
            }

            return suffix;
        } else {
            return "";
        }
    }

    int textWidth(String text) {
        return this.useKaraokeFont() ? this.karaokeFont.getWidth(text) : this.mc.textRenderer.getWidth(text);
    }

    boolean useKaraokeFont() {
        return FontManager.isUsingCustomFont() && this.karaokeFont.isAvailable();
    }

    static int darken(int rgb, float factor) {
        int red = Math.round((rgb >> 16 & 0xFF) * factor);
        int green = Math.round((rgb >> 8 & 0xFF) * factor);
        int blue = Math.round((rgb & 0xFF) * factor);
        return red << 16 | green << 8 | blue;
    }

    static int withAlpha(int rgb, int alpha) {
        return MathHelper.clamp(alpha, 0, 255) << 24 | rgb & 16777215;
    }
}
