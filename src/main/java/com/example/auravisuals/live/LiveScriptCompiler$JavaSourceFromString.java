package com.example.auravisuals.live;

import java.net.URI;
import javax.tools.SimpleJavaFileObject;
import javax.tools.JavaFileObject.Kind;

public class LiveScriptCompiler$JavaSourceFromString extends SimpleJavaFileObject {
    final String code;

    public LiveScriptCompiler$JavaSourceFromString(String name, String code) {
        super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
        this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return this.code;
    }
}
