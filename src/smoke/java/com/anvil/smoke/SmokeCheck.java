package com.anvil.smoke;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.anvil.client.*;
import com.example.auravisuals.module.ModuleManager;
public final class SmokeCheck implements ClientModInitializer {
 private int ticks,stage;
 public void onInitializeClient(){
  if(Boolean.getBoolean("anvil.worldSmoke")){MythWorldCheck.install();return;}
  ClientTickEvents.END_CLIENT_TICK.register(client->{
   if(client.getOverlay()!=null||++ticks%60!=0)return;
   try{
    int step=stage++;
    if(step==10){client.setScreen(new ShadowCheckScreen());return;}
    if(step==5){var f=rich.screens.clickgui.ClickGui.class.getDeclaredField("cosmeticsGallery");f.setAccessible(true);((CosmeticsGallery)f.get(rich.screens.clickgui.ClickGui.INSTANCE)).setCategory(6);}
    if(step>=2&&step<=10){rich.screens.clickgui.ClickGui.INSTANCE.selectCategory(step==2?rich.modules.module.category.ModuleCategory.SWORDS:NativeGuiCheck.TABS[step-2]);System.out.println("ANVIL TAB: "+(step==2?"SWORDS":NativeGuiCheck.TABS[step-2]));return;}
    switch(step){
     case 0 -> {
      if(Boolean.getBoolean("anvil.rpcSmoke")){
       if(!com.example.auravisuals.util.integration.DiscordRPC.applicationId().equals("1568234202254807080"))throw new AssertionError("Discord Application ID");
       ModuleManager.INSTANCE.getModule(com.example.auravisuals.module.misc.DiscordRCP.class).setEnabled(true);
      }

      Class.forName("com.example.auravisuals.util.render.MirageGlassPipeline");
      Class.forName("com.example.auravisuals.util.render.HandFxPipeline");
      var texture=net.minecraft.util.Identifier.of("minecraft","textures/entity/player/wide/steve.png");
      if(com.example.auravisuals.util.render.HitColorRenderLayers.tint(texture)==null || com.example.auravisuals.util.render.HitColorRenderLayers.full(texture)==null)throw new AssertionError("Hit color render layers");
      System.out.println("ANVIL SMOKE: glass, hand effects and hit-color pipelines initialized");
      client.setScreen(new rich.screens.menu.MainMenuScreen());System.out.println("ANVIL SMOKE: menu loaded");}
     case 1 -> {
      NativeGuiCheck.verify(client);
      SwordGalleryCheck.verify();
      PulsePortCheck.verify(client);
      if(ModuleManager.INSTANCE.getModules().size()!=97)throw new AssertionError("Expected 97 modules");
      if(FrontendBridge.modules().size()!=97)throw new AssertionError("GUI module coverage");
      client.setScreen(rich.screens.clickgui.ClickGui.INSTANCE);
      System.out.println("ANVIL SMOKE: GUI loaded with all modules");
     }
     case 11 -> {
      if(Boolean.getBoolean("anvil.rpcSmoke")&&!"Подключено".equals(com.example.auravisuals.util.integration.DiscordRPC.status()))throw new AssertionError("Discord RPC not connected");
      if(ModuleManager.INSTANCE.getModules().stream().anyMatch(m->m.getName().toLowerCase().contains("trigger")))throw new AssertionError("Trigger remains registered");
      System.out.println("ANVIL SMOKE: trigger removed; Discord RPC module registered");
     }
     case 12 -> {
      client.setScreen(null);ModuleManager.INSTANCE.getModule(Unhook.class).setEnabled(true);
      var configFile=java.nio.file.Path.of(client.runDirectory.getAbsolutePath(),"anvil","configs","default.json");
      var config=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(configFile)).getAsJsonObject();
      if(config.getAsJsonObject("Unhook").get("enabled").getAsBoolean())throw new AssertionError("Unhook persisted enabled");
      if(AnvilRuntime.isActive())throw new AssertionError("Unhook active flag");
      for(var m:ModuleManager.INSTANCE.getModules())if(m.isEnabled())throw new AssertionError("Module still enabled: "+m.getName());
      if(client.currentScreen!=null && !(client.currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen))throw new AssertionError("Custom GUI still open");
      System.out.println("ANVIL SMOKE PASSED: menu, GUI, 97 modules, Discord RPC, unhook");
      client.scheduleStop();
     }
    }
   }catch(Throwable e){e.printStackTrace();System.err.println("ANVIL SMOKE FAILED");client.scheduleStop();}
  });
 }
}
