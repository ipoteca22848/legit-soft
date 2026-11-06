package com.bamber.client;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.particle.ParticlesMode;
/** Opt-in Minecraft settings profiles. Existing choices are kept for restoration. */
public final class MinecraftPerformanceProfiles {
 private static final Map<SimpleOption<?>,Object> saved=new LinkedHashMap<>();
 private static <T> void set(SimpleOption<T> option,T value){saved.putIfAbsent(option,option.getValue());option.setValue(value);}
 public static void apply(boolean maximum){
  var options=MinecraftClient.getInstance().options;
  set(options.getViewDistance(),Math.min(options.getViewDistance().getValue(),maximum?6:8));
  set(options.getSimulationDistance(),Math.min(options.getSimulationDistance().getValue(),5));
  set(options.getBiomeBlendRadius(),0);
  set(options.getCloudRenderMode(),CloudRenderMode.OFF);
  set(options.getEntityShadows(),false);
  set(options.getEntityDistanceScaling(),Math.min(options.getEntityDistanceScaling().getValue(),maximum?.6:.8));
  var particles=options.getParticles().getValue();
  set(options.getParticles(),maximum?ParticlesMode.MINIMAL:particles==ParticlesMode.MINIMAL?particles:ParticlesMode.DECREASED);
  options.write();
 }
 @SuppressWarnings({"rawtypes","unchecked"}) public static void restore(){
  if(saved.isEmpty())return;
  saved.forEach((option,value)->((SimpleOption)option).setValue(value));saved.clear();MinecraftClient.getInstance().options.write();
 }
 public static boolean applied(){return !saved.isEmpty();}
 private MinecraftPerformanceProfiles(){}
}
