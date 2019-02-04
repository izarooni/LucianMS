package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleJob;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.constants.ServerConstants;
import com.lucianms.events.PacketEvent;
import com.lucianms.server.world.MapleParty;
import com.lucianms.server.world.MaplePartyCharacter;
import com.lucianms.server.world.PartyOperation;
import tools.MaplePacketCreator;

import java.util.Collection;

/**
 * @author XoticStory
 * @author BubblesDev
 * @author izarooni
 */
public class PlayerPartySearchBeginEvent extends PacketEvent {

    private int min, max;
    private int jobs;

    @Override
    public void processInput(MaplePacketReader reader) {
        min = reader.readInt();
        max = reader.readInt();
        reader.readInt();
        jobs = reader.readInt();
        if (!ServerConstants.USE_PARTY_SEARCH) {
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleParty party = player.getParty();
        if (party.getMembers().size() > 5) {
            return null;
        }
        Collection<MapleCharacter> players = player.getMap().getAllPlayer();
        for (MapleCharacter chrs : players) {
            int cLevel = chrs.getLevel();
            if (cLevel >= min && cLevel <= max && isValidJob(chrs.getJob(), jobs)) {
                if (party.getMembers().size() < 6) {
                    getClient().getWorldServer().updateParty(party.getId(), PartyOperation.JOIN, new MaplePartyCharacter(chrs));
                    chrs.receivePartyMemberHP();
                    chrs.updatePartyMemberHP();
                } else {
                    getClient().announce(MaplePacketCreator.partyStatusMessage(17));
                }
            }
        }
        return null;
    }

    private static boolean isValidJob(MapleJob job, int jobs) {
        int jobID = job.getId();
        if (jobID == 0) {
            return ((jobs & 2) > 0);
        } else if (jobID == 100) {
            return ((jobs & 4) > 0);
        } else if (jobID > 100 && jobID < 113) {
            return ((jobs & 8) > 0);
        } else if (jobID > 110 && jobID < 123) {
            return ((jobs & 16) > 0);
        } else if (jobID > 120 && jobID < 133) {
            return ((jobs & 32) > 0);
        } else if (jobID == 200) {
            return ((jobs & 64) > 0);
        } else if (jobID > 209 && jobID < 213) {
            return ((jobs & 128) > 0);
        } else if (jobID > 219 && jobID < 223) {
            return ((jobs & 256) > 0);
        } else if (jobID > 229 && jobID < 233) {
            return ((jobs & 512) > 0);
        } else if (jobID == 500) {
            return ((jobs & 1024) > 0);
        } else if (jobID > 509 && jobID < 513) {
            return ((jobs & 2048) > 0);
        } else if (jobID > 519 && jobID < 523) {
            return ((jobs & 4096) > 0);
        } else if (jobID == 400) {
            return ((jobs & 8192) > 0);
        } else if (jobID > 400 && jobID < 413) {
            return ((jobs & 16384) > 0);
        } else if (jobID > 419 && jobID < 423) {
            return ((jobs & 32768) > 0);
        } else if (jobID == 300) {
            return ((jobs & 65536) > 0);
        } else if (jobID > 300 && jobID < 313) {
            return ((jobs & 131072) > 0);
        } else if (jobID > 319 && jobID < 323) {
            return ((jobs & 262144) > 0);
        }
        return false;
    }
}