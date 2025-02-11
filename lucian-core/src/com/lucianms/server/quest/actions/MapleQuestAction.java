/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lucianms.server.quest.actions;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleQuestStatus;
import provider.MapleData;
import com.lucianms.server.quest.MapleQuest;
import com.lucianms.server.quest.MapleQuestActionType;

/**
 *
 * @author Tyler (Twdtwd)
 */
public abstract class MapleQuestAction {
	private final MapleQuestActionType type;
	protected int questID;
	
	public MapleQuestAction(MapleQuestActionType action, MapleQuest quest) {
		this.type = action;
		this.questID = quest.getId();
	}
	
	public abstract void run(MapleCharacter chr, Integer extSelection);
	public abstract void processData(MapleData data);
	
	
	public boolean check(MapleCharacter chr, Integer extSelection) {
		MapleQuestStatus status = chr.getQuest(MapleQuest.getInstance(questID));
		return !(status.getStatus() == MapleQuestStatus.Status.NOT_STARTED && status.getForfeited() > 0);
	}
	
	public MapleQuestActionType getType() {
        return type;
    }
}
