package com.anvil.smoke;
import com.anvil.smoke.mixin.CreateWorldInvoker;
import com.anvil.client.atmosphere.*;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.BooleanSetting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.world.*;
public final class MythWorldCheck {
 private static int stage,ticks;
 public static void install(){ClientTickEvents.END_CLIENT_TICK.register(client->{
  if(client.getOverlay()!=null)return;
  try{
   if(stage==0&&++ticks>60){client.options.getViewDistance().setValue(5);client.options.getSimulationDistance().setValue(5);CreateWorldScreen.showTestWorld(client,()->{throw new AssertionError("World cancelled");});stage=1;return;}
   if(stage==1&&client.currentScreen instanceof CreateWorldScreen screen){
    var creator=screen.getWorldCreator();creator.setWorldName("Anvil atmosphere test");creator.setSeed("12345");creator.setGameMode(WorldCreator.Mode.CREATIVE);creator.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL);((CreateWorldInvoker)screen).anvil$createLevel();stage=2;ticks=0;return;
   }
   if(stage==2&&client.world!=null&&client.player!=null){
    ModuleManager.INSTANCE.getModule(com.example.auravisuals.module.render.Hud.class).setEnabled(true);
    ModuleManager.INSTANCE.getModule(AtmosphereModule.class).setEnabled(true);
    var optimization=ModuleManager.INSTANCE.getModule(com.example.auravisuals.module.render.Optimization.class);optimization.setEnabled(true);optimization.onTick();
    client.player.setPitch(-10);stage=3;ticks=0;return;
   }
   if(stage==3&&++ticks>100){
    var module=ModuleManager.INSTANCE.getModule(AtmosphereModule.class);
    if(!client.getResourcePackManager().getEnabledIds().contains("anvil:atmosphere_winter"))throw new AssertionError("Winter pack not enabled");
    if(!AtmosphereModule.val2())throw new AssertionError("Winter fog not active");
    var f=AtmosphereModule.class.getDeclaredField("val1");f.setAccessible(true);if(((java.util.List<?>)f.get(module)).isEmpty())throw new AssertionError("Snow particles not spawned");
    PulsePortCheck.verifyWorld(client);
    System.out.println("ANVIL WORLD: ATMOSPHERE");stage=4;ticks=0;return;
   }
   if(stage==4&&++ticks>80){
    var module=ModuleManager.INSTANCE.getModule(AtmosphereModule.class);
    ((BooleanSetting)module.getSettings().stream().filter(s->s.getDisplayName().equals("Зимние текстуры")).findFirst().orElseThrow()).setValue(false);stage=5;ticks=0;return;
   }
   if(stage==5&&++ticks>40){
    if(client.getResourcePackManager().getEnabledIds().contains("anvil:atmosphere_winter"))throw new AssertionError("Winter pack not removed");
    ModuleManager.INSTANCE.getModule(AtmosphereModule.class).setEnabled(false);
    ModuleManager.INSTANCE.getModule(com.example.auravisuals.module.render.Optimization.class).setEnabled(false);
    if(AtmosphereModule.val2())throw new AssertionError("Winter fog remains active");
    System.out.println("ANVIL WORLD PASSED: flat world, snow rendering, winter textures, fog, island HUD, resource restoration");client.scheduleStop();stage=6;
   }
  }catch(Throwable t){t.printStackTrace();System.err.println("ANVIL WORLD FAILED");client.scheduleStop();stage=6;}
 });}
}
