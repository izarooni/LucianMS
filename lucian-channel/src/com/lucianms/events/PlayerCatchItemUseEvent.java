package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.SpamTracker;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.constants.ItemConstants;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.life.MapleMonster;
import tools.MaplePacketCreator;

/**
 * @author kevintjuh93
 * @author izarooni
 */
public class PlayerCatchItemUseEvent extends PacketEvent {

    private int itemID;
    private int monsterID;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.readShort();
        itemID = reader.readInt();
        monsterID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        SpamTracker.SpamData spamTracker = player.getSpamTracker(SpamTracker.SpamOperation.CatchItem);

        MapleMonster mob = player.getMap().getMonsterByOid(monsterID);
        if (mob == null || player.getInventory(ItemConstants.getInventoryType(itemID)).countById(itemID) <= 0) {
            return null;
        }
        switch (itemID) {
            case 2270000:
                if (mob.getId() == 9300101) {
                    player.getMap().broadcastMessage(MaplePacketCreator.catchMonster(monsterID, itemID, (byte) 1));
                    mob.getMap().killMonster(mob, null, false);
                    MapleInventoryManipulator.removeById(getClient(), MapleInventoryType.USE, itemID, 1, true, true);
                    MapleInventoryManipulator.addById(getClient(), 1902000, (short) 1, "", -1);
                }
                break;
            case 2270001:
                if (spamTracker.testFor(1500)) {
                    break;
                }
                spamTracker.record();
                if (mob.getId() == 9500197) {
                    if (mob.getHp() < ((mob.getMaxHp() / 10) * 4)) {
                        player.getMap().broadcastMessage(MaplePacketCreator.catchMonster(monsterID, itemID, (byte) 1));
                        mob.getMap().killMonster(mob, null, false);
                        MapleInventoryManipulator.removeById(getClient(), MapleInventoryType.USE, itemID, 1, true, true);
                        MapleInventoryManipulator.addById(getClient(), 4031830, (short) 1, "", -1);
                    } else {
                        getClient().announce(MaplePacketCreator.catchMessage(0));
                    }
                }
                break;
            case 2270002:
                if (spamTracker.testFor(800)) {
                    break;
                }
                spamTracker.record();
                if (mob.getId() == 9300157) {
                    if (mob.getHp() < ((mob.getMaxHp() / 10) * 4)) {
                        if (Math.random() < 0.5) { // 50% chance
                            player.getMap().broadcastMessage(MaplePacketCreator.catchMonster(monsterID, itemID, (byte) 1));
                            mob.getMap().killMonster(mob, null, false);
                            MapleInventoryManipulator.removeById(getClient(), MapleInventoryType.USE, itemID, 1, true, true);
                            MapleInventoryManipulator.addById(getClient(), 4031868, (short) 1, "", -1);
                        } else {
                            player.getMap().broadcastMessage(MaplePacketCreator.catchMonster(monsterID, itemID, (byte) 0));
                        }
                    } else {
                        getClient().announce(MaplePacketCreator.catchMessage(0));
                    }
                }
                break;
            case 2270003:
                if (mob.getId() == 9500320) {
                    if (mob.getHp() < ((mob.getMaxHp() / 10) * 4)) {
                        player.getMap().broadcastMessage(MaplePacketCreator.catchMonster(monsterID, itemID, (byte) 1));
                        mob.getMap().killMonster(mob, null, false);
                        MapleInventoryManipulator.removeById(getClient(), MapleInventoryType.USE, itemID, 1, true, true);
                        MapleInventoryManipulator.addById(getClient(), 4031887, (short) 1, "", -1);
                    } else {
                        getClient().announce(MaplePacketCreator.catchMessage(0));
                    }
                }
                break;
            case 2270005:
                if (mob.getId() == 9300187) {
                    if (mob.getHp() < ((mob.getMaxHp() / 10) * 3)) {
                        player.getMap().broadcastMessage(MaplePacketCreator.catchMonster(monsterID, itemID, (byte) 1));
                        mob.getMap().killMonster(mob, null, false);
                        MapleInventoryManipulator.removeById(getClient(), MapleInventoryType.USE, itemID, 1, true, true);
                        MapleInventoryManipulator.addById(getClient(), 2109001, (short) 1, "", -1);
                    } else {
                        getClient().announce(MaplePacketCreator.catchMessage(0));
                    }
                }
                break;
            case 2270006:
                if (mob.getId() == 9300189) {
                    if (mob.getHp() < ((mob.getMaxHp() / 10) * 3)) {
                        player.getMap().broadcastMessage(MaplePacketCreator.catchMonster(monsterID, itemID, (byte) 1));
                        mob.getMap().killMonster(mob, null, false);
                        MapleInventoryManipulator.removeById(getClient(), MapleInventoryType.USE, itemID, 1, true, true);
                        MapleInventoryManipulator.addById(getClient(), 2109002, (short) 1, "", -1);
                    } else {
                        getClient().announce(MaplePacketCreator.catchMessage(0));
                    }
                }
                break;
            case 2270007:
                if (mob.getId() == 9300191) {
                    if (mob.getHp() < ((mob.getMaxHp() / 10) * 3)) {
                        player.getMap().broadcastMessage(MaplePacketCreator.catchMonster(monsterID, itemID, (byte) 1));
                        mob.getMap().killMonster(mob, null, false);
                        MapleInventoryManipulator.removeById(getClient(), MapleInventoryType.USE, itemID, 1, true, true);
                        MapleInventoryManipulator.addById(getClient(), 2109003, (short) 1, "", -1);
                    } else {
                        getClient().announce(MaplePacketCreator.catchMessage(0));
                    }
                }
                break;
            case 2270004:
                if (mob.getId() == 9300175) {
                    if (mob.getHp() < ((mob.getMaxHp() / 10) * 4)) {
                        player.getMap().broadcastMessage(MaplePacketCreator.catchMonster(monsterID, itemID, (byte) 1));
                        mob.getMap().killMonster(mob, null, false);
                        MapleInventoryManipulator.removeById(getClient(), MapleInventoryType.USE, itemID, 1, true, true);
                        MapleInventoryManipulator.addById(getClient(), 4001169, (short) 1, "", -1);
                    } else {
                        getClient().announce(MaplePacketCreator.catchMessage(0));
                    }
                }
                break;
            case 2270008:
                if (spamTracker.testFor(3000)) {
                    player.message("You cannot use the Fishing Net yet.");
                    break;
                }
                spamTracker.record();
                if (mob.getId() == 9500336) {
                    player.getMap().broadcastMessage(MaplePacketCreator.catchMonster(monsterID, itemID, (byte) 1));
                    mob.getMap().killMonster(mob, null, false);
                    MapleInventoryManipulator.removeById(getClient(), MapleInventoryType.USE, itemID, 1, true, true);
                    MapleInventoryManipulator.addById(getClient(), 2022323, (short) 1, "", -1);
                }
                break;
        }
        getClient().announce(MaplePacketCreator.enableActions());
        return null;
    }
}
