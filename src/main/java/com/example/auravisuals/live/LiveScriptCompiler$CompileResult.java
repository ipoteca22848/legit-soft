package com.example.auravisuals.live;

import java.util.Collections;
import java.util.List;

public class LiveScriptCompiler$CompileResult {
    final boolean success;
    final Class<?> compiledClass;
    final List<String> errors;

    public LiveScriptCompiler$CompileResult(boolean success, Class<?> compiledClass, List<String> errors) {
        this.success = success;
        this.compiledClass = compiledClass;
        this.errors = errors != null ? errors : Collections.emptyList();
    }

    public boolean isSuccess() {
        return this.success;
    }

    public Class<?> getCompiledClass() {
        return this.compiledClass;
    }

    public List<String> getErrors() {
        return this.errors;
    }
}
