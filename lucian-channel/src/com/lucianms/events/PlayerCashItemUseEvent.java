package com.lucianms.events;

import com.lucianms.client.*;
import com.lucianms.client.inventory.Equip;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.client.inventory.MaplePet;
import com.lucianms.constants.ExpTable;
import com.lucianms.constants.ItemConstants;
import com.lucianms.helpers.JailManager;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.*;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.MapleTVEffect;
import tools.MaplePacketCreator;
import tools.Pair;

import java.lang.ref.Cleaner;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author izarooni
 */
public class PlayerCashItemUseEvent extends PacketEvent implements Cleaner.Cleanable {

    private static final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
    public final static int NameTag = 5060000;
    public final static int ApReset = 5050000;
    public static final int ItemGuard = 5060001; // item guard
    public static final int ItemGuard7 = 5061000; // item guard (7 days)
    public static final int ItemGuard30 = 5061001; // item guard (30 days)
    public static final int ItemGuard90 = 5061002; // item guard (90 days)
    public static final int ItemGuard365 = 5061003; // itme guard (365 days)
    public static final int Incubator = 5060002;
    public static final int VIPTeleportRock = 5041000;

    private String content;
    private String username;
    private String[] messages;
    private int itemID;
    private int pointFrom, pointTo;
    private int inventoryType;
    private int fieldID;
    private short slot, itemSlot;
    private boolean whisperEnabled;
    private boolean isItem; // ? what
    private boolean isVIPRock;

    @Override
    public void clean() {
        messages = null;
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.readShort();
        itemID = reader.readInt();
        int itemType = itemID / 10000;
        if (itemType == 504) {
            isVIPRock = reader.readByte() == 1;
            if (!isVIPRock) {
                fieldID = reader.readInt();
            } else {
                username = reader.readMapleAsciiString();
            }
        } else if (itemType == 505) {
            pointTo = reader.readInt();
            pointFrom = reader.readInt();
        } else if (itemType == 506) {
            if (itemID == NameTag) {
                slot = reader.readShort();
            } else if (itemID == ItemGuard
                    || itemID == ItemGuard7
                    || itemID == ItemGuard30
                    || itemID == ItemGuard90
                    || itemID == ItemGuard365) {
                inventoryType = reader.readInt(); // this is wrong the fuck
                itemID = reader.readInt(); // should both be short
            } else if (itemID == Incubator) {
                inventoryType = reader.readInt();
                itemID = reader.readInt();
            }
            reader.readInt(); // timestamp
        } else if (itemType == 507) {
            switch (itemID / 1000 % 10) {
                case 1: // megaphone
                    content = reader.readMapleAsciiString();
                    break;
                case 2: // super megaphone
                    content = reader.readMapleAsciiString();
                    whisperEnabled = reader.readByte() != 0;
                    break;
                case 5: // maple tv
                    int type = itemID % 10;
                    if (type != 1) {
                        if (type >= 3) {
                            if (type == 3) {
                                reader.readByte();
                            }
                            whisperEnabled = reader.readByte() == 1;
                        } else if (type != 2) {
                            reader.readByte();
                        }
                        if (type != 4) {
                            username = reader.readMapleAsciiString();
                        }
                    }
                    messages = new String[5];
                    for (int i = 0; i < 5; i++) {
                        messages[i] = reader.readMapleAsciiString();
                    }
                    break;
                case 6: // item megaphone
                    content = reader.readMapleAsciiString();
                    whisperEnabled = reader.readByte() == 1;
                    isItem = reader.readByte() == 1;
                    if (isItem) {
                        inventoryType = reader.readInt();
                        itemSlot = (short) reader.readInt();
                    }
                    break;
                case 7: // triple megaphone
                    byte count = reader.readByte();
                    if (count < 1 || count > 3) {
                        setCanceled(true);
                    }
                    messages = new String[count];
                    for (int i = 0; i < count; i++) {
                        messages[i] = reader.readMapleAsciiString();
                    }
                    whisperEnabled = reader.readByte() == 1;
                    break;
            }
        } else if (itemType == 508) {
            content = reader.readMapleAsciiString();
        } else if (itemType == 509) {
            username = reader.readMapleAsciiString();
            content = reader.readMapleAsciiString();
        } else if (itemType == 512) {
            content = reader.readMapleAsciiString();
        } else if (itemType == 517) {
            username = reader.readMapleAsciiString();
        } else if (itemType == 537) {
            content = reader.readMapleAsciiString();
        } else if (itemType == 539) {
            messages = new String[4];
            for (int i = 0; i < 4; i++) {
                messages[i] = reader.readMapleAsciiString();
            }
            whisperEnabled = reader.readByte() != 0;
        } else if (itemType == 552) {
            inventoryType = reader.readInt();
            itemID = reader.readInt(); // item slot
        } else if (itemType == 557) {
            reader.readInt();
            itemID = reader.readInt();
            reader.readInt();
        } else {
            getLogger().info("unhandled cash item {}:\r\n{}", itemID, reader.toString());
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        MapleChannel ch = getClient().getChannelServer();
        MapleCharacter player = getClient().getPlayer();
        SpamTracker.SpamData spamTracker = player.getSpamTracker(SpamTracker.SpamOperation.CashItemUse);
        if (spamTracker.testFor(3000)) {
            return null;
        }
        spamTracker.record();
        int itemType = itemID / 10000;
        Item toUse = player.getInventory(MapleInventoryType.CASH).findById(itemID);

        if (toUse == null || toUse.getItemId() != itemID || toUse.getQuantity() < 1) {
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        }
        if (itemType == 505) { // AP/SP reset
            if (itemID > ApReset) {
                Skill skillSPTo = SkillFactory.getSkill(pointTo);
                Skill skillSPFrom = SkillFactory.getSkill(pointFrom);
                byte curLevel = player.getSkillLevel(skillSPTo);
                byte curLevelSPFrom = player.getSkillLevel(skillSPFrom);
                if ((curLevel < skillSPTo.getMaxLevel()) && curLevelSPFrom > 0) {
                    player.changeSkillLevel(skillSPFrom, (byte) (curLevelSPFrom - 1), player.getMasterLevel(skillSPFrom), -1);
                    player.changeSkillLevel(skillSPTo, (byte) (curLevel + 1), player.getMasterLevel(skillSPTo), -1);
                }
            } else {
                List<Pair<MapleStat, Integer>> statupdate = new ArrayList<>(2);
                try {
                    switch (pointFrom) {
                        case 64: // str
                            if (player.getStr() < 5) {
                                return null;
                            }
                            player.addStat(1, -1);
                            break;
                        case 128: // dex
                            if (player.getDex() < 5) {
                                return null;
                            }
                            player.addStat(2, -1);
                            break;
                        case 256: // int
                            if (player.getInt() < 5) {
                                return null;
                            }
                            player.addStat(3, -1);
                            break;
                        case 512: // luk
                            if (player.getLuk() < 5) {
                                return null;
                            }
                            player.addStat(4, -1);
                            break;
                        case 2048: { // HP
                            if (pointTo != 8192) {
                                getClient().announce(MaplePacketCreator.enableActions());
                                return null;
                            }
                            int hplose = 0;
                            final int jobid = player.getJob().getId();
                            if (jobid == 0 || jobid == 1000 || jobid == 2000 || jobid >= 1200 && jobid <= 1211) { // Beginner
                                hplose -= 12;
                            } else if (jobid >= 100 && jobid <= 132) { // Warrior
                                Skill improvinghplose = SkillFactory.getSkill(1000001);
                                int improvinghploseLevel = player.getSkillLevel(improvinghplose);
                                hplose -= 24;
                                if (improvinghploseLevel >= 1) {
                                    hplose -= improvinghplose.getEffect(improvinghploseLevel).getY();
                                }
                            } else if (jobid >= 200 && jobid <= 232) { // Magician
                                hplose -= 10;
                            } else if (jobid >= 500 && jobid <= 522) { // Pirate
                                Skill improvinghplose = SkillFactory.getSkill(5100000);
                                int improvinghploseLevel = player.getSkillLevel(improvinghplose);
                                hplose -= 22;
                                if (improvinghploseLevel > 0) {
                                    hplose -= improvinghplose.getEffect(improvinghploseLevel).getY();
                                }
                            } else if (jobid >= 1100 && jobid <= 1111) { // Soul Master
                                Skill improvinghplose = SkillFactory.getSkill(11000000);
                                int improvinghploseLevel = player.getSkillLevel(improvinghplose);
                                hplose -= 27;
                                if (improvinghploseLevel >= 1) {
                                    hplose -= improvinghplose.getEffect(improvinghploseLevel).getY();
                                }
                            } else if ((jobid >= 1300 && jobid <= 1311) || (jobid >= 1400 && jobid <= 1411)) { // Wind Breaker and Night Walker
                                hplose -= 17;
                            } else if (jobid >= 300 && jobid <= 322 || jobid >= 400 && jobid <= 422 || jobid >= 2000 && jobid <= 2112) { // Aran
                                hplose -= 20;
                            } else { // GameMaster
                                hplose -= 20;
                            }
                            player.setHp(player.getHp() + hplose);
                            player.setMaxHp(player.getMaxHp() + hplose);
                            statupdate.add(new Pair<>(MapleStat.HP, player.getHp()));
                            statupdate.add(new Pair<>(MapleStat.MAXHP, player.getMaxHp()));
                            break;
                        }
                        case 8192: { // MP
                            if (pointTo != 2048) {
                                getClient().announce(MaplePacketCreator.enableActions());
                                return null;
                            }
                            int mp = player.getMp();
                            int level = player.getLevel();
                            MapleJob job = player.getJob();
                            boolean canWash = true;
                            if (job.isA(MapleJob.SPEARMAN) && mp < 4 * level + 156) {
                                canWash = false;
                            } else if (job.isA(MapleJob.FIGHTER) && mp < 4 * level + 56) {
                                canWash = false;
                            } else if (job.isA(MapleJob.THIEF) && job.getId() % 100 > 0 && mp < level * 14 - 4) {
                                canWash = false;
                            } else if (mp < level * 14 + 148) {
                                canWash = false;
                            }
                            if (canWash) {
                                int minmp = 0;
                                if (job.isA(MapleJob.WARRIOR) || job.isA(MapleJob.DAWNWARRIOR1) || job.isA(MapleJob.ARAN1)) {
                                    minmp += 4;
                                } else if (job.isA(MapleJob.MAGICIAN) || job.isA(MapleJob.BLAZEWIZARD1)) {
                                    minmp += 36;
                                } else if (job.isA(MapleJob.BOWMAN) || job.isA(MapleJob.WINDARCHER1) || job.isA(MapleJob.THIEF) || job.isA(MapleJob.NIGHTWALKER1)) {
                                    minmp += 12;
                                } else if (job.isA(MapleJob.PIRATE) || job.isA(MapleJob.THUNDERBREAKER1)) {
                                    minmp += 16;
                                } else {
                                    minmp += 8;
                                }
                                player.setMp(player.getMp() - minmp);
                                player.setMaxMp(player.getMaxMp() - minmp);
                                statupdate.add(new Pair<>(MapleStat.MP, player.getMp()));
                                statupdate.add(new Pair<>(MapleStat.MAXMP, player.getMaxMp()));
                            }
                            break;
                        }
                        default:
                            getClient().announce(MaplePacketCreator.updatePlayerStats(MaplePacketCreator.EMPTY_STATUPDATE, true, player));
                            return null;
                    }
                    DistributeAPEvent.addStat(player, pointTo);
                    getClient().announce(MaplePacketCreator.updatePlayerStats(statupdate, true, player));
                } finally {
                    statupdate.clear();
                }
            }
            remove(getClient(), itemID);
        } else if (itemType == 506) {
            Item eq = null;
            if (itemID == NameTag) {
                eq = player.getInventory(MapleInventoryType.EQUIPPED).getItem(slot);
                if (eq != null) {
                    eq.setOwner(player.getName());
                }
            } else if (itemID == ItemGuard
                    || itemID == ItemGuard7
                    || itemID == ItemGuard30
                    || itemID == ItemGuard90
                    || itemID == ItemGuard365) {
                MapleInventoryType type = MapleInventoryType.getByType((byte) inventoryType);
                eq = player.getInventory(type).getItem((short) itemID);
                if (eq == null) {
                    return null;
                }
                byte flag = eq.getFlag();
                flag |= ItemConstants.LOCK;
                if (eq.getExpiration() > -1) {
                    return null;
                }
                eq.setFlag(flag);

                long period = 0;
                if (itemID == ItemGuard7) {
                    period = 7;
                } else if (itemID == ItemGuard30) {
                    period = 30;
                } else if (itemID == ItemGuard90) {
                    period = 90;
                } else if (itemID == ItemGuard365) {
                    period = 365;
                }

                if (period > 0) {
                    eq.setExpiration(System.currentTimeMillis() + (period * 60 * 60 * 24 * 1000));
                }

                remove(getClient(), itemID);
            } else if (itemID == Incubator) { // Incubator
                MapleInventoryType inventoryType = MapleInventoryType.getByType((byte) this.inventoryType);
                Item item = player.getInventory(inventoryType).getItem((short) itemID);
                if (item == null) {
                    return null;
                }
                if (getIncubatedItem(getClient(), itemID)) {
                    MapleInventoryManipulator.removeFromSlot(getClient(), inventoryType, (short) itemID, (short) 1, false);
                    remove(getClient(), itemID);
                }
                return null;
            }
            if (eq != null) {
                player.forceUpdateItem(eq);
                remove(getClient(), itemID);
            }
        } else {
            if (itemType == 507) {
                if (JailManager.isJailed(player.getId())) {
                    player.sendMessage(1, "You may not use megaphones while in jail");
                    getClient().announce(MaplePacketCreator.enableActions());
                    return null;
                }

                String medal = "";
                Item medalItem = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -49);
                if (medalItem != null) {
                    medal = "<" + ii.getName(medalItem.getItemId()) + "> ";
                }

                switch (itemID / 1000 % 10) {
                    case 1: // Megaphone
                        if (player.getLevel() > 9) {
                            player.getClient().getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(2, medal + player.getName() + " : " + content));
                        } else {
                            player.dropMessage(1, "You may not use this until you're level 10.");
                        }
                        break;
                    case 2: // Super megaphone
                        Server.broadcastMessage(MaplePacketCreator.serverNotice(3, getClient().getChannel(), medal + player.getName() + " : " + content, whisperEnabled));
                        break;
                    case 5: { // Maple TV
                        int type = itemID % 10;
                        boolean messenger = type >= 3;
                        MapleCharacter victim = null;
                        if (type != 1) {
                            if (type != 4) {
                                victim = ch.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                            }
                        }
                        ArrayList<String> messages = new ArrayList<>();
                        StringBuilder builder = new StringBuilder();
                        try {
                            for (int i = 0; i < 5; i++) {
                                String message = this.messages[i];
                                if (messenger) {
                                    builder.append(" ").append(message);
                                }
                                messages.add(message);
                            }
                            if (messenger) {
                                Server.broadcastMessage(MaplePacketCreator.serverNotice(3, getClient().getChannel(), medal + player.getName() + " : " + builder.toString(), whisperEnabled));
                            }
                            if (!MapleTVEffect.isActive()) {
                                new MapleTVEffect(player, victim, messages, type);
                                remove(getClient(), itemID);
                            } else {
                                player.dropMessage(1, "MapleTV is already in use.");
                                return null;
                            }
                        } finally {
                            messages.clear();
                            builder = null;
                        }
                        break;
                    }
                    case 6: //item megaphone
                        String msg = medal + player.getName() + " : " + content;
                        Item item = null;
                        if (isItem) { //item
                            item = player.getInventory(MapleInventoryType.getByType((byte) inventoryType)).getItem(itemSlot);
                            if (item == null) {
                                return null;
                            } else if (ii.isDropRestricted(item.getItemId())) {
                                player.dropMessage(1, "You cannot trade this item.");
                                getClient().announce(MaplePacketCreator.enableActions());
                                return null;
                            }
                        }
                        Server.broadcastMessage(MaplePacketCreator.itemMegaphone(msg, whisperEnabled, getClient().getChannel(), item));
                        break;
                    case 7: //triple megaphone
                        for (int i = 0; i < messages.length; i++) {
                            messages[i] = medal + player.getName() + " : " + messages[i];
                        }
                        Server.broadcastMessage(MaplePacketCreator.getMultiMegaphone(messages, getClient().getChannel(), whisperEnabled));
                        break;
                }
                remove(getClient(), itemID);
            } else if (itemType == 508) { //graduation banner
                getClient().announce(MaplePacketCreator.enableActions());
            } else if (itemType == 509) {
                player.sendNote(username, content, (byte) 0);
                remove(getClient(), itemID);
            } else if (itemType == 510) {
                player.getMap().broadcastMessage(MaplePacketCreator.musicChange("Jukebox/Congratulation"));
                remove(getClient(), itemID);
            } else if (itemType == 512) {
                int stateChange = ii.getStateChangeItem(itemID);
                if (stateChange != 0) {
                    for (MapleCharacter mChar : player.getMap().getCharacters()) {
                        ii.getItemEffect(stateChange).applyTo(mChar);
                    }
                }
                player.getMap().startMapEffect(ii.getMsg(itemID).replaceFirst("%s", player.getName()).replaceFirst("%s", content), itemID);
                remove(getClient(), itemID);
            } else if (itemType == 517) {
                MaplePet pet = player.getPet(0);
                if (pet == null) {
                    getClient().announce(MaplePacketCreator.enableActions());
                    return null;
                }
                Item item = player.getInventory(MapleInventoryType.CASH).getItem(pet.getPosition());
                if (item == null) {
                    player.sendDebugMessage(5, "Your pet (UID:{}) item {} in slot {} does not seem to exist", pet.getUniqueId(), pet.getItemId(), pet.getPosition());
                    getClient().announce(MaplePacketCreator.enableActions());
                    return null;
                }
                pet.setName(username);
                try (Connection con = ch.getConnection()) {
                    pet.saveToDb(con);
                } catch (SQLException ignore) {
                }
                player.forceUpdateItem(item);
                player.getMap().broadcastMessage(player, MaplePacketCreator.changePetName(player, username, 1), true);
                getClient().announce(MaplePacketCreator.enableActions());
                remove(getClient(), itemID);
            } else if (itemType == 504) { // vip teleport rock
                String error1 = "Either the player could not be found or you were trying to teleport to an illegal location.";
                if (!isVIPRock) {
                    if (ch.getMap(fieldID).getForcedReturnId() == 999999999) {
                        player.changeMap(ch.getMap(fieldID));
                    } else {
                        MapleInventoryManipulator.addById(getClient(), itemID, (short) 1);
                        player.dropMessage(1, error1);
                        getClient().announce(MaplePacketCreator.enableActions());
                    }
                } else {
                    MapleCharacter victim = ch.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                    if (victim != null) {
                        MapleMap target = victim.getMap();
                        if (ch.getMap(victim.getMapId()).getForcedReturnId() == 999999999 || victim.getMapId() < 100000000) {
                            if (victim.getGMLevel() <= player.getGMLevel()) {
                                if (itemID == VIPTeleportRock || victim.getMapId() / player.getMapId() == 1) { // viprock & same continent
                                    player.changeMap(target, target.findClosestSpawnpoint(victim.getPosition()));
                                    remove(getClient(), itemID);
                                    return null;
                                } else {
                                    player.dropMessage(1, "You cannot teleport between continents with this teleport rock.");
                                }
                            } else {
                                player.dropMessage(1, error1);
                            }
                        } else {
                            player.dropMessage(1, "You cannot teleport to this map.");
                        }
                    } else {
                        player.dropMessage(1, "Player could not be found in this channel.");
                    }
                    getClient().announce(MaplePacketCreator.enableActions());
                }
            } else if (itemType == 520) {
                player.gainMeso(ii.getMeso(itemID), true, false, true);
                remove(getClient(), itemID);
                getClient().announce(MaplePacketCreator.enableActions());
            } else if (itemType == 524) {
                for (byte i = 0; i < 3; i++) {
                    MaplePet pet = player.getPet(i);
                    if (pet != null) {
                        if (pet.canConsume(itemID)) {
                            pet.setFullness(100);
                            if (pet.getCloseness() + 100 > 30000) {
                                pet.setCloseness(30000);
                            } else {
                                pet.gainCloseness(100);
                            }

                            while (pet.getCloseness() >= ExpTable.getClosenessNeededForLevel(pet.getLevel())) {
                                pet.setLevel((byte) (pet.getLevel() + 1));
                                byte index = player.getPetIndex(pet);
                                getClient().announce(MaplePacketCreator.showOwnPetLevelUp(index));
                                player.getMap().broadcastMessage(MaplePacketCreator.showPetLevelUp(player, index));
                            }
                            Item item = player.getInventory(MapleInventoryType.CASH).getItem(pet.getPosition());
                            player.forceUpdateItem(item);
                            player.getMap().broadcastMessage(player, MaplePacketCreator.commandResponse(player.getId(), i, 1, true), true);
                            remove(getClient(), itemID);
                            break;
                        }
                    } else {
                        break;
                    }
                }
                getClient().announce(MaplePacketCreator.enableActions());
            } else if (itemType == 530) {
                ii.getItemEffect(itemID).applyTo(player);
                remove(getClient(), itemID);
            } else if (itemType == 533) {
                NPCScriptManager.start(getClient(), 9010009, null);
            } else if (itemType == 537) {
                player.setChalkboard(content);
                player.getMap().broadcastMessage(MaplePacketCreator.useChalkboard(player, false));
                player.getClient().announce(MaplePacketCreator.enableActions());
            } else if (itemType == 539) {
                if (JailManager.isJailed(player.getId())) {
                    player.sendMessage(1, "You may not use megaphones while in jail");
                    getClient().announce(MaplePacketCreator.enableActions());
                    return null;
                }
                String medal = "";
                Item medalItem = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -49);
                if (medalItem != null) {
                    medal = "<" + ii.getName(medalItem.getItemId()) + "> ";
                }

                Server.broadcastMessage(MaplePacketCreator.getAvatarMega(player, medal, getClient().getChannel(), itemID, Arrays.asList(messages), whisperEnabled));
                TaskExecutor.createTask(() -> Server.broadcastMessage(MaplePacketCreator.byeAvatarMega()), 1000 * 10);
                remove(getClient(), itemID);
            } else if (itemType == 545) { // MiuMiu's travel store
                if (player.getShop() == null) {
                    MapleShop shop = MapleShopFactory.getInstance().getShop(1338);
                    if (shop != null) {
                        shop.sendShop(getClient());
                        remove(getClient(), itemID);
                    }
                } else {
                    getClient().announce(MaplePacketCreator.enableActions());
                }
            } else if (itemType == 550) { //Extend item expiration
                getClient().announce(MaplePacketCreator.enableActions());
            } else if (itemType == 552) {
                MapleInventoryType type = MapleInventoryType.getByType((byte) inventoryType);
                Item item = player.getInventory(type).getItem((short) itemID);
                if (item == null
                        || item.getQuantity() <= 0
                        || (item.getFlag() & ItemConstants.KARMA) > 0 && ii.isKarmaAble(item.getItemId())) {
                    getClient().announce(MaplePacketCreator.enableActions());
                    return null;
                }
                if (type == MapleInventoryType.USE) {
                    item.setFlag((byte) ItemConstants.SPIKES);
                } else {
                    item.setFlag((byte) ItemConstants.KARMA);
                }

                player.forceUpdateItem(item);
                remove(getClient(), itemID);
                getClient().announce(MaplePacketCreator.enableActions());
            } else if (itemType == 557) {
                final Equip equip = (Equip) player.getInventory(MapleInventoryType.EQUIP).getItem((short) itemID);
                if (equip.getVicious() == 2
                        || player.getInventory(MapleInventoryType.CASH).findById(5570000) == null) {
                    return null;
                }
                equip.setVicious(equip.getVicious() + 1);
                equip.setUpgradeSlots(equip.getUpgradeSlots() + 1);
                remove(getClient(), itemID);
                getClient().announce(MaplePacketCreator.enableActions());
                getClient().announce(MaplePacketCreator.sendHammerData(equip.getVicious()));
                player.forceUpdateItem(equip);
            } else if (itemType == 561) { //VEGA'S SPELL
                getClient().announce(MaplePacketCreator.enableActions());
            }
        }
        return null;
    }

    private static void remove(MapleClient c, int itemId) {
        MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, itemId, 1, true, false);
    }

    private static final int[] IncubatedIDs = {
            1012070, 1302049, 1302063, 1322027, 2000004, 2000005, 2020013,
            2020015, 2040307, 2040509, 2040519, 2040521, 2040533, 2040715,
            2040717, 2040810, 2040811, 2070005, 2070006, 4020009,};
    private static final int[] IncubatedQuantities = {
            1, 1, 1, 1, 240, 200, 200,
            200, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 3};

    private static boolean getIncubatedItem(MapleClient client, int itemID) {
        MapleCharacter player = client.getPlayer();
        int amount = 0;
        for (int i = 0; i < IncubatedIDs.length; i++) {
            if (i == itemID) {
                amount = IncubatedQuantities[i];
            }
        }
        if (player.getInventory(MapleInventoryType.getByType((byte) (itemID / 1000000))).isFull()) {
            return false;
        }
        MapleInventoryManipulator.addById(client, itemID, (short) amount);
        return true;
    }
}
