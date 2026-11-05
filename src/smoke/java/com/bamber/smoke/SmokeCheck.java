package com.bamber.smoke;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.bamber.client.*;
import com.example.auravisuals.module.ModuleManager;
public final class SmokeCheck implements ClientModInitializer {
 private int ticks,stage;
 public void onInitializeClient(){
  ClientTickEvents.END_CLIENT_TICK.register(client->{
   if(client.getOverlay()!=null||++ticks%60!=0)return;
   try{
    int step=stage++;
    if(step>=2&&step<=10){rich.screens.clickgui.ClickGui.INSTANCE.selectCategory(NativeGuiCheck.TABS[step-2]);System.out.println("BAMBER TAB: "+NativeGuiCheck.TABS[step-2]);return;}
    switch(step){
     case 0 -> {
      Class.forName("com.example.auravisuals.util.render.MirageGlassPipeline");
      Class.forName("com.example.auravisuals.util.render.HandFxPipeline");
      var texture=net.minecraft.util.Identifier.of("minecraft","textures/entity/player/wide/steve.png");
      if(com.example.auravisuals.util.render.HitColorRenderLayers.tint(texture)==null || com.example.auravisuals.util.render.HitColorRenderLayers.full(texture)==null)throw new AssertionError("Hit color render layers");
      System.out.println("BAMBER SMOKE: glass, hand effects and hit-color pipelines initialized");
      client.setScreen(new rich.screens.menu.MainMenuScreen());System.out.println("BAMBER SMOKE: menu loaded");}
     case 1 -> {
      NativeGuiCheck.verify(client);
      if(ModuleManager.INSTANCE.getModules().size()!=96)throw new AssertionError("Expected 96 modules");
      if(FrontendBridge.modules().size()!=96)throw new AssertionError("GUI module coverage");
      client.setScreen(rich.screens.clickgui.ClickGui.INSTANCE);
      System.out.println("BAMBER SMOKE: GUI loaded with all modules");
     }
     case 11 -> {
      var trigger=ModuleManager.INSTANCE.getModule(TriggerBot.class);
      trigger.setEnabled(true);trigger.onTick();
      if(!trigger.isEnabled())throw new AssertionError("Trigger engine activation");
      trigger.setKey(86);
      com.example.auravisuals.config.ConfigManager.INSTANCE.saveConfig("bamber-smoke");
      trigger.setKey(82);
      com.example.auravisuals.config.ConfigManager.INSTANCE.loadConfig("bamber-smoke");
      if(trigger.getKey()!=86)throw new AssertionError("Trigger rebind persistence");
      trigger.setKey(82);trigger.setEnabled(false);
      System.out.println("BAMBER SMOKE: original trigger engine loads; rebind persists");
     }
     case 12 -> {
      client.setScreen(null);ModuleManager.INSTANCE.getModule(Unhook.class).setEnabled(true);
      var configFile=java.nio.file.Path.of(client.runDirectory.getAbsolutePath(),"bamber","configs","default.json");
      var config=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(configFile)).getAsJsonObject();
      if(config.getAsJsonObject("Unhook").get("enabled").getAsBoolean())throw new AssertionError("Unhook persisted enabled");
      if(BamberRuntime.isActive())throw new AssertionError("Unhook active flag");
      for(var m:ModuleManager.INSTANCE.getModules())if(m.isEnabled())throw new AssertionError("Module still enabled: "+m.getName());
      if(client.currentScreen!=null && !(client.currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen))throw new AssertionError("Custom GUI still open");
      System.out.println("BAMBER SMOKE PASSED: menu, GUI, 96 modules, trigger lifecycle, binding persistence, unhook");
      client.scheduleStop();
     }
    }
   }catch(Throwable e){e.printStackTrace();System.err.println("BAMBER SMOKE FAILED");client.scheduleStop();}
  });
 }
}
