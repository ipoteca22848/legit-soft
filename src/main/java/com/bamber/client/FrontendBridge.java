package com.bamber.client;

import java.util.ArrayList;
import java.util.List;
import com.example.auravisuals.module.ModuleManager;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.Setting;
import rich.modules.module.setting.implement.*;

/** Live adapters: the original modules remain the only source of values and state. */
public final class FrontendBridge {
 private static final List<ModuleStructure> modules=new ArrayList<>();
 public static List<ModuleStructure> modules(){
  if(modules.isEmpty()){
   for(var source:ModuleManager.INSTANCE.getModules())modules.add(new Adapter(source));
  }
  return modules;
 }
 public static void refresh(){for(var module:modules())if(module instanceof Adapter a)a.refresh();}
 private static ModuleCategory category(com.example.auravisuals.module.Module m){
  if(m instanceof TriggerBot)return ModuleCategory.COMBAT;
  if(m instanceof com.example.auravisuals.module.render.Hud)return ModuleCategory.HUD;
  if(m instanceof com.example.auravisuals.module.render.ClickGui)return ModuleCategory.SETTINGS;
  return switch(m.getCategory()){
   case VISUALS,INTERFACE -> ModuleCategory.RENDER;
   case PLAYER -> ModuleCategory.PLAYER;
   case MARKS -> ModuleCategory.MARKS;
   case CONFIGS -> ModuleCategory.CONFIGS;
   case THEMES -> ModuleCategory.THEMES;
   case COSMETICS -> ModuleCategory.COSMETICS;
   case SERVER_EVENTS,EVENTS -> ModuleCategory.EVENTS;
   case MINES -> ModuleCategory.MINES;
   case MESSENGER -> ModuleCategory.MESSENGER;
   default -> ModuleCategory.MISC;
  };
 }
 private static final class Adapter extends ModuleStructure {
  private final com.example.auravisuals.module.Module source;
  private List<com.example.auravisuals.setting.Setting<?>> snapshot;
  private final BindSetting activationBind;
  Adapter(com.example.auravisuals.module.Module m){
   super(m.getName(),m.getDescription(),category(m));source=m;
   snapshot=new ArrayList<>(m.getSettings());
   for(var setting:m.getSettings()){
    for(Setting adapted:adaptAll(setting,source)){adapted.setVisible(setting::isVisible);settings(adapted);}
   }
   // Rich's module bind control is also exposed in the settings panel.
   activationBind=new BindSetting("Бинд активации", "Delete — убрать бинд"){
    @Override public int getKey(){return source.getKey()==0?-1:source.getKey();}
    @Override public BindSetting setKey(int key){source.setKey(key);return this;}
   };settings(activationBind);
   refresh();
  }
  void refresh(){
   if(!snapshot.equals(source.getSettings())){
    snapshot=new ArrayList<>(source.getSettings());settings().clear();
    for(var setting:snapshot)for(var adapted:adaptAll(setting,source)){adapted.setVisible(setting::isVisible);settings(adapted);}
    settings(activationBind);
   }
   state=source.isEnabled();getAnimation().setDirection(state?rich.util.animations.Direction.FORWARDS:rich.util.animations.Direction.BACKWARDS);}
  @Override public boolean isState(){return source.isEnabled();}
  @Override public void setState(boolean value){source.setEnabled(value);refresh();}
  @Override public int getKey(){return source.getKey()==0?-1:source.getKey();}
  @Override public void setKey(int key){source.setKey(key);}
 }
 public static Setting adapt(com.example.auravisuals.setting.Setting<?> setting,com.example.auravisuals.module.Module source){
  String name=setting.getDisplayName();
  if(setting instanceof com.example.auravisuals.setting.SeparatorSetting)return new LabelSetting("");
  if(setting instanceof com.example.auravisuals.setting.LabelSetting)return new LabelSetting(name);
  if(setting instanceof com.example.auravisuals.setting.BooleanSetting a){
   return new BooleanSetting(name, ""){
    @Override public boolean isValue(){return a.getValue();}
    @Override public BooleanSetting setValue(boolean value){a.setValue(value);return this;}
   };
  }
  if(setting instanceof com.example.auravisuals.setting.NumberSetting a){
   return new SliderSettings(name, ""){
    @Override public float getValue(){return a.getFloatValue();}
    @Override public SliderSettings setValue(float value){a.setValue((double)value);return this;}
   }.range((float)a.getMin(),(float)a.getMax()).setInteger(a.getIncrement()>=1);
  }
  if(setting instanceof com.example.auravisuals.setting.ModeSetting a){
   return new SelectSetting(name, ""){
    @Override public String getSelected(){return a.getValue();}
    @Override public void setSelected(String value){a.setValue(value);}
    @Override public SelectSetting selected(String value){a.setValue(value);return this;}
    @Override public List<String> getList(){return a.getModes();}
    @Override public boolean isSelected(String value){return a.is(value);}
   };
  }
  if(setting instanceof com.example.auravisuals.setting.BindSetting a){
   return new BindSetting(name, ""){
    @Override public int getKey(){return a.getValue()==0?-1:a.getValue();}
    @Override public BindSetting setKey(int key){a.setValue(key);return this;}
   };
  }
  if(setting instanceof com.example.auravisuals.setting.StringSetting a){
   return new TextSetting(name, ""){
    @Override public String getText(){return a.getValue();}
    @Override public TextSetting setText(String text){a.setValue(text);return this;}
   }.setMin(0).setMax(4096);
  }
  if(setting instanceof com.example.auravisuals.setting.ActionSetting a){
   return new ButtonSetting(name, ""){@Override public String getName(){return a.getDisplayName();}@Override public String getButtonName(){return a.getDisplayName();}}.setButtonName(name).setRunnable(a::trigger);
  }
  if(setting instanceof com.example.auravisuals.setting.MultiBooleanSetting a){
   return new MultiSelectSetting(name, ""){
    @Override public List<String> getList(){return a.getValues();}
    @Override public List<String> getSelected(){return a.getValues().stream().filter(a::isEnabled).collect(java.util.stream.Collectors.toCollection(ArrayList::new));}
    @Override public void setSelected(List<String> values){for(String v:a.getValues())a.set(v,values.contains(v));}
    @Override public boolean isSelected(String value){return a.isEnabled(value);}
   };
  }
  if(setting instanceof com.example.auravisuals.setting.ColorSetting a)return NativeSections.color(name,a::getValue,a::setValue);
  throw new IllegalArgumentException("Unsupported setting: "+setting.getClass().getName());
 }
 public static List<Setting> adaptAll(com.example.auravisuals.setting.Setting<?> s,com.example.auravisuals.module.Module source){
  String name=s.getDisplayName();
  if(s instanceof com.example.auravisuals.setting.ItemHighlightSetting a)return List.of(
   NativeSections.bool(name,a::getValue,a::setValue),NativeSections.color(name+" — цвет",a::getColor,a::setColor));
  if(s instanceof com.example.auravisuals.setting.ItemScaleSetting a)return List.of(NativeSections.bool(name,a::getValue,a::setValue));
  if(s instanceof com.example.auravisuals.setting.FriendRowSetting a)return List.of(
   NativeSections.bool(name,()->!a.isHidden(),v->{if(v==a.isHidden())a.toggle();}),
   new BindSetting("Бинд друга", ""){@Override public int getKey(){return a.getKey();}@Override public BindSetting setKey(int key){a.setKey(key);return this;}});
  if(s instanceof com.example.auravisuals.setting.FriendRequestRowSetting a)return List.of(
   NativeSections.button(name+" — принять",a::accept),NativeSections.button(name+" — отклонить",a::decline));
  return List.of(adapt(s,source));
 }
}
