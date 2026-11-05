package com.bamber.client;

import java.util.*;
import java.util.function.*;
import net.minecraft.client.MinecraftClient;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.Setting;
import rich.modules.module.setting.implement.*;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.Markers;
import com.example.auravisuals.module.render.Markers$MarkerData;
import com.example.auravisuals.config.ConfigManager;
import com.example.auravisuals.cosmetics.*;
import com.example.auravisuals.theme.*;
import com.example.auravisuals.util.events.*;
import com.example.auravisuals.util.chat.*;
import com.example.auravisuals.util.friend.*;

/** Native Rich controls backed by the visual client's managers. No legacy ClickGui screens. */
public final class NativeSections {
 private static final Map<String,ModuleStructure> forms=new LinkedHashMap<>();
 private static final Map<ModuleCategory,List<ModuleStructure>> pages=new EnumMap<>(ModuleCategory.class);
 private static boolean dirty=true;
 private static String status="";
 private static long lastLiveRefresh;
 private static int liveSignature;
 private static final SelectSetting cosmeticFilter=new SelectSetting("Категория", ""){@Override public void setSelected(String v){super.setSelected(v);invalidate();}}.value("all","wings","hats","back","pets").selected("all");
 private static final SelectSetting eventServer=new SelectSetting("Сервер", ""){@Override public void setSelected(String v){super.setSelected(v);invalidate();}}.value("FunTime","HolyWorld").selected("FunTime");
 private static final TextSetting configName=text("Название конфига",""),configCode=text("Код импорта", "");
 private static final TextSetting markerName=text("Название метки",""),markerX=text("X","0"),markerY=text("Y","64"),markerZ=text("Z","0");
 private static final TextSetting themeName=text("Название темы", "");
 private static final TextSetting recipient=text("Получатель", ""),message=text("Сообщение", ""),groupName=text("Название группы", "");
 private static final SelectSetting channel=new SelectSetting("Канал", ""){@Override public void setSelected(String v){super.setSelected(v);invalidate();}}.value("Общий","ЛС","Группа").selected("Общий");
 private static final SelectSetting markerIcon=new SelectSetting("Иконка", "").value(Arrays.stream(new String[]{"A","m","o","q","r","t","[","]","<","^","="}).toArray(String[]::new));
 private static final ColorSetting markerBg=new ColorSetting("Фон", "").value(0xff181b26),markerText=new ColorSetting("Текст", "").value(0xffffffff),markerColor=new ColorSetting("Иконка — цвет", "").value(0xffff4e5a);
 private static final SliderSettings markerSize=new SliderSettings("Размер", "").range(.5f,2.5f).setValue(1);
 private NativeSections(){}
 public static void invalidate(){var mc=MinecraftClient.getInstance();if(mc!=null&&!mc.isOnThread()){mc.execute(NativeSections::invalidate);return;}pages.clear();dirty=true;}
 public static boolean consumeRefresh(){
  long now=System.currentTimeMillis();
  if(now-lastLiveRefresh>1000){lastLiveRefresh=now;
   var f=FriendHubClient.INSTANCE;int sig=Objects.hash(ChatHubClient.INSTANCE.getMessageSeq(),f.getFriends().size(),f.getIncomingRequests().size(),f.getGroups().size(),f.getIncomingGroupInvites().size());
   if(sig!=liveSignature){liveSignature=sig;pages.remove(ModuleCategory.MESSENGER);dirty=true;}
  }
  boolean result=dirty;dirty=false;return result;
 }
 public static List<ModuleStructure> cards(ModuleCategory category){return pages.computeIfAbsent(category,NativeSections::build);}
 public static TextSetting text(String name,String value){return new TextSetting(name,"").setText(value).setMin(0).setMax(8192);}
 public static ButtonSetting button(String name,Runnable action){return new ButtonSetting(name,"").setButtonName(name).setRunnable(()->{try{action.run();}catch(Exception e){status=e.getMessage()==null?"Не удалось выполнить действие":e.getMessage();}});}
 public static BooleanSetting bool(String name,BooleanSupplier read,Consumer<Boolean> write){return new BooleanSetting(name,""){@Override public boolean isValue(){return read.getAsBoolean();}@Override public BooleanSetting setValue(boolean value){write.accept(value);return this;}};}
 public static ColorSetting color(String name,IntSupplier read,IntConsumer write){
  return new ColorSetting(name,""){
   private boolean ready;
   @Override public int getColor(){return read.getAsInt();}
   @Override public int getColorNoAlpha(){return read.getAsInt()|0xff000000;}
   @Override public float getHue(){super.setColor(read.getAsInt());return super.getHue();}
   @Override public float getSaturation(){super.setColor(read.getAsInt());return super.getSaturation();}
   @Override public float getBrightness(){super.setColor(read.getAsInt());return super.getBrightness();}
   @Override public float getAlpha(){super.setColor(read.getAsInt());return super.getAlpha();}
   private ColorSetting sync(){if(ready)write.accept(super.getColor());return this;}
   @Override public ColorSetting setHue(float v){super.setHue(v);return sync();}
   @Override public ColorSetting setSaturation(float v){super.setSaturation(v);return sync();}
   @Override public ColorSetting setBrightness(float v){super.setBrightness(v);return sync();}
   @Override public ColorSetting setAlpha(float v){super.setAlpha(v);return sync();}
   @Override public ColorSetting setColor(int v){super.setColor(v);if(ready)write.accept(v);ready=true;return this;}
  }.value(read.getAsInt());
 }
 private static LabelSetting label(Supplier<String> value){return new LabelSetting(""){@Override public String getName(){return value.get();}};}
 private static ModuleStructure form(String key,String name,ModuleCategory cat,Supplier<Setting[]> settings){
  return forms.computeIfAbsent(key,k->{var f=new ModuleStructure(name,"",cat){@Override public void setState(boolean enabled){}@Override public boolean isState(){return false;}};f.settings(settings.get());return f;});
 }
 private static void save(){ConfigManager.INSTANCE.saveConfig("default");}
 private static void done(String text){status=text;invalidate();}
 private static String safeName(TextSetting field){String v=field.getText().trim();if(v.isEmpty()||v.contains("/")||v.contains("\\")||v.equals(".")||v.equals(".."))throw new IllegalArgumentException("Укажи корректное название");return v;}
 private static double coordinate(TextSetting field){double v=Double.parseDouble(field.getText().replace(',','.'));if(!Double.isFinite(v)||Math.abs(v)>30000000)throw new IllegalArgumentException("Координата вне допустимого диапазона");return v;}
 private static List<ModuleStructure> build(ModuleCategory category){
  List<ModuleStructure> list=new ArrayList<>();
  switch(category){
   case CONFIGS -> configs(list);
   case MARKS -> markers(list);
   case COSMETICS -> cosmetics(list);
   case THEMES -> themes(list);
   case HUD -> hud(list);
   case EVENTS,MINES -> events(list,category);
   case MESSENGER -> messenger(list);
   case SETTINGS -> settings(list);
   default -> {}
  }
  return List.copyOf(list);
 }
 private static void configs(List<ModuleStructure> out){
  var c=ConfigManager.INSTANCE;
  out.add(form("config:create","Создать / импортировать",ModuleCategory.CONFIGS,()->new Setting[]{configName,
   button("Сохранить новый",()->{c.saveConfig(safeName(configName));done("Конфиг сохранён");}),
   configCode,button("Импортировать код",()->{if(!c.importFromCode(configCode.getText()))throw new IllegalArgumentException("Некорректный код");save();done("Конфиг импортирован");}),
   button("Вставить из буфера",()->configCode.setText(MinecraftClient.getInstance().keyboard.getClipboard())),label(()->status)}));
  for(String name:c.listConfigs())out.add(form("config:"+name,name,ModuleCategory.CONFIGS,()->new Setting[]{
   label(()->"Активный: "+c.getActiveConfigName()),button("Загрузить",()->{c.loadConfig(name);c.setActiveConfigName(name);done("Загружен: "+name);}),
   button("Перезаписать",()->{c.saveConfig(name);done("Сохранён: "+name);}),
   button("Копировать код",()->MinecraftClient.getInstance().keyboard.setClipboard(c.exportConfigToCode(name))),
   button("Удалить",()->{if(name.equals("default"))throw new IllegalArgumentException("Основной конфиг нельзя удалить");c.deleteConfig(name);forms.remove("config:"+name);done("Конфиг удалён");})}));
 }
 private static Markers markers(){return ModuleManager.INSTANCE.getModule(Markers.class);}
 private static void markers(List<ModuleStructure> out){
  out.add(form("marker:create","Новая метка",ModuleCategory.MARKS,()->new Setting[]{markerName,markerX,markerY,markerZ,markerIcon,markerBg,markerText,markerColor,markerSize,
   button("Моя позиция",()->{var p=MinecraftClient.getInstance().player;if(p==null)throw new IllegalArgumentException("Сначала зайди в мир");markerX.setText(""+p.getBlockX());markerY.setText(""+p.getBlockY());markerZ.setText(""+p.getBlockZ());}),
   button("Добавить метку",()->{markers().addMarker(new Markers$MarkerData(safeName(markerName),coordinate(markerX),coordinate(markerY),coordinate(markerZ),markerIcon.getSelected().charAt(0),markerBg.getColor(),markerText.getColor(),markerColor.getColor(),255,markerSize.getValue()));save();done("Метка добавлена");}),label(()->status)}));
  for(var m:List.copyOf(markers().getMarkers())){
   String key="marker:"+System.identityHashCode(m);
   out.add(form(key,m.name,ModuleCategory.MARKS,()->{
    TextSetting name=text("Название",m.name),x=text("X",""+m.x),y=text("Y",""+m.y),z=text("Z",""+m.z);
    var bg=new ColorSetting("Фон","").value(m.backgroundColor);var fg=new ColorSetting("Текст","").value(m.textColor);var icon=new ColorSetting("Цвет иконки","").value(m.iconColor);var ib=new ColorSetting("Фон иконки","").value(m.iconBackgroundColor);
    var ic=new SelectSetting("Иконка","").value("A","m","o","q","r","t","[","]","<","^","=").selected(""+m.iconChar);
    var size=new SliderSettings("Размер","").range(.5f,2.5f).setValue(m.size);
    return new Setting[]{name,x,y,z,ic,bg,fg,icon,ib,size,
     button("Применить",()->{int i=markers().getMarkers().indexOf(m);if(i>=0){markers().getMarkers().set(i,new Markers$MarkerData(safeName(name),coordinate(x),coordinate(y),coordinate(z),ic.getSelected().charAt(0),bg.getColor(),fg.getColor(),icon.getColor(),255,size.getValue(),ib.getColor()));forms.remove(key);save();done("Метка изменена");}}),
     button("GPS к метке",()->{var hud=ModuleManager.INSTANCE.getModule(com.example.auravisuals.module.render.Hud.class);hud.setGpsTarget(m.x,m.z);done("GPS установлен");}),
     button("Удалить",()->{markers().getMarkers().remove(m);forms.remove(key);save();done("Метка удалена");})};
   }));
  }
 }
 private static void cosmetics(List<ModuleStructure> out){
  var c=CosmeticsManager.INSTANCE;c.ensureLoaded();
  out.add(form("cosmetic:filter","Каталог косметики",ModuleCategory.COSMETICS,()->new Setting[]{cosmeticFilter,
   button("Показать категорию",NativeSections::invalidate),button("Снять всю косметику",()->{for(var item:new ArrayList<>(c.getEquippedMap().values()))c.toggleEquip(item);save();done("Косметика снята");}),label(()->"Всего: "+c.getAllCosmetics().size())}));
  for(var item:c.getByCategory(cosmeticFilter.getSelected())){
   var module=forms.computeIfAbsent("cosmetic:"+item.getId(),key->{
    var f=new ModuleStructure(item.getName(),item.getCategory(),ModuleCategory.COSMETICS){
     @Override public boolean isState(){return c.isEquipped(item);}
     @Override public void setState(boolean value){if(value!=c.isEquipped(item)){c.toggleEquip(item);save();}}
    };
    f.settings(new PreviewSetting(item),label(()->c.isEquipped(item)?"Надето":"Не надето"),button("Надеть / снять",()->{c.toggleEquip(item);save();}));return f;
   });out.add(module);
  }
 }
 private static void themes(List<ModuleStructure> out){
  var tm=ThemeManager.INSTANCE;
  out.add(form("theme:create","Своя тема",ModuleCategory.THEMES,()->new Setting[]{themeName,button("Создать из текущей",()->{var theme=tm.createTheme(safeName(themeName),tm.getCurrentTheme(),true);tm.setTheme(theme);tm.saveCustomTheme(theme);done("Тема создана");}),label(()->status)}));
  for(var theme:tm.getThemes())out.add(form("theme:"+theme.getName(),theme.getName(),ModuleCategory.THEMES,()->{
   List<Setting> controls=new ArrayList<>();controls.add(button("Применить тему",()->{tm.setTheme(theme);save();}));
   if(theme.isMutable()){
    for(var getter:Theme.class.getMethods()){
     if(!getter.getName().startsWith("get")||!getter.getName().endsWith("Color")||getter.getParameterCount()!=0||getter.getReturnType()!=int.class)continue;
     try{var setter=Theme.class.getMethod("set"+getter.getName().substring(3),int.class);
      controls.add(color(getter.getName().substring(3),()->{try{return (int)getter.invoke(theme);}catch(Exception e){throw new IllegalStateException(e);}},v->{try{setter.invoke(theme,v);tm.saveCustomTheme(theme);}catch(Exception e){throw new IllegalStateException(e);}}));
     }catch(NoSuchMethodException ignored){}
    }
    controls.add(button("Удалить тему",()->{tm.removeTheme(theme);forms.remove("theme:"+theme.getName());done("Тема удалена");}));
   }
   return controls.toArray(Setting[]::new);
  }));
 }
 private static void hud(List<ModuleStructure> out){
  out.add(form("hud:all","Все элементы HUD",ModuleCategory.HUD,()->{
   var module=ModuleManager.INSTANCE.getModule(com.example.auravisuals.module.render.Hud.class);com.example.auravisuals.module.render.Hud.presetMode.setModes(com.example.auravisuals.config.HudPresetManager.modes());List<Setting> controls=new ArrayList<>();
   for(var setting:module.getSettings())controls.addAll(FrontendBridge.adaptAll(setting,module));return controls.toArray(Setting[]::new);
  }));
  Map<String,com.example.auravisuals.setting.BooleanSetting> items=new LinkedHashMap<>();
  items.put("Инвентарь",com.example.auravisuals.module.render.Hud.inventory);items.put("Хотбар",com.example.auravisuals.module.render.Hud.customHotbar);items.put("Броня",com.example.auravisuals.module.render.Hud.armorHud);items.put("Ватермарка",com.example.auravisuals.module.render.Hud.watermark);items.put("Таргет",com.example.auravisuals.module.render.Hud.target);items.put("Уведомления",com.example.auravisuals.module.render.Hud.notifications);items.put("Хоткеи",com.example.auravisuals.module.render.Hud.hotkeys);items.put("Зелья",com.example.auravisuals.module.render.Hud.potions);items.put("Кулдауны",com.example.auravisuals.module.render.Hud.cooldowns);items.put("Медиаплеер",com.example.auravisuals.module.render.Hud.mediaPlayer);
  items.forEach((name,value)->out.add(forms.computeIfAbsent("hud:"+name,key->{var f=new ModuleStructure(name,"Показать / скрыть элемент",ModuleCategory.HUD){@Override public boolean isState(){return value.getValue();}@Override public void setState(boolean enabled){value.setValue(enabled);save();}};f.settings(bool("Показывать",value::getValue,v->{value.setValue(v);save();}));
    String id=switch(name){case "Инвентарь"->"inventory";case "Хотбар"->"customHotbar";case "Броня"->"armorHud";case "Ватермарка"->"watermark";case "Таргет"->"target";case "Уведомления"->"notifications";case "Хоткеи"->"hotkeys";case "Зелья"->"potions";case "Кулдауны"->"cooldowns";default->"mediaPlayer";};
    f.settings(HudStyleControls.controls(id).toArray(Setting[]::new));return f;})));
 }
 private static void events(List<ModuleStructure> out,ModuleCategory category){
  out.add(form("events:controls:"+category,"Сервер и обновление",category,()->{
   var apiToken=text("Свой ключ API FunTime",System.getProperty("auravisuals.funtimeApiToken",""));
   return new Setting[]{eventServer,apiToken,button("Применить ключ",()->{System.setProperty("auravisuals.funtimeApiToken",apiToken.getText().trim());done("Ключ применён до перезапуска игры");}),
   button("Обновить",()->{if(eventServer.isSelected("FunTime"))FunTimeEventsClient.refreshAsync(NativeSections::invalidate);else HolyWorldEventsClient.refreshAsync(NativeSections::invalidate);}),
   button("Показать выбранный сервер",NativeSections::invalidate),label(()->eventServer.isSelected("FunTime")?FunTimeEventsClient.getLastError():HolyWorldEventsClient.getLastError())};}));
  if(category==ModuleCategory.MINES){
   for(var mine:FunTimeEventsClient.getMines())out.add(form("mine:"+mine.id+mine.server,mine.name+" "+mine.getDisplayServer(),category,()->new Setting[]{label(mine::getDisplayTime),label(mine::getDisplayPhaseLabel)}));
  }else if(eventServer.isSelected("FunTime")){
   for(var e:FunTimeEventsClient.getEvents())out.add(form("event:"+e.id+e.server,e.name+" "+e.getDisplayServer(),category,()->new Setting[]{label(e::getDisplayTime),label(e::getCoordinatesDisplay),label(e::getDisplayPhaseLabel),button("GPS",()->{if(e.x==null||e.z==null)throw new IllegalArgumentException("Координаты пока не объявлены");ModuleManager.INSTANCE.getModule(com.example.auravisuals.module.render.Hud.class).setGpsTarget(e.x,e.z);})}));
  }else for(var e:HolyWorldEventsClient.getEvents())out.add(form("hw:"+e.id()+e.serverId(),e.name()+" "+e.serverName(),category,()->new Setting[]{new LabelSetting(e.rarity())}));
 }
 private static void messenger(List<ModuleStructure> out){
  var chat=ChatHubClient.INSTANCE;var f=FriendHubClient.INSTANCE;
  out.add(form("chat:profile","Профиль",ModuleCategory.MESSENGER,()->{
   var username=text("Имя",com.example.auravisuals.util.MessengerPreferences.getCustomUsername());
   var avatar=text("URL аватара",com.example.auravisuals.util.MessengerPreferences.getCustomAvatarUrl());
   return new Setting[]{username,avatar,button("Сохранить профиль",()->{com.example.auravisuals.util.MessengerPreferences.setCustomUsername(username.getText());com.example.auravisuals.util.MessengerPreferences.setCustomAvatarUrl(avatar.getText());chat.refreshIdentity();f.refreshIdentity();done("Профиль сохранён");})};
  }));
  out.add(form("chat:compose","Сообщения",ModuleCategory.MESSENGER,()->new Setting[]{channel,recipient,message,
   button("Отправить",()->{String body=message.getText().trim();if(body.isEmpty())return;if(channel.isSelected("Общий")){if(!chat.sendMessage(body))throw new IllegalArgumentException("Нет соединения с чатом");}else if(channel.isSelected("ЛС"))f.sendDm(recipient.getText().trim(),body);else f.sendGroupMessage(recipient.getText().trim(),body);message.setText("");invalidate();}),
   button("Обновить историю",()->{if(channel.isSelected("ЛС"))f.requestDmHistory(recipient.getText().trim());invalidate();}),
   button("Подключиться",()->{chat.connect(ChatHubClient.DEFAULT_URL);f.connect(FriendHubClient.DEFAULT_URL);}),
   label(()->"Чат: "+chat.getState()+" / Друзья: "+f.getState()),label(()->status)}));
  out.add(form("chat:friends","Друзья и группы",ModuleCategory.MESSENGER,()->new Setting[]{recipient,
   button("Добавить друга",()->f.sendFriendRequest(recipient.getText().trim())),groupName,button("Создать группу",()->f.createGroup(safeName(groupName))),
   bool("Скрывать мой статус",()->FriendHubClient.hideMyStatusFromFriends,v->FriendHubClient.hideMyStatusFromFriends=v),
   bool("Скрывать сервер",()->FriendHubClient.hideGameServerFromFriends,v->FriendHubClient.hideGameServerFromFriends=v),
   bool("Показывать музыку",()->FriendHubClient.showMusicPresence,v->FriendHubClient.showMusicPresence=v)}));
  for(var friend:f.getFriends())out.add(form("friend:"+friend.login,friend.login,ModuleCategory.MESSENGER,()->new Setting[]{
   button("Личные сообщения",()->{recipient.setText(friend.login);channel.setSelected("ЛС");f.requestDmHistory(friend.login);invalidate();}),button("Удалить друга",()->{f.removeFriend(friend.login);invalidate();})}));
  for(var request:f.getIncomingRequests())out.add(form("request:"+request.login,"Заявка: "+request.login,ModuleCategory.MESSENGER,()->new Setting[]{button("Принять",()->{f.acceptFriendRequest(request.login);invalidate();}),button("Отклонить",()->{f.declineFriendRequest(request.login);invalidate();})}));
  for(var group:f.getGroups())out.add(form("group:"+group.id,group.name,ModuleCategory.MESSENGER,()->new Setting[]{
   label(()->"ID: "+group.id),label(()->String.join(", ",group.members)),recipient,
   text("Владелец",group.owner),
   button("Чат группы",()->{recipient.setText(group.id);channel.setSelected("Группа");invalidate();}),
   button("Пригласить игрока",()->f.inviteToGroup(group.id,recipient.getText().trim())),
   button("Исключить игрока",()->f.kickGroupMember(group.id,recipient.getText().trim())),
   button("Покинуть группу",()->{f.leaveGroup(group.id);invalidate();}),
   button("Удалить группу",()->{f.deleteGroup(group.id);invalidate();})}));
  for(var invite:f.getIncomingGroupInvites())out.add(form("invite:"+invite.groupId,"Приглашение: "+invite.groupName,ModuleCategory.MESSENGER,()->new Setting[]{button("Вступить",()->{f.acceptGroupInvite(invite.groupId,invite.from);invalidate();}),button("Отклонить",()->{f.declineGroupInvite(invite.groupId,invite.from);invalidate();})}));
  if(channel.isSelected("Общий"))for(var msg:chat.getMessages())out.add(form("msg:"+msg.hashCode(),msg.login()+": "+msg.message(),ModuleCategory.MESSENGER,()->new Setting[]{new LabelSetting(msg.time()),new LabelSetting(msg.message())}));
  else if(channel.isSelected("ЛС"))for(var msg:f.getDmMessages(recipient.getText()))out.add(form("dm:"+msg.hashCode(),msg.from+": "+msg.message,ModuleCategory.MESSENGER,()->new Setting[]{new LabelSetting(msg.time),new LabelSetting(msg.message)}));
  else for(var group:f.getGroups())if(group.id.equals(recipient.getText()))for(var msg:group.messages)out.add(form("gm:"+msg.hashCode(),msg.from+": "+msg.message,ModuleCategory.MESSENGER,()->new Setting[]{new LabelSetting(msg.time),new LabelSetting(msg.message)}));
 }
 private static void settings(List<ModuleStructure> out){
  out.add(form("settings:gui","Click GUI",ModuleCategory.SETTINGS,()->{
   var m=ModuleManager.INSTANCE.getModule(com.example.auravisuals.module.render.ClickGui.class);List<Setting> controls=new ArrayList<>();for(var s:m.getSettings())controls.addAll(FrontendBridge.adaptAll(s,m));controls.add(button("Сохранить настройки",NativeSections::save));return controls.toArray(Setting[]::new);
  }));
 }
}
