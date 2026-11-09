package com.anvil.client;
import java.util.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import com.example.auravisuals.cosmetics.*;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;
import rich.util.render.shader.Scissor;
/** Image-first catalogue rendered directly in the new Click GUI. */
public final class CosmeticsGallery {
 private static final String[] KEYS={"all","wings","hats","back","pets","capes","pulse"};
 private static final String[] NAMES={"Все","Крылья","Шляпы","Спина","Питомцы","Плащи","Pulse"};
 private int category;
 private float scroll;
 public void setCategory(int value){category=Math.max(0,Math.min(KEYS.length-1,value));scroll=0;}
 public List<CosmeticItem> items(){CosmeticsManager.INSTANCE.ensureLoaded();return category==6?CosmeticsManager.INSTANCE.getAllCosmetics().stream().filter(item->item.getSlug().startsWith("pulse_")).toList():CosmeticsManager.INSTANCE.getByCategory(KEYS[category]);}
 private static boolean inside(double mx,double my,float x,float y,float w,float h){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;}
 private static int alpha(int rgb,float opacity){return ((int)(255*Math.max(0,Math.min(1,opacity)))<<24)|(rgb&0xffffff);}
 private static String fit(String text,float width,float size){if(Fonts.BOLD.getWidth(text,size)<=width)return text;while(!text.isEmpty()&&Fonts.BOLD.getWidth(text+"…",size)>width)text=text.substring(0,text.length()-1);return text+"…";}
 public void render(DrawContext context,float x,float y,float width,float height,float mx,float my,int guiScale,float opacity){
  var list=items();float tabW=(width-(KEYS.length-1)*4)/KEYS.length;
  for(int i=0;i<KEYS.length;i++){float xx=x+i*(tabW+4);boolean hover=inside(mx,my,xx,y,tabW,17);Render2D.rect(xx,y,tabW,17,alpha(i==category?0x4a4a4a:hover?0x363636:0x272727,opacity),4);Fonts.BOLD.drawCentered(NAMES[i],xx+tabW/2,y+5,6,alpha(i==category?0xffffff:0xaaaaaa,opacity));}
  float top=y+23,areaH=height-23,gap=6,cardW=(width-gap*2)/3,cardH=84;
  int rows=(list.size()+2)/3;scroll=Math.min(scroll,Math.max(0,rows*(cardH+gap)-gap-areaH));
  Scissor.enable(x,top,width,areaH,guiScale);context.enableScissor((int)x,(int)top,(int)(x+width),(int)(top+areaH));
  for(int i=0;i<list.size();i++){
   var item=list.get(i);float xx=x+(i%3)*(cardW+gap),yy=top+(i/3)*(cardH+gap)-scroll;
   if(yy+cardH<top||yy>top+areaH)continue;
   boolean hover=inside(mx,my,xx,yy,cardW,cardH)&&my>=top&&my<=top+areaH;boolean equipped=CosmeticsManager.INSTANCE.isEquipped(item);
   Render2D.rect(xx,yy,cardW,cardH,alpha(hover?0x383838:0x262626,opacity),5);
   Render2D.outline(xx,yy,cardW,cardH,equipped?1:.5f,alpha(equipped?0x96d7ae:0x424242,opacity),5);
   preview(context,item,xx+5,yy+4,cardW-10,58,hover);
   Fonts.BOLD.drawCentered(fit(item.getName(),cardW-8,5.5f),xx+cardW/2,yy+65,5.5f,alpha(0xe5e5e5,opacity));
   Fonts.BOLD.drawCentered(equipped?"Надето · нажми, чтобы снять":"Нажми, чтобы надеть",xx+cardW/2,yy+74,4,alpha(equipped?0x9ddbb0:0x929292,opacity));
  }
  context.disableScissor();Scissor.disable();
 }
 public boolean mouseClicked(double mx,double my,int button,float x,float y,float width,float height){
  if(!inside(mx,my,x,y,width,height))return false;
  if(button!=0)return true;float tabW=(width-(KEYS.length-1)*4)/KEYS.length;
  if(my<=y+17){for(int i=0;i<KEYS.length;i++)if(inside(mx,my,x+i*(tabW+4),y,tabW,17)){setCategory(i);return true;}}
  float top=y+23,gap=6,cardW=(width-12)/3,cardH=84;if(my<top)return true;
  var list=items();for(int i=0;i<list.size();i++){float xx=x+(i%3)*(cardW+gap),yy=top+(i/3)*(cardH+gap)-scroll;if(inside(mx,my,xx,yy,cardW,cardH)){
   CosmeticsManager.INSTANCE.toggleEquip(list.get(i));com.example.auravisuals.config.ConfigManager.INSTANCE.saveConfig("default");return true;
  }}return true;
 }
 public boolean mouseScrolled(double mx,double my,double delta,float x,float y,float width,float height){if(!inside(mx,my,x,y,width,height))return false;float max=Math.max(0,((items().size()+2)/3)*90-6-(height-23));scroll=Math.max(0,Math.min(max,scroll-(float)delta*30));return true;}
 private static void preview(DrawContext context,CosmeticItem item,float x,float y,float width,float height,boolean hover){CosmeticPreviewRenderer.render(context,item,x,y,width,height,hover);}
}
