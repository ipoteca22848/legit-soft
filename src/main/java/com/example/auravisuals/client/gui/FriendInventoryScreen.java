package com.example.auravisuals.client.gui;

import com.example.auravisuals.util.friendscreen.FriendScreenClient;
import com.example.auravisuals.util.friendscreen.FriendScreenClient$PlayerInfo;
import com.example.auravisuals.util.render.RenderUtil;
import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class FriendInventoryScreen extends Screen {
    private static final Identifier INVENTORY = Identifier.ofVanilla("textures/gui/container/inventory.png");
    private static final int BACKGROUND_WIDTH = 176;
    private static final int BACKGROUND_HEIGHT = 166;
    private final Screen parent;
    private final String login;
    private final String displayName;
    private float animation;
    private OtherClientPlayerEntity previewPlayer;
    private List<Text> hoveredTooltip;

    public FriendInventoryScreen(Screen parent, String login, String displayName) {
        super(Text.literal("Инвентарь друга"));
        this.parent = parent;
        this.login = login;
        this.displayName = displayName;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this.animation = this.animation + (1.0F - this.animation) * MathHelper.clamp(deltaTicks * 0.18F, 0.0F, 1.0F);
        context.fill(0, 0, this.width, this.height, 1912602624);
        int x = (this.width - 176) / 2;
        int y = (this.height - 166) / 2;
        float ease = 1.0F - (float)Math.pow(1.0F - this.animation, 3.0);
        this.hoveredTooltip = null;
        context.getMatrices().pushMatrix();

        try {
            context.getMatrices().translate(this.width / 2.0F, this.height / 2.0F);
            context.getMatrices().scale(0.86F + ease * 0.14F, 0.86F + ease * 0.14F);
            context.getMatrices().translate(-this.width / 2.0F, -this.height / 2.0F);
            RenderUtil.drawTexture(context, INVENTORY, x, y, 176.0F, 166.0F, 0.0F, 0.0F, 176.0F, 166.0F, 256.0F, 256.0F, false);
            context.drawTextWithShadow(this.textRenderer, this.displayName, x + 8, y - 12, -11141291);
            FriendScreenClient$PlayerInfo info = FriendScreenClient.INSTANCE.getPlayerInfo(this.login);
            this.drawPlayerModel(context, info, x, y, mouseX, mouseY);
            this.drawInventory(context, info, x, y, mouseX, mouseY);
        } finally {
            context.getMatrices().popMatrix();
        }

        if (this.hoveredTooltip != null && !this.hoveredTooltip.isEmpty()) {
            context.drawTooltip(this.textRenderer, this.hoveredTooltip, mouseX, mouseY);
        }
    }

    private void drawInventory(DrawContext context, FriendScreenClient$PlayerInfo info, int x, int y, int mouseX, int mouseY) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = 9 + row * 9 + column;
                this.drawStack(
                    context,
                    info == null ? ItemStack.EMPTY : stack(info.inventoryIds[slot], info.inventoryCounts[slot]),
                    info == null ? null : info.inventoryTooltips[slot],
                    x + 8 + column * 18,
                    y + 84 + row * 18,
                    mouseX,
                    mouseY
                );
            }
        }

        for (int column = 0; column < 9; column++) {
            this.drawStack(
                context,
                info == null ? ItemStack.EMPTY : stack(info.inventoryIds[column], info.inventoryCounts[column]),
                info == null ? null : info.inventoryTooltips[column],
                x + 8 + column * 18,
                y + 142,
                mouseX,
                mouseY
            );
        }

        for (int armor = 0; armor < 4; armor++) {
            this.drawStack(
                context,
                info == null ? ItemStack.EMPTY : stack(info.equipmentIds[armor], info.equipmentCounts[armor]),
                info == null ? null : info.equipmentTooltips[armor],
                x + 8,
                y + 8 + armor * 18,
                mouseX,
                mouseY
            );
        }

        this.drawStack(
            context,
            info == null ? ItemStack.EMPTY : stack(info.equipmentIds[5], info.equipmentCounts[5]),
            info == null ? null : info.equipmentTooltips[5],
            x + 77,
            y + 62,
            mouseX,
            mouseY
        );
    }

    private void drawStack(DrawContext context, ItemStack stack, String[] tooltip, int x, int y, int mouseX, int mouseY) {
        if (!stack.isEmpty()) {
            context.drawItem(stack, x, y);
            context.drawStackOverlay(this.textRenderer, stack, x, y);
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                this.hoveredTooltip = this.tooltipText(tooltip, stack);
            }
        }
    }

    private List<Text> tooltipText(String[] lines, ItemStack fallback) {
        List<Text> result = new ArrayList<>();
        if (lines != null) {
            for (int i = 0; i < lines.length; i++) {
                if (lines[i] != null && !lines[i].isBlank()) {
                    result.add(i == 0 ? Text.literal(lines[i]) : Text.literal(lines[i]).formatted(Formatting.GRAY));
                }
            }
        }

        if (result.isEmpty()) {
            result.add(fallback.getName());
        }

        return result;
    }

    private void drawPlayerModel(DrawContext context, FriendScreenClient$PlayerInfo info, int x, int y, int mouseX, int mouseY) {
        if (this.client != null && this.client.world != null) {
            if (this.previewPlayer == null) {
                UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + this.displayName).getBytes(StandardCharsets.UTF_8));
                this.previewPlayer = new OtherClientPlayerEntity(this.client.world, new GameProfile(uuid, this.displayName));
            }

            if (info != null) {
                this.previewPlayer.equipStack(EquipmentSlot.HEAD, stack(info.equipmentIds[0], info.equipmentCounts[0]));
                this.previewPlayer.equipStack(EquipmentSlot.CHEST, stack(info.equipmentIds[1], info.equipmentCounts[1]));
                this.previewPlayer.equipStack(EquipmentSlot.LEGS, stack(info.equipmentIds[2], info.equipmentCounts[2]));
                this.previewPlayer.equipStack(EquipmentSlot.FEET, stack(info.equipmentIds[3], info.equipmentCounts[3]));
                this.previewPlayer.equipStack(EquipmentSlot.MAINHAND, stack(info.equipmentIds[4], info.equipmentCounts[4]));
                this.previewPlayer.equipStack(EquipmentSlot.OFFHAND, stack(info.equipmentIds[5], info.equipmentCounts[5]));
            }

            LivingEntity model = this.previewPlayer;

            for (PlayerEntity player : this.client.world.getPlayers()) {
                if (player.getName().getString().equalsIgnoreCase(this.displayName)) {
                    model = player;
                    break;
                }
            }

            InventoryScreen.drawEntity(context, x + 26, y + 8, x + 75, y + 78, 30, 0.0625F, x + 51 - mouseX, y + 38 - mouseY, model);
        }
    }

    private static ItemStack stack(String value, int count) {
        if (value != null && !value.isBlank() && count > 0) {
            Identifier id = Identifier.tryParse(value);
            if (id == null) {
                return ItemStack.EMPTY;
            }

            Item item = Registries.ITEM.get(id);
            return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
