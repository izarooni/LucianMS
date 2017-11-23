package scripting.portal;

import client.MapleClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.ScriptUtil;
import server.MaplePortal;

import javax.script.Invocable;
import javax.script.ScriptException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

/**
 * @author izarooni
 */
public class PortalScriptManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortalScriptManager.class);

    private static HashMap<String, Invocable> scripts = new HashMap<>();

    private PortalScriptManager() {
    }

    private static Invocable getPortalScript(MapleClient client, String script) throws IOException, ScriptException {
        if (scripts.containsKey(script)) {
            return scripts.get(script);
        }
        Invocable iv = ScriptUtil.eval(client, "portal/" + script + ".js", Collections.emptyList());
        if (iv != null) {
            return scripts.put(script, iv);
        }
        return null;
    }

    public static void clearPortalScripts() {
        scripts.clear();
    }

    public static boolean executePortalScript(MapleClient client, MaplePortal portal) {
        try {
            Invocable iv = getPortalScript(client, portal.getScriptName());
            if (iv != null) {
                return (boolean) iv.invokeFunction("start", new PortalPlayerInteraction(client, portal));
            }
        } catch (IOException | ScriptException | NoSuchMethodException e) {
            LOGGER.info("Unable to invoke function 'start' in portal script {}/{} in map", portal.getScriptName(), portal.getId(), client.getPlayer().getMapId(), e);
        }
        return false;
    }
}