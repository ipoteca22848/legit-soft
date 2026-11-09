package com.anvil.smoke;
import com.example.auravisuals.module.render.Optimization;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.cosmetics.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.*;
public final class PulsePortCheck {
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void verify(MinecraftClient client)throws Exception{
  var module=ModuleManager.INSTANCE.getModule(Optimization.class);var options=client.options;
  int max=options.getMaxFps().getValue(),view=options.getViewDistance().getValue(),sim=options.getSimulationDistance().getValue();boolean vsync=options.getEnableVsync().getValue();var particles=options.getParticles().getValue();var clouds=options.getCloudRenderMode().getValue();boolean focused=client.isWindowFocused();
  var fast=(com.example.auravisuals.setting.BooleanSetting)module.getSettings().stream().filter(s->s instanceof com.example.auravisuals.setting.BooleanSetting&&s.getDisplayName().equals("Быстрая графика")).findFirst().orElseThrow();boolean oldFast=fast.getValue();var graphics=options.getPreset().getValue();boolean ao=options.getAo().getValue();
  var cap=(NumberSetting)module.getSettings().stream().filter(s->s instanceof NumberSetting&&s.getDisplayName().equals("Потолок частиц")).findFirst().orElseThrow();double oldCap=cap.getValue();
  try{
   fast.setValue(true);module.setEnabled(true);cap.setValue(250.0);module.onTick();
   check(options.getPreset().getValue()==net.minecraft.client.option.GraphicsMode.FAST&&!options.getAo().getValue(),"Fast graphics not applied");
   check(options.getMaxFps().getValue()==260,"Pulse video settings not applied");
   check(options.getViewDistance().getValue()<=view&&options.getSimulationDistance().getValue()>=5,"Invalid chunk distance");
   client.onWindowFocusChanged(false);check(module.b(300)==10,"Dynamic FPS cap");client.onWindowFocusChanged(true);check(module.b(300)==300,"Foreground FPS changed");
   check(module.c(512)==128,"Chunk updates not budgeted");int updates=0;for(int i=0;i<4;i++)if(!module.p())updates++;check(updates==1,"Lightmap throttle");
   var camera=client.gameRenderer.getCamera().getCameraPos();for(int i=0;i<250;i++)check(!module.a(camera.x,camera.y,camera.z),"Particle cap too early");check(module.a(camera.x,camera.y,camera.z),"Particle cap not enforced");
   check(module.r()&&module.s(),"Text optimizations unavailable");
   options.getMaxFps().setValue(160);module.setEnabled(false);
   check(options.getMaxFps().getValue()==160,"Manual video change overwritten on restore");
   check(options.getViewDistance().getValue()==view&&options.getSimulationDistance().getValue()==sim&&options.getEnableVsync().getValue()==vsync&&options.getParticles().getValue()==particles&&options.getCloudRenderMode().getValue()==clouds&&options.getPreset().getValue()==graphics&&options.getAo().getValue()==ao,"Video restoration");
  }finally{module.setEnabled(false);cap.setValue(oldCap);fast.setValue(oldFast);options.getMaxFps().setValue(max);client.onWindowFocusChanged(focused);options.write();}
  var manager=CosmeticsManager.INSTANCE;
  check(manager.getAllCosmetics().size()==142,"Merged cosmetic catalogue incomplete");
  var ids=new HashSet<String>();var slugs=new HashSet<String>();int pulse=0,animated=0;
  for(var item:manager.getAllCosmetics()){
   check(ids.add(item.getId())&&slugs.add(item.getSlug()),"Cosmetic ID collision");
   manager.ensureModelLoaded(item);check(item.getModel()!=null,"Model missing: "+item.getSlug());
   if(item.getSlug().startsWith("pulse_")){pulse++;if(item.getTextureFrames()>1){animated++;check(item.getModel().textureFrame(0)!=item.getModel().textureFrame(.25f),"Texture animation frozen");}}
  }
  check(pulse==64&&animated==8,"Pulse models/animations missing");
  var volcano=manager.getAllCosmetics().stream().filter(i->i.getSlug().equals("pulse_bundle_26")).findFirst().orElseThrow();check(volcano.getAnimation()!=null&&volcano.getAnimation().bones.values().stream().anyMatch(t->!t.rotationKeyframes.isEmpty()),"Vector/easing animation lost");
  check(manager.getAllCosmetics().stream().filter(i->i.getSlug().startsWith("pulse_bundle_")&&i.getAnimation()!=null).flatMap(i->i.getAnimation().bones.values().stream()).anyMatch(t->!t.scaleKeyframes.isEmpty()),"Scale animation lost");
  try(var stream=PulsePortCheck.class.getResourceAsStream("/legacy-cosmetic-ids.json")){
   for(var id:JsonParser.parseReader(new java.io.InputStreamReader(stream)).getAsJsonArray())check(manager.getCosmeticById(id.getAsString())!=null,"Old cosmetic missing");
  }
  var newHat=manager.getAllCosmetics().stream().filter(i->i.getSlug().startsWith("pulse_bundle_")&&i.getCategory().equals("hats")).findFirst().orElseThrow();
  var animatedModel=manager.getAllCosmetics().stream().filter(i->i.getSlug().equals("pulse_bundle_58")).findFirst().orElseThrow().getModel();
  float[] range={Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY};float[] v={Float.NaN};var recorder=(net.minecraft.client.render.VertexConsumer)java.lang.reflect.Proxy.newProxyInstance(PulsePortCheck.class.getClassLoader(),new Class[]{net.minecraft.client.render.VertexConsumer.class},(proxy,method,args)->{if(method.getParameterCount()==2&&method.getParameterTypes()[0]==float.class&&method.getParameterTypes()[1]==float.class){float texV=((Number)args[1]).floatValue();range[0]=Math.min(range[0],texV);range[1]=Math.max(range[1],texV);if(Float.isNaN(v[0]))v[0]=texV;}return method.getReturnType()==void.class?null:proxy;});
  animatedModel.render(new net.minecraft.client.util.math.MatrixStack(),recorder,15728880,0,1,1,1,1,null,0);check(range[1]-range[0]>.35f/30,"Texture UV scaled twice");float initialV=v[0];v[0]=Float.NaN;animatedModel.render(new net.minecraft.client.util.math.MatrixStack(),recorder,15728880,0,1,1,1,1,null,.25f);check(Float.isFinite(v[0])&&Math.abs(initialV-v[0])>.001f,"Animated UV not used by renderer");
  var oldHat=manager.getEquipped("hats");manager.toggleEquip(newHat);var config=com.example.auravisuals.config.ConfigManager.INSTANCE;
  config.saveConfig("pulse-cosmetics-smoke");manager.toggleEquip(newHat);config.loadConfig("pulse-cosmetics-smoke");check(manager.isEquipped(newHat),"New cosmetic selection not persisted");manager.toggleEquip(newHat);if(oldHat!=null&&!manager.isEquipped(oldHat))manager.toggleEquip(oldHat);config.deleteConfig("pulse-cosmetics-smoke");
  System.out.println("ANVIL PULSE PASSED: video apply/conditional restore, real background FPS cap, chunk/light/particle budgets, 78 original and 64 Pulse cosmetics, unique IDs, models, animated textures and config persistence");
 }

 public static void verifyWorld(MinecraftClient client){
  var module=ModuleManager.INSTANCE.getModule(Optimization.class);var pos=client.player.getEntityPos().add(2,0,2);
  var item=new net.minecraft.entity.ItemEntity(client.world,pos.x,pos.y,pos.z,new net.minecraft.item.ItemStack(net.minecraft.item.Items.STONE));
  module.beginFrame();int rendered=0;for(int i=0;i<8;i++)if(!module.a((net.minecraft.entity.Entity)item))rendered++;check(rendered==4,"Item render pile limit");
  int ticked=0;for(int i=0;i<8;i++)if(!module.a(item))ticked++;check(ticked==4,"Item tick pile limit");
  var far=client.player.getBlockPos().add(200,0,0);check(module.a(far,net.minecraft.block.entity.BlockEntityType.CHEST),"Block entity distance");check(!module.a(far,net.minecraft.block.entity.BlockEntityType.BEACON),"Beacon culled");check(module.a(far),"Sign text distance");
  var wall=new java.util.LinkedHashMap<net.minecraft.util.math.BlockPos,net.minecraft.block.BlockState>();
  var base=client.player.getBlockPos();
  var hidden=new net.minecraft.entity.ItemEntity(client.world,base.getX()+12.5,base.getY()+1,base.getZ()+.5,new net.minecraft.item.ItemStack(net.minecraft.item.Items.STONE));
  try{
   for(int y=-1;y<=4;y++)for(int z=-2;z<=2;z++){var block=base.add(8,y,z);wall.put(block,client.world.getBlockState(block));client.world.setBlockState(block,net.minecraft.block.Blocks.STONE.getDefaultState(),3);}
   module.setEnabled(false);module.setEnabled(true);module.onTick();check(module.a((net.minecraft.entity.Entity)hidden),"Entity behind opaque wall not culled");
   for(var block:wall.keySet())client.world.setBlockState(block,net.minecraft.block.Blocks.GLASS.getDefaultState(),3);
   module.setEnabled(false);module.setEnabled(true);module.onTick();check(!module.a((net.minecraft.entity.Entity)hidden),"Entity behind glass incorrectly culled");
  }finally{for(var entry:wall.entrySet())client.world.setBlockState(entry.getKey(),entry.getValue(),3);}
  var cosmetics=CosmeticsManager.INSTANCE;cosmetics.ensureLoaded();
  for(String slug:new String[]{"pulse_bundle_53","pulse_bundle_21","pulse_bundle_28","pulse_bundle_0","pulse_bundle_38"}){var cosmetic=cosmetics.getAllCosmetics().stream().filter(i->i.getSlug().equals(slug)).findFirst().orElseThrow();if(!cosmetics.isEquipped(cosmetic))cosmetics.toggleEquip(cosmetic);}
  client.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_BACK);client.player.setPitch(10);
  System.out.println("ANVIL PULSE WORLD PASSED: limited item piles, distant block entities/signs, beacon exemption, opaque/glass visibility and enabled render hooks");
 }
}
