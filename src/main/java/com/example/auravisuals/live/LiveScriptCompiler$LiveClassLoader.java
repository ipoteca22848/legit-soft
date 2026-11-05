package com.example.auravisuals.live;

import java.util.Map;

public class LiveScriptCompiler$LiveClassLoader extends ClassLoader {
    final Map<String, byte[]> bytecodes;

    public LiveScriptCompiler$LiveClassLoader(ClassLoader parent, Map<String, byte[]> bytecodes) {
        super(parent);
        this.bytecodes = bytecodes;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = this.bytecodes.get(name);
        return bytes != null ? this.defineClass(name, bytes, 0, bytes.length) : super.findClass(name);
    }
}
