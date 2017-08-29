package scripting.npc;

import client.MapleCharacter;
import client.MapleClient;
import scripting.ScriptUtil;
import tools.Pair;

import javax.script.Invocable;
import javax.script.ScriptException;
import java.util.ArrayList;
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
                return;
            }
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
            } catch (NullPointerException e) {
                cm.sendOk("Hey! I don't have a purpose right now\r\nThis is my ID: #b" + npc + "");
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
                dispose(client);
                return;
            }
            storage.put(client.getAccID(), new Pair<>(iv, cm));
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
                            System.out.println(String.format("No start function for script '%s' from npc %d", fileName, npc));
                            dispose(client);
                        }
                    }
                }
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
            } catch (Exception e) {
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
    }

    public static void dispose(MapleClient client) {
        if (storage.containsKey(client.getAccID())) {
            dispose(storage.get(client.getAccID()).right);
        }
    }

    public static NPCConversationManager getConversationManager(MapleClient client) {
        Pair<Invocable, NPCConversationManager> pair = storage.get(client.getAccID());
        return pair == null ? null : pair.getRight();
    }
}
