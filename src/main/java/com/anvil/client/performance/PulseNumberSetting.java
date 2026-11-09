package com.anvil.client.performance;
public final class PulseNumberSetting extends com.example.auravisuals.setting.NumberSetting {
 public PulseNumberSetting(String n,double v,double min,double max,double step){super(n,v,min,max,step);}
 public PulseNumberSetting(String n,String d,double v,double min,double max,double step){super(n,v,min,max,step);}
 public float b(){return getFloatValue();} public int c(){return getValue().intValue();}
 public PulseNumberSetting a(java.util.function.Supplier<Boolean> v){setDependency(v);return this;}
}
