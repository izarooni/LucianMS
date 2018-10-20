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

package com.lucianms.server.partyquest;

import com.lucianms.client.MapleCharacter;
import com.lucianms.server.Server;
import com.lucianms.server.world.MapleParty;
import com.lucianms.server.world.MaplePartyCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kevintjuh93
 */
public class PartyQuest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PartyQuest.class);

    private MapleParty party;
    private final List<MapleCharacter> participants = new ArrayList<>();

    public PartyQuest(MapleParty party) {
        this.party = party;
        MaplePartyCharacter leader = party.getLeader();
        int channel = leader.getChannel();
        int world = leader.getWorld();
        int mapid = leader.getMapId();
        for (MaplePartyCharacter pchr : party.getMembers()) {
            if (pchr.getChannel() == channel && pchr.getMapId() == mapid) {
                MapleCharacter chr = Server.getInstance().getWorld(world).getChannel(channel).getPlayerStorage().getPlayerByID(pchr.getId());
                if (chr != null) {
                    this.participants.add(chr);
                }
            }
        }
    }

    public MapleParty getParty() {
        return party;
    }

    public List<MapleCharacter> getParticipants() {
        return participants;
    }

    public void removeParticipant(MapleCharacter chr) throws Throwable {
        synchronized (participants) {
            participants.remove(chr);
            chr.setPartyQuest(null);
        }
    }

    public static int getExp(String PQ, int level) {
        if (PQ.equals("HenesysPQ")) {
            return 1250 * level / 5;
        } else if (PQ.equals("KerningPQFinal")) {
            return 500 * level / 5;
        } else if (PQ.equals("KerningPQ4th")) {
            return 400 * level / 5;
        } else if (PQ.equals("KerningPQ3rd")) {
            return 300 * level / 5;
        } else if (PQ.equals("KerningPQ2nd")) {
            return 200 * level / 5;
        } else if (PQ.equals("KerningPQ1st")) {
            return 100 * level / 5;
        } else if (PQ.equals("LudiMazePQ")) {
            return 2000 * level / 5;
        } else if (PQ.equals("LudiPQ1st")) {
            return 100 * level / 5;
        } else if (PQ.equals("LudiPQ2nd")) {
            return 250 * level / 5;
        } else if (PQ.equals("LudiPQ3rd")) {
            return 350 * level / 5;
        } else if (PQ.equals("LudiPQ4th")) {
            return 350 * level / 5;
        } else if (PQ.equals("LudiPQ5th")) {
            return 400 * level / 5;
        } else if (PQ.equals("LudiPQ6th")) {
            return 450 * level / 5;
        } else if (PQ.equals("LudiPQ7th")) {
            return 500 * level / 5;
        } else if (PQ.equals("LudiPQ8th")) {
            return 650 * level / 5;
        } else if (PQ.equals("LudiPQLast")) {
            return 800 * level / 5;
        } else {
            LOGGER.warn("Unhandled party quest: {}", PQ);
            return 0;
        }
    }
}
