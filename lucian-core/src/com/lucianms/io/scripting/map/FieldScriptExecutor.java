package com.lucianms.io.scripting.map;

import com.lucianms.client.MapleClient;
import com.lucianms.io.scripting.ScriptProcessor;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author izarooni
 */
public class FieldScriptExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldScriptExecutor.class);
    private static final ScriptProcessor SCRIPT_PROCESSOR = new ScriptProcessor();
    private static final HashMap<String, CompiledScript> COMPILED_SCRIPTS = new HashMap<>();

    private FieldScriptExecutor() {
    }

    public static void clearCache() {
        COMPILED_SCRIPTS.clear();
    }

    private static CompiledScript getCompiledScript(MapleClient client, String script) throws IOException, ScriptException {
        NashornScriptEngine engine = SCRIPT_PROCESSOR.newEngine();
        try (FileReader reader = new FileReader(script)) {
            Bindings bindings = engine.createBindings();
            bindings.put("client", client.getPlayer());
            bindings.put("player", client.getPlayer());
            bindings.put("packet", MaplePacketCreator.class);
            CompiledScript compile = SCRIPT_PROCESSOR.compile(engine, reader, bindings);
            COMPILED_SCRIPTS.put(script, compile);
        }
        throw new RuntimeException(String.format("Failed to compile script '%s'", script));
    }

    public static boolean executeFirstEnter(MapleClient client, String script) {
        String format = String.format("onFirstUserEnter/%s", script);
        CompiledScript compile = COMPILED_SCRIPTS.get(format);
        try {
            if (compile == null) {
                compile = getCompiledScript(client, format);
                COMPILED_SCRIPTS.put(format, compile);
            }
            ((Invocable) compile).invokeFunction("start");
            return true;
        } catch (NoSuchMethodException | FileNotFoundException ignore) {
        } catch (ScriptException | IOException e) {
            LOGGER.error("Failed to process script '{}'", format, e);
        }
        return false;
    }

    public static boolean executeEnter(MapleClient client, String script) {
        String format = String.format("onUserEnter/%s", script);
        CompiledScript compile = COMPILED_SCRIPTS.get(format);
        try {
            if (compile == null) {
                compile = getCompiledScript(client, format);
                COMPILED_SCRIPTS.put(format, compile);
            }
            ((Invocable) compile).invokeFunction("start");
            return true;
        } catch (NoSuchMethodException | FileNotFoundException ignore) {
        } catch (ScriptException | IOException e) {
            LOGGER.error("Failed to process script '{}'", format, e);
        }
        return false;
    }
}