package com.anvil.smoke;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import com.example.auravisuals.client.gui.render.ShadowRender;
import com.example.auravisuals.client.gui.render.RoundedRenderer;
public final class ShadowCheckScreen extends Screen {
 private int ticks;
 public ShadowCheckScreen(){super(Text.literal("Shadow stability check"));}
 @Override public void tick(){if(++ticks==20||ticks==40||ticks==60)System.out.println("ANVIL SHADOW FRAME: "+ticks+" SCALE: "+client.getWindow().getScaleFactor());}
 @Override public void render(DrawContext c,int mx,int my,float delta){
  c.fill(0,0,width,height,0xffb0b0b0);
  ShadowRender.draw(c,80,90,120,60,8,12,0xc0000000);
  RoundedRenderer.drawRoundedRect(c,80,90,120,60,8,0xffededed);
  ShadowRender.draw(c,260,90,100,60,12,16,0xa05030e0);
  RoundedRenderer.drawRoundedRect(c,260,90,100,60,12,0xffededed);
 }
}
