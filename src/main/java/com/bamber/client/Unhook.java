package com.bamber.client;
public final class Unhook extends com.example.auravisuals.module.Module {
 public Unhook(){super("Unhook","Отключить клиент до перезапуска игры",com.example.auravisuals.module.Category.MISC);}
 @Override public void onEnable(){setEnabledSilently(false);BamberRuntime.unhook();}
}
