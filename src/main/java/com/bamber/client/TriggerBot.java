package com.bamber.client;
import java.lang.reflect.Field;
import com.example.auravisuals.setting.ModeSetting;
public final class TriggerBot extends com.example.auravisuals.module.Module {
 private static Object engine;
 private static Field enabled, stopped, mode;
 private final ModeSetting attackMode=new ModeSetting("Режим атаки","Криты","Криты","Обычный");
 public TriggerBot(){super("TriggerBot","Триггербот из присланного мода. Бинд меняется средней кнопкой мыши по модулю.",com.example.auravisuals.module.Category.PLAYER);addSettings(attackMode);setKey(82);}
 private static void initEngine() throws ReflectiveOperationException {
  if(engine!=null)return;
  Class<?> c=Class.forName("com.bamber.client.TriggerEngine");
  engine=c.getConstructor().newInstance();
  enabled=c.getDeclaredField("E");enabled.setAccessible(true);
  stopped=c.getDeclaredField("e");stopped.setAccessible(true);
  mode=c.getDeclaredField("I");mode.setAccessible(true);
  c.getMethod("onInitializeClient").invoke(engine);
 }
 @Override public void onEnable(){
  try{initEngine();stopped.setBoolean(engine,false);enabled.setBoolean(engine,true);resetEngine();}
  catch(ReflectiveOperationException e){setEnabledSilently(false);throw new IllegalStateException("Cannot initialize triggerbot",e);}
 }
 @Override public void onTick(){
  try{initEngine();mode.setInt(engine,attackMode.is("Криты")?0:1);stopped.setBoolean(engine,mc.currentScreen!=null||mc.player==null||mc.world==null||!BamberRuntime.isActive());}
  catch(ReflectiveOperationException e){stopEngine();setEnabledSilently(false);}
 }
 @Override public void onDisable(){stopEngine();}
 private static void resetEngine() throws ReflectiveOperationException {
  for(var method:engine.getClass().getDeclaredMethods()){
   if(method.getName().equals("C") && method.getReturnType()==void.class && method.getParameterCount()==0){method.setAccessible(true);method.invoke(engine);return;}
  }
  throw new NoSuchMethodException("Trigger engine reset");
 }
 public static void stopEngine(){
  if(engine!=null)try{enabled.setBoolean(engine,false);stopped.setBoolean(engine,true);resetEngine();}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
 }
}
