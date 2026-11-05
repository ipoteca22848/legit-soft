package com.bamber.client;
import java.util.*;
import com.google.gson.*;
import com.example.auravisuals.client.gui.hud.HudElementStyleManager;
import com.example.auravisuals.client.gui.hud.HudEditor;
import rich.modules.module.setting.Setting;
import rich.modules.module.setting.implement.*;
public final class HudStyleControls {
 private static JsonObject value(String id){var all=HudElementStyleManager.snapshot();return all.has(id)?all.getAsJsonObject(id):new JsonObject();}
 private static void put(String id,String key,Object v){var object=new JsonObject();if(v instanceof Boolean b)object.addProperty(key,b);else if(v instanceof Number n)object.addProperty(key,n);else object.addProperty(key,v.toString());var patch=new JsonObject();patch.add(id,object);HudElementStyleManager.applyPatch(patch);}
 private static float number(String id,String key,float fallback){var v=HudStyleControls.value(id).get(key);return v==null?fallback:v.getAsFloat();}
 private static SliderSettings number(String id,String key,String name,float fallback,float min,float max){return new SliderSettings(name,""){@Override public float getValue(){return number(id,key,fallback);}@Override public SliderSettings setValue(float v){put(id,key,v);return this;}}.range(min,max);}
 private static SelectSetting select(String id,String key,String name,String fallback,String... choices){return new SelectSetting(name,""){@Override public String getSelected(){var v=HudStyleControls.value(id).get(key);return v==null?fallback:v.getAsString();}@Override public void setSelected(String v){put(id,key,v);}@Override public SelectSetting selected(String v){put(id,key,v);return this;}@Override public boolean isSelected(String v){return v.equals(getSelected());}}.value(choices);}
 private static SliderSettings layout(String id,String key,String name,float fallback,float min,float max){return new SliderSettings(name,""){
  @Override public float getValue(){var all=HudEditor.saveLayout();var e=all.getAsJsonObject(id);return e!=null&&e.has(key)?e.get(key).getAsFloat():fallback;}
  @Override public SliderSettings setValue(float value){var all=HudEditor.saveLayout();var e=all.getAsJsonObject(id);if(e==null){e=new JsonObject();all.add(id,e);}e.addProperty(key,value);HudEditor.loadLayout(all);return this;}
 }.range(min,max);}
 public static List<Setting> controls(String id){
  List<Setting> list=new ArrayList<>();
  if(!id.equals("customHotbar")){list.add(layout(id,"x","Смещение X",0,-1920,1920));list.add(layout(id,"y","Смещение Y",0,-1080,1080));list.add(layout(id,"scale","Масштаб",1,.6f,1.6f));}
  list.add(select(id,"material","Материал","Inherit","Inherit","Classic","Liquid Glass"));
  String[][] colors={{"panelColor","Цвет панели"},{"panelColor2","Второй цвет панели"},{"textColor","Цвет текста"},{"accentColor","Акцент"},{"shadowColor","Цвет тени"},{"glowColor","Цвет свечения"},{"outlineColor","Цвет обводки"},{"stripeColor","Цвет полосы"},{"labelColor","Цвет подписи"}};
  for(var c:colors)list.add(NativeSections.color(c[1],()->{var v=value(id).get(c[0]);try{return v==null?0xffffffff:(int)(0xff000000L|Long.parseLong(v.getAsString().substring(1),16));}catch(Exception e){return 0xffffffff;}},v->put(id,c[0],String.format("#%06X",v&0xffffff))));
  list.add(number(id,"panelOpacity","Непрозрачность",255,0,255).setInteger(true));list.add(number(id,"radius","Скругление",8,0,28));
  String[][] booleans={{"shadow","Тень"},{"blur","Блюр"},{"glow","Свечение"},{"outline","Обводка"},{"stripe","Цветная полоса"}};
  for(var b:booleans)list.add(NativeSections.bool(b[1],()->{var v=value(id).get(b[0]);return v==null?(b[0].equals("shadow")||b[0].equals("blur")):v.getAsBoolean();},v->put(id,b[0],v)));
  list.add(number(id,"shadowStrength","Сила тени",1,.25f,2.5f));list.add(number(id,"blurStrength","Сила блюра",1,.25f,2.5f));list.add(number(id,"glowStrength","Сила свечения",1,.25f,3));list.add(number(id,"outlineWidth","Толщина обводки",1,.5f,3));list.add(number(id,"stripeWidth","Ширина полосы",2,1,8));
  list.add(select(id,"stripeSide","Сторона полосы","left","left","right","top","bottom"));
  list.add(select(id,"icon","Иконка","star",HudElementStyleManager.ICONS.stream().sorted().toArray(String[]::new)));list.add(number(id,"iconScale","Размер иконки",1,.55f,1.8f));
  list.add(new TextSetting("Подпись",""){@Override public String getText(){var v=value(id).get("label");return v==null?"":v.getAsString();}@Override public TextSetting setText(String v){put(id,"label",v);return this;}}.setMax(24));
  list.add(NativeSections.button("Сбросить оформление",()->{var all=HudElementStyleManager.snapshot();all.remove(id);HudElementStyleManager.load(all);NativeSections.invalidate();}));
  return list;
 }
}
