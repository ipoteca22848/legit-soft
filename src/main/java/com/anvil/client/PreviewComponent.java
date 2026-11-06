package com.anvil.client;
public final class PreviewComponent extends rich.util.interfaces.AbstractSettingComponent {
 public PreviewComponent(PreviewSetting s){super(s);}
 @Override public void render(net.minecraft.client.gui.DrawContext context,int mouseX,int mouseY,float delta){
  var item=((PreviewSetting)getSetting()).item;
  float size=Math.min(height-12,68);
  rich.util.render.Render2D.texture(item.getPreviewId(),x+(width-size)/2,y+1,size,size,1,3,applyAlpha(0xffffffff));
  rich.util.render.font.Fonts.BOLD.draw(item.getCategory(),x+1,y+height-8,6,applyAlpha(0xffaaaaaa));
 }
}
