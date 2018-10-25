package com.lucianms.io.scripting.npc;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.SpamTracker;
import com.lucianms.constants.PlayerToggles;
import com.lucianms.features.PlayerCreative;
import com.lucianms.io.scripting.ScriptUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.Pair;

import javax.script.Invocable;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Matze
 * @author izarooni
 */
public class NPCScriptManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(NPCScriptManager.class);
    private static ConcurrentHashMap<Integer, Pair<Invocable, NPCConversationManager>> storage = new ConcurrentHashMap<>();

    private NPCScriptManager() {
    }

    public static void start(MapleClient client, int npc) {
        start(client, npc, null);
    }

    public static void start(MapleClient client, int npc, String fileName) {
        start(client, 0, npc, fileName);
    }

    public static void start(MapleClient client, int objectID, int npc, String fileName) {
        MapleCharacter player = client.getPlayer();
        SpamTracker.SpamData spamTracker = player.getSpamTracker(SpamTracker.SpamOperation.NpcTalk);
        if (spamTracker.testFor(1000)) {
            return;
        }
        final String path = "npc/world" + client.getWorld() + "/" + (fileName == null ? npc : fileName) + ".js";
        try {
            if (storage.containsKey(client.getAccID())) {
                dispose(client);
            }
            NPCConversationManager cm = new NPCConversationManager(client, objectID, npc, fileName);
            ArrayList<Pair<String, Object>> binds = new ArrayList<>();
            binds.add(new Pair<>("client", client));
            binds.add(new Pair<>("player", player));
            binds.add(new Pair<>("ch", client.getChannelServer()));
            binds.add(new Pair<>("cm", cm));

            Invocable iv;
            try {
                iv = ScriptUtil.eval(client, path, binds);
            } catch (FileNotFoundException e) {
                dispose(client);
                return;
            }

            boolean revoked = player.getToggles().checkProperty(PlayerToggles.CommandNPCAccess, false);
            // allow talking to the PlayerCreative NPC
            if (player.getGenericEvents().stream().anyMatch((g) -> g instanceof PlayerCreative)) {
                if (npc == 9899958) revoked = false;
            }

            if (iv == null || revoked) {
                if (revoked) {
                    player.sendMessage(5, PlayerToggles.ErrorMessage);
                }
                dispose(client);
                return;
            }
            spamTracker.record();
            storage.put(client.getAccID(), new Pair<>(iv, cm));
            try {
                iv.invokeFunction("start");
            } catch (NoSuchMethodException e1) {
                try {
                    iv.invokeFunction("action", 1, 0, -1);
                } catch (NoSuchMethodException e3) {
                    LOGGER.warn("No initializer function for script '{}' npc '{}' using player '{}'", fileName, npc, player.getName());
                    dispose(client);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            String response = "An error occurred in this NPC";
            if (fileName != null) {
                response += "\r\nName: " + fileName;
            }
            response += "\r\nNPC ID: " + npc;
            player.dropMessage(1, response);
            LOGGER.error("Unable to execute script '{}' npc '{}' using player '{}'", path, npc, player.getName(), e.getMessage());
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
                if (cm != null) {
                    String response = "An error occurred in this NPC";
                    if (cm.getScriptName() != null) {
                        response += "\r\nName: " + cm.getScriptName();
                    }
                    response += "\r\nNPC ID: " + cm.getNpc();
                    client.getPlayer().dropMessage(1, response);
                    dispose(client);
                    LOGGER.error("Unable to invoke 'action' function for script '{}' npc '{}' using player '{}'", cm.getScriptName(), cm.getNpc(), client.getPlayer().getName(), e);
                }
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
        System.gc();
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
