package scripting.npc;

import client.MapleCharacter;
import client.MapleClient;
import scripting.ScriptUtil;
import tools.MaplePacketCreator;
import tools.Pair;

import javax.script.Invocable;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Matze
 * @author izarooni
 */
public class NPCScriptManager {

    private static ConcurrentHashMap<Integer, Pair<Invocable, NPCConversationManager>> storage = new ConcurrentHashMap<>();

    private NPCScriptManager() {
    }

    public static void start(MapleClient client, int npc, MapleCharacter chr) {
        start(client, npc, null, chr);
    }

    public static void start(MapleClient client, int npc, String fileName, MapleCharacter chr) {
        try {
            if (storage.containsKey(client.getAccID())) {
                dispose(client);
            }
            if (npc >= 9901000 && fileName == null) {
                // can't display a player npc without a player
                fileName = Integer.toString(npc);
                npc = 10200;
            }
            if (client.canClickNPC()) {

                NPCConversationManager cm = new NPCConversationManager(client, npc, fileName);
                String path = "npc/world" + client.getWorld() + "/" + (fileName == null ? npc : fileName) + ".js";
                ArrayList<Pair<String, Object>> binds = new ArrayList<>();
                binds.add(new Pair<>("client", client));
                binds.add(new Pair<>("player", client.getPlayer()));
                binds.add(new Pair<>("ch", client.getChannelServer()));
                binds.add(new Pair<>("cm", cm));

                Invocable iv = null;
                try {
                    iv = ScriptUtil.eval(client, path, binds);
                } catch (Exception e) {
                    String response = "An error occurred in this NPC";
                    if (fileName != null) {
                        response += "\r\nName: " + fileName;
                    }
                    response += "\r\nNPC ID: " + npc;
                    client.getPlayer().dropMessage(1, response);

                    System.err.println(String.format("Unable to eval script file '%s' for player %s", path, client.getPlayer().getName()));
                    e.printStackTrace();
                }
                if (iv == null) {
                    if (client.getPlayer().gmLevel() >= 6) {
                        client.getPlayer().dropMessage(6, String.format("Unable to eval script {script:%s, id:%d}", fileName, npc));
                    }
                    dispose(client);
                    return;
                }
                try {
                    try {
                        iv.invokeFunction("start");
                    } catch (NoSuchMethodException e1) {
                        try {
                            iv.invokeFunction("start", chr);
                        } catch (NoSuchMethodException e2) {
                            try {
                                iv.invokeFunction("action", 1, 0, -1);
                            } catch (NoSuchMethodError e3) {
                                e3.printStackTrace();
                                dispose(client);
                                return;
                            }
                        }
                    }
                    client.setClickedNPC();
                } catch (ScriptException e) {
                    String response = "An error occurred in this NPC";
                    if (fileName != null) {
                        response += "\r\nName: " + fileName;
                    }
                    response += "\r\nNPC ID: " + npc;
                    client.getPlayer().dropMessage(1, response);

                    System.err.println("Error invoking function 'action' for NPC script " + (cm.getScriptName() == null ? cm.getNpc() : cm.getScriptName()) + ".js");
                    e.printStackTrace();
                }
                storage.put(client.getAccID(), new Pair<>(iv, cm));
            } else {
                client.announce(MaplePacketCreator.enableActions());
            }
        } catch (Exception e) {
            e.printStackTrace();
            dispose(client);
        }
    }

    public static void action(MapleClient client, byte mode, byte type, int selection) {
        Pair<Invocable, NPCConversationManager> pair = storage.get(client.getAccID());
        if (pair != null) {
            try {
                pair.left.invokeFunction("action", mode, type, selection);
                client.setClickedNPC();
            } catch (ScriptException | NoSuchMethodException e) {
                NPCConversationManager cm = pair.getRight();

                String response = "An error occurred in this NPC";
                if (cm.getScriptName() != null) {
                    response += "\r\nName: " + cm.getScriptName();
                }
                response += "\r\nNPC ID: " + cm.getNpc();
                client.getPlayer().dropMessage(1, response);

                System.err.println("Error invoking function 'action' for NPC script " + (cm.getScriptName() == null ? cm.getNpc() : cm.getScriptName()) + ".js");
                e.printStackTrace();
                dispose(client);
            }
        }
    }

    public static void dispose(NPCConversationManager cm) {
        MapleClient client = cm.getClient();
        String path = "npc/world" + client.getWorld() + "/" + (cm.getScriptName() == null ? cm.getNpc() : cm.getScriptName()) + ".js";
        Pair<Invocable, NPCConversationManager> pair = storage.remove(client.getAccID());
        if (pair != null) {
            pair.left = null;
            pair.right = null;
        }
        ScriptUtil.removeScript(client, path);
        client.announce(MaplePacketCreator.enableActions());
    }

    public static void dispose(MapleClient client) {
        if (storage.containsKey(client.getAccID())) {
            dispose(storage.get(client.getAccID()).right);
        }
    }

    public static NPCConversationManager getConversationManager(MapleClient client) {
        if (storage.containsKey(client.getAccID())) {
            return storage.get(client.getAccID()).right;
        }
        return null;
    }
}
