package com.example.auravisuals.util.integration;

import com.google.gson.*;
import java.io.*;
import java.net.UnixDomainSocketAddress;
import java.nio.*;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

/** Desktop Discord IPC only. No bot token or external service is used. */
public final class DiscordRPC {
 private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
 private static volatile boolean running;
 private static volatile RandomAccessFile pipe;
 private static volatile SocketChannel socket;
 private static ScheduledExecutorService executor;
 private static DiscordRPC$Config config;
 private static String lastActivity="";
 private static long startEpoch;
 private static volatile String status="Выключено";
 private static Path configPath(){return FabricLoader.getInstance().getConfigDir().resolve("anvil_visual_discord.json");}
 private static synchronized DiscordRPC$Config config(){
  if(config==null){
   try{config=GSON.fromJson(Files.readString(configPath()),DiscordRPC$Config.class);}catch(Exception ignored){}
   if(config==null)config=new DiscordRPC$Config();
   if(config.clientId==null||config.clientId.isBlank())config.clientId=new DiscordRPC$Config().clientId;
  }
  return config;
 }
 public static String applicationId(){return config().clientId;}
 public static String status(){return status;}
 public static synchronized void configure(String id){
  id=id.trim();if(!id.matches("[0-9]{17,20}"))throw new IllegalArgumentException("Application ID: 17–20 цифр");
  boolean restart=running;shutdown();config().clientId=id;
  try{Files.createDirectories(configPath().getParent());Files.writeString(configPath(),GSON.toJson(config));}
  catch(IOException e){throw new IllegalStateException("Не удалось сохранить Discord Application ID",e);}
  if(restart)start();
 }
 public static synchronized void start(){
  if(running)return;
  if(!applicationId().matches("[0-9]{17,20}")){status="Укажите Discord Application ID";return;}
  running=true;startEpoch=System.currentTimeMillis()/1000;status="Ожидание Discord";
  executor=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"anvil-visual-DiscordRPC");t.setDaemon(true);return t;});
  executor.scheduleWithFixedDelay(DiscordRPC::tick,0,5,TimeUnit.SECONDS);
 }
 public static synchronized void shutdown(){
  running=false;
  if(executor!=null){executor.shutdownNow();executor=null;}
  // Closing the IPC session clears its activity and interrupts pending I/O.
  close();status="Выключено";
 }
 private static void close(){
  var p=pipe;pipe=null;var s=socket;socket=null;lastActivity="";
  try{if(p!=null)p.close();}catch(IOException ignored){}
  try{if(s!=null)s.close();}catch(IOException ignored){}
 }
 private static void tick(){
  try{
   if(!running)return;
   if(pipe==null&&socket==null)connect();
   if(!running){close();return;}
   String activity=activity().toString();
   if(!activity.equals(lastActivity)){
    JsonObject args=new JsonObject();args.addProperty("pid",ProcessHandle.current().pid());args.add("activity",JsonParser.parseString(activity));
    JsonObject command=new JsonObject();command.addProperty("cmd","SET_ACTIVITY");command.addProperty("nonce",UUID.randomUUID().toString());command.add("args",args);
    write(1,command.toString());var reply=read();
    if(reply.has("evt")&&!reply.get("evt").isJsonNull()&&"ERROR".equals(reply.get("evt").getAsString()))throw new IOException("Discord rejected activity");
    lastActivity=activity;status="Подключено";
   }
  }catch(Exception e){close();if(running)status="Discord недоступен; повтор через 5 с";}
 }
 private static void connect() throws IOException {
  boolean windows=System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win");
  LinkedHashSet<String> dirs=new LinkedHashSet<>();
  for(String key:List.of("XDG_RUNTIME_DIR","TMPDIR","TMP","TEMP")){String v=System.getenv(key);if(v!=null&&!v.isBlank())dirs.add(v);}
  dirs.add("/tmp");
  for(int i=0;i<10&&running;i++){
   List<String> candidates=new ArrayList<>();
   if(windows)candidates.add("\\\\.\\pipe\\discord-ipc-"+i);
   else for(String dir:dirs){candidates.add(dir+"/discord-ipc-"+i);candidates.add(dir+"/app/com.discordapp.Discord/discord-ipc-"+i);candidates.add(dir+"/snap.discord/discord-ipc-"+i);}
   for(String path:candidates){
    try{
     if(windows)pipe=new RandomAccessFile(path,"rw");
     else{if(!Files.exists(Path.of(path)))continue;var s=SocketChannel.open(java.net.StandardProtocolFamily.UNIX);socket=s;s.connect(UnixDomainSocketAddress.of(path));s.configureBlocking(false);}
     if(!running){close();return;}
     JsonObject handshake=new JsonObject();handshake.addProperty("v",1);handshake.addProperty("client_id",applicationId());write(0,handshake.toString());
     var ready=read();if(!ready.has("evt")||!"READY".equals(ready.get("evt").getAsString()))throw new IOException("Invalid Discord handshake");
     return;
    }catch(Exception e){close();}
   }
  }
  throw new IOException("Discord desktop IPC not found");
 }
 private static JsonObject activity(){
  var a=new JsonObject();a.addProperty("details","anvil visual · Minecraft 1.21.11");
  var mc=MinecraftClient.getInstance();String state=mc==null||mc.world==null?"В главном меню":mc.isInSingleplayer()?"Одиночная игра":"На сервере";
  a.addProperty("state",state);var timestamps=new JsonObject();timestamps.addProperty("start",startEpoch);a.add("timestamps",timestamps);return a;
 }
 private static void write(int opcode,String json)throws IOException{
  byte[] body=json.getBytes(StandardCharsets.UTF_8);var frame=ByteBuffer.allocate(body.length+8).order(ByteOrder.LITTLE_ENDIAN).putInt(opcode).putInt(body.length).put(body);frame.flip();
  var p=pipe;if(p!=null){p.write(frame.array());return;}
  var s=socket;if(s==null)throw new EOFException();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
  while(frame.hasRemaining()){if(s.write(frame)==0)pause(deadline);}
 }
 private static void readFully(ByteBuffer b)throws IOException{
  var p=pipe;if(p!=null){p.readFully(b.array());b.position(b.limit());return;}
  var s=socket;if(s==null)throw new EOFException();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
  while(b.hasRemaining()){int n=s.read(b);if(n<0)throw new EOFException();if(n==0)pause(deadline);}
 }
 private static void pause(long deadline)throws IOException{
  if(!running||Thread.currentThread().isInterrupted()||System.nanoTime()>deadline)throw new IOException("Discord IPC timeout");
  try{Thread.sleep(5);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}
 }
 private static JsonObject read()throws IOException{
  for(int count=0;count<8;count++){
   var header=ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);readFully(header);header.flip();int opcode=header.getInt(),length=header.getInt();
   if(length<0||length>1048576)throw new IOException("Invalid Discord frame length");
   var body=ByteBuffer.allocate(length);readFully(body);String json=new String(body.array(),StandardCharsets.UTF_8);
   if(opcode==3){write(4,json);continue;}
   if(opcode==2)throw new EOFException("Discord closed IPC");
   return JsonParser.parseString(json).getAsJsonObject();
  }
  throw new IOException("Too many Discord control frames");
 }
 private DiscordRPC(){}
}
