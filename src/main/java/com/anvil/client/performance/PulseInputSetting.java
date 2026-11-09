package com.anvil.client.performance;
public final class PulseInputSetting extends com.example.auravisuals.setting.StringSetting {
 public PulseInputSetting(String n,String v){super(n,v);} public boolean c(){return getValue().isEmpty();}
 public String i(){return getValue();} public void b(String v){setValue(v);}
 public PulseInputSetting c(java.util.function.Supplier<Boolean> v){setDependency(v);return this;}
}
