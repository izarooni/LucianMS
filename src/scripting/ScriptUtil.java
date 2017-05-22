package scripting;

import client.MapleClient;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import tools.Pair;

import javax.script.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;

public final class ScriptUtil {

    public static Invocable eval(MapleClient client, String path, Collection<Pair<String, Object>> binds) {
        path = "scripts/" + path;
        File file = new File(path);
        if (!file.exists()) {
            throw new NullPointerException("No script found for path " + path);
        }
        ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine();
        try (FileReader reader = new FileReader(file)) {
            if (binds != null) {
                Bindings b = engine.createBindings();
                for (Pair<String, Object> pair : binds) {
                    b.put(pair.left, pair.right);
                }
                engine.setBindings(b, ScriptContext.ENGINE_SCOPE);
            }
            engine.eval("load(\"nashorn:mozilla_compat.js\");");
            engine.eval(reader);
            if (client != null) {
                client.setEngine(path, engine);
            }
            return (Invocable) engine;
        } catch (ScriptException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void removeScript(MapleClient client, String path) {
        client.removeEngine("scripts/ " + path);
    }
}
