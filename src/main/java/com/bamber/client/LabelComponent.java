package com.bamber.client;
public final class LabelComponent extends rich.util.interfaces.AbstractSettingComponent {
 public LabelComponent(LabelSetting setting){super(setting);}
 @Override public void render(net.minecraft.client.gui.DrawContext context,int mouseX,int mouseY,float delta){
  rich.util.render.font.Fonts.BOLD.draw(getSetting().getName(),x+1,y+height/2-3,6,applyAlpha(0xffaaaaaa));
 }
}
