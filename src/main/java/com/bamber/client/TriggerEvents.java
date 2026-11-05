package com.bamber.client;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
public final class TriggerEvents {
 public static final Event<ClientTickEvents.StartTick> START_CLIENT_TICK=EventFactory.createArrayBacked(ClientTickEvents.StartTick.class,callbacks->client->{for(var callback:callbacks)callback.onStartTick(client);});
 public static final Event<ClientTickEvents.EndTick> END_CLIENT_TICK=EventFactory.createArrayBacked(ClientTickEvents.EndTick.class,callbacks->client->{for(var callback:callbacks)callback.onEndTick(client);});
 static {
  ClientTickEvents.START_CLIENT_TICK.register(client->{if(allowed(client))START_CLIENT_TICK.invoker().onStartTick(client);});
  ClientTickEvents.END_CLIENT_TICK.register(client->{if(allowed(client))END_CLIENT_TICK.invoker().onEndTick(client);});
 }
 private static boolean allowed(MinecraftClient client){
  return BamberRuntime.isActive()&&client.currentScreen==null&&client.player!=null&&client.world!=null&&com.example.auravisuals.module.ModuleManager.INSTANCE.getModule(TriggerBot.class).isEnabled();
 }
}
