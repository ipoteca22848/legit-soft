package com.bamber.smoke;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.bamber.client.*;
import com.example.auravisuals.module.ModuleManager;
public final class SmokeCheck implements ClientModInitializer {
 private int ticks,stage;
 public void onInitializeClient(){
  ClientTickEvents.END_CLIENT_TICK.register(client->{
   if(client.getOverlay()!=null||++ticks%100!=0)return;
   try{
    switch(stage++){
     case 0 -> {client.setScreen(new rich.screens.menu.MainMenuScreen());System.out.println("BAMBER SMOKE: menu loaded");}
     case 1 -> {
      if(ModuleManager.INSTANCE.getModules().size()!=96)throw new AssertionError("Expected 96 modules");
      if(FrontendBridge.modules().size()!=97)throw new AssertionError("GUI module coverage");
      client.setScreen(rich.screens.clickgui.ClickGui.INSTANCE);
      System.out.println("BAMBER SMOKE: GUI loaded with all modules");
     }
     case 2 -> {
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
     case 3 -> {
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
