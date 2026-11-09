package com.anvil.client.performance;
import java.util.*;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
public final class PulseMixinCompatibility implements IMixinConfigPlugin {
 public void onLoad(String name){}
 public String getRefMapperConfig(){return null;}
 public boolean shouldApplyMixin(String target,String mixin){
  var loader=net.fabricmc.loader.api.FabricLoader.getInstance();
  if(mixin.endsWith("TextRendererGlyphDrawerMixin")||mixin.endsWith("GlyphAtlasTextureMixin"))return !loader.isModLoaded("immediatelyfast");
  if(mixin.endsWith("LeavesCullingMixin"))return !loader.isModLoaded("cullleaves")&&!loader.isModLoaded("moreculling");
  return true;
 }
 public void acceptTargets(Set<String> mine,Set<String> others){}
 public List<String> getMixins(){return null;}
 public void preApply(String target,ClassNode node,String mixin,IMixinInfo info){}
 public void postApply(String target,ClassNode node,String mixin,IMixinInfo info){}
}
