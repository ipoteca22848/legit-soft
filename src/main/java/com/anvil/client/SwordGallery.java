package com.anvil.client;
import java.util.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import com.example.auravisuals.module.misc.CustomSword;
import com.example.auravisuals.config.ConfigManager;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;
import rich.util.render.shader.Scissor;
/** Uses Minecraft's actual custom-model-data item renderer, matching the selected model. */
public final class SwordGallery {
 private static final String[] TABS={"Дерево","Камень","Железо","Золото","Алмаз","Незерит"};
 private int type;private float scroll;private List<ItemStack> previews=List.of();
 public List<String> items(){return CustomSword.modelNames();}
 public void setType(int value){type=Math.max(0,Math.min(5,value));scroll=0;previews=List.of();}
 private CustomSword module(){return CustomSword.INSTANCE;}
 public void select(String model){module().selectModel(type,model);ConfigManager.INSTANCE.saveConfig("default");}
 private static boolean inside(double mx,double my,float x,float y,float w,float h){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;}
 private static int color(int rgb,float a){return ((int)(255*Math.max(0,Math.min(1,a)))<<24)|(rgb&0xffffff);}
 private static String fit(String s,float width){while(!s.isEmpty()&&Fonts.BOLD.getWidth(s,5.5f)>width)s=s.substring(0,s.length()-1);return s;}
 private void ensurePreviews(){if(previews.size()!=items().size())previews=items().stream().map(name->CustomSword.previewStack(type,name)).toList();}
 public void render(DrawContext context,float x,float y,float width,float height,float mx,float my,int guiScale,float alpha){
  float tabW=(width-20)/6;
  for(int i=0;i<6;i++){float xx=x+i*(tabW+4);Render2D.rect(xx,y,tabW,17,color(i==type?0x4a4a4a:0x272727,alpha),4);Fonts.BOLD.drawCentered(TABS[i],xx+tabW/2,y+5,5.5f,color(0xffffff,alpha));}
  boolean active=module().isEnabled()&&module().appliesTo(type);
  Render2D.rect(x,y+21,width,17,color(active?0x314b39:0x333333,alpha),4);
  Fonts.BOLD.drawCentered(active?"Включено для этого меча · нажми, чтобы выключить":"Выключено для этого меча · нажми, чтобы включить",x+width/2,y+26,5.5f,color(0xe5e5e5,alpha));
  ensurePreviews();float top=y+43,areaH=height-43,gap=6,cardW=(width-12)/3,cardH=84;
  scroll=Math.min(scroll,Math.max(0,((items().size()+2)/3)*90-6-areaH));
  Scissor.enable(x,top,width,areaH,guiScale);context.enableScissor((int)x,(int)top,(int)(x+width),(int)(top+areaH));
  try{
   for(int i=0;i<items().size();i++){
    float xx=x+(i%3)*(cardW+gap),yy=top+(i/3)*90-scroll;
    if(yy+cardH<top||yy>top+areaH)continue;
    String name=items().get(i);boolean selected=module().selectedModel(type).equals(name);
    boolean hover=inside(mx,my,xx,yy,cardW,cardH);
    Render2D.rect(xx,yy,cardW,cardH,color(hover?0x383838:0x262626,alpha),5);Render2D.outline(xx,yy,cardW,cardH,selected?1:.5f,color(selected?0x96d7ae:0x424242,alpha),5);
    context.getMatrices().pushMatrix();
    try{context.getMatrices().translate(xx+(cardW-48)/2,yy+5);context.getMatrices().scale(3,3);context.drawItem(previews.get(i),0,0);}finally{context.getMatrices().popMatrix();}
    Fonts.BOLD.drawCentered(fit(name,cardW-8),xx+cardW/2,yy+64,5.5f,color(0xe5e5e5,alpha));
    Fonts.BOLD.drawCentered(selected?"Выбрано":"Нажми, чтобы выбрать",xx+cardW/2,yy+74,4.5f,color(selected?0x9ddbb0:0x929292,alpha));
   }
  }finally{context.disableScissor();Scissor.disable();}
 }
 public boolean mouseClicked(double mx,double my,int button,float x,float y,float width,float height){
  if(!inside(mx,my,x,y,width,height))return false;if(button!=0)return true;
  float tabW=(width-20)/6;
  for(int i=0;i<6;i++)if(inside(mx,my,x+i*(tabW+4),y,tabW,17)){setType(i);return true;}
  if(inside(mx,my,x,y+21,width,17)){boolean enable=!(module().isEnabled()&&module().appliesTo(type));module().setAppliesTo(type,enable);if(enable)module().setEnabled(true);ConfigManager.INSTANCE.saveConfig("default");return true;}
  float top=y+43,cardW=(width-12)/3;
  for(int i=0;i<items().size();i++){float xx=x+i%3*(cardW+6),yy=top+i/3*90-scroll;if(my>=top&&inside(mx,my,xx,yy,cardW,84)){select(items().get(i));return true;}}
  return true;
 }
 public boolean mouseScrolled(double mx,double my,double delta,float x,float y,float width,float height){if(!inside(mx,my,x,y,width,height))return false;float max=Math.max(0,((items().size()+2)/3)*90-6-(height-43));scroll=Math.max(0,Math.min(max,scroll-(float)delta*30));return true;}
}
