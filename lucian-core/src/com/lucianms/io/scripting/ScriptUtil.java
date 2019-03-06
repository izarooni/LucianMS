package com.lucianms.io.scripting;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import tools.Pair;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;

public final class ScriptUtil {

    public static Invocable eval(String path, Collection<Pair<String, Object>> binds) throws IOException, ScriptException {
        path = "scripts/" + path;
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException("No script found for path " + path);
        }
        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        NashornScriptEngine engine = (NashornScriptEngine) factory.getScriptEngine();
        try (FileReader reader = new FileReader(file)) {
            if (binds != null) {
                Bindings b = engine.createBindings();
                for (Pair<String, Object> pair : binds) {
                    b.put(pair.left, pair.right);
                }
                engine.setBindings(b, ScriptContext.ENGINE_SCOPE);
            }
            engine.eval(reader);
            return engine;
        }
    }
}
