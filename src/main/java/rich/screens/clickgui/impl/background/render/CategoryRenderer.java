package rich.screens.clickgui.impl.background.render;
import rich.modules.module.category.ModuleCategory;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;
import rich.util.render.shader.Scissor;
import rich.screens.clickgui.impl.background.BackgroundComponent;
import java.util.*;
public final class CategoryRenderer {
 private static final ModuleCategory[] CATEGORIES=ModuleCategory.values();
 private final Map<ModuleCategory,Float> animations=new EnumMap<>(ModuleCategory.class);
 private float scroll;
 private ModuleCategory lastSelected;
 public void updateAnimations(ModuleCategory selected,float delta){
  if(selected!=lastSelected){lastSelected=selected;float row=6+selected.ordinal()*15-scroll;float viewport=BackgroundComponent.BG_HEIGHT-96;if(row<0)scroll+=row;else if(row+15>viewport)scroll+=row+15-viewport;}
  for(var cat:CATEGORIES)animations.put(cat,animations.getOrDefault(cat,0f)+(cat==selected?1f-animations.getOrDefault(cat,0f):-animations.getOrDefault(cat,0f))*Math.min(1f,delta*8));}
 public void render(float x,float y,ModuleCategory selected,float alpha){
  Scissor.enable(x+10,y+54,80,BackgroundComponent.BG_HEIGHT-96,2);
  for(int i=0;i<CATEGORIES.length;i++){
   var cat=CATEGORIES[i];float row=y+60+i*15-scroll;if(row<y+40||row>y+BackgroundComponent.BG_HEIGHT-35)continue;
   float anim=animations.getOrDefault(cat,0f);int a=(int)((128+127*anim)*alpha);int c=(a<<24)|0xd0d0d0;
   if(cat==selected)Render2D.rect(x+12,row+3,3,3,(a<<24)|0xffffff,1.5f);
   Fonts.CATEGORY_ICONS.draw(""+(char)('a'+i%7),x+18+anim*3,row+.5f,6,c);
   String title=switch(cat){case MISC->"Утилиты";case RENDER->"Визуалы";case PLAYER->"Игрок";default->cat.getReadableName();};
   Fonts.BOLD.draw(title,x+29+anim*3,row,6,c);
  }
  float viewport=BackgroundComponent.BG_HEIGHT-96,content=6+CATEGORIES.length*15;
  if(content>viewport){float thumb=Math.max(20,viewport*viewport/content),max=content-viewport;Render2D.rect(x+88,y+54,1.5f,viewport,((int)(40*alpha)<<24)|0xffffff,1);Render2D.rect(x+88,y+54+(viewport-thumb)*scroll/max,1.5f,thumb,((int)(130*alpha)<<24)|0xffffff,1);}
  Scissor.disable();
 }
 public ModuleCategory getCategoryAtPosition(double mx,double my,float x,float y){if(mx<x+10||mx>x+90||my<y+54||my>y+BackgroundComponent.BG_HEIGHT-42)return null;int i=(int)Math.floor((my-y-60+scroll)/15);return i>=0&&i<CATEGORIES.length?CATEGORIES[i]:null;}
 public boolean scroll(double mx,double my,double delta,float x,float y){
  if(mx<x+10||mx>x+90||my<y+54||my>y+BackgroundComponent.BG_HEIGHT-42)return false;
  scroll=Math.max(0,Math.min(Math.max(0,6+CATEGORIES.length*15-(BackgroundComponent.BG_HEIGHT-96)),scroll-(float)delta*22));return true;
 }
}
