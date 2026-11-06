package com.anvil.client;
/** Native GUI coordinates, independent of the 1920x1080 HUD design canvas. */
public final class HudViewport {
 public static final float HOTBAR_WIDTH=182, HOTBAR_DESIGN_WIDTH=382, HOTBAR_DESIGN_HEIGHT=48;
 public static float hotbarScale(){return HOTBAR_WIDTH/HOTBAR_DESIGN_WIDTH;}
 public static float hotbarLeft(int width){return width/2f-HOTBAR_WIDTH/2;}
 public static float hotbarBottom(int height){return height-1f;}
 public static float hotbarTop(int height){return hotbarBottom(height)-HOTBAR_DESIGN_HEIGHT*hotbarScale();}
 private HudViewport(){}
}
