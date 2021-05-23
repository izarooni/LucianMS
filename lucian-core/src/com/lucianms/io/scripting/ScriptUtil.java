package com.lucianms.io.scripting;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import tools.Pair;

import javax.script.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;

public final class ScriptUtil {

    private static final NashornScriptEngineFactory FACTORY = new NashornScriptEngineFactory();

    private ScriptUtil() {
    }

    private static NashornScriptEngine getEngine(Collection<Pair<String, Object>> binds) {
        NashornScriptEngine engine = (NashornScriptEngine) FACTORY.getScriptEngine(
                "--language=es6",
                "--no-deprecation-warning",
                "-scripting");
        if (binds != null) {
            Bindings b = engine.createBindings();
            for (Pair<String, Object> pair : binds) {
                b.put(pair.left, pair.right);
            }
            engine.setBindings(b, ScriptContext.ENGINE_SCOPE);
        }
        return engine;
    }

    public static CompiledScript compile(String path, Collection<Pair<String, Object>> binds) throws IOException, ScriptException {
        path = "scripts/" + path;
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException("No script found for path " + path);
        }
        try (FileReader reader = new FileReader(file)) {
            NashornScriptEngine engine = getEngine(binds);
            CompiledScript compile = engine.compile(reader);
            compile.eval();
            return compile;
        }
    }

    public static Invocable eval(String path, Collection<Pair<String, Object>> binds) throws IOException, ScriptException {
        path = "scripts/" + path;
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException("No script found for path " + path);
        }
        try (FileReader reader = new FileReader(file)) {
            NashornScriptEngine engine = getEngine(binds);
            engine.eval(reader);
            return engine;
        }
    }
}
