package com.example.auravisuals.client.gui;

import com.example.auravisuals.friend.FriendManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class FriendManagerScreen extends Screen {
    private TextFieldWidget nameField;

    public FriendManagerScreen() {
        super(Text.of("Friend Manager"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.nameField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 150, 200, 20, Text.of("Name"));
        this.nameField.setMaxLength(16);
        this.addDrawableChild(this.nameField);
        this.addDrawableChild(ButtonWidget.builder(Text.of("Add"), button -> {
            if (!this.nameField.getText().isEmpty()) {
                FriendManager.INSTANCE.addFriend(this.nameField.getText());
                this.nameField.setText("");
            }
        }).dimensions(centerX - 100, centerY - 120, 95, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.of("Remove"), button -> {
            if (!this.nameField.getText().isEmpty()) {
                FriendManager.INSTANCE.removeFriend(this.nameField.getText());
                this.nameField.setText("");
            }
        }).dimensions(centerX + 5, centerY - 120, 95, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this.renderBackground(context, mouseX, mouseY, deltaTicks);
        super.render(context, mouseX, mouseY, deltaTicks);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, "Friends:", centerX, centerY - 90, 16777215);
        int y = centerY - 70;

        for (String friend : FriendManager.INSTANCE.getFriends()) {
            context.drawCenteredTextWithShadow(this.textRenderer, friend, centerX, y, 11184810);
            y += 12;
            if (y > this.height - 30) {
                break;
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
