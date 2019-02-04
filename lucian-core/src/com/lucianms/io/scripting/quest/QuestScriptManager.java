/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.lucianms.io.scripting.quest;

import com.lucianms.client.MapleClient;
import com.lucianms.client.MapleQuestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lucianms.io.scripting.ScriptUtil;
import com.lucianms.server.quest.MapleQuest;
import tools.Pair;

import javax.script.Invocable;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author RMZero213
 * @author izarooni
 */
public class QuestScriptManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestScriptManager.class);
    private static Map<MapleClient, Pair<Invocable, QuestActionManager>> storage = new HashMap<>();

    private QuestScriptManager() {
    }

    public static void start(MapleClient client, short questId, int npc) {
        MapleQuest quest = MapleQuest.getInstance(questId);
        if (!client.getPlayer().getQuest(quest).getStatus().equals(MapleQuestStatus.Status.NOT_STARTED)) {
            dispose(client);
            return;
        }
        try {
            if (storage.containsKey(client)) {
                return;
            }
            QuestActionManager qm = new QuestActionManager(client, questId, npc, true);
            Collection<Pair<String, Object>> binds = Collections.singletonList(new Pair<>("qm", qm));
            Invocable iv = ScriptUtil.eval(client, "quest/" + questId + ".js", binds);
            if (iv == null) {
                LOGGER.warn("No script for quest {}", questId);
                qm.dispose();
                return;
            }
            storage.put(client, new Pair<>(iv, qm));
            iv.invokeFunction("start", (byte) 1, (byte) 0, 0);
        } catch (FileNotFoundException e) {
            LOGGER.info("No script file for quest {}", questId);
        } catch (Exception e) {
            e.printStackTrace();
            dispose(client);
        }
    }

    public static void start(MapleClient client, byte mode, byte type, int selection) {
        if (storage.containsKey(client)) {
            try {
                storage.get(client).left.invokeFunction("start", mode, type, selection);
            } catch (Exception e) {
                e.printStackTrace();
                dispose(client);
            }
        }
    }

    public static void end(MapleClient client, short questid, int npc) {
        MapleQuest quest = MapleQuest.getInstance(questid);
        if (!client.getPlayer().getQuest(quest).getStatus().equals(MapleQuestStatus.Status.STARTED) || !client.getPlayer().getMap().containsNPC(npc)) {
            dispose(client);
            return;
        }
        try {
            QuestActionManager qm = new QuestActionManager(client, questid, npc, false);
            if (storage.containsKey(client)) {
                return;
            }
            Invocable iv = ScriptUtil.eval(client, "quest/" + questid + ".js", Collections.singletonList(new Pair<>("qm", qm)));
            storage.put(client, new Pair<>(iv, qm));
            if (iv == null) {
                qm.dispose();
                return;
            }
            iv.invokeFunction("end", (byte) 1, (byte) 0, 0);
        } catch (Throwable e) {
            e.printStackTrace();
            dispose(client);
        }
    }

    public static void end(MapleClient client, byte mode, byte type, int selection) {
        if (storage.containsKey(client)) {
            Invocable iv = storage.get(client).left;
            try {
                iv.invokeFunction("end", mode, type, selection);
            } catch (Exception e) {
                e.printStackTrace();
                dispose(client);
            }
        }
    }

    public static void dispose(QuestActionManager qm, MapleClient client) {
        storage.remove(client);
        ScriptUtil.removeScript(client, "quest/" + qm.getQuest() + ".js");
    }

    public static void dispose(MapleClient client) {
        if (storage.containsKey(client)) {
            dispose(storage.get(client).right, client);
        }
    }

    public static QuestActionManager getActionManager(MapleClient client) {
        if (storage.containsKey(client)) {
            return storage.get(client).right;
        }
        return null;
    }
}
