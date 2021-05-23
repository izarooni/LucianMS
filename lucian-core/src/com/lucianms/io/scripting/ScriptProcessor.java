package com.lucianms.io.scripting;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import tools.Functions;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import java.io.FileReader;

/**
 * @author izarooni
 */
public class ScriptProcessor {

    private NashornScriptEngineFactory factory;

    public ScriptProcessor() {
        factory = new NashornScriptEngineFactory();
    }

    public NashornScriptEngine newEngine() {
        return (NashornScriptEngine) factory.getScriptEngine("--language=es6", "--no-deprecation-warning", "-scripting");
    }

    public CompiledScript compile(NashornScriptEngine engine, FileReader reader, Bindings bindings) throws ScriptException {
        Functions.requireNotNull(bindings, b -> engine.setBindings(b, ScriptContext.ENGINE_SCOPE));
        CompiledScript compile = engine.compile(reader);
        compile.eval();
        return compile;
    }
}
