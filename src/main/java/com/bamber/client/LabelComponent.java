package com.bamber.client;
public final class LabelComponent extends rich.util.interfaces.AbstractSettingComponent {
 public LabelComponent(LabelSetting setting){super(setting);}
 private java.util.List<String> lines(){
  java.util.List<String> lines=new java.util.ArrayList<>();float available=width>10?width-4:144;
  for(String paragraph:getSetting().getName().split("\\n",-1)){
   StringBuilder line=new StringBuilder();
   for(int i=0;i<paragraph.length();){int cp=paragraph.codePointAt(i);i+=Character.charCount(cp);String piece=new String(Character.toChars(cp));
    if(!line.isEmpty()&&rich.util.render.font.Fonts.BOLD.getWidth(line+piece,6)>available){lines.add(line.toString());line.setLength(0);}line.append(piece);
   }lines.add(line.toString());
  }return lines;
 }
 public float getTotalHeight(){return Math.max(20,lines().size()*9+4);}
 @Override public void render(net.minecraft.client.gui.DrawContext context,int mouseX,int mouseY,float delta){float yy=y+4;for(String line:lines()){rich.util.render.font.Fonts.BOLD.draw(line,x+1,yy,6,applyAlpha(0xffaaaaaa));yy+=9;}}
}
