package com.lucianms.io.scripting.map;

import com.lucianms.client.MapleClient;
import com.lucianms.io.scripting.ScriptProcessor;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;

/**
 * @author izarooni
 */
public class FieldScriptExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldScriptExecutor.class);
    private static final ScriptProcessor SCRIPT_PROCESSOR = new ScriptProcessor();
    private static final Hashtable<String, CompiledScript> COMPILED_SCRIPTS = new Hashtable<>();

    private FieldScriptExecutor() {
    }

    public static void clearCache() {
        COMPILED_SCRIPTS.clear();
    }

    private static CompiledScript getCompiledScript(String script) throws IOException, ScriptException {
        NashornScriptEngine engine = SCRIPT_PROCESSOR.newEngine();
        try (FileReader reader = new FileReader(script)) {
            return SCRIPT_PROCESSOR.compile(engine, reader, null);
        }
    }

    public static boolean executeFirstEnter(MapleClient client, String script) {
        String path = String.format("scripts/map/onFirstUserEnter/%s.js", script);
        CompiledScript compile = COMPILED_SCRIPTS.get(path);
        try {
            if (compile == null) {
                compile = getCompiledScript(path);
                COMPILED_SCRIPTS.put(path, compile);
            }
            ((Invocable) compile.getEngine()).invokeFunction("start", new MapScriptMethods(client));
            return true;
        } catch (NoSuchMethodException | FileNotFoundException ignore) {
        } catch (ScriptException | IOException e) {
            LOGGER.error("Failed to process script '{}'", path, e);
        }
        return false;
    }

    public static boolean executeEnter(MapleClient client, String script) {
        String path = String.format("scripts/map/onUserEnter/%s.js", script);
        CompiledScript compile = COMPILED_SCRIPTS.get(path);
        try {
            if (compile == null) {
                compile = getCompiledScript(path);
                COMPILED_SCRIPTS.put(path, compile);
            }
            ((Invocable) compile.getEngine()).invokeFunction("start", new MapScriptMethods(client));
            return true;
        } catch (NoSuchMethodException | FileNotFoundException ignore) {
        } catch (ScriptException | IOException e) {
            LOGGER.error("Failed to process script '{}'", path, e);
        }
        return false;
    }
}