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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import provider.MapleData;
import provider.MapleDataTool;
import com.lucianms.server.quest.MapleQuest;
import com.lucianms.server.quest.MapleQuestRequirementType;
import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleQuestStatus;

/**
 *
 * @author Tyler (Twdtwd)
 */
public class MobRequirement extends MapleQuestRequirement {

	private static final Logger LOGGER = LoggerFactory.getLogger(MobRequirement.class);
	private Map<Integer, Integer> mobs = new HashMap<>();
	private int questID;
	
	public MobRequirement(MapleQuest quest, MapleData data) {
		super(MapleQuestRequirementType.MOB);
		processData(data);
		questID = quest.getId();
	}
	
	@Override
	public void processData(MapleData data) {
		for (MapleData questEntry : data.getChildren()) {
			int mobID = MapleDataTool.getInt(questEntry.getChildByPath("id"));
			int countReq = MapleDataTool.getInt(questEntry.getChildByPath("count"));
			mobs.put(mobID, countReq);
		}
	}
	
	
	@Override
	public boolean check(MapleCharacter chr, Integer npcid) {
		MapleQuestStatus status = chr.getQuest(MapleQuest.getInstance(questID));
		for(Integer mobID : mobs.keySet()) {
			int countReq = mobs.get(mobID);
			int progress;
			
			try {
				progress = Integer.parseInt(status.getProgress(mobID));
			} catch (NumberFormatException ex) {
				LOGGER.warn("Invalid progress value({}) for quest {} monster {}", status.getProgress(mobID), questID, mobID, ex);
				return false;
			}
			
			if(progress < countReq)
				return false;
		}
		return true;
	}
	
	public int getRequiredMobCount(int mobid) {
		if(mobs.containsKey(mobid)) {
			return mobs.get(mobid);
		}
		return 0;
	}
}
