package com.anvil.client;
import net.minecraft.client.MinecraftClient;
import com.example.auravisuals.module.ModuleManager;
public final class AnvilRuntime {
 private static boolean active=true;
 public static boolean isActive(){return active;}
 public static void init(){
  rich.Initialization.getInstance();
  rich.util.render.font.FontInitializer.register();
  rich.util.render.gif.GifRender.init();
 }
 public static void unhook(){
  if(!active)return;
  com.example.auravisuals.config.ConfigManager.INSTANCE.saveConfig("default");
  for(var module:ModuleManager.INSTANCE.getModules()){
   if(module.isEnabled()){
    try{module.onDisable();}catch(Throwable t){com.example.auravisuals.AuraVisuals.LOGGER.warn("Unable to disable {}",module.getName(),t);}
    module.setEnabledSilently(false);
   }
  }
  com.example.auravisuals.live.LiveScriptManager.INSTANCE.clearVisuals();
  com.example.auravisuals.util.integration.DiscordRPC.shutdown();
  active=false;
  com.example.auravisuals.module.misc.Globals.shutdown();
  com.example.auravisuals.util.media.MediaSessionReader.getInstance().stop();
  com.example.auravisuals.cosmetics.CosmeticsNetworkClient.INSTANCE.shutdown();
  com.example.auravisuals.util.chat.ChatHubClient.INSTANCE.disconnect();
  com.example.auravisuals.util.friend.FriendHubClient.INSTANCE.disconnect();
  com.example.auravisuals.util.friendlabels.FriendLabelsClient.INSTANCE.disconnect();
  com.example.auravisuals.util.friendscreen.FriendScreenClient.INSTANCE.disconnect();
  MinecraftClient mc=MinecraftClient.getInstance();mc.setScreen(null);
 }
}
