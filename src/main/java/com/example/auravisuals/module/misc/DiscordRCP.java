package com.example.auravisuals.module.misc;
import com.example.auravisuals.module.*;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.*;
import com.example.auravisuals.util.integration.DiscordRPC;
public class DiscordRCP extends Module {
 private final StringSetting applicationId=new StringSetting("Discord Application ID",DiscordRPC.applicationId());
 public DiscordRCP(){
  super("DiscordRPC","Статус anvil visual в Discord. Нужен Application ID вашего Discord-приложения.",Category.MISC);
  setEnabledSilently(true);
  addSettings(applicationId,new ActionSetting("Применить Application ID",()->{
   try{DiscordRPC.configure(applicationId.getValue());setEnabled(true);DiscordRPC.start();}
   catch(IllegalArgumentException e){net.minecraft.client.MinecraftClient mc=net.minecraft.client.MinecraftClient.getInstance();if(mc.player!=null)mc.player.sendMessage(net.minecraft.text.Text.literal(e.getMessage()),false);}
  }));
 }
 @Override public void onEnable(){DiscordRPC.start();}
 @Override public void onDisable(){DiscordRPC.shutdown();}
}
