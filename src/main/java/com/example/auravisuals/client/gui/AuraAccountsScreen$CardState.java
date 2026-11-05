package com.example.auravisuals.client.gui;

public final class AuraAccountsScreen$CardState {
    AuraAccountsScreen$AccountView view;
    float displayX;
    float displayY;
    float targetX;
    float targetY;
    float alpha;
    float alphaTarget = 1.0F;
    boolean removing;

    AuraAccountsScreen$CardState(AuraAccountsScreen$AccountView view, float displayX, float displayY) {
        this.view = view;
        this.displayX = displayX;
        this.displayY = displayY;
        this.targetX = displayX;
        this.targetY = displayY;
        this.alpha = 0.0F;
    }
}
