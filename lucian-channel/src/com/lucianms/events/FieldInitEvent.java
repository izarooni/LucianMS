package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.Relationship;
import com.lucianms.constants.GameConstants;
import com.lucianms.io.scripting.Achievements;
import com.lucianms.io.scripting.event.EventInstanceManager;
import com.lucianms.nio.SendOpcode;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.nio.send.MaplePacketWriter;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.MapleMiniDungeon;
import com.lucianms.server.world.MapleParty;
import com.lucianms.server.world.MaplePartyCharacter;
import com.lucianms.server.world.MapleWorld;
import tools.MaplePacketCreator;

import java.util.Calendar;

/**
 * @author izarooni
 */
public class FieldInitEvent extends PacketEvent {

    public static byte[] getMarriedPartnerFieldTransfer(int fieldID, int playerID) {
        MaplePacketWriter w = new MaplePacketWriter(10);
        w.writeShort(SendOpcode.NOTIFY_MARRIED_PARTNER_MAP_TRANSFER.getValue());
        w.writeInt(fieldID);
        w.writeInt(playerID);
        return w.getPacket();
    }

    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        MapleClient client = getClient();
        MapleWorld world = client.getWorldServer();
        MapleCharacter player = client.getPlayer();
        MapleMap map = player.getMap();
        final int mapID = map.getId();

        Achievements.testFor(player, -1);
        player.setRates();
        player.checkBerserk();

        MapleParty party = player.getParty();
        if (party != null) {
            MaplePartyCharacter member = party.get(player.getId());
            if (member != null) {
                member.setFieldID(mapID);
                player.sendPartyGaugeRefresh();
                player.refreshPartyMemberGauges();
            } else {
                player.setPartyID(0);
            }
        }

        if (player.getForcedStat() != null) {
            player.setForcedStat(null);
            client.announce(MaplePacketCreator.getForcedStatReset());
        }

        if (MapleMiniDungeon.isDungeonMap(mapID)) {
            MapleMiniDungeon dungeon = MapleMiniDungeon.getDungeon(mapID);
            if (dungeon != null) {
                client.announce(MaplePacketCreator.getClock(30 * 60));
                TaskExecutor.createTask(new Runnable() {

                    @Override
                    public void run() {
                        if (MapleMiniDungeon.isDungeonMap(player.getMapId())) {
                            player.changeMap(dungeon.getBase());
                        }
                    }
                }, 30 * 60 * 1000);
            }
        }

        EventInstanceManager eim = player.getEventInstance();
        if (eim != null && eim.isTimerStarted()) {
            client.announce(MaplePacketCreator.getClock((int) (eim.getTimeLeft() / 1000)));
        }
        if (player.getFitness() != null && player.getFitness().isTimerStarted()) {
            client.announce(MaplePacketCreator.getClock((int) (player.getFitness().getTimeLeft() / 1000)));
        }

        if (player.getOla() != null && player.getOla().isTimerStarted()) {
            client.announce(MaplePacketCreator.getClock((int) (player.getOla().getTimeLeft() / 1000)));
        }

        if (mapID == 109060000) {
            client.announce(MaplePacketCreator.rollSnowBall(true, 0, null, null));
        }
        if (mapID == 914000200 || mapID == 914000210 || mapID == 914000220) {
            // aran tutorial map
            client.announce(MaplePacketCreator.aranGodlyStats());
        }

        if (map.hasClock()) {
            Calendar cal = Calendar.getInstance();
            client.announce((MaplePacketCreator.getClockTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))));
        }

        if (player.getDragon() == null && GameConstants.hasSPTable(player.getJob())) {
            player.createDragon();
            map.sendPacket(MaplePacketCreator.spawnDragon(player.getDragon()));
        }

        Relationship rltn = player.getRelationship();
        if (rltn.getStatus() != Relationship.Status.Single) {
            int partnerID = rltn.getPartnerID(player);
            MapleCharacter target = world.getPlayerStorage().get(partnerID);
            if (target != null) {
                player.announce(getMarriedPartnerFieldTransfer(target.getMapId(), target.getId()));
                target.announce(getMarriedPartnerFieldTransfer(player.getMapId(), player.getId()));
            }
        }
        return null;
    }
}
