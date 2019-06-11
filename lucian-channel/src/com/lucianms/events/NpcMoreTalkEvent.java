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
package com.lucianms.events;

import com.lucianms.io.scripting.npc.NPCConversationManager;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.io.scripting.quest.QuestActionManager;
import com.lucianms.io.scripting.quest.QuestScriptManager;
import com.lucianms.nio.receive.MaplePacketReader;

/**
 * @author Matze
 */
public class NpcMoreTalkEvent extends PacketEvent {

    private String text = null;
    private int selection = -1;
    private byte dialogType;
    private byte action;

    @Override
    public void processInput(MaplePacketReader reader) {
        dialogType = reader.readByte();
        action = reader.readByte();
        if (dialogType == 2) {
            if (action != 0) {
                text = reader.readMapleAsciiString();
            }
        } else if (reader.available() >= 4) {
            selection = reader.readInt();
        } else if (reader.available() > 0) {
            selection = reader.readByte();
        }
    }

    @Override
    public Object onPacket() {
        QuestActionManager qm = getClient().getQM();
        NPCConversationManager cm = getClient().getCM();

        if (cm == null || !cm.isProc()) {
            return null;
        } else {
            cm.setProc(false);
        }

        if (dialogType == 2) {
            if (action != 0) {
                if (qm != null) {
                    qm.setGetText(text);
                    if (qm.isStart()) {
                        QuestScriptManager.start(getClient(), action, dialogType, -1);
                    } else {
                        QuestScriptManager.end(getClient(), action, dialogType, -1);
                    }
                } else {
                    cm.setGetText(text);
                    NPCScriptManager.action(getClient(), action, dialogType, -1);
                }
            } else if (qm != null) {
                qm.dispose();
            } else {
                cm.dispose();
            }
        } else {
            if (qm != null) {
                if (qm.isStart()) {
                    QuestScriptManager.start(getClient(), action, dialogType, selection);
                } else {
                    QuestScriptManager.end(getClient(), action, dialogType, selection);
                }
            } else {
                NPCScriptManager.action(getClient(), action, dialogType, selection);
            }
        }
        return null;
    }
}