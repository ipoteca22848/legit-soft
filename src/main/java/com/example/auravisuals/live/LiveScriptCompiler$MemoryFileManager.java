package com.example.auravisuals.live;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject.Kind;

public class LiveScriptCompiler$MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
    final Map<String, LiveScriptCompiler$MemoryByteCodeFile> bytecodeMap = new HashMap<>();

    public LiveScriptCompiler$MemoryFileManager(JavaFileManager fileManager) {
        super(fileManager);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) {
        LiveScriptCompiler$MemoryByteCodeFile byteCodeFile = new LiveScriptCompiler$MemoryByteCodeFile(className);
        this.bytecodeMap.put(className, byteCodeFile);
        return byteCodeFile;
    }

    public Map<String, byte[]> getBytecodeMap() {
        Map<String, byte[]> result = new HashMap<>();

        for (Entry<String, LiveScriptCompiler$MemoryByteCodeFile> entry : this.bytecodeMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getBytes());
        }

        return result;
    }
}
