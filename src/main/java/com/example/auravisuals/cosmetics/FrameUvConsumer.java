package com.example.auravisuals.cosmetics;
import net.minecraft.client.render.VertexConsumer;
/** Selects an animation frame in a resident texture strip without CPU pixel copies or GPU uploads. */
public final class FrameUvConsumer implements VertexConsumer {
 private final VertexConsumer delegate;private final float inverseFrames,offset;
 public FrameUvConsumer(VertexConsumer delegate,int frames,int frame){this.delegate=delegate;this.inverseFrames=1f/frames;this.offset=frame*inverseFrames;}
 public VertexConsumer vertex(float x,float y,float z){delegate.vertex(x,y,z);return this;}
 public VertexConsumer color(int r,int g,int b,int a){delegate.color(r,g,b,a);return this;}
 public VertexConsumer color(int c){delegate.color(c);return this;}
 public VertexConsumer texture(float u,float v){delegate.texture(u,v*inverseFrames+offset);return this;}
 public VertexConsumer overlay(int u,int v){delegate.overlay(u,v);return this;}
 public VertexConsumer light(int u,int v){delegate.light(u,v);return this;}
 public VertexConsumer normal(float x,float y,float z){delegate.normal(x,y,z);return this;}
 public VertexConsumer lineWidth(float width){delegate.lineWidth(width);return this;}
}
