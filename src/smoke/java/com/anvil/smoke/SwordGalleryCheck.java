package com.anvil.smoke;
import com.anvil.client.SwordGallery;
import com.example.auravisuals.module.misc.CustomSword;
import net.minecraft.component.DataComponentTypes;
public final class SwordGalleryCheck {
 public static void verify(){
  var categories=new rich.screens.clickgui.impl.background.render.CategoryRenderer();
  if(categories.getCategoryAtPosition(40,108,0,0)!=rich.modules.module.category.ModuleCategory.SWORDS)throw new AssertionError("Swords not visible/clickable without sidebar scrolling");

  var module=CustomSword.INSTANCE;var gallery=new SwordGallery();
  if(gallery.items().size()!=119)throw new AssertionError("Incomplete sword catalogue");
  boolean enabled=module.isEnabled();String[] old=new String[6];boolean[] applied=new boolean[6];
  for(int i=0;i<6;i++){old[i]=module.selectedModel(i);applied[i]=module.appliesTo(i);}
  try{
   for(int i=0;i<6;i++){
    gallery.setType(i);String chosen=gallery.items().get(i+1);gallery.select(chosen);
    if(!module.selectedModel(i).equals(chosen)||!module.appliesTo(i)||!module.isEnabled())throw new AssertionError("Sword choice not applied");
    var preview=CustomSword.previewStack(i,chosen);var input=new net.minecraft.item.ItemStack(preview.getItem());var rendered=module.getRenderStack(input);
    if(input.contains(DataComponentTypes.CUSTOM_MODEL_DATA))throw new AssertionError("Render changed original stack");
    if(!preview.get(DataComponentTypes.CUSTOM_MODEL_DATA).equals(rendered.get(DataComponentTypes.CUSTOM_MODEL_DATA)))throw new AssertionError("Preview/game model mismatch");
   }
   var unselected=CustomSword.previewStack(0,gallery.items().get(20));if(module.getRenderStack(unselected)!=unselected)throw new AssertionError("Preview overwritten by equipped model");
   var config=com.example.auravisuals.config.ConfigManager.INSTANCE;config.saveConfig("sword-gallery-smoke");
   module.selectModel(0,gallery.items().get(10));config.loadConfig("sword-gallery-smoke");
   if(!module.selectedModel(0).equals(gallery.items().get(1)))throw new AssertionError("Sword config persistence");
   gallery.setType(4);gallery.mouseClicked(110,90,0,96,38,294,204);
   if(!module.selectedModel(4).equals(gallery.items().get(0)))throw new AssertionError("Sword card click");
   System.out.println("ANVIL SWORDS PASSED: 119 models, six independent targets, live preview stacks, card click and config persistence");
  }finally{com.example.auravisuals.config.ConfigManager.INSTANCE.deleteConfig("sword-gallery-smoke");for(int i=0;i<6;i++){module.selectModel(i,old[i]);module.setAppliesTo(i,applied[i]);}module.setEnabled(enabled);}
 }
}
