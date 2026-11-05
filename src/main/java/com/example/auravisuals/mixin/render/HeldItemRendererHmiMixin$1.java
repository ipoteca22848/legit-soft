package com.example.auravisuals.mixin.render;

import net.minecraft.item.consume.UseAction;

public class HeldItemRendererHmiMixin$1 {
    static final int[] $SwitchMap$net$minecraft$item$consume$UseAction = new int[UseAction.values().length];

    static {
        try {
            $SwitchMap$net$minecraft$item$consume$UseAction[UseAction.NONE.ordinal()] = 1;
        } catch (NoSuchFieldError var8) {
        }

        try {
            $SwitchMap$net$minecraft$item$consume$UseAction[UseAction.EAT.ordinal()] = 2;
        } catch (NoSuchFieldError var7) {
        }

        try {
            $SwitchMap$net$minecraft$item$consume$UseAction[UseAction.DRINK.ordinal()] = 3;
        } catch (NoSuchFieldError var6) {
        }

        try {
            $SwitchMap$net$minecraft$item$consume$UseAction[UseAction.BLOCK.ordinal()] = 4;
        } catch (NoSuchFieldError var5) {
        }

        try {
            $SwitchMap$net$minecraft$item$consume$UseAction[UseAction.BOW.ordinal()] = 5;
        } catch (NoSuchFieldError var4) {
        }

        try {
            $SwitchMap$net$minecraft$item$consume$UseAction[UseAction.SPEAR.ordinal()] = 6;
        } catch (NoSuchFieldError var3) {
        }

        try {
            $SwitchMap$net$minecraft$item$consume$UseAction[UseAction.BRUSH.ordinal()] = 7;
        } catch (NoSuchFieldError var2) {
        }

        try {
            $SwitchMap$net$minecraft$item$consume$UseAction[UseAction.BUNDLE.ordinal()] = 8;
        } catch (NoSuchFieldError var1) {
        }
    }
}
