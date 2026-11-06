package com.example.auravisuals.client.gui.render;

import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import com.example.auravisuals.compat.Gui;

/** Static alpha masks submitted through the same deferred GUI queue as HUD panels. */
public final class ShadowRender {
 public static final float REACH=3;
 private static final int CACHE_LIMIT=48;
 private record Shape(int width,int height,int radius,int sigma){}
 private record Mask(Identifier texture,float padding){}
 private static final Map<Shape,Mask> CACHE=new LinkedHashMap<>(64,.75f,true);
 private static long serial;
 private ShadowRender(){}
 /** Trim only at the next frame: this frame's queued elements still own their textures. */
 public static void beginFrame(){
  var it=CACHE.entrySet().iterator();
  while(CACHE.size()>CACHE_LIMIT&&it.hasNext()){
   var entry=it.next();MinecraftClient.getInstance().getTextureManager().destroyTexture(entry.getValue().texture());it.remove();
  }
 }
 public static void draw(DrawContext context,float x,float y,float width,float height,float radius,float sigma,int color){
  if(!(width>0&&height>0&&sigma>0)||!Float.isFinite(width+height+radius+sigma)||(color>>>24)==0)return;
  var shape=new Shape(Math.max(2,Math.round(width/2)*2),Math.max(2,Math.round(height/2)*2),Math.max(0,Math.round(radius)),Math.max(1,Math.round(sigma)));
  var mask=CACHE.computeIfAbsent(shape,ShadowRender::bake);float pad=mask.padding();
  var batch=Gui.tex(context,mask.texture());
  batch.quad(x-pad,y-pad,0,0,x+width+pad,y-pad,1,0,x+width+pad,y+height+pad,1,1,x-pad,y+height+pad,0,1,color).submit(context);
 }
 private static Mask bake(Shape shape){
  float padding=shape.sigma()*REACH;
  float scale=Math.min(.5f,512f/Math.max(shape.width()+2*padding,shape.height()+2*padding));
  int width=Math.max(1,(int)Math.ceil((shape.width()+2*padding)*scale));
  int height=Math.max(1,(int)Math.ceil((shape.height()+2*padding)*scale));
  float[] inside=new float[width*height];float halfW=shape.width()*scale/2,halfH=shape.height()*scale/2;
  float radius=Math.min(shape.radius()*scale,Math.min(halfW,halfH));
  for(int y=0;y<height;y++)for(int x=0;x<width;x++){
   float qx=Math.abs(x+.5f-width/2f)-halfW+radius,qy=Math.abs(y+.5f-height/2f)-halfH+radius;
   float distance=(float)Math.hypot(Math.max(0,qx),Math.max(0,qy))+Math.min(Math.max(qx,qy),0)-radius;
   inside[y*width+x]=Math.max(0,Math.min(1,.5f-distance));
  }
  float[] blurred=inside.clone(),scratch=new float[inside.length];
  // Three box filters approximate a Gaussian in linear time, once per cached shape.
  double sigma=shape.sigma()*scale;int lower=(int)Math.floor(Math.sqrt(4*sigma*sigma+1));if((lower&1)==0)lower--;lower=Math.max(1,lower);
  int count=(int)Math.round((12*sigma*sigma-3.0*lower*lower-12.0*lower-9)/(-4.0*lower-4));
  for(int i=0;i<3;i++){int r=((i<count?lower:lower+2)-1)/2;blur(blurred,scratch,width,height,r,true);blur(scratch,blurred,width,height,r,false);}
  NativeImage image=new NativeImage(width,height,false);
  for(int y=0;y<height;y++)for(int x=0;x<width;x++){
   int index=y*width+x,alpha=Math.max(0,Math.min(255,Math.round(255*blurred[index]*(1-inside[index]))));
   image.setColorArgb(x,y,(alpha<<24)|0xffffff);
  }
  Identifier id=Identifier.of("anvil","hud_shadow/"+(serial++));
  MinecraftClient.getInstance().getTextureManager().registerTexture(id,new NativeImageBackedTexture(()->"anvil HUD shadow",image));
  return new Mask(id,padding);
 }
 private static void blur(float[] source,float[] target,int width,int height,int radius,boolean horizontal){
  int lines=horizontal?height:width,length=horizontal?width:height,step=horizontal?1:width;float divisor=2*radius+1;
  for(int line=0;line<lines;line++){
   int start=horizontal?line*width:line;float sum=0;
   for(int i=0;i<=Math.min(radius,length-1);i++)sum+=source[start+i*step];
   for(int i=0;i<length;i++){
    target[start+i*step]=sum/divisor;
    int remove=i-radius,add=i+radius+1;
    if(remove>=0)sum-=source[start+remove*step];if(add<length)sum+=source[start+add*step];
   }
  }
 }
 public static void shutdown(){for(var mask:CACHE.values())MinecraftClient.getInstance().getTextureManager().destroyTexture(mask.texture());CACHE.clear();}
}
