package com.example.auravisuals.live;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaCompiler.CompilationTask;

public class LiveScriptCompiler {
    public static LiveScriptCompiler$CompileResult compileSource(String fullClassName, String sourceCode) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new LiveScriptCompiler$CompileResult(
                false, null, List.of("System JavaCompiler not found. Make sure you are running Minecraft with a JDK (Java Development Kit).")
            );
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        LiveScriptCompiler$MemoryFileManager fileManager = new LiveScriptCompiler$MemoryFileManager(
            compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)
        );
        LiveScriptCompiler$JavaSourceFromString sourceFile = new LiveScriptCompiler$JavaSourceFromString(fullClassName, sourceCode);
        List<JavaFileObject> compilationUnits = List.of(sourceFile);
        List<String> options = new ArrayList<>();
        String classpath = buildClasspath();
        if (!classpath.isEmpty()) {
            options.add("-classpath");
            options.add(classpath);
        }

        options.add("-proc:none");
        CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);
        boolean success = Boolean.TRUE.equals(task.call());
        List<String> errors = new ArrayList<>();

        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() == Kind.ERROR) {
                errors.add(String.format("Line %d: %s", diagnostic.getLineNumber(), diagnostic.getMessage(Locale.ENGLISH)));
            }
        }

        if (success && !fileManager.getBytecodeMap().isEmpty()) {
            try {
                LiveScriptCompiler$LiveClassLoader classLoader = new LiveScriptCompiler$LiveClassLoader(
                    LiveScriptCompiler.class.getClassLoader(), fileManager.getBytecodeMap()
                );
                Class<?> loadedClass = classLoader.loadClass(fullClassName);
                return new LiveScriptCompiler$CompileResult(true, loadedClass, Collections.emptyList());
            } catch (Throwable var14) {
                errors.add("Class loading error: " + var14.getMessage());
                return new LiveScriptCompiler$CompileResult(false, null, errors);
            }
        } else {
            return new LiveScriptCompiler$CompileResult(false, null, errors.isEmpty() ? List.of("Unknown compilation failure.") : errors);
        }
    }

    static String buildClasspath() {
        Set<String> paths = new LinkedHashSet<>();
        String systemCp = System.getProperty("java.class.path");
        if (systemCp != null && !systemCp.isEmpty()) {
            for (String entry : systemCp.split(File.pathSeparator)) {
                if (new File(entry).exists()) {
                    paths.add(entry);
                }
            }
        }

        for (ClassLoader cl = LiveScriptCompiler.class.getClassLoader(); cl != null; cl = cl.getParent()) {
            if (cl instanceof URLClassLoader urlCl) {
                for (URL url : urlCl.getURLs()) {
                    try {
                        File file = new File(url.toURI());
                        if (file.exists()) {
                            paths.add(file.getAbsolutePath());
                        }
                    } catch (Throwable var9) {
                    }
                }
            }
        }

        return String.join(File.pathSeparator, paths);
    }
}
