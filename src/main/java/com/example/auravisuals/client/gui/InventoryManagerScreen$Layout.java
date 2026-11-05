package com.example.auravisuals.client.gui;

public final class InventoryManagerScreen$Layout {
    final int leftX;
    final int topY;
    final int leftW;
    final int previewX;
    final int previewW;
    final int panelY;
    final int panelH;
    final int inputX;
    final int inputY;
    final int inputW;
    final int saveX;
    final int saveW;
    final int controlH;
    final int listX;
    final int listY;
    final int listW;
    final int listH;
    final int missingX;
    final int missingY;
    final int missingW;
    final int missingH;
    final int rowH;
    final int rowGap;
    final int backX;
    final int backY;

    InventoryManagerScreen$Layout(int leftX, int topY, int leftW, int previewW, int panelH, int gap) {
        this.leftX = leftX;
        this.topY = topY;
        this.leftW = leftW;
        this.previewX = leftX + leftW + gap;
        this.previewW = previewW;
        this.panelY = topY + 28;
        this.panelH = panelH - 28;
        this.controlH = 25;
        this.inputX = leftX + 10;
        this.inputY = this.panelY + 10;
        this.saveW = 82;
        this.inputW = leftW - this.saveW - 30;
        this.saveX = this.inputX + this.inputW + 10;
        this.listX = leftX + 10;
        this.listY = this.inputY + this.controlH + 10;
        this.listW = leftW - 20;
        this.listH = panelH - 55;
        this.missingW = 66;
        this.missingX = this.previewX + previewW - this.missingW - 12;
        this.missingY = this.panelY + 42;
        this.missingH = panelH - 66;
        this.rowH = 42;
        this.rowGap = 6;
        this.backX = leftX;
        this.backY = topY - 24;
    }
}
