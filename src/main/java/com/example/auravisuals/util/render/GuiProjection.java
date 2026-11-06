package com.example.auravisuals.util.render;
import org.joml.Matrix4f;
/** Render-thread scratch projection. Callers copy it into GPU data immediately. */
public final class GuiProjection {
 private static final Matrix4f projection=new Matrix4f();
 private static int width=-1,height=-1;
 public static Matrix4f get(int w,int h){if(width!=w||height!=h){width=w;height=h;projection.setOrtho(0,w,h,0,-1000,1000);}return projection;}
 private GuiProjection(){}
}
