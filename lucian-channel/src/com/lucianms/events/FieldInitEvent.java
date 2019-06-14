package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.Relationship;
import com.lucianms.constants.GameConstants;
import com.lucianms.features.carnival.MCarnivalGame;
import com.lucianms.features.carnival.MCarnivalPacket;
import com.lucianms.features.coconut.CoconutEvent;
import com.lucianms.features.emergency.Emergency;
import com.lucianms.features.emergency.EmergencyAttack;
import com.lucianms.features.emergency.EmergencyDuel;
import com.lucianms.io.scripting.Achievements;
import com.lucianms.io.scripting.event.EventInstanceManager;
import com.lucianms.nio.SendOpcode;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.nio.send.MaplePacketWriter;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.Server;
import com.lucianms.server.life.SpawnPoint;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.MapleMiniDungeon;
import com.lucianms.server.world.MapleParty;
import com.lucianms.server.world.MaplePartyCharacter;
import com.lucianms.server.world.MapleWorld;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        DoEmergencyEvent();
        ShowMCarnivalBoard();
        player.setRates();
        player.checkBerserk();

        CoconutEvent coconutEvent = map.getCoconut();
        if (coconutEvent != null) {
            if (coconutEvent.getBeginTimestamp() > 0) {
                player.announce(MaplePacketCreator.getCoconutScore(coconutEvent.getRedPoints(), coconutEvent.getBluePoints()));
                List<Pair<String, Integer>> collect = coconutEvent.getCoconuts().stream()
                        .map(c -> new Pair<>(c.getName(), (int) c.getState())).collect(Collectors.toList());
                player.announce(MaplePacketCreator.setObjectState(collect));
                collect.clear();
                int timeLeft = (int) (((coconutEvent.getBeginTimestamp() + (coconutEvent.getTimeDefault() * 1000)) - System.currentTimeMillis()) / 1000);
                if (timeLeft > 0) {
                    player.announce(MaplePacketCreator.getClock(timeLeft));
                }
            }
        }

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

    private void DoEmergencyEvent() {
        //region emergency attack
        // empty map or contains only party members
        MapleCharacter player = getClient().getPlayer();
        MapleMap map = player.getMap();
        Collection<MapleCharacter> characters = new ArrayList<>(map.getCharacters());
        if (characters.size() == 1
                || (player.getPartyID() > 0 && characters.stream().allMatch(p -> p.getPartyID() == player.getPartyID()))) {
            /*
            May only activate under the following conditions:
            Contains monster spawn points,
            Contains no boss-type monsters,
            Map is a non-town type,
            Is not explicity excluded via server configuration
            must be a map that contains monster spawnpoints, contains no boss entity and is a hutning field
             */
            ArrayList<SpawnPoint> spawnPoints = map.getMonsterSpawnPoints();
            if (!map.isTown()
                    && spawnPoints.stream().noneMatch(sp -> sp.getMonster().isBoss() || player.getLevel() - sp.getMonster().getLevel() > 30)
                    && !spawnPoints.isEmpty()
                    && Arrays.binarySearch(Server.getConfig().getIntArray("EmergencyExcludes"), map.getId()) < 0) {
                // 1/25 chance to trigger emergency
                if ((player.isGM() && player.isDebug())
                        || ((System.currentTimeMillis() > map.getNextEmergency())
                        && player.getEventInstance() == null
                        && Randomizer.nextInt(25) == 0
                        && player.getGenericEvents().isEmpty())) {
                    Emergency event = Randomizer.nextBoolean() && player.getLevel() >= 30 ? new EmergencyDuel(player) : new EmergencyAttack(player);
                    TaskExecutor.createTask(new Runnable() {
                        @Override
                        public void run() {
                            event.registerPlayer(player);
                            if (!event.isCanceled()) {
                                map.setNextEmergency(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(8));
                            }
                        }
                    }, 1500);
                } else {
                    map.setNextEmergency(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(15));
                }
            }
        }
        characters.clear();
        //endregion
    }

    private void ShowMCarnivalBoard() {
        MapleCharacter player = getClient().getPlayer();
        int mapId = player.getMapId();

        MCarnivalGame carnivalGame = (MCarnivalGame) player.getGenericEvents().stream().filter(o -> o instanceof MCarnivalGame).findFirst().orElse(null);
        if (carnivalGame != null && GameConstants.isCarnivalField(mapId)) {
            player.announce(MaplePacketCreator.getClock((int) (carnivalGame.getTimeLeft() / 1000)));
            player.announce(MCarnivalPacket.getMonsterCarnivalStart(player, carnivalGame));
            player.announce(MaplePacketCreator.getUpdateFieldSpecificData(player.getTeam()));
        }
    }
}
