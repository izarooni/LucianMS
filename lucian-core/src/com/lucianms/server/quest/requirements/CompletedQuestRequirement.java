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
package com.lucianms.server.quest.requirements;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleQuestStatus;
import com.lucianms.server.quest.MapleQuest;
import com.lucianms.server.quest.MapleQuestRequirementType;
import provider.MapleData;
import provider.MapleDataTool;

import java.util.List;

/**
 * @author Tyler (Twdtwd)
 */
public class CompletedQuestRequirement extends MapleQuestRequirement {
    private int reqQuest;


    public CompletedQuestRequirement(MapleQuest quest, MapleData data) {
        super(MapleQuestRequirementType.COMPLETED_QUEST);
        processData(data);
    }

    @Override
    public void processData(MapleData data) {
        reqQuest = MapleDataTool.getInt(data);
    }


    @Override
    public boolean check(MapleCharacter chr, Integer npcid) {
        List<MapleQuestStatus> quests = chr.getCompletedQuests();
        try {
            return quests.size() >= reqQuest;
        } finally {
            // mother fuckin memory usage
            quests.clear();
        }
    }
}
