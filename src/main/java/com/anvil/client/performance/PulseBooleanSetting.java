package com.anvil.client.performance;
public final class PulseBooleanSetting extends com.example.auravisuals.setting.BooleanSetting {
 public PulseBooleanSetting(String n,String d,boolean v){super(n,v);} public boolean a(){return getValue();}
 public PulseBooleanSetting a(java.util.function.Supplier<Boolean> v){setDependency(v);return this;}
}
