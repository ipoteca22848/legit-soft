package com.example.auravisuals.live;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import javax.tools.SimpleJavaFileObject;
import javax.tools.JavaFileObject.Kind;

public class LiveScriptCompiler$MemoryByteCodeFile extends SimpleJavaFileObject {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    public LiveScriptCompiler$MemoryByteCodeFile(String name) {
        super(URI.create("byte:///" + name.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
    }

    @Override
    public OutputStream openOutputStream() {
        return this.outputStream;
    }

    public byte[] getBytes() {
        return this.outputStream.toByteArray();
    }
}
