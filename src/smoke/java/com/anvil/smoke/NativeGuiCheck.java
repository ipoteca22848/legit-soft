package com.anvil.smoke;
import com.anvil.client.*;
import rich.modules.module.*;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.*;
import java.util.*;
public final class NativeGuiCheck {
 public static final ModuleCategory[] TABS={ModuleCategory.HUD,ModuleCategory.MARKS,ModuleCategory.CONFIGS,ModuleCategory.COSMETICS,ModuleCategory.THEMES,ModuleCategory.EVENTS,ModuleCategory.MINES,ModuleCategory.MESSENGER,ModuleCategory.SETTINGS};
 private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 private static void click(ModuleStructure m,String name){((ButtonSetting)m.get(name)).getRunnable().run();}
 private static void text(ModuleStructure m,String name,String value){((TextSetting)m.get(name)).setText(value);}
 public static void verify(net.minecraft.client.MinecraftClient client) throws Exception {
  for(int[] viewport:new int[][]{{427,240},{640,360},{320,240},{640,270}}){
   float top=HudViewport.hotbarTop(viewport[1]);
   check(top>viewport[1]-30,"Hotbar overlaps survival health/food");
   check(HudViewport.hotbarBottom(viewport[1])<=viewport[1],"Hotbar outside viewport");
   check(Math.abs(HudViewport.hotbarLeft(viewport[0])+91-viewport[0]/2f)<.01f,"Hotbar not centered");
  }
  var options=client.options;int view=options.getViewDistance().getValue(),simulation=options.getSimulationDistance().getValue();
  var clouds=options.getCloudRenderMode().getValue();var particles=options.getParticles().getValue();boolean shadows=options.getEntityShadows().getValue();
  MinecraftPerformanceProfiles.apply(false);check(options.getViewDistance().getValue()<=view,"Balance raises render distance");
  MinecraftPerformanceProfiles.apply(true);check(options.getViewDistance().getValue()<=6,"Maximum render distance");check(options.getSimulationDistance().getValue()==Math.min(simulation,5),"Valid simulation distance");
  MinecraftPerformanceProfiles.restore();check(options.getViewDistance().getValue()==view&&options.getSimulationDistance().getValue()==simulation&&options.getCloudRenderMode().getValue()==clouds&&options.getParticles().getValue()==particles&&options.getEntityShadows().getValue()==shadows&&!MinecraftPerformanceProfiles.applied(),"Performance settings restoration");
  System.out.println("ANVIL PERFORMANCE PASSED: hotbar geometry at four viewports and reversible Minecraft profiles");
  for(var cat:TABS)check(!NativeSections.cards(cat).isEmpty(),"Missing native tab "+cat);
  var config=NativeSections.cards(ModuleCategory.CONFIGS).get(0);text(config,"Название конфига","native-ui-smoke");click(config,"Сохранить новый");
  check(com.example.auravisuals.config.ConfigManager.INSTANCE.listConfigs().contains("native-ui-smoke"),"Native config creation");
  var inventory=NativeSections.cards(ModuleCategory.HUD).stream().filter(m->m.getName().equals("Инвентарь")).findFirst().orElseThrow();
  boolean original=com.example.auravisuals.module.render.Hud.inventory.getValue();inventory.setState(false);
  check(!com.example.auravisuals.module.render.Hud.inventory.getValue(),"Inventory toggle");
  com.example.auravisuals.client.gui.hud.Inventory.INSTANCE.render(null,null);
  var c=com.example.auravisuals.config.ConfigManager.INSTANCE;c.saveConfig("native-ui-smoke");inventory.setState(true);c.loadConfig("native-ui-smoke");
  check(!com.example.auravisuals.module.render.Hud.inventory.getValue(),"Inventory visibility persistence");inventory.setState(original);
  var markers=com.example.auravisuals.module.ModuleManager.INSTANCE.getModule(com.example.auravisuals.module.render.Markers.class);int count=markers.getMarkers().size();
  var marker=NativeSections.cards(ModuleCategory.MARKS).get(0);text(marker,"Название метки","AnvilSmokeMarker");text(marker,"X","12");text(marker,"Y","64");text(marker,"Z","-34");click(marker,"Добавить метку");
  check(markers.getMarkers().size()==count+1,"Native marker creation");
  var entry=NativeSections.cards(ModuleCategory.MARKS).stream().filter(m->m.getName().equals("AnvilSmokeMarker")).findFirst().orElseThrow();text(entry,"X","17");click(entry,"Применить");
  check(markers.getMarkers().stream().anyMatch(m->m.name.equals("AnvilSmokeMarker")&&m.x==17),"Native marker editing");
  entry=NativeSections.cards(ModuleCategory.MARKS).stream().filter(m->m.getName().equals("AnvilSmokeMarker")).findFirst().orElseThrow();click(entry,"Удалить");check(markers.getMarkers().size()==count,"Native marker deletion");
  var cosmetics=com.example.auravisuals.cosmetics.CosmeticsManager.INSTANCE;var all=NativeSections.cards(ModuleCategory.COSMETICS);check(all.size()==cosmetics.getAllCosmetics().size()+1,"Entire cosmetic catalogue");
  if(all.size()>1){var item=all.get(1);boolean was=item.isState();item.setState(!was);check(item.isState()!=was,"Native cosmetics equip");item.setState(was);}
  var gallery=new CosmeticsGallery();check(gallery.items().size()==cosmetics.getAllCosmetics().size(),"Gallery all items");gallery.setCategory(1);check(gallery.items().stream().allMatch(i->i.getCategory().equals("wings")),"Gallery category filter");gallery.setCategory(0);
  if(!gallery.items().isEmpty()){var item=gallery.items().get(0);boolean before=cosmetics.isEquipped(item);gallery.mouseClicked(110,65,0,96,38,294,204);check(cosmetics.isEquipped(item)!=before,"Gallery card click");gallery.mouseClicked(110,65,0,96,38,294,204);check(cosmetics.isEquipped(item)==before,"Gallery card remove");}
  var themes=NativeSections.cards(ModuleCategory.THEMES).get(0);text(themes,"Название темы","AnvilSmokeTheme");click(themes,"Создать из текущей");
  var theme=NativeSections.cards(ModuleCategory.THEMES).stream().filter(m->m.getName().equals("AnvilSmokeTheme")).findFirst().orElseThrow();click(theme,"Удалить тему");
  var delete=NativeSections.cards(ModuleCategory.CONFIGS).stream().filter(m->m.getName().equals("native-ui-smoke")).findFirst().orElseThrow();click(delete,"Удалить");
  // Validate the actual shader data and capture lock, not a development-only rendering path.
  com.example.auravisuals.module.render.Hud.stableEffects.setValue(true);
  var pipeline=com.example.auravisuals.util.render.MirageGlassPipeline.class;
  com.example.auravisuals.util.render.MirageGlassPipeline.beginRenderFrame();com.example.auravisuals.util.render.MirageGlassPipeline.captureHudFrame();
  var captured=pipeline.getDeclaredField("lastCaptureNanos");captured.setAccessible(true);long stamp=captured.getLong(null);
  com.example.auravisuals.util.render.MirageGlassPipeline.captureHudFrame();check(stamp==captured.getLong(null),"HUD captured more than once in a frame");
  var ctx=new net.minecraft.client.gui.DrawContext(client,new net.minecraft.client.gui.render.state.GuiRenderState(),0,0);
  com.example.auravisuals.util.render.MirageGlassPipeline.drawLiquidAt(ctx,4,4,30,20,3,2,1,.6f,1,new java.awt.Color(255,176,158,72));
  var data=pipeline.getDeclaredField("dataBuffer");data.setAccessible(true);check(((java.nio.ByteBuffer)data.get(null)).getFloat(136)==0,"HUD shader still animates brightness/warp");
  System.out.println("ANVIL NATIVE PASSED: nine tabs, config CRUD, marker CRUD, cosmetic catalogue/equip, themes, inventory persistence, calm HUD shader and single frame capture");
 }
}
