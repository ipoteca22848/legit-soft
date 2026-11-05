package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.misc.StreamerMode;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.render.RenderUtil;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class Scoreboard extends Module {
    public static final NumberSetting x = new NumberSetting("Поз X", -1.0, -1.0, 3000.0, 1.0, () -> false);
    public static final NumberSetting y = new NumberSetting("Поз Y", -1.0, -1.0, 2000.0, 1.0, () -> false);
    public static final NumberSetting size = new NumberSetting("Размер", 1.0, 0.5, 2.0, 0.05);
    private boolean wasMouseDown = false;
    private boolean dragging = false;
    private double dragOffsetX = 0.0;
    private double dragOffsetY = 0.0;
    private int lastWidth = 0;
    private int lastHeight = 0;

    public Scoreboard() {
        super("Scoreboard", "Кастомный скорборд", Category.VISUALS);
        this.addSettings(x, y, size);
    }

    @Override
    public void onHudRender(DrawContext context) {
        if (this.mc.world != null && this.mc.player != null) {
            net.minecraft.scoreboard.Scoreboard scoreboard = this.mc.world.getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (objective != null || this.mc.currentScreen instanceof ChatScreen && this.mc.getCurrentServerEntry() != null) {
                float scale = size.getFloatValue();
                int currentW = this.lastWidth > 0 ? this.lastWidth : 100;
                int currentH = this.lastHeight > 0 ? this.lastHeight : 50;
                int scaledW = Math.round(currentW * scale);
                int scaledH = Math.round(currentH * scale);
                int posX = x.getValue().intValue();
                int posY = y.getValue().intValue();
                if (posX == -1) {
                    posX = context.getScaledWindowWidth() - scaledW - 5;
                }

                if (posY == -1) {
                    posY = context.getScaledWindowHeight() / 2 - scaledH / 2;
                }

                if (this.mc.currentScreen instanceof ChatScreen && this.mc.getCurrentServerEntry() != null) {
                    double mouseX = this.mc.mouse.getX() * this.mc.getWindow().getScaledWidth() / this.mc.getWindow().getWidth();
                    double mouseY = this.mc.mouse.getY() * this.mc.getWindow().getScaledHeight() / this.mc.getWindow().getHeight();
                    boolean isMouseDown = GLFW.glfwGetMouseButton(this.mc.getWindow().getHandle(), 0) == 1;
                    if (isMouseDown && !this.wasMouseDown) {
                        if (mouseX >= posX && mouseX <= posX + scaledW && mouseY >= posY && mouseY <= posY + scaledH) {
                            this.dragging = true;
                            this.dragOffsetX = mouseX - posX;
                            this.dragOffsetY = mouseY - posY;
                        }
                    } else if (!isMouseDown) {
                        this.dragging = false;
                    }

                    if (this.dragging) {
                        x.setValue(mouseX - this.dragOffsetX);
                        y.setValue(mouseY - this.dragOffsetY);
                        posX = x.getValue().intValue();
                        posY = y.getValue().intValue();
                    }

                    this.wasMouseDown = isMouseDown;
                    if (objective != null) {
                        RenderUtil.drawBorder(context, posX, posY, scaledW, scaledH, -1);
                    }

                    if (objective == null) {
                        return;
                    }
                } else {
                    this.dragging = false;
                    this.wasMouseDown = false;
                }

                if (objective != null) {
                    context.getMatrices().pushMatrix();

                    try {
                        context.getMatrices().translate(posX, posY);
                        context.getMatrices().scale(scale, scale);
                        this.renderScoreboard(context, objective, 0, 0);
                    } finally {
                        context.getMatrices().popMatrix();
                    }
                }
            }
        }
    }

    private int getBackgroundColor() {
        int color = this.mc.options.getTextBackgroundColor(0);
        float opacity = this.mc.options.getTextBackgroundOpacity(0.3F);
        return (int)(opacity * 255.0F) << 24 | color & 16777215;
    }

    private void renderScoreboard(DrawContext context, ScoreboardObjective objective, int x, int y) {
        net.minecraft.scoreboard.Scoreboard scoreboard = objective.getScoreboard();
        Text title = StreamerMode.replaceInText(objective.getDisplayName());
        List<ScoreboardEntry> scores = scoreboard.getScoreboardEntries(objective)
            .stream()
            .filter(score -> !score.owner().startsWith("#"))
            .sorted(Comparator.comparingInt(ScoreboardEntry::value).reversed().thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER))
            .limit(15L)
            .collect(Collectors.toList());
        int titleWidth;
        int maxWidth = titleWidth = this.mc.textRenderer.getWidth(title);

        for (ScoreboardEntry entry : scores) {
            Team team = scoreboard.getScoreHolderTeam(entry.owner());
            Text name = StreamerMode.replaceInText(Team.decorateName(team, Text.literal(entry.owner())));
            int nameW = this.mc.textRenderer.getWidth(name);
            if (nameW > maxWidth) {
                maxWidth = nameW;
            }
        }

        int finalWidth = maxWidth + 4;
        int currentY = y;
        int bgColor = this.getBackgroundColor();
        int titleX = x + (finalWidth - titleWidth) / 2;
        context.fill(x, currentY, x + finalWidth, currentY + 9, bgColor);
        context.drawText(this.mc.textRenderer, title, titleX, currentY + 1, -1, false);
        currentY += 9;

        for (ScoreboardEntry entry : scores) {
            Team team = scoreboard.getScoreHolderTeam(entry.owner());
            Text name = StreamerMode.replaceInText(Team.decorateName(team, Text.literal(entry.owner())));
            context.fill(x, currentY, x + finalWidth, currentY + 9, bgColor);
            context.drawText(this.mc.textRenderer, name, x + 2, currentY + 1, -1, false);
            currentY += 9;
        }

        this.lastWidth = finalWidth;
        this.lastHeight = currentY - y;
    }
}
