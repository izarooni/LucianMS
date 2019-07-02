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
package tools;

import com.lucianms.client.*;
import com.lucianms.client.inventory.*;
import com.lucianms.client.inventory.Equip.ScrollResult;
import com.lucianms.client.meta.ForcedStat;
import com.lucianms.client.status.MonsterStatus;
import com.lucianms.client.status.MonsterStatusEffect;
import com.lucianms.constants.GameConstants;
import com.lucianms.constants.ItemConstants;
import com.lucianms.constants.ServerConstants;
import com.lucianms.constants.skills.Buccaneer;
import com.lucianms.constants.skills.Corsair;
import com.lucianms.constants.skills.ThunderBreaker;
import com.lucianms.events.gm.MapleSnowball;
import com.lucianms.events.meta.CommunityActions;
import com.lucianms.nio.SendOpcode;
import com.lucianms.nio.send.MaplePacketWriter;
import com.lucianms.server.*;
import com.lucianms.server.CashShop.CashItem;
import com.lucianms.server.CashShop.CashItemFactory;
import com.lucianms.server.CashShop.SpecialCashItem;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.guild.MapleAlliance;
import com.lucianms.server.guild.MapleGuild;
import com.lucianms.server.guild.MapleGuildCharacter;
import com.lucianms.server.guild.MapleGuildSummary;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MapleNPC;
import com.lucianms.server.life.MobSkill;
import com.lucianms.server.maps.*;
import com.lucianms.server.movement.LifeMovementFragment;
import com.lucianms.server.world.MapleMessengerCharacter;
import com.lucianms.server.world.MapleParty;
import com.lucianms.server.world.MaplePartyCharacter;
import com.lucianms.server.world.PartyOperation;

import java.awt.*;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @author Frz
 */
public class MaplePacketCreator {

    public static final List<Pair<MapleStat, Integer>> EMPTY_STATUPDATE = Collections.emptyList();
    public final static long ZERO_TIME = 94354848000000000L;//00 40 E0 FD 3B 37 4F 01
    private final static long FT_UT_OFFSET = 116444592000000000L; // EDT
    private final static long DEFAULT_TIME = 150842304000000000L;//00 80 05 BB 46 E6 17 02
    private final static long PERMANENT = 150841440000000000L; // 00 C0 9B 90 7D E5 17 02

    public static byte[] BBSThreadList(ResultSet rs, int start) throws SQLException {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_BBS_PACKET.getValue());
        mplew.write(0x06);
        if (!rs.last()) {
            mplew.write(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        int threadCount = rs.getRow();
        if (rs.getInt("localthreadid") == 0) { //has a notice
            mplew.write(1);
            addThread(mplew, rs);
            threadCount--; //one thread didn't count (because it's a notice)
        } else {
            mplew.write(0);
        }
        if (!rs.absolute(start + 1)) { //seek to the thread before where we start
            rs.first(); //uh, we're trying to start at a place past possible
            start = 0;
        }
        mplew.writeInt(threadCount);
        mplew.writeInt(Math.min(10, threadCount - start));
        for (int i = 0; i < Math.min(10, threadCount - start); i++) {
            addThread(mplew, rs);
            rs.next();
        }
        return mplew.getPacket();
    }

    public static byte[] MTSConfirmBuy() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MTS_OPERATION.getValue());
        mplew.write(0x33);
        return mplew.getPacket();
    }

    public static byte[] MTSConfirmSell() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MTS_OPERATION.getValue());
        mplew.write(0x1D);
        return mplew.getPacket();
    }

    public static byte[] MTSConfirmTransfer(int quantity, int pos) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MTS_OPERATION.getValue());
        mplew.write(0x27);
        mplew.writeInt(quantity);
        mplew.writeInt(pos);
        return mplew.getPacket();
    }

    public static byte[] MTSFailBuy() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MTS_OPERATION.getValue());
        mplew.write(0x34);
        mplew.write(0x42);
        return mplew.getPacket();
    }

    public static byte[] MTSWantedListingOver(int nx, int items) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MTS_OPERATION.getValue());
        mplew.write(0x3D);
        mplew.writeInt(nx);
        mplew.writeInt(items);
        return mplew.getPacket();
    }

    public static byte[] MobDamageMobFriendly(MapleMonster mob, int damage, int hp, int maxHP) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(mob.getObjectId());
        mplew.write(1); // direction ?
        mplew.writeInt(damage);
        mplew.writeInt(hp);
        mplew.writeInt(maxHP);
        return mplew.getPacket();
    }

    /**
     * Adds a announcement box to an existing MaplePacketWriter.
     *
     * @param mplew The MaplePacketWriter to add an announcement box to.
     * @param shop  The shop to announce.
     */
    private static void addAnnounceBox(final MaplePacketWriter mplew, MaplePlayerShop shop, int availability) {
        mplew.write(4);
        mplew.writeInt(shop.getObjectId());
        mplew.writeMapleString(shop.getDescription());
        mplew.write(0);
        mplew.write(0);
        mplew.write(1);
        mplew.write(availability);
        mplew.write(0);
    }

    /**
     * @param mplew      packet buffer
     * @param game       game instance
     * @param gameMode   1: match cards, 2: omok
     * @param gameType   game set type (e.g. matchcards size, omok piece type)
     * @param userCount  player count in the minigame box
     * @param inProgress if the game is in progress
     */
    private static void addAnnounceBox(final MaplePacketWriter mplew, MapleMiniGame game, int gameMode, int gameType, int userCount, boolean inProgress) {
        mplew.write(gameMode);
        mplew.writeInt(game.getObjectId()); // gameid/shopid
        mplew.writeMapleString(game.getDescription()); // desc
        mplew.writeBoolean(game.getPassword() != null);
        mplew.write(gameType);
        mplew.write(userCount);
        mplew.write(2);
        mplew.writeBoolean(inProgress);
    }

    private static void addAreaInfo(final MaplePacketWriter mplew, MapleCharacter chr) {
        Map<Short, String> areaInfos = chr.getAreaInfos();
        mplew.writeShort(areaInfos.size());
        for (Short area : areaInfos.keySet()) {
            mplew.writeShort(area);
            mplew.writeMapleString(areaInfos.get(area));
        }
    }

    private static void addAttackBody(MaplePacketWriter w, MapleCharacter chr, int skill, int skilllevel, int stance, int numAttackedAndDamage, int projectile, Map<Integer, List<Integer>> damage, int speed, int direction, int display) {
        w.writeInt(chr.getId());
        w.write(numAttackedAndDamage);
        w.write(0x5B);//?
        w.write(skilllevel);
        if (skilllevel > 0) {
            w.writeInt(skill);
        }
        w.write(display);
        w.write(direction);
        w.write(stance);
        w.write(speed);
        w.write(0x0A);
        w.writeInt(projectile);
        for (Integer oned : damage.keySet()) {
            List<Integer> onedList = damage.get(oned);
            if (onedList != null) {
                w.writeInt(oned);
                w.write(0xFF);
                if (skill == 4211006) {
                    w.write(onedList.size());
                }
                for (Integer eachd : onedList) {
                    w.writeInt(eachd);
                }
            }
        }
    }

    public static byte[] addCard(boolean full, int cardid, int level) {
        final MaplePacketWriter mplew = new MaplePacketWriter(11);
        mplew.writeShort(SendOpcode.MONSTER_BOOK_SET_CARD.getValue());
        mplew.write(full ? 0 : 1);
        mplew.writeInt(cardid);
        mplew.writeInt(level);
        return mplew.getPacket();
    }

    public static void addCashItemInformation(final MaplePacketWriter mplew, Item item, int accountId) {
        addCashItemInformation(mplew, item, accountId, null);
    }

    public static void addCashItemInformation(final MaplePacketWriter mplew, Item item, int accountId, String giftMessage) {
        boolean isGift = giftMessage != null;
        boolean isRing = false;
        Equip equip = null;
        if (item.getType() == 1) {
            equip = (Equip) item;
            isRing = equip.getRingId() > -1;
        }
        mplew.writeLong(item.getPetId() > -1 ? item.getPetId() : isRing ? equip.getRingId() : item.getCashId());
        if (!isGift) {
            mplew.writeInt(accountId);
            mplew.writeInt(0);
        }
        mplew.writeInt(item.getItemId());
        if (!isGift) {
            mplew.writeInt(item.getSN());
            mplew.writeShort(item.getQuantity());
        }
        mplew.writeAsciiString(StringUtil.getRightPaddedStr(item.getGiftFrom(), '\0', 13));
        if (isGift) {
            mplew.writeAsciiString(StringUtil.getRightPaddedStr(giftMessage, '\0', 73));
            return;
        }
        mplew.writeLong(getTime(item.getExpiration()));
        mplew.writeLong(0);
    }

    public static byte[] addCharBox(MapleCharacter c, int type) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        addAnnounceBox(mplew, c.getPlayerShop(), type);
        return mplew.getPacket();
    }

    private static void addCharEntry(final MaplePacketWriter mplew, MapleCharacter chr, boolean viewall) {
        addCharStats(mplew, chr);
        addCharLook(mplew, chr, false);
        if (!viewall) {
            mplew.write(0);
        }
        if (chr.isGM() || chr.getJob().getId() / 100 == 9) {
            mplew.write(0);
            return;
        }
        mplew.write(1); // world rank enabled (next 4 ints are not sent if disabled) Short??
        mplew.writeInt(chr.getRank()); // world rank
        mplew.writeInt(chr.getRankMove()); // move (negative is downwards)
        mplew.writeInt(chr.getJobRank()); // job rank
        mplew.writeInt(chr.getJobRankMove()); // move (negative is downwards)
    }

    private static void addCharEquips(final MaplePacketWriter mplew, MapleCharacter chr) {
        MapleInventory equip = chr.getInventory(MapleInventoryType.EQUIPPED);
        Collection<Item> ii = MapleItemInformationProvider.getInstance().canWearEquipment(chr, equip.list());
        Map<Short, Integer> myEquip = new LinkedHashMap<>();
        Map<Short, Integer> maskedEquip = new LinkedHashMap<>();
        for (Item item : ii) {
            short pos = (byte) (item.getPosition() * -1);
            if (pos < 100 && myEquip.get(pos) == null) {
                myEquip.put(pos, item.getItemId());
            } else if (pos > 100 && pos != 111) { // don't ask. o.o
                pos -= 100;
                if (myEquip.get(pos) != null) {
                    maskedEquip.put(pos, myEquip.get(pos));
                }
                myEquip.put(pos, item.getItemId());
            } else if (myEquip.get(pos) != null) {
                maskedEquip.put(pos, item.getItemId());
            }
        }
        for (Entry<Short, Integer> entry : myEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF);
        for (Entry<Short, Integer> entry : maskedEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF);
        Item cWeapon = equip.getItem((short) -111);
        mplew.writeInt(cWeapon != null ? cWeapon.getItemId() : 0);
        for (int i = 0; i < 3; i++) {
            if (chr.getPet(i) != null) {
                mplew.writeInt(chr.getPet(i).getItemId());
            } else {
                mplew.writeInt(0);
            }
        }
    }

    private static void addCharLook(final MaplePacketWriter mplew, MapleCharacter chr, boolean mega) {
        mplew.write(chr.getGender());
        mplew.write(chr.getSkinColor().getId()); // skin color
        mplew.writeInt(chr.getFace()); // face
        mplew.write(mega ? 0 : 1);
        mplew.writeInt(chr.getHair()); // hair
        addCharEquips(mplew, chr);
    }

    private static void addCharStats(final MaplePacketWriter w, final MapleCharacter player) {
        w.writeInt(player.getId()); // character id
        w.writeAsciiString(StringUtil.getRightPaddedStr(player.getName(), '\0', 13));
        w.write(player.getGender()); // gender (0 = male, 1 = female)
        w.write(player.getSkinColor().getId()); // skin color
        w.writeInt(player.getFace()); // face
        w.writeInt(player.getHair()); // hair
        for (MaplePet pet : player.getPets()) {
            long petID = Optional.ofNullable(pet).map(MaplePet::getUniqueId).orElse(0);
            w.writeLong(petID);
        }
        w.write(player.getLevel());
        final MapleJob job = player.getJob();
        w.writeShort(job.getId());
        w.writeShort(player.getStr());
        w.writeShort(player.getDex());
        w.writeShort(player.getInt());
        w.writeShort(player.getLuk());
        w.writeShort(player.getHp());
        w.writeShort(player.getMaxHp());
        w.writeShort(player.getMp());
        w.writeShort(player.getMaxMp());
        w.writeShort(player.getRemainingAp());
        if (job.isEvan()) {
            encodeEvanSkillPoints(w, job.getId());
        } else {
            w.writeShort(player.getRemainingSp());
        }
        w.writeInt(player.getExp());
        w.writeShort(player.getFame());
        w.writeInt(player.getGachaExp());
        w.writeInt(player.getMapId());
        w.write(player.getInitialSpawnpoint());
        w.writeInt(0);
    }

    private static void encodePlayerData(final MaplePacketWriter w, final MapleCharacter player) {
        w.writeLong(-1); // GW_CharacterStat flags?
        w.write(0);
        addCharStats(w, player);
        w.write(player.getBuddylist().getCapacity());

        if (player.getLinkedName() == null) {
            w.write(0);
        } else {
            w.write(1);
            w.writeMapleString(player.getLinkedName());
        }

        w.writeInt(player.getMeso());
        addInventoryInfo(w, player);
        addSkillInfo(w, player);
        addQuestInfo(w, player);
        addMiniGameInfo(w, player);
        encodePlayerRings(w, player);
        addTeleportInfo(w, player);
        addMonsterBookInfo(w, player);
        addNewYearInfo(w, player);//have fun!
        addAreaInfo(w, player);//assuming it stayed here xd
        w.writeShort(0);
    }

    public static byte[] addGuildToAlliance(MapleAlliance alliance, int newGuild, MapleClient c) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x12);
        mplew.writeInt(alliance.getId());
        mplew.writeMapleString(alliance.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleString(alliance.getRankTitle(i));
        }
        mplew.write(alliance.getGuilds().size());
        for (Integer guild : alliance.getGuilds()) {
            mplew.writeInt(guild);
        }
        mplew.writeInt(2);
        mplew.writeMapleString(alliance.getNotice());
        mplew.writeInt(newGuild);
        getGuildInfo(mplew, Server.getGuild(newGuild, c.getWorld(), null));
        return mplew.getPacket();
    }

    private static void addInventoryInfo(final MaplePacketWriter w, final MapleCharacter player) {
        for (byte i = 1; i <= 5; i++) {
            w.write(player.getInventory(MapleInventoryType.getByType(i)).getSlotLimit());
        }
        w.writeLong(getTime(-2));
        MapleInventory iv = player.getInventory(MapleInventoryType.EQUIPPED);
        Collection<Item> equippedC = iv.list();
        List<Item> equipped = new ArrayList<>(equippedC.size());
        List<Item> equippedCash = new ArrayList<>(equippedC.size());
        for (Item item : equippedC) {
            if (item.getPosition() <= -100) {
                equippedCash.add(item);
            } else {
                equipped.add(item);
            }
        }
        Collections.sort(equipped);
        for (Item item : equipped) {
            addItemInfo(w, item, false);
        }
        w.writeShort(0); // start of equip cash
        for (Item item : equippedCash) {
            addItemInfo(w, item, false);
        }
        w.writeShort(0); // start of equip inventory
        Collection<Item> equips = player.getInventory(MapleInventoryType.EQUIP).list();
        for (Item item : equips) {
            addItemInfo(w, item, false);
        }
        w.writeShort(0);
        equips.stream().filter(i -> i.getPosition() <= -1000 && i.getPosition() > -1100).forEach(i -> addItemInfo(w, i, false));
        w.writeShort(0);
        for (Item item : player.getInventory(MapleInventoryType.USE).list()) {
            addItemInfo(w, item, false);
        }
        w.write(0);
        for (Item item : player.getInventory(MapleInventoryType.SETUP).list()) {
            addItemInfo(w, item, false);
        }
        w.write(0);
        for (Item item : player.getInventory(MapleInventoryType.ETC).list()) {
            addItemInfo(w, item, false);
        }
        w.write(0);
        for (Item item : player.getInventory(MapleInventoryType.CASH).list()) {
            addItemInfo(w, item, false);
        }
    }

    public static void addItemInfo(final MaplePacketWriter w, final Item item, boolean zeroPosition) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        boolean isCash = ii.isCash(item.getItemId());
        boolean isPet = item.getPetId() > -1;
        boolean isRing = false;
        Equip equip = null;
        short pos = item.getPosition();
        if (item.getType() == 1) {
            equip = (Equip) item;
            isRing = equip.getRingId() > -1;
        }
        if (!zeroPosition) {
            if (equip != null) {
                if (pos < 0) {
                    pos *= -1;
                }
                w.writeShort(pos > 100 ? pos - 100 : pos);
            } else {
                w.write(pos);
            }
        }
        w.write(item.getType());
        w.writeInt(item.getItemId());
        w.writeBoolean(isCash);
        if (isCash) {
            w.writeLong(isPet ? item.getPetId() : isRing ? equip.getRingId() : item.getCashId());
        }
        w.writeLong(getTime(item.getExpiration()));
        if (isPet) {
            MaplePet pet = item.getPet();
            w.writeAsciiString(StringUtil.getRightPaddedStr(pet.getName(), '\0', 13));
            w.write(pet.getLevel());
            w.writeShort(pet.getCloseness());
            w.write(pet.getFullness());
            w.writeLong(getTime(item.getExpiration()));
            w.writeInt(0);
            w.write(new byte[]{(byte) 0x50, (byte) 0x46}); //wonder what this is
            w.writeInt(0);
            return;
        }
        if (equip == null) {
            w.writeShort(item.getQuantity());
            w.writeMapleString(item.getOwner());
            w.writeShort(item.getFlag()); // flag

            if (ItemConstants.isRechargable(item.getItemId())) {
                w.writeInt(2);
                w.write(new byte[]{(byte) 0x54, 0, 0, (byte) 0x34});
            }
            return;
        }
        w.write(equip.getUpgradeSlots()); // upgrade slots
        w.write(equip.getLevel()); // level
        w.writeShort(equip.getStr()); // str
        w.writeShort(equip.getDex()); // dex
        w.writeShort(equip.getInt()); // int
        w.writeShort(equip.getLuk()); // luk
        w.writeShort(equip.getHp()); // hp
        w.writeShort(equip.getMp()); // mp
        w.writeShort(equip.getWatk()); // watk
        w.writeShort(equip.getMatk()); // matk
        w.writeShort(equip.getWdef()); // wdef
        w.writeShort(equip.getMdef()); // mdef
        w.writeShort(equip.getAcc()); // accuracy
        w.writeShort(equip.getAvoid()); // avoid
        w.writeShort(equip.getHands()); // hands
        w.writeShort(equip.getSpeed()); // speed
        w.writeShort(equip.getJump()); // jump
        w.writeMapleString(equip.getOwner()); // owner name
        w.writeShort(equip.getFlag()); //Item Flags

        if (isCash) {
            for (int i = 0; i < 10; i++) {
                w.write(0x40);
            }
        } else {
            w.write(0);
            w.write(equip.getItemLevel()); //Item Level
            w.writeShort(0);
            w.writeShort(equip.getItemExp()); //Works pretty weird :s
            w.writeInt(equip.getVicious()); //WTF NEXON ARE YOU SERIOUS?
            w.writeLong(0);
        }
        w.writeLong(getTime(-2));
        w.writeInt(-1);

    }

    public static byte[] addItemToWeddingRegistry(MapleCharacter chr, Item item) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.WEDDING_GIFT_RESULT.getValue());
        mplew.write(0x0B);
        mplew.writeInt(0);
        for (int i = 0; i < 0; i++) {
            mplew.write(0);
        }
        addItemInfo(mplew, item, true);
        return mplew.getPacket();
    }

    private static void encodeMarriageData(final MaplePacketWriter w, MapleCharacter player) {
        Optional<MapleRing> first = player.getWeddingRings().stream().findFirst();
        if (!first.isPresent()) {
            w.writeBoolean(false);
            return;
        }
        MapleRing ring = first.get();
        w.writeBoolean(ring.isEquipped());
        if (!ring.isEquipped()) {
            return;
        }
        w.writeInt(player.getId());
        w.writeInt(ring.getPartnerChrId());
        w.writeInt(ring.getItemId());
    }

    public static byte[] addMatchCardBox(MapleCharacter c, MapleMiniGame game) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        addAnnounceBox(mplew, c.getMiniGame(), game.getMode(), game.getPieceType(), game.hasFreeSlot() ? 1 : 2, game.isStarted());
        return mplew.getPacket();
    }

    public static byte[] addMessengerPlayer(MapleMessengerCharacter member) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MESSENGER.getValue());
        mplew.write(0x00);
        mplew.write(member.getPosition());
        addCharLook(mplew, member.getPlayer(), true);
        mplew.writeMapleString(member.getUsername());
        mplew.write(member.getChannelID());
        mplew.write(0x00);
        return mplew.getPacket();
    }

    private static void addMiniGameInfo(final MaplePacketWriter mplew, MapleCharacter chr) {
        mplew.writeShort(0);
    }

    private static void addMonsterBookInfo(final MaplePacketWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getMonsterBookCover()); // cover
        mplew.write(0);
        Map<Integer, Integer> cards = chr.getMonsterBook().getCards();
        mplew.writeShort(cards.size());
        for (Entry<Integer, Integer> all : cards.entrySet()) {
            mplew.writeShort(all.getKey() % 10000); // Id
            mplew.write(all.getValue()); // Level
        }
    }

    public static byte[] addNewCharEntry(MapleCharacter chr) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.ADD_NEW_CHAR_ENTRY.getValue());
        mplew.write(0);
        addCharEntry(mplew, chr, false);
        return mplew.getPacket();
    }

    private static void addNewYearInfo(final MaplePacketWriter mplew, MapleCharacter chr) {
        mplew.writeShort(0);
    }

    public static byte[] addOmokBox(MapleCharacter c, MapleMiniGame game) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        addAnnounceBox(mplew, c.getMiniGame(), game.getMode(), game.getPieceType(), game.hasFreeSlot() ? 1 : 2, game.isStarted());
        return mplew.getPacket();
    }

    private static void addPartyStatus(int forchannel, MapleParty party, MaplePacketWriter w, boolean leaving) {
        ArrayList<MaplePartyCharacter> members = new ArrayList<>();
        members.addAll(Collections.nCopies(6 - party.size(), new MaplePartyCharacter()));
        members.addAll(party.values());
        members.forEach(m -> w.writeInt(m.getPlayerID()));
        members.forEach(m -> w.writeAsciiString(StringUtil.getRightPaddedStr(m.getUsername(), '\0', 13)));
        members.forEach(m -> w.writeInt(m.getJobID()));
        members.forEach(m -> w.writeInt(m.getLevel()));
        members.forEach(m -> w.writeInt(m.getChannelID() - 1));
        w.writeInt(party.getLeaderPlayerID());
        members.forEach(m -> w.writeInt(m.getChannelID() == forchannel ? m.getFieldID() : 0));
        for (MaplePartyCharacter member : members) {
            if (member.getChannelID() == forchannel && !leaving && !member.getDoors().isEmpty()) {
                for (MapleDoor doors : member.getDoors()) {
                    w.writeInt(doors.getTown().getId());
                    w.writeInt(doors.getTarget().getId());
                    w.writeInt(doors.getPosition().x);
                    w.writeInt(doors.getPosition().y);
                }
            } else {
                w.writeInt(999999999);
                w.writeInt(999999999);
                w.writeInt(0);
                w.writeInt(0);
            }
        }
        members.clear();
    }

    public static void addPetInfo(final MaplePacketWriter mplew, MaplePet pet, boolean showpet) {
        mplew.write(1);
        if (showpet) {
            mplew.write(0);
        }

        mplew.writeInt(pet.getItemId());
        mplew.writeMapleString(pet.getName());
        mplew.writeInt(pet.getUniqueId());
        mplew.writeInt(0);
        mplew.writeLocation(pet.getPos());
        mplew.write(pet.getStance());
        mplew.writeInt(pet.getFh());
    }

    private static void addQuestInfo(final MaplePacketWriter mplew, MapleCharacter chr) {
        mplew.writeShort(chr.getStartedQuestsSize());
        for (MapleQuestStatus q : chr.getStartedQuests()) {
            mplew.writeShort(q.getQuest().getId());
            mplew.writeMapleString(q.getQuestData());
            if (q.getQuest().getInfoNumber() > 0) {
                mplew.writeShort(q.getQuest().getInfoNumber());
                mplew.writeMapleString(q.getQuestData());
            }
        }
        List<MapleQuestStatus> completed = chr.getCompletedQuests();
        mplew.writeShort(completed.size());
        for (MapleQuestStatus q : completed) {
            mplew.writeShort(q.getQuest().getId());
            mplew.writeLong(getTime(q.getCompletionTime()));
        }
        completed.clear();
    }

    public static byte[] addQuestTimeLimit(final short quest, final int time) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(6);
        mplew.writeShort(1);//Size but meh, when will there be 2 at the same time? And it won't even replace the old one :)
        mplew.writeShort(quest);
        mplew.writeInt(time);
        return mplew.getPacket();
    }

    private static void encodePlayerRings(final MaplePacketWriter w, final MapleCharacter player) {
        w.writeShort(player.getCrushRings().size());
        for (MapleRing ring : player.getCrushRings()) {
            w.writeInt(ring.getPartnerChrId());
            w.writeAsciiString(StringUtil.getRightPaddedStr(ring.getPartnerName(), '\0', 13));
            w.writeInt(ring.getRingId());
            w.writeInt(0);
            w.writeInt(ring.getPartnerRingId());
            w.writeInt(0);
        }
        w.writeShort(player.getFriendshipRings().size());
        for (MapleRing ring : player.getFriendshipRings()) {
            w.writeInt(ring.getPartnerChrId());
            w.writeAsciiString(StringUtil.getRightPaddedStr(ring.getPartnerName(), '\0', 13));
            w.writeInt(ring.getRingId());
            w.writeInt(0);
            w.writeInt(ring.getPartnerRingId());
            w.writeInt(0);
            w.writeInt(ring.getItemId());
        }
        w.writeShort(player.getWeddingRings().size());
        for (MapleRing ring : player.getWeddingRings()) {
            int nGroomID = (player.getGender() == 0) ? player.getId() : ring.getPartnerChrId();
            int nBrideID = (player.getGender() == 0) ? ring.getPartnerChrId() : player.getId();
            String sGroomName = (player.getGender() == 0) ? player.getName() : ring.getPartnerName();
            String sBrideName = (player.getGender() == 0) ? ring.getPartnerName() : player.getName();

            w.writeInt(30000 + ring.getRingId()); // dwMarriageNo
            w.writeInt(nGroomID);
            w.writeInt(nBrideID);
            w.writeShort(3); // usStatus
            w.writeInt(ring.getItemId()); // nGroomItemID
            w.writeInt(ring.getItemId()); // nBrideItemID
            w.writeAsciiString(StringUtil.getRightPaddedStr(sGroomName, '\0', 13));
            w.writeAsciiString(StringUtil.getRightPaddedStr(sBrideName, '\0', 13));
        }
    }

    private static void encodeRingData(final MaplePacketWriter w, MapleRing ring) {
        w.writeBoolean(ring != null);
        if (ring != null) {
            w.writeLong(ring.getRingId());
            w.writeLong(ring.getPartnerRingId());
            w.writeInt(ring.getItemId());
        }
    }

    private static void addSkillInfo(final MaplePacketWriter mplew, MapleCharacter chr) {
        mplew.write(0); // start of skills
        Map<Integer, SkillEntry> skills = chr.getSkills();
        mplew.writeShort(skills.size());
        for (Entry<Integer, SkillEntry> skill : skills.entrySet()) {
            mplew.writeInt(skill.getKey());
            mplew.writeInt(skill.getValue().level);
            mplew.writeLong(getTime(skill.getValue().expiration));
            if (GameConstants.isSkillNeedMasterLevel(skill.getKey())) {
                mplew.writeInt(skill.getValue().masterLevel);
            }
        }
        mplew.writeShort(chr.getAllCooldowns().size());
        for (PlayerCoolDownValueHolder cooling : chr.getAllCooldowns()) {
            mplew.writeInt(cooling.skillId);
            int timeLeft = (int) (cooling.length + cooling.startTime - System.currentTimeMillis());
            mplew.writeShort(timeLeft / 1000);
        }
    }

    private static void addTeleportInfo(final MaplePacketWriter mplew, MapleCharacter chr) {
        final List<Integer> tele = chr.getTrockMaps();
        final List<Integer> viptele = chr.getVipTrockMaps();
        for (int i = 0; i < 5; i++) {
            mplew.writeInt(tele.get(i));
        }
        for (int i = 0; i < 10; i++) {
            mplew.writeInt(viptele.get(i));
        }
    }

    public static void addThread(final MaplePacketWriter mplew, ResultSet rs) throws SQLException {
        mplew.writeInt(rs.getInt("localthreadid"));
        mplew.writeInt(rs.getInt("postercid"));
        mplew.writeMapleString(rs.getString("name"));
        mplew.writeLong(getTime(rs.getLong("timestamp")));
        mplew.writeInt(rs.getInt("icon"));
        mplew.writeInt(rs.getInt("replycount"));
    }

    public static byte[] allianceMemberOnline(MapleCharacter mc, boolean online) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0E);
        mplew.writeInt(mc.getGuild().getAllianceId());
        mplew.writeInt(mc.getGuildId());
        mplew.writeInt(mc.getId());
        mplew.write(online ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] allianceNotice(int id, String notice) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x1C);
        mplew.writeInt(id);
        mplew.writeMapleString(notice);
        return mplew.getPacket();
    }

    public static byte[] applyMonsterStatus(final int oid, final MonsterStatusEffect mse, final List<Integer> reflection) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.APPLY_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        mplew.writeLong(0);
        writeIntMask(mplew, mse.getStati());
        for (Map.Entry<MonsterStatus, Integer> stat : mse.getStati().entrySet()) {
            mplew.writeShort(stat.getValue());
            if (mse.isMonsterSkill()) {
                mplew.writeShort(mse.getMobSkill().getSkillId());
                mplew.writeShort(mse.getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(mse.getSkill().getId());
            }
            mplew.writeShort(-1); // might actually be the buffTime but it's not displayed anywhere
        }
        int size = mse.getStati().size(); // size
        if (reflection != null) {
            for (Integer ref : reflection) {
                mplew.writeInt(ref);
            }
            if (reflection.size() > 0) {
                size /= 2; // This gives 2 buffs per reflection but it's really one buff
            }
        }
        mplew.write(size); // size
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] aranGodlyStats() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FORCED_STAT_SET.getValue());
        mplew.write(new byte[]{(byte) 0x1F, (byte) 0x0F, 0, 0, (byte) 0xE7, 3, (byte) 0xE7, 3, (byte) 0xE7, 3, (byte) 0xE7, 3, (byte) 0xFF, 0, (byte) 0xE7, 3, (byte) 0xE7, 3, (byte) 0x78, (byte) 0x8C});
        return mplew.getPacket();
    }

    public static byte[] getForcedStats(ForcedStat stats) {
        MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.FORCED_STAT_SET.getValue());
        stats.encode(w);
        return w.getPacket();
    }

    /**
     * Gets a "block" packet (ie. the cash shop is unavailable, etc)
     * <p>
     * Possible values for <code>type</code>:<br> 1: The portal is closed for now.<br> 2: You cannot go to that
     * place.<br> 3: Unable to approach due to the force of the ground.<br> 4: You cannot teleport to or on this
     * map.<br> 5: Unable to approach due to the force of the ground.<br> 6: This map can only be entered by party
     * members.<br> 7: The Cash Shop is currently not available. Stay tuned...<br>
     *
     * @param type The type
     * @return The "block" packet.
     */
    public static byte[] blockedMessage(int type) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.BLOCKED_MAP.getValue());
        mplew.write(type);
        return mplew.getPacket();
    }

    /**
     * Gets a "block" packet (ie. the cash shop is unavailable, etc)
     * <p>
     * Possible values for <code>type</code>:<br> 1: You cannot move that channel. Please try again later.<br> 2: You
     * cannot go into the cash shop. Please try again later.<br> 3: The Item-Trading Shop is currently unavailable.
     * Please try again later.<br> 4: You cannot go into the trade shop, due to limitation of user count.<br> 5: You do
     * not meet the minimum level requirement to access the Trade Shop.<br>
     *
     * @param type The type
     * @return The "block" packet.
     */
    public static byte[] blockedMessage2(int type) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.BLOCKED_SERVER.getValue());
        mplew.write(type);
        return mplew.getPacket();
    }

    public static byte[] boatPacket(boolean type) {//don't think this is correct..
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CONTI_STATE.getValue());
        mplew.write(type ? 1 : 2);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] buddylistMessage(byte message) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.BUDDYLIST.getValue());
        mplew.write(message);
        return mplew.getPacket();
    }

    public static byte[] bunnyPacket() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(9);
        mplew.writeAsciiString("Protect the Moon Bunny!!!");
        return mplew.getPacket();
    }

    public static byte[] getByteAvatarMegaphone() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CLEAR_AVATAR_MEGAPHONE.getValue());
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] cancelBuff(Set<MapleBuffStat> stats) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CANCEL_BUFF.getValue());
        encodeBuffMask(mplew, stats);
        mplew.write(1);//?
        return mplew.getPacket();
    }

    public static byte[] cancelChair(int id) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CANCEL_CHAIR.getValue());
        if (id == -1) {
            mplew.write(0);
        } else {
            mplew.write(1);
            mplew.writeShort(id);
        }


        return mplew.getPacket();
    }

    public static byte[] cancelDebuff(long mask) {
        final MaplePacketWriter mplew = new MaplePacketWriter(19);
        mplew.writeShort(SendOpcode.CANCEL_BUFF.getValue());
        mplew.writeLong(0);
        mplew.writeLong(mask);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] cancelForeignBuff(int cid, Set<MapleBuffStat> stats) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        encodeBuffMask(mplew, stats);
        return mplew.getPacket();
    }

    public static byte[] cancelForeignDebuff(int cid, long mask) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        mplew.writeLong(0);
        mplew.writeLong(mask);
        return mplew.getPacket();
    }

    public static byte[] cancelMonsterStatus(int oid, Map<MonsterStatus, Integer> stats) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CANCEL_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        mplew.writeLong(0);
        writeIntMask(mplew, stats);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] catchMessage(int message) { // not done, I guess
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.BRIDLE_MOB_CATCH_FAIL.getValue());
        mplew.write(message); // 1 = too strong, 2 = Elemental Rock
        mplew.writeInt(0);//Maybe itemid?
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] catchMonster(int monsobid, int itemid, byte success) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CATCH_MONSTER.getValue());
        mplew.writeInt(monsobid);
        mplew.writeInt(itemid);
        mplew.write(success);
        return mplew.getPacket();
    }

    public static byte[] changeAllianceRankTitle(int alliance, String[] ranks) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x1A);
        mplew.writeInt(alliance);
        for (int i = 0; i < 5; i++) {
            mplew.writeMapleString(ranks[i]);
        }
        return mplew.getPacket();
    }

    /**
     * Changes the current background effect to either being rendered or not. Data is still missing, so this is pretty
     * binary at the moment in how it behaves.
     *
     * @param remove     whether or not the remove or add the specified layer.
     * @param layer      the targeted layer for removal or addition.
     * @param transition the time it takes to transition the effect.
     * @return a packet to change the background effect of a specified layer.
     */
    public static byte[] changeBackgroundEffect(boolean remove, int layer, int transition) {
        MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SET_BACK_EFFECT.getValue());
        mplew.writeBoolean(remove);
        mplew.writeInt(0); // not sure what this int32 does yet
        mplew.write(layer);
        mplew.writeInt(transition);
        return mplew.getPacket();
    }

    public static byte[] changeCover(int cardid) {
        final MaplePacketWriter mplew = new MaplePacketWriter(6);
        mplew.writeShort(SendOpcode.MONSTER_BOOK_SET_COVER.getValue());
        mplew.writeInt(cardid);
        return mplew.getPacket();
    }

    public static byte[] changePetName(MapleCharacter chr, String newname, int slot) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PET_NAMECHANGE.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(0);
        mplew.writeMapleString(newname);
        mplew.write(0);
        return mplew.getPacket();
    }

    //rank change
    public static byte[] changeRank(MapleGuildCharacter mgc) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x40);
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.write(mgc.getGuildRank());
        return mplew.getPacket();
    }

    public static byte[] getCharacterInfo(MapleCharacter player) {
        MapleInventory eqqInventory = player.getInventory(MapleInventoryType.EQUIPPED);

        final MaplePacketWriter w = new MaplePacketWriter(150);
        w.writeShort(SendOpcode.CHAR_INFO.getValue());
        //region CUIUserInfo::SetAvatarInfo
        w.writeInt(player.getId());
        w.write(player.getLevel());
        w.writeShort(player.getJob().getId());
        w.writeShort(player.getFame());
        w.writeBoolean(!player.getWeddingRings().isEmpty());
        String guildName = "";
        String allianceName = "";
        MapleGuildSummary gs = player.getClient().getWorldServer().getGuildSummary(player.getGuildId(), player.getWorld());
        if (player.getGuildId() > 0 && gs != null) {
            guildName = gs.getName();
            MapleAlliance alliance = Server.getAlliance(gs.getAllianceId());
            if (alliance != null) {
                allianceName = alliance.getName();
            }
        }
        w.writeMapleString(guildName);
        w.writeMapleString(allianceName);
        w.write(0);
        //endregion
        //region CUIUserInfo::SetMultiPetInfo
        int petCount = player.getNoPets();
        w.writeBoolean(petCount > 0);
        Item inv = eqqInventory.getItem((short) -114);
        for (MaplePet pet : player.getPets()) {
            if (pet != null) {
                w.writeInt(pet.getItemId()); // nTemplateID
                w.writeMapleString(pet.getName());
                w.write(pet.getLevel());
                w.writeShort(pet.getCloseness());
                w.write(pet.getFullness());
                w.writeShort(0);
                w.writeInt(inv != null ? inv.getItemId() : 0);
                w.writeBoolean(--petCount > 0);
            } else {
                break;
            }
        }
        //endregion
        //region CUIUserInfo::SetTamingMobInfo
        MapleMount mount = player.getVehicle();
        boolean hasMount = mount != null && eqqInventory.getItem((short) -18) != null;
        w.write(hasMount ? mount.getId() : 0); //mount
        if (hasMount) {
            w.writeInt(mount.getLevel()); //level
            w.writeInt(mount.getExp()); //exp
            w.writeInt(mount.getTiredness()); //tiredness
        }
        //endregion
        //region CUIUserInfo::SetWishItemInfo
        CashShop cashShop = player.getCashShop();
        if (cashShop != null) {
            w.write(cashShop.getWishList().size());
            cashShop.getWishList().forEach(w::writeInt);
        } else {
            w.write(0);
        }
        //endregion
        Optional<MonsterBook> book = Optional.ofNullable(player.getMonsterBook());
        w.writeInt(book.map(MonsterBook::getBookLevel).orElse(0));
        w.writeInt(book.map(MonsterBook::getNormalCard).orElse(0));
        w.writeInt(book.map(MonsterBook::getSpecialCard).orElse(0));
        w.writeInt(book.map(MonsterBook::getTotalCards).orElse(0));
        w.writeInt(player.getMonsterBookCover() > 0 ? MapleItemInformationProvider.getInstance().getCardMobId(player.getMonsterBookCover()) : 0);
        //region MedalAchievementInfo::Decode
        Item medal = eqqInventory.getItem((short) -49);
        w.writeInt(medal == null ? 0 : medal.getItemId());
        List<MapleQuestStatus> cquests = player.getCompletedQuests();
        List<Short> quests = cquests.stream()
                .filter(q -> q.getQuestID() >= 29000)
                .map(MapleQuestStatus::getQuestID).collect(Collectors.toList());
        Collections.sort(quests);
        w.writeShort(quests.size());
        quests.forEach(w::writeShort);
        quests.clear();
        cquests.clear();
        //endregion
        return w.getPacket();
    }

    public static byte[] charNameResponse(String charname, boolean nameUsed) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CHAR_NAME_RESPONSE.getValue());
        mplew.writeMapleString(charname);
        mplew.write(nameUsed ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] closeRangeAttack(MapleCharacter chr, int skill, int skilllevel, int stance, int numAttackedAndDamage, Map<Integer, List<Integer>> damage, int speed, int direction, int display) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CLOSE_RANGE_ATTACK.getValue());
        addAttackBody(mplew, chr, skill, skilllevel, stance, numAttackedAndDamage, 0, damage, speed, direction, display);
        return mplew.getPacket();
    }

    public static byte[] getCoconutScore(int teamRed, int teamBlue) {
        final MaplePacketWriter mplew = new MaplePacketWriter(6);
        mplew.writeShort(SendOpcode.COCONUT_SCORE.getValue());
        mplew.writeShort(teamRed);
        mplew.writeShort(teamBlue);
        return mplew.getPacket();
    }

    public static byte[] getPetActionCommand(int playerID, byte petSlot, byte type, byte action, boolean failed) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PET_COMMAND.getValue());
        mplew.writeInt(playerID);
        mplew.write(petSlot);
        mplew.write((type == 1 || failed) ? 1 : 0);
        mplew.write(action);
        if (action == 1) {
            mplew.write(0);
        } else {
            mplew.writeBoolean(true);
            mplew.write(action);
        }
        return mplew.getPacket();
    }

    public static byte[] completeQuest(short quest, long time) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(quest);
        mplew.write(2);
        mplew.writeLong(time);
        return mplew.getPacket();
    }

    /**
     * Gets a control monster packet.
     *
     * @param life     The monster to give control to.
     * @param newSpawn Is it a new spawn?
     * @param aggro    Aggressive monster?
     * @return The monster control packet.
     */
    public static byte[] controlMonster(MapleMonster life, boolean newSpawn, boolean aggro) {
        return spawnMonsterInternal(life, true, newSpawn, aggro, 0, false);
    }

    public static byte[] customPacket(String packet) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.write(HexTool.getByteArrayFromHexString(packet));
        return mplew.getPacket();
    }

    public static byte[] customPacket(byte[] packet) {
        final MaplePacketWriter mplew = new MaplePacketWriter(packet.length);
        mplew.write(packet);
        return mplew.getPacket();
    }

    public static byte[] damageMonster(int oid, int damage) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.writeInt(damage);
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] damagePlayer(int skill, int monsteridfrom, int cid, int damage, int fake, int direction, boolean pgmr, int pgmr_1, boolean is_pg, int oid, int pos_x, int pos_y) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.DAMAGE_PLAYER.getValue());
        mplew.writeInt(cid);
        mplew.write(skill);
        mplew.writeInt(damage);
        mplew.writeInt(monsteridfrom);
        mplew.write(direction);
        if (pgmr) {
            mplew.write(pgmr_1);
            mplew.write(is_pg ? 1 : 0);
            mplew.writeInt(oid);
            mplew.write(6);
            mplew.writeShort(pos_x);
            mplew.writeShort(pos_y);
            mplew.write(0);
        } else {
            mplew.writeShort(0);
        }
        mplew.writeInt(damage);
        if (fake > 0) {
            mplew.writeInt(fake);
        }
        return mplew.getPacket();
    }

    public static byte[] damageSummon(int cid, int summonSkillId, int damage, int unkByte, int monsterIdFrom) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SUMMONED_HIT.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(summonSkillId);
        mplew.write(unkByte);
        mplew.writeInt(damage);
        mplew.writeInt(monsterIdFrom);
        mplew.write(0);
        return mplew.getPacket();
    }

    /**
     * state 0 = del ok state 12 = invalid bday state 14 = incorrect pic
     *
     * @param cid
     * @param state
     * @return
     */
    public static byte[] deleteCharResponse(int cid, int state) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.DELETE_CHAR_RESPONSE.getValue());
        mplew.writeInt(cid);
        mplew.write(state);
        return mplew.getPacket();
    }

    /**
     * 'Char' has denied your guild invitation.
     *
     * @param charname
     * @return
     */
    public static byte[] denyGuildInvitation(String charname) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x37);
        mplew.writeMapleString(charname);
        return mplew.getPacket();
    }

    public static byte[] destroyHiredMerchant(int id) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.DESTROY_HIRED_MERCHANT.getValue());
        mplew.writeInt(id);
        return mplew.getPacket();
    }

    public static byte[] destroyReactor(MapleReactor reactor) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        Point pos = reactor.getPosition();
        mplew.writeShort(SendOpcode.REACTOR_DESTROY.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.write(reactor.getState());
        mplew.writeLocation(pos);
        return mplew.getPacket();
    }

    public static byte[] disableMinimap() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.ADMIN_RESULT.getValue());
        mplew.writeShort(0x1C);
        return mplew.getPacket();
    }

    public static byte[] disableUI(boolean enable) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.DISABLE_UI.getValue());
        mplew.write(enable ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] disbandAlliance(int alliance) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x1D);
        mplew.writeInt(alliance);
        return mplew.getPacket();
    }

    public static byte[] dojoWarpUp() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.DOJO_WARP_UP.getValue());
        mplew.write(0);
        mplew.write(6);
        return mplew.getPacket();
    }

    private static int doubleToShortBits(double d) {
        return (int) (Double.doubleToLongBits(d) >> 48);
    }

    public static byte[] dropItemFromMapObject(MapleMapItem drop, Point dropfrom, Point dropto, byte mod) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.DROP_ITEM_FROM_MAPOBJECT.getValue());
        mplew.write(mod);
        mplew.writeInt(drop.getObjectId());
        mplew.writeBoolean(drop.getMeso() > 0); // 1 mesos, 0 item, 2 and above all item meso bag,
        mplew.writeInt(drop.getItemId()); // drop object ID
        mplew.writeInt(drop.getOwner()); // owner charid/paryid :)
        mplew.write(drop.getDropType()); // 0 = timeout for non-owner, 1 = timeout for non-owner's party, 2 = FFA, 3 = explosive/FFA
        mplew.writeLocation(dropto);
        mplew.writeInt(drop.getDropType() == 0 ? drop.getOwner() : 0); //test

        if (mod != 2) {
            mplew.writeLocation(dropfrom);
            mplew.writeShort(0);//Fh?
        }
        if (drop.getMeso() == 0) {
            mplew.writeLong(getTime(drop.getItem().getExpiration()));
        }
        mplew.write(drop.isPlayerDrop() ? 0 : 1); //pet EQP pickup
        return mplew.getPacket();
    }

    public static byte[] earnTitleMessage(String msg) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SCRIPT_PROGRESS_MESSAGE.getValue());
        mplew.writeMapleString(msg);
        return mplew.getPacket();
    }

    /**
     * Gets an empty stat update.
     *
     * @return The empy stat update packet.
     */
    public static byte[] enableActions() {
        return updatePlayerStats(EMPTY_STATUPDATE, true, null);
    }

    public static byte[] enableCSUse() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.write(0x12);
        mplew.skip(6);
        return mplew.getPacket();
    }

    public static byte[] enableReport() { // by snow
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.CLAIM_STATUS_CHANGED.getValue());
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] getMapleTvSendMessage() {
        final MaplePacketWriter mplew = new MaplePacketWriter(7);
        mplew.writeShort(SendOpcode.MAPLE_TV_SEND_MESSAGE.getValue());
        mplew.writeInt(0);
        mplew.write(0);
        return mplew.getPacket();
    }

    private static void encodeEvanSkillPoints(MaplePacketWriter w, int jobID) {
        int nJobLevel = 0;
        if (jobID >= 2200) {
            nJobLevel++;
        }
        if (jobID >= 2210) {
            nJobLevel++;
        }
        nJobLevel += (jobID % 10);
        w.write(nJobLevel);
        for (int i = 1; i <= nJobLevel; i++) {
            w.write(i);
            w.write(0);
        }
    }

    public static byte[] environmentChange(String env, int mode) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FIELD_EFFECT.getValue());
        mplew.write(mode);
        mplew.writeMapleString(env);
        return mplew.getPacket();
    }

    public static byte[] facialExpression(MapleCharacter from, int expression) {
        final MaplePacketWriter mplew = new MaplePacketWriter(10);
        mplew.writeShort(SendOpcode.FACIAL_EXPRESSION.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(expression);
        return mplew.getPacket();
    }

    public static byte[] findMerchantResponse(boolean map, int extra) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.ADMIN_RESULT.getValue());
        mplew.write(0x13);
        mplew.write(map ? 0 : 1); //00 = mapid, 01 = ch
        if (map) {
            mplew.writeInt(extra);
        } else {
            mplew.write(extra); //-1 = unable to find
        }
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] getInventoryGatherItems(int inv) {
        final MaplePacketWriter mplew = new MaplePacketWriter(4);
        mplew.writeShort(SendOpcode.GATHER_ITEM_RESULT.getValue());
        mplew.write(0);
        mplew.write(inv);
        return mplew.getPacket();
    }

    public static byte[] getInventorySortItems(int inv) {
        final MaplePacketWriter mplew = new MaplePacketWriter(4);
        mplew.writeShort(SendOpcode.SORT_ITEM_RESULT.getValue());
        mplew.write(0);
        mplew.write(inv);
        return mplew.getPacket();
    }

    public static byte[] forfeitQuest(short quest) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(quest);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] fredrickMessage(byte operation) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FREDRICK_MESSAGE.getValue());
        mplew.write(operation);
        return mplew.getPacket();
    }

    /**
     * Sends the Gachapon green message when a user uses a gachapon ticket.
     *
     * @param item
     * @param town
     * @param player
     * @return
     */
    public static byte[] gachaponMessage(Item item, String town, MapleCharacter player) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SERVERMESSAGE.getValue());
        mplew.write(0x0B);
        mplew.writeMapleString(player.getName() + " : got a(n)");
        mplew.writeInt(0); //random?
        mplew.writeMapleString(town);
        addItemInfo(mplew, item, true);
        return mplew.getPacket();
    }

    public static byte[] genericGuildMessage(byte code) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(code);
        return mplew.getPacket();
    }

    /**
     * Gets a login failed packet.
     * <p>
     * Possible values for <code>reason</code>:<br> 2: ID deleted or blocked<br> 3: ID deleted or blocked<br> 4:
     * Incorrect password<br> 5: Not a registered id<br> 6: Trouble logging into the game?<br> 7: Already logged in<br>
     * 8: Trouble logging into the game?<br> 9: Trouble logging into the game?<br> 10: Cannot process so many
     * connections<br> 11: Only users older than 20 can use this channel<br> 12: Trouble logging into the game?<br> 13:
     * Unable to log on as master at this ip<br> 14: Wrong gateway or personal info and weird korean button<br> 15:
     * Processing request with that korean button!<br> 16: Please verify your account through email...<br> 17: Wrong
     * gateway or personal info<br> 21: Please verify your account through email...<br> 23: Crashes<br> 25: Maple Europe
     * notice =[ FUCK YOU NEXON<br> 27: Some weird full client notice, probably for trial versions<br>
     *
     * @param reason The reason logging in failed.
     * @return The login failed packet.
     */
    public static byte[] getAfterLoginError(int reason) {//same as above o.o
        final MaplePacketWriter mplew = new MaplePacketWriter(8);
        mplew.writeShort(SendOpcode.SELECT_CHARACTER_BY_VAC.getValue());
        mplew.writeShort(reason);//using other types then stated above = CRASH
        return mplew.getPacket();
    }

    public static byte[] getAllianceInfo(MapleAlliance alliance) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0C);
        mplew.write(1);
        mplew.writeInt(alliance.getId());
        mplew.writeMapleString(alliance.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleString(alliance.getRankTitle(i));
        }
        mplew.write(alliance.getGuilds().size());
        mplew.writeInt(2); // probably capacity
        for (Integer guild : alliance.getGuilds()) {
            mplew.writeInt(guild);
        }
        mplew.writeMapleString(alliance.getNotice());
        return mplew.getPacket();
    }

    public static byte[] getAuthSuccess(MapleClient c) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.LOGIN_STATUS.getValue());
        mplew.writeInt(0);
        mplew.writeShort(0);
        mplew.writeInt(c.getAccID()); //user id
        mplew.write(c.getGender());
        mplew.writeBoolean(c.getGMLevel() > 0); //admin byte
        short toWrite = (short) (c.getGMLevel() * 64);
        //toWrite = toWrite |= 0x100; only in higher versions
        mplew.write(0);//0x80 is admin, 0x20 and 0x40 = subgm
        mplew.writeBoolean(c.getGMLevel() > 0);
        //mplew.writeShort(toWrite > 0x80 ? 0x80 : toWrite); only in higher versions...
        mplew.writeMapleString(c.getAccountName());
        mplew.write(0);
        mplew.write(0); //isquietbanned
        mplew.writeLong(0);//isquietban time
        mplew.writeLong(c.getSessionId()); //creation time
        mplew.writeInt(0);
        mplew.writeShort(2);//PIN

        return mplew.getPacket();
    }

    /**
     * Sends a Avatar Super Megaphone packet.
     *
     * @param chr     The character name.
     * @param medal   The medal text.
     * @param channel Which channel.
     * @param itemId  Which item used.
     * @param message The message sent.
     * @param ear     Whether or not the ear is shown for whisper.
     * @return
     */
    public static byte[] getAvatarMega(MapleCharacter chr, String medal, int channel, int itemId, String[] message, boolean ear) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SET_AVATAR_MEGAPHONE.getValue());
        mplew.writeInt(itemId);
        mplew.writeMapleString(medal + chr.getName());
        for (String s : message) {
            mplew.writeMapleString(s);
        }
        mplew.writeInt(channel - 1); // channel
        mplew.writeBoolean(ear);
        addCharLook(mplew, chr, true);
        return mplew.getPacket();
    }

    public static byte[] getBuddyFindResult(MapleCharacter target, byte resultType) {
        MaplePacketWriter writer = new MaplePacketWriter();
        writer.writeShort(SendOpcode.WHISPER.getValue());
        writer.write(0x48);
        writer.writeMapleString(target.getName());
        writer.write(resultType);
        if (resultType == 1) {
            writer.writeInt(target.getMapId());
            writer.write(new byte[8]);
        } else {
            writer.writeInt(target.getClient().getChannel() - 1);
        }
        return writer.getPacket();
    }

    /**
     * Gets a packet telling the client the IP of the new channel.
     *
     * @param inetAddr The InetAddress of the requested channel server.
     * @param port     The port the channel is on.
     * @return The server IP packet.
     */
    public static byte[] getChannelChange(InetAddress inetAddr, int port) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CHANGE_CHANNEL.getValue());
        mplew.write(1);
        byte[] addr = inetAddr.getAddress();
        mplew.write(addr);
        mplew.writeShort(port);
        return mplew.getPacket();
    }

    /**
     * Gets character info for a character.
     *
     * @param chr The character to get info about.
     * @return The character info packet.
     */
    public static byte[] getCharInfo(MapleCharacter chr) {
        MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.SET_FIELD.getValue());
        w.writeInt(chr.getClient().getChannel() - 1);
        w.write(1);
        w.write(1);
        w.writeShort(0);
        for (int i = 0; i < 3; i++) {
            w.writeInt(Randomizer.nextInt());
        }
        encodePlayerData(w, chr);
        w.writeLong(getTime(System.currentTimeMillis()));
        return w.getPacket();
    }

    /**
     * Gets a packet with a list of characters.
     *
     * @param c        The MapleClient to load characters of.
     * @param serverId The ID of the server requested.
     * @return The character list packet.
     */
    public static byte[] getCharList(MapleClient c, int serverId) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CHARLIST.getValue());
        mplew.write(0);
        List<MapleCharacter> chars = c.loadCharacters();
        mplew.write((byte) chars.size());
        for (MapleCharacter chr : chars) {
            addCharEntry(mplew, chr, false);
        }
        if (ServerConstants.ENABLE_PIC) {
            mplew.write(c.getPic() == null || c.getPic().length() == 0 ? 0 : 1);
        } else {
            mplew.write(2);
        }

        mplew.writeInt(c.getCharacterSlots());
        return mplew.getPacket();
    }

    /**
     * Gets a general chat packet.
     *
     * @param cidfrom The character ID who sent the chat.
     * @param text    The text of the chat.
     * @param hide    hide the message from the chat box
     * @return The general chat packet.
     */
    public static byte[] getChatText(int cidfrom, String text, boolean gm, boolean hide) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CHATTEXT.getValue());
        mplew.writeInt(cidfrom);
        mplew.writeBoolean(gm);
        mplew.writeMapleString(text);
        mplew.writeBoolean(hide);
        return mplew.getPacket();
    }

    public static byte[] getClock(int time) { // time in seconds
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CLOCK.getValue());
        mplew.write(2); // clock type. if you send 3 here you have to send another byte (which does not matter at all) before the timestamp
        mplew.writeInt(time);
        return mplew.getPacket();
    }

    public static byte[] getClockTime(int hour, int min, int sec) { // Current Time
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CLOCK.getValue());
        mplew.write(1); //Clock-Type
        mplew.write(hour);
        mplew.write(min);
        mplew.write(sec);
        return mplew.getPacket();
    }

    public static byte[] getDimensionalMirror(String talk) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.NPC_TALK.getValue());
        mplew.write(4); // ?
        mplew.writeInt(9010022);
        mplew.write(0x0E);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeMapleString(talk);
        return mplew.getPacket();
    }

    public static byte[] getDojoInfo(String info) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(10);
        mplew.write(new byte[]{(byte) 0xB7, 4});//QUEST ID f5
        mplew.writeMapleString(info);
        return mplew.getPacket();
    }

    public static byte[] getDojoInfoMessage(String message) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(9);
        mplew.writeMapleString(message);
        return mplew.getPacket();
    }

    /**
     * Gets a packet saying that the server list is over.
     *
     * @return The end of server list packet.
     */
    public static byte[] getEndOfServerList() {
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.SERVERLIST.getValue());
        mplew.write(0xFF);
        return mplew.getPacket();
    }

    public static byte[] getPartyResult(int result) {
        if (result == 36) throw new IllegalArgumentException("Use getPartyMessage(String)");
        MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        w.write(result);
        return w.getPacket();
    }

    public static byte[] getPartyResultMessage(String content) {
        MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        w.write(36);
        w.writeMapleString(content);
        return w.getPacket();
    }

    public static byte[] setObjectState(Collection<Pair<String, Integer>> objects) {
        MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.FIELD_OBSTACLE_ONOFF_STATUS.getValue());
        w.writeInt(objects.size());
        for (Pair<String, Integer> pair : objects) {
            w.writeMapleString(pair.getLeft());
            w.writeInt(pair.getRight());
        }
        return w.getPacket();
    }

    public static byte[] setObjectState(String name, int state) {
        MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.SET_OBJECT_STATE.getValue());
        w.writeMapleString(name);
        w.writeInt(state);
        return w.getPacket();
    }

    public static byte[] setSessionValue(String info, int amount) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SESSION_VALUE.getValue());
        mplew.writeMapleString(info);
        mplew.writeMapleString(Integer.toString(amount));
        return mplew.getPacket();
    }

    public static byte[] getFamilyInfo(MapleFamilyEntry f) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FAMILY_INFO_RESULT.getValue());
        mplew.writeInt(f.getReputation()); // cur rep left
        mplew.writeInt(f.getTotalReputation()); // tot rep left
        mplew.writeInt(f.getTodaysRep()); // todays rep
        mplew.writeShort(f.getJuniors()); // juniors added
        mplew.writeShort(f.getTotalJuniors()); // juniors allowed
        mplew.writeShort(0); //Unknown
        mplew.writeInt(f.getId()); // id?
        mplew.writeMapleString(f.getFamilyName());
        mplew.writeInt(0);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    /**
     * @param target
     * @param mapid
     * @param MTSmapCSchannel 0: MTS 1: Map 2: CS 3: Different Channel
     * @return
     */
    public static byte[] getFindReply(String target, int mapid, int MTSmapCSchannel) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.WHISPER.getValue());
        mplew.write(9);
        mplew.writeMapleString(target);
        mplew.write(MTSmapCSchannel); // 0: mts 1: map 2: cs
        mplew.writeInt(mapid); // -1 if mts, cs
        if (MTSmapCSchannel == 1) {
            mplew.write(new byte[8]);
        }
        return mplew.getPacket();
    }

    public static byte[] getFredrick(byte op) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FREDRICK.getValue());
        mplew.write(op);

        switch (op) {
            case 0x24:
                mplew.skip(8);
                break;
            default:
                mplew.write(0);
                break;
        }

        return mplew.getPacket();
    }

    public static byte[] getFredrick(MapleCharacter chr) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FREDRICK.getValue());
        mplew.write(0x23);
        mplew.writeInt(9030000); // Fredrick
        mplew.writeInt(32272); //id
        mplew.skip(5);
        mplew.writeInt(chr.getMerchantMeso());
        mplew.write(0);
        try (Connection con = chr.getClient().getWorldServer().getConnection()) {
            List<Pair<Item, MapleInventoryType>> items = ItemFactory.MERCHANT.loadItems(con, chr.getId(), false);
            mplew.write(items.size());

            for (Pair<Item, MapleInventoryType> item : items) {
                addItemInfo(mplew, item.getLeft(), true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        mplew.skip(3);
        return mplew.getPacket();
    }

    /**
     * Gets a gm effect packet (ie. hide, banned, etc.)
     * <p>
     * Possible values for <code>type</code>:<br> 0x04: You have successfully blocked access.<br> 0x05: The unblocking
     * has been successful.<br> 0x06 with Mode 0: You have successfully removed the name from the ranks.<br> 0x06 with
     * Mode 1: You have entered an invalid character name.<br> 0x10: GM Hide, mode determines whether or not it is
     * on.<br> 0x1E: Mode 0: Failed to send warning Mode 1: Sent warning<br> 0x13 with Mode 0: + mapid 0x13 with Mode 1:
     * + ch (FF = Unable to find merchant)
     *
     * @param type The type
     * @param mode The mode
     * @return The gm effect packet
     */
    public static byte[] getAdminResult(int type, byte mode) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.ADMIN_RESULT.getValue());
        mplew.write(type);
        mplew.write(mode);
        return mplew.getPacket();
    }

    public static byte[] getGPMessage(int gpChange) {
        final MaplePacketWriter mplew = new MaplePacketWriter(7);
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(6);
        mplew.writeInt(gpChange);
        return mplew.getPacket();
    }

    public static byte[] getGuildAlliances(MapleAlliance alliance, MapleClient c) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0D);
        mplew.writeInt(alliance.getGuilds().size());
        for (Integer guild : alliance.getGuilds()) {
            getGuildInfo(mplew, Server.getGuild(guild, c.getWorld(), null));
        }
        return mplew.getPacket();
    }

    private static void getGuildInfo(final MaplePacketWriter mplew, MapleGuild guild) {
        mplew.writeInt(guild.getId());
        mplew.writeMapleString(guild.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleString(guild.getRankTitle(i));
        }
        Collection<MapleGuildCharacter> members = guild.getMembers();
        mplew.write(members.size());
        for (MapleGuildCharacter mgc : members) {
            mplew.writeInt(mgc.getId());
        }
        for (MapleGuildCharacter mgc : members) {
            mplew.writeAsciiString(StringUtil.getRightPaddedStr(mgc.getName(), '\0', 13));
            mplew.writeInt(mgc.getJobId());
            mplew.writeInt(mgc.getLevel());
            mplew.writeInt(mgc.getGuildRank());
            mplew.writeInt(mgc.isOnline() ? 1 : 0);
            mplew.writeInt(guild.getSignature());
            mplew.writeInt(mgc.getAllianceRank());
        }
        mplew.writeInt(guild.getCapacity());
        mplew.writeShort(guild.getLogoBG());
        mplew.write(guild.getLogoBGColor());
        mplew.writeShort(guild.getLogo());
        mplew.write(guild.getLogoColor());
        mplew.writeMapleString(guild.getNotice());
        mplew.writeInt(guild.getGP());
        mplew.writeInt(guild.getAllianceId());
    }

    /**
     * Sends a hello packet.
     *
     * @param mapleVersion The maple client version.
     * @param sendIv       the IV used by the server for sending
     * @param recvIv       the IV used by the server for receiving
     * @return
     */
    public static byte[] getHello(short mapleVersion, byte[] sendIv, byte[] recvIv) {
        final MaplePacketWriter mplew = new MaplePacketWriter(8);
        mplew.writeShort(0x0E);
        mplew.writeShort(mapleVersion);
        mplew.writeShort(1);
        mplew.write(49);
        mplew.write(recvIv);
        mplew.write(sendIv);
        mplew.write(8);
        return mplew.getPacket();
    }

    public static byte[] getHiredMerchant(MapleCharacter chr, HiredMerchant hm, boolean firstTime) {//Thanks Dustin
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.ROOM.value);
        mplew.write(0x05);
        mplew.write(0x04);
        mplew.writeShort(hm.getVisitorSlot(chr) + 1);
        mplew.writeInt(hm.getItemId());
        mplew.writeMapleString("Hired Merchant");
        for (int i = 0; i < 3; i++) {
            if (hm.getVisitors()[i] != null) {
                mplew.write(i + 1);
                addCharLook(mplew, hm.getVisitors()[i], false);
                mplew.writeMapleString(hm.getVisitors()[i].getName());
            }
        }
        mplew.write(-1);
        if (hm.isOwner(chr)) {
            mplew.writeShort(hm.getMessages().size());
            for (int i = 0; i < hm.getMessages().size(); i++) {
                mplew.writeMapleString(hm.getMessages().get(i).getLeft());
                mplew.write(hm.getMessages().get(i).getRight());
            }
        } else {
            mplew.writeShort(0);
        }
        mplew.writeMapleString(hm.getOwner());
        if (hm.isOwner(chr)) {
            mplew.writeInt(hm.getTimeLeft());
            mplew.write(firstTime ? 1 : 0);
            //List<SoldItem> sold = hm.getSold();
            mplew.write(0);//sold.size()
					   /*for (SoldItem s : sold) { fix this
             mplew.writeInt(s.getItemId());
             mplew.writeShort(s.getQuantity());
             mplew.writeInt(s.getMesos());
             mplew.writeMapleString(s.getBuyer());
             }*/
            mplew.writeInt(chr.getMerchantMeso());//:D?
        }
        mplew.writeMapleString(hm.getDescription());
        mplew.write(0x10); //TODO SLOTS, which is 16 for most stores...slotMax
        mplew.writeInt(chr.getMeso());
        mplew.write(hm.getItems().size());
        if (hm.getItems().isEmpty()) {
            mplew.write(0);//Hmm??
        } else {
            for (MaplePlayerShopItem item : hm.getItems()) {
                mplew.writeShort(item.getBundles());
                mplew.writeShort(item.getItem().getQuantity());
                mplew.writeInt(item.getPrice());
                addItemInfo(mplew, item.getItem(), true);
            }
        }
        return mplew.getPacket();
    }

    public static byte[] getInventoryFull() {
        return modifyInventory(true, Collections.<ModifyInventory>emptyList());
    }

    public static byte[] getItemMessage(int itemid) {
        final MaplePacketWriter mplew = new MaplePacketWriter(7);
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(7);
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    public static byte[] getKeymap(Map<Integer, MapleKeyBinding> keybindings) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.KEYMAP.getValue());
        mplew.write(0);
        for (int x = 0; x < 90; x++) {
            MapleKeyBinding binding = keybindings.get(x);
            if (binding != null) {
                mplew.write(binding.getType());
                mplew.writeInt(binding.getAction());
            } else {
                mplew.write(0);
                mplew.writeInt(0);
            }
        }
        return mplew.getPacket();
    }

    /**
     * <ol>
     * <li value="3">ID deleted or blocked</li>
     * <li value="4">Incorrect password</li>
     * <li value="5">Not a registered id</li>
     * <li value="6">System error</li>
     * <li value="7">Already logged in</li>
     * <li value="8">System error</li>
     * <li value="9">System error</li>
     * <li value="10">Cannot process so many connections</li>
     * <li value="11">Only users older than 20 can use this channel</li>
     * <li value="13">Unable to log on as master at this ip</li>
     * <li value="14">Wrong gateway or personal info and weird korean button</li>
     * <li value="15">Processing request with that korean button!</li>
     * <li value="16">Please verify your account through email...</li>
     * <li value="17">Wrong gateway or personal info</li>
     * <li value="21">Please verify your account through email...</li>
     * <li value="23">License agreement 25: Maple Europe notice =[ FUCK YOU NEXON</li>
     * <li value="27">Some weird full client notice, probably for trial versions</li>
     * </ol>
     */
    public static byte[] getLoginFailed(int reason) {
        final MaplePacketWriter mplew = new MaplePacketWriter(8);
        mplew.writeShort(SendOpcode.LOGIN_STATUS.getValue());
        mplew.write(reason);
        mplew.write(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] getMacros(SkillMacro[] macros) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MACRO_SYS_DATA_INIT.getValue());
        int count = 0;
        for (int i = 0; i < 5; i++) {
            if (macros[i] != null) {
                count++;
            }
        }
        mplew.write(count);
        for (int i = 0; i < 5; i++) {
            SkillMacro macro = macros[i];
            if (macro != null) {
                mplew.writeMapleString(macro.getName());
                mplew.write(macro.getShout());
                mplew.writeInt(macro.getSkill1());
                mplew.writeInt(macro.getSkill2());
                mplew.writeInt(macro.getSkill3());
            }
        }
        return mplew.getPacket();
    }

    public static byte[] getMatchCard(MapleClient c, MapleMiniGame minigame, boolean owner, int piece) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.ROOM.value);
        mplew.write(2);
        mplew.write(2);
        mplew.write(owner ? 0 : 1);
        mplew.write(0);
        addCharLook(mplew, minigame.getOwner(), false);
        mplew.writeMapleString(minigame.getOwner().getName());
        if (minigame.getVisitor() != null) {
            MapleCharacter visitor = minigame.getVisitor();
            mplew.write(1);
            addCharLook(mplew, visitor, false);
            mplew.writeMapleString(visitor.getName());
        }
        mplew.write(0xFF);
        mplew.write(0);
        mplew.writeInt(2);
        mplew.writeInt(minigame.getOwner().getMiniGamePoints("wins", false));
        mplew.writeInt(minigame.getOwner().getMiniGamePoints("ties", false));
        mplew.writeInt(minigame.getOwner().getMiniGamePoints("losses", false));
        mplew.writeInt(2000);
        if (minigame.getVisitor() != null) {
            MapleCharacter visitor = minigame.getVisitor();
            mplew.write(1);
            mplew.writeInt(2);
            mplew.writeInt(visitor.getMiniGamePoints("wins", false));
            mplew.writeInt(visitor.getMiniGamePoints("ties", false));
            mplew.writeInt(visitor.getMiniGamePoints("losses", false));
            mplew.writeInt(2000);
        }
        mplew.write(0xFF);
        mplew.writeMapleString(minigame.getDescription());
        mplew.write(piece);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] getMatchCardNewVisitor(MapleCharacter c, int slot) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.VISIT.value);
        mplew.write(slot);
        addCharLook(mplew, c, false);
        mplew.writeMapleString(c.getName());
        mplew.writeInt(1);
        mplew.writeInt(c.getMiniGamePoints("wins", false));
        mplew.writeInt(c.getMiniGamePoints("ties", false));
        mplew.writeInt(c.getMiniGamePoints("losses", false));
        mplew.writeInt(2000);
        return mplew.getPacket();
    }

    public static byte[] getMatchCardOwnerWin(MapleMiniGame game) {
        return getMiniGameResult(game, 1, 0, 0, 1, 0, false);
    }

    public static byte[] getMatchCardSelect(MapleMiniGame game, int turn, int slot, int firstslot, int type) {
        final MaplePacketWriter mplew = new MaplePacketWriter(6);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.SELECT_CARD.value);
        mplew.write(turn);
        if (turn == 1) {
            mplew.write(slot);
        } else if (turn == 0) {
            mplew.write(slot);
            mplew.write(firstslot);
            mplew.write(type);
        }
        return mplew.getPacket();
    }

    public static byte[] getMatchCardStart(MapleMiniGame game, int loser) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.START.value);
        mplew.write(loser);
        mplew.write(game.getMatchesToWin() * 2);
        int last = (game.getMatchesToWin() * 2) + 1;
        for (int i = 1; i < last; i++) {
            mplew.writeInt(game.getCardId(i));
        }
        return mplew.getPacket();
    }

    public static byte[] getMatchCardTie(MapleMiniGame game) {
        return getMiniGameResult(game, 0, 0, 1, 3, 0, false);
    }

    public static byte[] getMatchCardVisitorWin(MapleMiniGame game) {
        return getMiniGameResult(game, 0, 1, 0, 2, 0, false);
    }

    public static byte[] getMiniGame(MapleClient c, MapleMiniGame game, boolean owner) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.ROOM.value);
        mplew.write(1);
        mplew.write(0);
        mplew.write(owner ? 0 : 1);
        mplew.write(0);
        addCharLook(mplew, game.getOwner(), false);
        mplew.writeMapleString(game.getOwner().getName());
        if (game.getVisitor() != null) {
            MapleCharacter visitor = game.getVisitor();
            mplew.write(1);
            addCharLook(mplew, visitor, false);
            mplew.writeMapleString(visitor.getName());
        }
        mplew.write(0xFF);
        mplew.write(0);
        mplew.writeInt(1);
        mplew.writeInt(game.getOwner().getMiniGamePoints("wins", true));
        mplew.writeInt(game.getOwner().getMiniGamePoints("ties", true));
        mplew.writeInt(game.getOwner().getMiniGamePoints("losses", true));
        mplew.writeInt(2000);
        if (game.getVisitor() != null) {
            MapleCharacter visitor = game.getVisitor();
            mplew.write(1);
            mplew.writeInt(1);
            mplew.writeInt(visitor.getMiniGamePoints("wins", true));
            mplew.writeInt(visitor.getMiniGamePoints("ties", true));
            mplew.writeInt(visitor.getMiniGamePoints("losses", true));
            mplew.writeInt(2000);
        }
        mplew.write(0xFF);
        mplew.writeMapleString(game.getDescription());
        mplew.write(game.getPieceType());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameClose() {
        final MaplePacketWriter mplew = new MaplePacketWriter(5);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.EXIT.value);
        mplew.write(1);
        mplew.write(3);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameDenyTie(MapleMiniGame game) {
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.ANSWER_TIE.value);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameFull() {
        final MaplePacketWriter mplew = new MaplePacketWriter(5);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.ROOM.value);
        mplew.write(0);
        mplew.write(2);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameMoveOmok(MapleMiniGame game, int move1, int move2, int move3) {
        final MaplePacketWriter mplew = new MaplePacketWriter(12);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.MOVE_OMOK.value);
        mplew.writeInt(move1);
        mplew.writeInt(move2);
        mplew.write(move3);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameNewVisitor(MapleCharacter c, int slot) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.VISIT.value);
        mplew.write(slot);
        addCharLook(mplew, c, false);
        mplew.writeMapleString(c.getName());
        mplew.writeInt(1);
        mplew.writeInt(c.getMiniGamePoints("wins", true));
        mplew.writeInt(c.getMiniGamePoints("ties", true));
        mplew.writeInt(c.getMiniGamePoints("losses", true));
        mplew.writeInt(2000);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameOwnerForfeit(MapleMiniGame game) {
        return getMiniGameResult(game, 0, 1, 0, 2, 1, true);
    }

    public static byte[] getMiniGameOwnerWin(MapleMiniGame game) {
        return getMiniGameResult(game, 0, 1, 0, 1, 0, true);
    }

    public static byte[] getMiniGameReady(MapleMiniGame game) {
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.READY.value);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameRemoveVisitor() {
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.EXIT.value);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameRequestTie(MapleMiniGame game) {
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.REQUEST_TIE.value);
        return mplew.getPacket();
    }

    private static byte[] getMiniGameResult(MapleMiniGame game, int win, int lose, int tie, int result, int forfeit, boolean omok) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.GET_RESULT.value);
        if (tie == 0 && forfeit != 1) {
            mplew.write(0);
        } else if (tie == 1) {
            mplew.write(1);
        } else if (forfeit == 1) {
            mplew.write(2);
        }
        mplew.write(game.getLoser()); // owner
        mplew.writeInt(1); // unknown
        mplew.writeInt(game.getOwner().getMiniGamePoints("wins", omok) + win); // wins
        mplew.writeInt(game.getOwner().getMiniGamePoints("ties", omok) + tie); // ties
        mplew.writeInt(game.getOwner().getMiniGamePoints("losses", omok) + lose); // losses
        mplew.writeInt(2000); // points
        mplew.writeInt(1); // start of visitor; unknown
        mplew.writeInt(game.getVisitor().getMiniGamePoints("wins", omok) + lose); // wins
        mplew.writeInt(game.getVisitor().getMiniGamePoints("ties", omok) + tie); // ties
        mplew.writeInt(game.getVisitor().getMiniGamePoints("losses", omok) + win); // losses
        mplew.writeInt(2000); // points
        game.getOwner().setMiniGamePoints(game.getVisitor(), result, omok);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameSkipOwner(MapleMiniGame game) {
        final MaplePacketWriter mplew = new MaplePacketWriter(4);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.SKIP.value);
        mplew.write(0x01);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameSkipVisitor(MapleMiniGame game) {
        final MaplePacketWriter mplew = new MaplePacketWriter(4);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.writeShort(CommunityActions.SKIP.value);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameStart(MapleMiniGame game, int loser) {
        final MaplePacketWriter mplew = new MaplePacketWriter(4);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.START.value);
        mplew.write(loser);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameTie(MapleMiniGame game) {
        return getMiniGameResult(game, 0, 0, 1, 3, 0, true);
    }

    public static byte[] getMiniGameUnReady(MapleMiniGame game) {
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.UN_READY.value);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameVisitorForfeit(MapleMiniGame game) {
        return getMiniGameResult(game, 1, 0, 0, 1, 1, true);
    }

    public static byte[] getMiniGameVisitorWin(MapleMiniGame game) {
        return getMiniGameResult(game, 1, 0, 0, 2, 0, true);
    }

    public static byte[] getMultiMegaphone(String[] messages, int channel, boolean showEar) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SERVERMESSAGE.getValue());
        mplew.write(0x0A);
        if (messages[0] != null) {
            mplew.writeMapleString(messages[0]);
        }
        mplew.write(messages.length);
        for (int i = 1; i < messages.length; i++) {
            if (messages[i] != null) {
                mplew.writeMapleString(messages[i]);
            }
        }
        for (int i = 0; i < 10; i++) {
            mplew.write(channel - 1);
        }
        mplew.write(showEar ? 1 : 0);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] getNPCShop(MapleClient c, int sid, List<MapleShopItem> items) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.OPEN_NPC_SHOP.getValue());
        mplew.writeInt(sid);
        mplew.writeShort(items.size()); // item count
        for (MapleShopItem item : items) {
            mplew.writeInt(item.getItemId());
            mplew.writeInt(item.getPrice());
            mplew.writeInt(item.getPrice() == 0 ? item.getPitch() : 0); //Perfect Pitch
            mplew.writeInt(0); //Can be used x minutes after purchase
            mplew.writeInt(0); //Hmm
            if (!ItemConstants.isRechargable(item.getItemId())) {
                mplew.writeShort(1); // stacksize o.o
                mplew.writeShort(item.getBuyable());
            } else {
                mplew.writeShort(0);
                mplew.writeInt(0);
                mplew.writeShort(doubleToShortBits(ii.getPrice(item.getItemId())));
                mplew.writeShort(ii.getSlotMax(c, item.getItemId()));
            }
        }
        return mplew.getPacket();
    }

    /**
     * Possible values for <code>speaker</code>:<br> 0: Npc talking (left)<br> 1: Npc talking (right)<br> 2: Player
     * talking (left)<br> 3: Player talking (left)<br>
     *
     * @param npc      Npcid
     * @param msgType
     * @param talk
     * @param endBytes
     * @param speaker
     * @return
     */
    public static byte[] getNPCTalk(int npc, byte msgType, String talk, String endBytes, byte speaker) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.NPC_TALK.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.write(msgType);
        mplew.write(speaker);
        mplew.writeMapleString(talk);
        mplew.write(HexTool.getByteArrayFromHexString(endBytes));
        return mplew.getPacket();
    }

    public static byte[] getNPCTalkNum(int npc, String talk, int def, int min, int max) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.NPC_TALK.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.write(3);
        mplew.write(0); //speaker
        mplew.writeMapleString(talk);
        mplew.writeInt(def);
        mplew.writeInt(min);
        mplew.writeInt(max);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] getNPCTalkStyle(int npc, String talk, int styles[]) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.NPC_TALK.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.write(7);
        mplew.write(0); //speaker
        mplew.writeMapleString(talk);
        mplew.write(styles.length);
        for (int style : styles) {
            mplew.writeInt(style);
        }
        return mplew.getPacket();
    }

    public static byte[] getNPCTalkText(int npc, String talk, String def) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.NPC_TALK.getValue());
        mplew.write(4); // Doesn't matter
        mplew.writeInt(npc);
        mplew.write(2);
        mplew.write(0); //speaker
        mplew.writeMapleString(talk);
        mplew.writeMapleString(def);//:D
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] getNpcAction(int objectID, byte action) {
        MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.NPC_ACTION.getValue());
        mplew.writeInt(objectID);
        mplew.write(action);
        mplew.write(0xFF);
        return mplew.getPacket();
    }

    public static byte[] getNpcChat(int objectID, byte chat) {
        MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.NPC_ACTION.getValue());
        mplew.writeInt(objectID);
        mplew.write(-1);
        mplew.write(chat);
        return mplew.getPacket();
    }

    public static byte[] getAccountBanned(Timestamp temporaryBan) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.LOGIN_STATUS.getValue());
        mplew.write(2); // login response type
        mplew.write(0);
        mplew.writeInt(0); // not used
        mplew.write(20);
        mplew.writeLong(getTime(temporaryBan == null ? -1 : temporaryBan.getTime()));
        return mplew.getPacket();
    }

    /**
     * Sends a ping packet.
     *
     * @return The packet.
     */
    public static byte[] getKeepAliveRequest() {
        final MaplePacketWriter mplew = new MaplePacketWriter(2);
        mplew.writeShort(SendOpcode.PING.getValue());
        return mplew.getPacket();
    }

    public static byte[] getPlayerNPC(PlayerNPC npc) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.IMITATED_NPC_DATA.getValue());
        mplew.write(0x01);
        mplew.writeInt(npc.getId());
        mplew.writeMapleString(npc.getName());
        mplew.write(0); // direction
        mplew.write(npc.getSkin());
        mplew.writeInt(npc.getFace());
        mplew.write(0);
        mplew.writeInt(npc.getHair());
        Map<Short, Integer> equip = npc.getEquips();
        Map<Short, Integer> myEquip = new LinkedHashMap<>();
        for (short position : equip.keySet()) {
            short pos = (short) (position * -1);
            if (pos > 100) {
                pos -= 100;
                myEquip.put(pos, equip.get(position));
            } else {
                myEquip.computeIfAbsent(pos, k -> equip.get(position));
            }
        }
        for (Entry<Short, Integer> entry : myEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.writeShort(-1);
        Integer cWeapon = equip.get((byte) -111);
        if (cWeapon != null) {
            mplew.writeInt(cWeapon);
        } else {
            mplew.writeInt(0);
        }
        for (int i = 0; i < 12; i++) {
            mplew.write(0);
        }
        return mplew.getPacket();
    }

    /**
     * @param c
     * @param shop
     * @param owner
     * @return
     */
    public static byte[] getPlayerShop(MapleClient c, MaplePlayerShop shop, boolean owner) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.ROOM.value);
        mplew.write(4);
        mplew.write(4);
        mplew.write(owner ? 0 : 1);
        mplew.write(0);
        addCharLook(mplew, shop.getOwner(), false);
        mplew.writeMapleString(shop.getOwner().getName());
        mplew.write(1);
        addCharLook(mplew, shop.getOwner(), false);
        mplew.writeMapleString(shop.getOwner().getName());
        mplew.write(0xFF);
        mplew.writeMapleString(shop.getDescription());
        List<MaplePlayerShopItem> items = shop.getItems();
        mplew.write(0x10);
        mplew.write(items.size());
        for (MaplePlayerShopItem item : items) {
            mplew.writeShort(item.getBundles());
            mplew.writeShort(item.getItem().getQuantity());
            mplew.writeInt(item.getPrice());
            addItemInfo(mplew, item.getItem(), true);
        }
        return mplew.getPacket();
    }

    public static byte[] getPlayerShopChat(MapleCharacter c, String chat, boolean owner) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.CHAT.value);
        mplew.write(CommunityActions.CHAT_THING.value);
        mplew.write(owner ? 0 : 1);
        mplew.writeMapleString(c.getName() + " : " + chat);
        return mplew.getPacket();
    }

    public static byte[] getPlayerShopChat(MapleCharacter c, String chat, byte slot) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.CHAT.value);
        mplew.write(CommunityActions.CHAT_THING.value);
        mplew.write(slot);
        mplew.writeMapleString(c.getName() + " : " + chat);
        return mplew.getPacket();
    }

    public static byte[] getPlayerShopItemUpdate(MaplePlayerShop shop) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.UPDATE_MERCHANT.value);
        mplew.write(shop.getItems().size());
        for (MaplePlayerShopItem item : shop.getItems()) {
            mplew.writeShort(item.getBundles());
            mplew.writeShort(item.getItem().getQuantity());
            mplew.writeInt(item.getPrice());
            addItemInfo(mplew, item.getItem(), true);
        }
        return mplew.getPacket();
    }

    public static byte[] getPlayerShopNewVisitor(MapleCharacter c, int slot) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.VISIT.value);
        mplew.write(slot);
        addCharLook(mplew, c, false);
        mplew.writeMapleString(c.getName());
        return mplew.getPacket();
    }

    public static byte[] getPlayerShopRemoveVisitor(int slot) {
        final MaplePacketWriter mplew = new MaplePacketWriter(4);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.EXIT.value);
        if (slot > 0) {
            mplew.write(slot);
        }
        return mplew.getPacket();
    }

    public static byte[] getRelogResponse() {
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.RELOG_RESPONSE.getValue());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] getScrollEffect(int chr, ScrollResult scrollSuccess, boolean legendarySpirit) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_SCROLL_EFFECT.getValue());
        mplew.writeInt(chr);
        switch (scrollSuccess) {
            case SUCCESS:
                mplew.writeShort(1);
                mplew.writeShort(legendarySpirit ? 1 : 0);
                break;
            case FAIL:
                mplew.writeShort(0);
                mplew.writeShort(legendarySpirit ? 1 : 0);
                break;
            case CURSE:
                mplew.write(0);
                mplew.write(1);
                mplew.writeShort(legendarySpirit ? 1 : 0);
                break;
        }
        return mplew.getPacket();
    }

    public static byte[] getSeniorMessage(String name) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FAMILY_JOIN_ACCEPTED.getValue());
        mplew.writeMapleString(name);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client the IP of the channel server.
     *
     * @param inetAddr The InetAddress of the requested channel server.
     * @param port     The port the channel is on.
     * @param clientId The ID of the client.
     * @return The server IP packet.
     */
    public static byte[] getServerIP(InetAddress inetAddr, int port, int clientId) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SERVER_IP.getValue());
        mplew.writeShort(0);
        byte[] addr = inetAddr.getAddress();
        mplew.write(addr);
        mplew.writeShort(port);
        mplew.writeInt(clientId);
        mplew.write(new byte[]{0, 0, 0, 0, 0});
        return mplew.getPacket();
    }

    public static byte[] getSelectPlayerFailed(byte error, byte error2) {
        MaplePacketWriter w = new MaplePacketWriter(4);
        w.writeShort(SendOpcode.SERVER_IP.getValue());
        w.write(error);
        w.write(error2);
        return w.getPacket();
    }

    /**
     * Gets a packet detailing a server and its channels.
     *
     * @param serverId
     * @param serverName  The name of the server.
     * @param channelLoad Load of the channel - 1200 seems to be max.
     * @return The server info packet.
     */
    public static byte[] getServerList(int serverId, String serverName, int flag, String eventmsg, List<MapleChannel> channelLoad) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SERVERLIST.getValue());
        mplew.write(serverId);
        mplew.writeMapleString(serverName);
        mplew.write(flag);
        mplew.writeMapleString(eventmsg);
        mplew.write(100); // rate modifier, don't ask O.O!
        mplew.write(0); // event xp * 2.6 O.O!
        mplew.write(100); // rate modifier, don't ask O.O!
        mplew.write(0); // drop rate * 2.6
        mplew.write(0);
        mplew.write(channelLoad.size());
        for (MapleChannel ch : channelLoad) {
            mplew.writeMapleString(serverName + "-" + ch.getId());
            mplew.writeInt((ch.getUserCount() * 1200) / ServerConstants.CHANNEL_LOAD);
            mplew.write(1);
            mplew.writeShort(ch.getId() - 1);
        }
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    /**
     * Gets a packet detailing a server status message.
     * <p>
     * Possible values for <code>status</code>:<br> 0 - Normal<br> 1 - Highly populated<br> 2 - Full
     *
     * @param status The server status.
     * @return The server status packet.
     */
    public static byte[] getServerStatus(int status) {
        final MaplePacketWriter mplew = new MaplePacketWriter(4);
        mplew.writeShort(SendOpcode.SERVERSTATUS.getValue());
        mplew.writeShort(status);
        return mplew.getPacket();
    }

    public static byte[] getShowExpGain(int gain, int equip, int party, boolean inChat, boolean white) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(3); // 3 = exp, 4 = fame, 5 = mesos, 6 = guildpoints
        mplew.writeBoolean(white);
        mplew.writeInt(gain);
        mplew.writeBoolean(inChat);
        mplew.writeInt(0); // monster book bonus (Bonus Event Exp)
        mplew.write(0); // third monster kill event
        mplew.write(0); // RIP byte, this is always a 0
        mplew.writeInt(0); //wedding bonus
        if (inChat) { // quest bonus rate stuff
            mplew.write(0);
        }

        int mod = ServerConstants.PARTY_EXPERIENCE_MOD != 1 ? ServerConstants.PARTY_EXPERIENCE_MOD * 100 : 0;

        mplew.write(mod); //0 = party bonus, 100 = 1x Bonus EXP, 200 = 2x Bonus EXP
        mplew.writeInt(party); // party bonus
        mplew.writeInt(equip); //equip bonus
        mplew.writeInt(0); //Internet Cafe Bonus
        mplew.writeInt(0); //Rainbow Week Bonus
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client to show a fame gain.
     *
     * @param gain How many fame gained.
     * @return The meso gain packet.
     */
    public static byte[] getShowFameGain(int gain) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(4);
        mplew.writeInt(gain);
        return mplew.getPacket();
    }

    public static byte[] getShowInventoryFull() {
        return getShowInventoryStatus(0xff);
    }

    public static byte[] getShowInventoryStatus(int mode) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(0);
        mplew.write(mode);
        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client to show a item gain.
     *
     * @param itemId   The ID of the item gained.
     * @param quantity How many items gained.
     * @return The item gain packet.
     */
    public static byte[] getShowItemGain(int itemId, short quantity) {
        return getShowItemGain(itemId, quantity, false);
    }

    /**
     * Gets a packet telling the client to show an item gain.
     *
     * @param itemId   The ID of the item gained.
     * @param quantity The number of items gained.
     * @param inChat   Show in the chat window?
     * @return The item gain packet.
     */
    public static byte[] getShowItemGain(int itemId, short quantity, boolean inChat) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        if (inChat) {
            mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
            mplew.write(3);
            mplew.write(1);
            mplew.writeInt(itemId);
            mplew.writeInt(quantity);
        } else {
            mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
            mplew.writeShort(0);
            mplew.writeInt(itemId);
            mplew.writeInt(quantity);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client to show a meso gain.
     *
     * @param gain How many mesos gained.
     * @return The meso gain packet.
     */
    public static byte[] getShowMesoGain(int gain) {
        return getShowMesoGain(gain, false);
    }

    /**
     * Gets a packet telling the client to show a meso gain.
     *
     * @param gain   How many mesos gained.
     * @param inChat Show in the chat window?
     * @return The meso gain packet.
     */
    public static byte[] getShowMesoGain(int gain, boolean inChat) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        if (!inChat) {
            mplew.write(0);
            mplew.writeShort(1); //v83
        } else {
            mplew.write(5);
        }
        mplew.writeInt(gain);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static byte[] getShowQuestCompletion(int id) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.QUEST_CLEAR.getValue());
        mplew.writeShort(id);
        return mplew.getPacket();
    }

    public static byte[] getStorage(int npcId, MapleStorage storage) {
        final MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.STORAGE.getValue());
        w.write(22);
        w.writeInt(npcId);

        w.write(storage.getSlotCount());
        w.writeLong(-1);
        w.writeInt(storage.getMoney());
        for (Entry<MapleInventoryType, Set<Item>> entry : storage.entrySet()) {
            w.write(entry.getValue().size());
            entry.getValue().forEach(item -> addItemInfo(w, item, true));
        }
        return w.getPacket();
    }

    private static long getTime(long realTimestamp) {
        if (realTimestamp == -1) {
            return DEFAULT_TIME;//high number ll
        } else if (realTimestamp == -2) {
            return ZERO_TIME;
        } else if (realTimestamp == -3) {
            return PERMANENT;
        }
        return realTimestamp * 10000 + FT_UT_OFFSET;
    }

    public static byte[] getTradeCancel(byte number) {
        final MaplePacketWriter mplew = new MaplePacketWriter(5);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.EXIT.value);
        mplew.write(number);
        mplew.write(2);
        return mplew.getPacket();
    }

    public static byte[] getTradeChat(MapleCharacter c, String chat, boolean owner) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.CHAT.value);
        mplew.write(CommunityActions.CHAT_THING.value);
        mplew.write(owner ? 0 : 1);
        mplew.writeMapleString(c.getName() + " : " + chat);
        return mplew.getPacket();
    }

    public static byte[] getTradeCompletion(byte number) {
        final MaplePacketWriter mplew = new MaplePacketWriter(5);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.EXIT.value);
        mplew.write(number);
        mplew.write(6);
        return mplew.getPacket();
    }

    public static byte[] getTradeConfirmation() {
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.CONFIRM.value);
        return mplew.getPacket();
    }

    public static byte[] getTradeInvite(MapleCharacter c) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.INVITE.value);
        mplew.write(3);
        mplew.writeMapleString(c.getName());
        mplew.write(new byte[]{(byte) 0xB7, (byte) 0x50, 0, 0});
        return mplew.getPacket();
    }

    public static byte[] getTradeItemAdd(byte number, Item item) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.SET_ITEMS.value);
        mplew.write(number);
        mplew.write(item.getPosition());
        addItemInfo(mplew, item, true);
        return mplew.getPacket();
    }

    public static byte[] getTradeMesoSet(byte number, int meso) {
        final MaplePacketWriter mplew = new MaplePacketWriter(8);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.SET_MESO.value);
        mplew.write(number);
        mplew.writeInt(meso);
        return mplew.getPacket();
    }

    public static byte[] getTradePartnerAdd(MapleCharacter c) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.VISIT.value);
        mplew.write(1);
        addCharLook(mplew, c, false);
        mplew.writeMapleString(c.getName());
        return mplew.getPacket();
    }

    public static byte[] getTradeStart(MapleClient c, MapleTrade trade, byte number) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.ROOM.value);
        mplew.write(3);
        mplew.write(2);
        mplew.write(number);
        if (number == 1) {
            mplew.write(0);
            addCharLook(mplew, trade.getPartner().getChr(), false);
            mplew.writeMapleString(trade.getPartner().getChr().getName());
        }
        mplew.write(number);
        addCharLook(mplew, c.getPlayer(), false);
        mplew.writeMapleString(c.getPlayer().getName());
        mplew.write(0xFF);
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client to change maps.
     *
     * @param to         The field to warp to
     * @param spawnPoint The spawn portal number to spawn at
     * @param player     The character warping
     * @return The map change packet.
     */
    public static byte[] getWarpToMap(int mapId, int spawnPoint, MapleCharacter player, Point pos) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SET_FIELD.getValue());
        mplew.writeInt(player.getClient().getChannel() - 1);
        mplew.writeInt(0);//updated
        mplew.write(0);//updated
        mplew.writeInt(mapId);
        mplew.write(spawnPoint);
        mplew.writeShort(player.getHp());
        mplew.write(pos != null ? 1 : 0);
        if (pos != null) {
            mplew.writeInt(pos.x);
            mplew.writeInt(pos.y);
        }
        mplew.writeLong(getTime(System.currentTimeMillis()));
        return mplew.getPacket();
    }

    public static byte[] getWhisper(String sender, int channel, String text) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.WHISPER.getValue());
        mplew.write(0x12);
        mplew.writeMapleString(sender);
        mplew.writeShort(channel - 1); // I guess this is the channel
        mplew.writeMapleString(text);
        return mplew.getPacket();
    }

    /**
     * @param target name of the target character
     * @param reply  error code: 0x0 = cannot find char, 0x1 = success
     * @return the MaplePacket
     */
    public static byte[] getWhisperReply(String target, byte reply) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.WHISPER.getValue());
        mplew.write(0x0A); // whisper?
        mplew.writeMapleString(target);
        mplew.write(reply);
        return mplew.getPacket();
    }

    public static byte[] getResetRemoteTempStats(int playerID, Set<MapleBuffStat> stats) {
        MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.CANCEL_FOREIGN_BUFF.getValue());
        w.writeInt(playerID);
        encodeBuffMask(w, stats);
        return w.getPacket();
    }

    public static byte[] getResetTempStats(Set<MapleBuffStat> stats) {
        MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.CANCEL_BUFF.getValue());
        encodeBuffMask(w, stats);
        w.write(0);
        return w.getPacket();
    }

    public static byte[] setRemoteTempStats(MapleCharacter player, Map<MapleBuffStat, BuffContainer> stats) {
        MaplePacketWriter w = new MaplePacketWriter(32 + (2 * stats.size()));
        w.writeShort(SendOpcode.GIVE_FOREIGN_BUFF.getValue());
        w.writeInt(player.getId());
        encodeTempStatForRemote(w, stats);
        return w.getPacket();
    }

    public static byte[] setTempStats(Map<MapleBuffStat, BuffContainer> stats) {
        final MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.GIVE_BUFF.getValue());
        encodeTempStatForLocal(w, stats);
        return w.getPacket();
    }

    public static void encodeTempStatForLocal(MaplePacketWriter w, Map<MapleBuffStat, BuffContainer> stats) {
        encodeBuffMask(w, stats.keySet());
        for (Entry<MapleBuffStat, BuffContainer> e : stats.entrySet()) {
            MapleBuffStat buff = e.getKey();
            BuffContainer container = e.getValue();

            if (buff == MapleBuffStat.PARTY_BOOSTER) {
                // does not seem to be encoded here
                continue;
            }
            int sourceID = container.getSourceID();
            int value = container.getValue();

            if (container.getMobSkill() != null) {
                w.writeShort(container.getMobSkill().getSkillLevel());
                w.writeShort(sourceID);
                w.writeShort(value);
            } else {
                w.writeShort(value);
                if (buff == MapleBuffStat.RIDE_VEHICLE) {
                    w.writeInt(value); // ID of mount
                } else if (!container.getEffect().isSkill()) {
                    w.writeInt(-sourceID);
                } else {
                    w.writeInt(sourceID);
                }
            }
            w.writeInt(container.getDuration());
        }
        w.write(0); // nDefenseAtt
        w.write(0); // nDefenseState
        for (int tsIndex = 0; tsIndex < 8; tsIndex++) { // must be in this order
            for (Entry<MapleBuffStat, BuffContainer> e : stats.entrySet()) {
                MapleBuffStat buff = e.getKey();
                BuffContainer container = e.getValue();
                if (buff.getIndex() == tsIndex) {
                    buff.encode(w, container);
                }
            }
        }
        w.writeShort(0); // nDelay
        // if(SecondaryStat::IsMovementAffectingStat)
        w.write(0); // CUserLocal::SetSecondaryStatChangedPoint
    }

    public static void encodeTempStatForRemote(MaplePacketWriter w, Map<MapleBuffStat, BuffContainer> stats) {
        Set<MapleBuffStat> keyset = stats.keySet();
        encodeBuffMask(w, keyset);
        for (Entry<MapleBuffStat, BuffContainer> entry : stats.entrySet()) {
            MapleBuffStat buff = entry.getKey();
            BuffContainer bh = entry.getValue();

            w.writeShort(bh.getValue());
            if (buff == MapleBuffStat.RIDE_VEHICLE) {
                w.writeInt(bh.getValue());
            }
            w.writeInt(bh.getDuration());
        }
        w.write(0); // nDefenseAtt
        w.write(0); // nDefenseState
        w.skip(16);
//        MapleBuffStat[] buffs = MapleBuffStat.values();
//        for (int i = 0; i < 8; i++) {
//            for (MapleBuffStat stat : keyset) {
//                if (stat.getIndex() == i) {
//                    w.writeInt(0);
//                    w.writeInt(0);
//                }
//            }
//        }
    }

    public static byte[] giveBuff(int buffid, int bufflength, Map<MapleBuffStat, Integer> statups) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GIVE_BUFF.getValue());
        encodeBuffMask(mplew, statups.keySet());
        for (Entry<MapleBuffStat, Integer> statup : statups.entrySet()) {
            mplew.writeShort(statup.getValue().shortValue());
            mplew.writeInt(buffid);
            mplew.writeInt(bufflength);
        }
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] giveDebuff(List<Pair<MapleDisease, Integer>> statups, MobSkill skill) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GIVE_BUFF.getValue());
        encode16ByteMask(mplew, statups);
        for (Pair<MapleDisease, Integer> statup : statups) {
            mplew.writeShort(statup.getRight().shortValue());
            mplew.writeShort(skill.getSkillId());
            mplew.writeShort(skill.getSkillLevel());
            mplew.writeInt((int) skill.getDuration());
        }
        mplew.writeShort(0); // ??? wk charges have 600 here o.o
        mplew.writeShort(900);//Delay
        mplew.write(1);
        return mplew.getPacket();
    }

    /**
     * status can be: <br> 0: ok, use giveFameResponse<br> 1: the username is incorrectly entered<br> 2: users under
     * level 15 are unable to toggle with fame.<br> 3: can't raise or drop fame anymore today.<br> 4: can't raise or
     * drop fame for this character for this month anymore.<br> 5: received fame, use receiveFame()<br> 6: level of fame
     * neither has been raised nor dropped due to an unexpected error
     *
     * @param status
     * @return
     */
    public static byte[] giveFameErrorResponse(int status) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FAME_RESPONSE.getValue());
        mplew.write(status);
        return mplew.getPacket();
    }

    public static byte[] giveFameResponse(int mode, String charname, int newfame) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FAME_RESPONSE.getValue());
        mplew.write(0);
        mplew.writeMapleString(charname);
        mplew.write(mode);
        mplew.writeShort(newfame);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static byte[] giveFinalAttack(int skillid, int time) {//packets found by lailainoob
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GIVE_BUFF.getValue());
        mplew.writeLong(0);
        mplew.writeShort(0);
        mplew.write(0);//some 80 and 0 bs DIRECTION
        mplew.write(0x80);//let's just do 80, then 0
        mplew.writeInt(0);
        mplew.writeShort(1);
        mplew.writeInt(skillid);
        mplew.writeInt(time);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] giveForeignBuff(int cid, Map<MapleBuffStat, Integer> stats) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        encodeBuffMask(mplew, stats.keySet());
        for (Entry<MapleBuffStat, Integer> e : stats.entrySet()) {
            mplew.writeShort(e.getValue().shortValue());
            if (e.getKey() == MapleBuffStat.POISON) {
                mplew.writeInt(0); // meh
            }
        }
        mplew.write(0);
        mplew.write(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] giveForeignDebuff(int cid, List<Pair<MapleDisease, Integer>> stats, MobSkill skill) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        encode16ByteMask(mplew, stats);
        for (Pair<MapleDisease, Integer> pair : stats) {
            mplew.writeShort(skill.getSkillId());
            if (pair.getLeft().isFirst()) {
                mplew.writeInt(skill.getSkillLevel());
            } else {
                mplew.writeShort(skill.getSkillLevel());
            }
        }
        mplew.write(0);
        mplew.write(0);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static byte[] giveForgeinPirateBuff(int cid, int buffid, int time, Map<MapleBuffStat, Integer> stats) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        boolean infusion = buffid == Buccaneer.SPEED_INFUSION || buffid == ThunderBreaker.SPEED_INFUSION || buffid == Corsair.SPEED_INFUSION;
        mplew.writeShort(SendOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        encodeBuffMask(mplew, stats.keySet());
        mplew.writeShort(0);
        for (Entry<MapleBuffStat, Integer> statup : stats.entrySet()) {
            mplew.writeInt(statup.getValue().shortValue());
            mplew.writeInt(buffid);
            mplew.skip(infusion ? 10 : 5);
            mplew.writeShort(time);
        }
        mplew.writeShort(0);
        mplew.write(2);
        return mplew.getPacket();
    }

    public static byte[] givePirateBuff(Map<MapleBuffStat, Integer> stats, int buffid, int duration) {
        boolean infusion = buffid == Buccaneer.SPEED_INFUSION
                || buffid == ThunderBreaker.SPEED_INFUSION
                || buffid == Corsair.SPEED_INFUSION;

        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GIVE_BUFF.getValue());
        encodeBuffMask(mplew, stats.keySet());
        mplew.writeShort(0);
        for (Entry<MapleBuffStat, Integer> stat : stats.entrySet()) {
            mplew.writeInt(stat.getValue().shortValue());
            mplew.writeInt(buffid);
            mplew.skip(infusion ? 10 : 5);
            mplew.writeShort(duration);
        }
        mplew.skip(3);
        return mplew.getPacket();
    }

    public static byte[] guideHint(int hint) {
        final MaplePacketWriter mplew = new MaplePacketWriter(11);
        mplew.writeShort(SendOpcode.TALK_GUIDE.getValue());
        mplew.write(1);
        mplew.writeInt(hint);
        mplew.writeInt(7000);
        return mplew.getPacket();
    }

    public static byte[] guildCapacityChange(int gid, int capacity) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3A);
        mplew.writeInt(gid);
        mplew.write(capacity);
        return mplew.getPacket();
    }

    public static byte[] guildDisband(int gid) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x32);
        mplew.writeInt(gid);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] guildEmblemChange(int gid, short bg, byte bgcolor, short logo, byte logocolor) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x42);
        mplew.writeInt(gid);
        mplew.writeShort(bg);
        mplew.write(bgcolor);
        mplew.writeShort(logo);
        mplew.write(logocolor);
        return mplew.getPacket();
    }

    public static byte[] guildInvite(int gid, String charName) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x05);
        mplew.writeInt(gid);
        mplew.writeMapleString(charName);
        return mplew.getPacket();
    }

    public static byte[] guildMemberLevelJobUpdate(MapleGuildCharacter mgc) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3C);
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeInt(mgc.getLevel());
        mplew.writeInt(mgc.getJobId());
        return mplew.getPacket();
    }

    public static byte[] guildMemberOnline(int gid, int cid, boolean bOnline) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3d);
        mplew.writeInt(gid);
        mplew.writeInt(cid);
        mplew.write(bOnline ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] guildNotice(int gid, String notice) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x44);
        mplew.writeInt(gid);
        mplew.writeMapleString(notice);
        return mplew.getPacket();
    }

    public static byte[] guildQuestWaitingNotice(byte channel, int waitingPos) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x4C);
        mplew.write(channel - 1);
        mplew.write(waitingPos);
        return mplew.getPacket();
    }

    public static byte[] healMonster(int oid, int heal) {
        return damageMonster(oid, -heal);
    }

    public static byte[] hiredMerchantBox() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.ENTRUSTED_SHOP_CHECK_RESULT.getValue()); // header.
        mplew.write(0x07);
        return mplew.getPacket();
    }

    public static byte[] hiredMerchantChat(String message, byte slot) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.CHAT.value);
        mplew.write(CommunityActions.CHAT_THING.value);
        mplew.write(slot);
        mplew.writeMapleString(message);
        return mplew.getPacket();
    }

    public static byte[] hiredMerchantOwnerLeave() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.REAL_CLOSE_MERCHANT.value);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] hiredMerchantVisitorAdd(MapleCharacter chr, int slot) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.VISIT.value);
        mplew.write(slot);
        addCharLook(mplew, chr, false);
        mplew.writeMapleString(chr.getName());
        return mplew.getPacket();
    }

    public static byte[] hiredMerchantVisitorLeave(int slot) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.EXIT.value);
        if (slot != 0) {
            mplew.write(slot);
        }
        return mplew.getPacket();
    }

    public static byte[] getCoconutHit(short a, short b, byte c) {
        MaplePacketWriter w = new MaplePacketWriter(7);
        w.writeShort(SendOpcode.COCONUT_HIT.getValue());
        w.writeShort(a);
        w.writeShort(b);
        w.write(c);
        return w.getPacket();
    }

    public static byte[] hitSnowBall(int what, int damage) {
        final MaplePacketWriter mplew = new MaplePacketWriter(7);
        mplew.writeShort(SendOpcode.SNOWBALL_HIT.getValue());
        mplew.write(what);
        mplew.writeInt(damage);
        return mplew.getPacket();
    }

    public static byte[] hpqMessage(String text) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.BLOW_WEATHER.getValue()); // not 100% sure
        mplew.write(0);
        mplew.writeInt(5120016);
        mplew.writeAsciiString(text);
        return mplew.getPacket();
    }

    public static byte[] incubatorResult() {//lol
        final MaplePacketWriter mplew = new MaplePacketWriter(8);
        mplew.writeShort(SendOpcode.INCUBATOR_RESULT.getValue());
        mplew.skip(6);
        return mplew.getPacket();
    }

    public static byte[] itemEffect(int characterid, int itemid) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_EFFECT.getValue());
        mplew.writeInt(characterid);
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    public static byte[] itemExpired(int itemid) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(2);
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    public static byte[] itemMegaphone(String msg, boolean whisper, int channel, Item item) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SERVERMESSAGE.getValue());
        mplew.write(8);
        mplew.writeMapleString(msg);
        mplew.write(channel - 1);
        mplew.write(whisper ? 1 : 0);
        if (item == null) {
            mplew.write(0);
        } else {
            mplew.write(item.getPosition());
            addItemInfo(mplew, item, true);
        }
        return mplew.getPacket();
    }

    /**
     * Sends a "job advance" packet to the guild or family.
     * <p>
     * Possible values for <code>type</code>:<br> 0: <Guild ? has advanced to a(an) ?.<br> 1: <Family ? has advanced to
     * a(an) ?.<br>
     *
     * @param type The type
     * @return The "job advance" packet.
     */
    public static byte[] jobMessage(int type, int job, String charname) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.NOTIFY_JOB_CHANGE.getValue());
        mplew.write(type);
        mplew.writeInt(job); //Why fking int?
        mplew.writeMapleString("> " + charname); //To fix the stupid packet lol

        return mplew.getPacket();
    }

    public static byte[] joinMessenger(int position) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MESSENGER.getValue());
        mplew.write(0x01);
        mplew.write(position);
        return mplew.getPacket();
    }

    public static byte[] killMonster(int oid, boolean animation) {
        return killMonster(oid, animation ? 1 : 0);
    }

    /**
     * Gets a packet telling the client that a monster was killed.
     *
     * @param oid       The objectID of the killed monster.
     * @param animation 0 = dissapear, 1 = fade out, 2+ = special
     * @return The kill monster packet.
     */
    public static byte[] killMonster(int oid, int animation) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.KILL_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(animation);
        mplew.write(animation);
        return mplew.getPacket();
    }

    public static byte[] leaveHiredMerchant(int slot, int status2) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.EXIT.value);
        mplew.write(slot);
        mplew.write(status2);
        return mplew.getPacket();
    }

    public static byte[] getSnowBallTouch() {
        final MaplePacketWriter mplew = new MaplePacketWriter(2);
        mplew.writeShort(SendOpcode.SNOWBALL_TOUCH.getValue());
        return mplew.getPacket();
    }

    /**
     * Sends a "levelup" packet to the guild or family.
     * <p>
     * Possible values for <code>type</code>:<br> 0: <Family> ? has reached Lv. ?.<br> - The Reps you have received from
     * ? will be reduced in half. 1:
     * <Family> ? has reached Lv. ?.<br> 2: <Guild> ? has reached Lv. ?.<br>
     *
     * @param type The type
     * @return The "levelup" packet.
     */
    public static byte[] levelUpMessage(int type, int level, String charname) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.NOTIFY_LEVELUP.getValue());
        mplew.write(type);
        mplew.writeInt(level);
        mplew.writeMapleString(charname);

        return mplew.getPacket();
    }

    public static byte[] loadFamily(MapleCharacter player) {
        String[] title = {"Family Reunion", "Summon Family", "My Drop Rate 1.5x (15 min)", "My EXP 1.5x (15 min)", "Family Bonding (30 min)", "My Drop Rate 2x (15 min)", "My EXP 2x (15 min)", "My Drop Rate 2x (30 min)", "My EXP 2x (30 min)", "My Party Drop Rate 2x (30 min)", "My Party EXP 2x (30 min)"};
        String[] description = {"[Target] Me\n[Effect] Teleport directly to the Family member of your choice.",
                "[Target] 1 Family member\n[Effect] Summon a Family member of choice to the map you're in.",
                "[Target] Me\n[Time] 15 min.\n[Effect] Monster drop rate will be increased #c1.5x#.\n*  If the Drop Rate event is in progress, this will be nullified.",
                "[Target] Me\n[Time] 15 min.\n[Effect] EXP earned from hunting will be increased #c1.5x#.\n* If the EXP event is in progress, this will be nullified.",
                "[Target] At least 6 Family members online that are below me in the Pedigree\n[Time] 30 min.\n[Effect] Monster drop rate and EXP earned will be increased #c2x#. \n* If the EXP event is in progress, this will be nullified.",
                "[Target] Me\n[Time] 15 min.\n[Effect] Monster drop rate will be increased #c2x#.\n* If the Drop Rate event is in progress, this will be nullified.",
                "[Target] Me\n[Time] 15 min.\n[Effect] EXP earned from hunting will be increased #c2x#.\n* If the EXP event is in progress, this will be nullified.",
                "[Target] Me\n[Time] 30 min.\n[Effect] Monster drop rate will be increased #c2x#.\n* If the Drop Rate event is in progress, this will be nullified.",
                "[Target] Me\n[Time] 30 min.\n[Effect] EXP earned from hunting will be increased #c2x#. \n* If the EXP event is in progress, this will be nullified.",
                "[Target] My party\n[Time] 30 min.\n[Effect] Monster drop rate will be increased #c2x#.\n* If the Drop Rate event is in progress, this will be nullified.",
                "[Target] My party\n[Time] 30 min.\n[Effect] EXP earned from hunting will be increased #c2x#.\n* If the EXP event is in progress, this will be nullified."};
        int[] repCost = {3, 5, 7, 8, 10, 12, 15, 20, 25, 40, 50};
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FAMILY_PRIVILEGE_LIST.getValue());
        mplew.writeInt(11);
        for (int i = 0; i < 11; i++) {
            mplew.write(i > 4 ? (i % 2) + 1 : i);
            mplew.writeInt(repCost[i] * 100);
            mplew.writeInt(1);
            mplew.writeMapleString(title[i]);
            mplew.writeMapleString(description[i]);
        }
        return mplew.getPacket();
    }

    public static byte[] lockUI(boolean enable) {
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.LOCK_UI.getValue());
        mplew.write(enable ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] magicAttack(MapleCharacter chr, int skill, int skilllevel, int stance, int numAttackedAndDamage, Map<Integer, List<Integer>> damage, int charge, int speed, int direction, int display) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MAGIC_ATTACK.getValue());
        addAttackBody(mplew, chr, skill, skilllevel, stance, numAttackedAndDamage, 0, damage, speed, direction, display);
        if (charge != -1) {
            mplew.writeInt(charge);
        }
        return mplew.getPacket();
    }

    /**
     * Makes a monster invisible for Ariant PQ.
     *
     * @param life
     * @return
     */
    public static byte[] makeMonsterInvisible(MapleMonster life) {
        return spawnMonsterInternal(life, true, false, false, 0, true);
    }

    /**
     * Makes a monster previously spawned as non-targettable, targettable.
     *
     * @param life The mob to make targettable.
     * @return The packet to make the mob targettable.
     */
    public static byte[] makeMonsterReal(MapleMonster life) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SPAWN_MONSTER.getValue());
        mplew.writeInt(life.getObjectId());
        mplew.write(5);
        mplew.writeInt(life.getId());
        mplew.skip(15);
        mplew.write(0x88);
        mplew.skip(6);
        mplew.writeLocation(life.getPosition());
        mplew.write(life.getStance());
        mplew.writeShort(0);//life.getStartFh()
        mplew.writeShort(life.getFh());
        mplew.writeShort(-1);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] makeNewAlliance(MapleAlliance alliance, MapleClient c) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0F);
        mplew.writeInt(alliance.getId());
        mplew.writeMapleString(alliance.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleString(alliance.getRankTitle(i));
        }
        mplew.write(alliance.getGuilds().size());
        for (Integer guild : alliance.getGuilds()) {
            mplew.writeInt(guild);
        }
        mplew.writeInt(2); // probably capacity
        mplew.writeShort(0);
        for (Integer guildd : alliance.getGuilds()) {
            getGuildInfo(mplew, Server.getGuild(guildd, c.getWorld(), c.getPlayer().getMGC()));
        }
        return mplew.getPacket();
    }

    public static byte[] mapEffect(String path) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FIELD_EFFECT.getValue());
        mplew.write(3);
        mplew.writeMapleString(path);
        return mplew.getPacket();
    }

    public static byte[] mapSound(String path) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FIELD_EFFECT.getValue());
        mplew.write(4);
        mplew.writeMapleString(path);
        return mplew.getPacket();
    }

    /**
     * Sends a "married" packet to the guild or family.
     * <p>
     * Possible values for <code>type</code>:<br> 0: <Guild ? is now married. Please congratulate them.<br> 1: <Family ?
     * is now married. Please congratulate them.<br>
     *
     * @param type The type
     * @return The "married" packet.
     */
    public static byte[] marriageMessage(int type, String charname) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.NOTIFY_MARRIAGE.getValue());
        mplew.write(type);
        mplew.writeMapleString("> " + charname); //To fix the stupid packet lol

        return mplew.getPacket();
    }

    //someone leaving, mode == 0x2c for leaving, 0x2f for expelled
    public static byte[] memberLeft(MapleGuildCharacter mgc, boolean bExpelled) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(bExpelled ? 0x2f : 0x2c);
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeMapleString(mgc.getName());
        return mplew.getPacket();
    }

    public static byte[] getStorageSetMoney(MapleStorage storage) {
        MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.STORAGE.getValue());
        w.write(19);
        w.write(storage.getSlotCount());
        w.writeLong(2); // dbcharFlag
        w.writeInt(storage.getMoney());
        return w.getPacket();
    }

    public static byte[] messengerChat(String text) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MESSENGER.getValue());
        mplew.write(0x06);
        mplew.writeMapleString(text);
        return mplew.getPacket();
    }

    public static byte[] messengerInvite(String from, int messengerid) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MESSENGER.getValue());
        mplew.write(0x03);
        mplew.writeMapleString(from);
        mplew.write(0);
        mplew.writeInt(messengerid);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] messengerNote(String text, int mode, int mode2) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MESSENGER.getValue());
        mplew.write(mode);
        mplew.writeMapleString(text);
        mplew.write(mode2);
        return mplew.getPacket();
    }

    public static byte[] modifyInventory(boolean updateTick, final List<ModifyInventory> mods) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.INVENTORY_OPERATION.getValue());
        mplew.writeBoolean(updateTick);
        mplew.write(mods.size());
        //mplew.write(0); v104 :)
        int addMovement = -1;
        for (ModifyInventory mod : mods) {
            mplew.write(mod.getMode());
            mplew.write(mod.getInventoryType());
            mplew.writeShort(mod.getMode() == 2 ? mod.getOldPosition() : mod.getPosition());
            switch (mod.getMode()) {
                case 0: {//add item
                    addItemInfo(mplew, mod.getItem(), true);
                    break;
                }
                case 1: {//update quantity
                    mplew.writeShort(mod.getQuantity());
                    break;
                }
                case 2: {//move
                    mplew.writeShort(mod.getPosition());
                    if (mod.getPosition() < 0 || mod.getOldPosition() < 0) {
                        addMovement = mod.getOldPosition() < 0 ? 1 : 2;
                    }
                    break;
                }
                case 3: {//remove
                    if (mod.getPosition() < 0) {
                        addMovement = 2;
                    }
                    break;
                }
            }
            mod.clear();
        }
        if (addMovement > -1) {
            mplew.write(addMovement);
        }
        return mplew.getPacket();
    }

    public static byte[] moveDragon(MapleDragon dragon, Point startPos, List<LifeMovementFragment> res) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MOVE_DRAGON.getValue());
        mplew.writeInt(dragon.getOwner().getId());
        mplew.writeLocation(startPos);
        serializeMovementList(mplew, res);
        return mplew.getPacket();
    }

    public static byte[] moveMonster(int useskill, int skill, int skill_1, int skill_2, int skill_3, int skill_4, int oid, Point startPos, List<LifeMovementFragment> moves) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MOVE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.write(useskill);
        mplew.write(skill);
        mplew.write(skill_1);
        mplew.write(skill_2);
        mplew.write(skill_3);
        mplew.write(skill_4);
        mplew.writeLocation(startPos);
        serializeMovementList(mplew, moves);
        return mplew.getPacket();
    }

    /**
     * Gets a response to a move monster packet.
     *
     * @param objectid  The ObjectID of the monster being moved.
     * @param moveid    The movement ID.
     * @param currentMp The current MP of the monster.
     * @param useSkills Can the monster use skills?
     * @return The move response packet.
     */
    public static byte[] moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills) {
        return moveMonsterResponse(objectid, moveid, currentMp, useSkills, 0, 0);
    }

    /**
     * Gets a response to a move monster packet.
     *
     * @param objectid   The ObjectID of the monster being moved.
     * @param moveid     The movement ID.
     * @param currentMp  The current MP of the monster.
     * @param useSkills  Can the monster use skills?
     * @param skillId    The skill ID for the monster to use.
     * @param skillLevel The level of the skill to use.
     * @return The move response packet.
     */
    public static byte[] moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills, int skillId, int skillLevel) {
        final MaplePacketWriter mplew = new MaplePacketWriter(13);
        mplew.writeShort(SendOpcode.MOVE_MONSTER_RESPONSE.getValue());
        mplew.writeInt(objectid);
        mplew.writeShort(moveid);
        mplew.writeBoolean(useSkills);
        mplew.writeShort(currentMp);
        mplew.write(skillId);
        mplew.write(skillLevel);
        return mplew.getPacket();
    }

    public static byte[] movePet(int cid, int pid, byte slot, List<LifeMovementFragment> moves) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MOVE_PET.getValue());
        mplew.writeInt(cid);
        mplew.write(slot);
        mplew.writeInt(pid);
        serializeMovementList(mplew, moves);
        return mplew.getPacket();
    }

    public static byte[] movePlayer(int cid, Point origin, List<LifeMovementFragment> moves) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MOVE_PLAYER.getValue());
        mplew.writeInt(cid);
        mplew.writeLocation(origin);
        serializeMovementList(mplew, moves);
        return mplew.getPacket();
    }

    public static byte[] moveSummon(int cid, int oid, Point startPos, List<LifeMovementFragment> moves) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SUMMONED_MOVE.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(oid);
        mplew.writeLocation(startPos);
        serializeMovementList(mplew, moves);
        return mplew.getPacket();
    }

    /**
     * mode: 0 buddychat; 1 partychat; 2 guildchat
     *
     * @param name
     * @param chattext
     * @param mode
     * @return
     */
    public static byte[] multiChat(String name, String chattext, int mode) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MULTICHAT.getValue());
        mplew.write(mode);
        mplew.writeMapleString(name);
        mplew.writeMapleString(chattext);
        return mplew.getPacket();
    }

    public static byte[] musicChange(String song) {
        return environmentChange(song, 6);
    }

    public static byte[] newGuildMember(MapleGuildCharacter mgc) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x27);
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeAsciiString(StringUtil.getRightPaddedStr(mgc.getName(), '\0', 13));
        mplew.writeInt(mgc.getJobId());
        mplew.writeInt(mgc.getLevel());
        mplew.writeInt(mgc.getGuildRank()); //should be always 5 but whatevs
        mplew.writeInt(mgc.isOnline() ? 1 : 0); //should always be 1 too
        mplew.writeInt(1); //? could be guild signature, but doesn't seem to matter
        mplew.writeInt(3);
        return mplew.getPacket();
    }

    public static byte[] notYetSoldInv(List<MTSItemInfo> items) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MTS_OPERATION.getValue());
        mplew.write(0x23);
        mplew.writeInt(items.size());
        if (!items.isEmpty()) {
            for (MTSItemInfo item : items) {
                addItemInfo(mplew, item.getItem(), true);
                mplew.writeInt(item.getID()); //id
                mplew.writeInt(item.getTaxes()); //this + below = price
                mplew.writeInt(item.getPrice()); //price
                mplew.writeInt(0);
                mplew.writeLong(getTime(item.getEndingDate()));
                mplew.writeMapleString(item.getSeller()); //account name (what was nexon thinking?)
                mplew.writeMapleString(item.getSeller()); //char name
                for (int i = 0; i < 28; i++) {
                    mplew.write(0);
                }
            }
        } else {
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    /*
     *  0 = Player online, use whisper
     *  1 = Check player's name
     *  2 = Receiver inbox full
     */
    public static byte[] noteError(byte error) {
        final MaplePacketWriter mplew = new MaplePacketWriter(4);
        mplew.writeShort(SendOpcode.MEMO_RESULT.getValue());
        mplew.write(5);
        mplew.write(error);
        return mplew.getPacket();
    }

    public static byte[] noteSendMsg() {
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.MEMO_RESULT.getValue());
        mplew.write(4);
        return mplew.getPacket();
    }

    public static byte[] openCashShop(MapleClient c, boolean mts) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(mts ? SendOpcode.SET_ITC.getValue() : SendOpcode.SET_CASH_SHOP.getValue());

        encodePlayerData(mplew, c.getPlayer());

        if (!mts) {
            mplew.write(1);
        }

        mplew.writeMapleString(c.getAccountName());
        if (mts) {
            mplew.write(new byte[]{(byte) 0x88, 19, 0, 0, 7, 0, 0, 0, (byte) 0xF4, 1, 0, 0, (byte) 0x18, 0, 0, 0, (byte) 0xA8, 0, 0, 0, (byte) 0x70, (byte) 0xAA, (byte) 0xA7, (byte) 0xC5, (byte) 0x4E, (byte) 0xC1, (byte) 0xCA, 1});
        } else {
            mplew.writeInt(0);
            List<SpecialCashItem> lsci = CashItemFactory.getSpecialCashItems();
            mplew.writeShort(lsci.size());//Guess what
            for (SpecialCashItem sci : lsci) {
                mplew.writeInt(sci.getSN());
                mplew.writeInt(sci.getModifier());
                mplew.write(sci.getInfo());
            }
            mplew.skip(121);

            for (int i = 1; i <= 8; i++) {
                for (int j = 0; j < 2; j++) {
                    mplew.writeInt(i);
                    mplew.writeInt(j);
                    mplew.writeInt(50200004);

                    mplew.writeInt(i);
                    mplew.writeInt(j);
                    mplew.writeInt(50200069);

                    mplew.writeInt(i);
                    mplew.writeInt(j);
                    mplew.writeInt(50200117);

                    mplew.writeInt(i);
                    mplew.writeInt(j);
                    mplew.writeInt(50100008);

                    mplew.writeInt(i);
                    mplew.writeInt(j);
                    mplew.writeInt(50000047);
                }
            }

            mplew.writeInt(0);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.writeInt(75);
        }
        return mplew.getPacket();
    }

    /**
     * Sends a UI utility. 0x01 - Equipment Inventory. 0x02 - Stat Window. 0x03 - Skill Window. 0x05 - Keyboard
     * Settings. 0x06 - Quest window. 0x09 - Monsterbook Window. 0x0A - Char Info 0x0B - Guild BBS 0x12 - Monster
     * Carnival Window 0x16 - Party Search. 0x17 - Item Creation Window. 0x1A - My Ranking O.O 0x1B - Family Window 0x1C
     * - Family Pedigree 0x1D - GM Story Board /funny shet 0x1E - Envelop saying you got mail from an admin. lmfao 0x1F
     * - Medal Window 0x20 - Maple Event (???) 0x21 - Invalid Pointer Crash
     *
     * @param ui
     * @return
     */
    public static byte[] openUI(byte ui) {
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.OPEN_UI.getValue());
        mplew.write(ui);
        return mplew.getPacket();
    }

    public static byte[] owlOfMinerva(MapleClient c, int itemid, List<HiredMerchant> hms, List<MaplePlayerShopItem> items) { //Thanks moongra, you save me some time :)
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOP_SCANNER_RESULT.getValue()); // header.
        mplew.write(6);
        mplew.writeInt(0);
        mplew.writeInt(itemid);
        mplew.writeInt(hms.size());
        for (HiredMerchant hm : hms) {
            for (MaplePlayerShopItem item : items) {
                mplew.writeMapleString(hm.getOwner());
                mplew.writeInt(hm.getMapId());
                mplew.writeMapleString(hm.getDescription());
                mplew.writeInt(item.getItem().getQuantity());
                mplew.writeInt(item.getBundles());
                mplew.writeInt(item.getPrice());
                mplew.writeInt(hm.getOwnerId());
                mplew.write(hm.getFreeSlot() == -1 ? 1 : 0);
                MapleCharacter chr = c.getWorldServer().getPlayerStorage().get(hm.getOwnerId());
                if ((chr != null) && (c.getChannel() == hm.getChannel())) {
                    mplew.write(1);
                } else {
                    mplew.write(2);
                }

                if (item.getItem().getItemId() / 1000000 == 1) {
                    addItemInfo(mplew, item.getItem(), true);
                }
            }
        }
        return mplew.getPacket();
    }

    public static byte[] partyCreated(MaplePartyCharacter partychar) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        mplew.write(8);
        mplew.writeShort(0x8b);
        mplew.writeShort(1);
        if (partychar.getDoors().size() > 0) {
            for (MapleDoor doors : partychar.getDoors()) {
                mplew.writeInt(doors.getTown().getId());
                mplew.writeInt(doors.getTarget().getId());
                mplew.writeInt(doors.getPosition().x);
                mplew.writeInt(doors.getPosition().y);
            }
        } else {
            mplew.writeInt(999999999);
            mplew.writeInt(999999999);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static byte[] partyInvite(MapleCharacter from) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        mplew.write(4);
        mplew.writeInt(from.getParty().getID());
        mplew.writeMapleString(from.getName());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] partyPortal(int townId, int targetId, Point position) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        mplew.writeShort(0x23);
        mplew.writeInt(townId);
        mplew.writeInt(targetId);
        mplew.writeLocation(position);
        return mplew.getPacket();
    }


    /**
     * 23: 'Char' have denied request to the party.
     *
     * @param message
     * @param charname
     * @return
     */
    public static byte[] partyStatusMessage(int message, String charname) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        mplew.write(message);
        mplew.writeMapleString(charname);
        return mplew.getPacket();
    }

    public static byte[] petChat(int cid, byte index, int act, String text) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PET_CHAT.getValue());
        mplew.writeInt(cid);
        mplew.write(index);
        mplew.write(0);
        mplew.write(act);
        mplew.writeMapleString(text);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] petStatUpdate(MapleCharacter chr) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.STAT_CHANGED.getValue());
        int mask = 0;
        mask |= MapleStat.PET.getValue();
        mplew.write(0);
        mplew.writeInt(mask);
        MaplePet[] pets = chr.getPets();
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                mplew.writeInt(pets[i].getUniqueId());
                mplew.writeInt(0);
            } else {
                mplew.writeLong(0);
            }
        }
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] pinAccepted() {
        return pinOperation((byte) 0);
    }

    /**
     * Gets a packet detailing a PIN operation.
     * <p>
     * Possible values for <code>mode</code>:<br> 0 - PIN was accepted<br> 1 - Register a new PIN<br> 2 - Invalid pin /
     * Reenter<br> 3 - Connection failed due to system error<br> 4 - Enter the pin
     *
     * @param mode The mode.
     * @return
     */
    private static byte[] pinOperation(byte mode) {
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.CHECK_PINCODE.getValue());
        mplew.write(mode);
        return mplew.getPacket();
    }

    public static byte[] pinRegistered() {
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.UPDATE_PINCODE.getValue());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] playPortalSound() {
        return showSpecialEffect(7);
    }

    public static byte[] playSound(String sound) {
        return environmentChange(sound, 4);
    }

    public static byte[] putIntoCashInventory(Item item, int accountId) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x6A);
        addCashItemInformation(mplew, item, accountId);

        return mplew.getPacket();
    }

    public static byte[] increaseMassacreGauge(int gauge) {
        final MaplePacketWriter mplew = new MaplePacketWriter(6);
        mplew.writeShort(SendOpcode.MASSACRE_GAUGE_INC.getValue());
        mplew.writeInt(gauge);
        return mplew.getPacket();
    }

    public static byte[] getMassacreResult(byte score, int exp) {
        if (score < 0 || score > 4) {
            throw new IllegalArgumentException("score value must be between 0 (incsluve) and 4 (incsluve). " + score + " was given");
        }
        final MaplePacketWriter mplew = new MaplePacketWriter(7);
        mplew.writeShort(SendOpcode.MASSACRE_RESULT.getValue());
        mplew.write(score);
        mplew.writeInt(exp);
        return mplew.getPacket();
    }

    public static byte[] questError(short quest) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(0x0A);
        mplew.writeShort(quest);
        return mplew.getPacket();
    }

    public static byte[] questExpire(short quest) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(0x0F);
        mplew.writeShort(quest);
        return mplew.getPacket();
    }

    public static byte[] questFailure(byte type) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(type);//0x0B = No meso, 0x0D = Worn by character, 0x0E = Not having the item ?
        return mplew.getPacket();
    }

    public static byte[] rangedAttack(MapleCharacter chr, int skill, int skilllevel, int stance, int numAttackedAndDamage, int projectile, Map<Integer, List<Integer>> damage, int speed, int direction, int display) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.RANGED_ATTACK.getValue());
        addAttackBody(mplew, chr, skill, skilllevel, stance, numAttackedAndDamage, projectile, damage, speed, direction, display);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] rankTitleChange(int gid, String[] ranks) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3E);
        mplew.writeInt(gid);
        for (int i = 0; i < 5; i++) {
            mplew.writeMapleString(ranks[i]);
        }
        return mplew.getPacket();
    }

    public static byte[] receiveFame(int mode, String charnameFrom) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FAME_RESPONSE.getValue());
        mplew.write(5);
        mplew.writeMapleString(charnameFrom);
        mplew.write(mode);
        return mplew.getPacket();
    }

    public static byte[] registerPin() {
        return pinOperation((byte) 1);
    }

    public static byte[] remoteChannelChange(byte ch) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.ENTRUSTED_SHOP_CHECK_RESULT.getValue()); // header.
        mplew.write(0x10);
        mplew.writeInt(0);//No idea yet
        mplew.write(ch);
        return mplew.getPacket();
    }

    public static byte[] removeCharBox(MapleCharacter c) {
        final MaplePacketWriter mplew = new MaplePacketWriter(7);
        mplew.writeShort(SendOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] removeClock() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.STOP_CLOCK.getValue());
        mplew.write(0);
        return mplew.getPacket();
    }

    /**
     * Gets a packet to remove a door.
     *
     * @param oid  The door's ID.
     * @param town
     * @return The remove door packet.
     */
    public static byte[] removeDoor(int oid, boolean town) {
        final MaplePacketWriter mplew = new MaplePacketWriter(10);
        if (town) {
            mplew.writeShort(SendOpcode.SPAWN_PORTAL.getValue());
            mplew.writeInt(999999999);
            mplew.writeInt(999999999);
        } else {
            mplew.writeShort(SendOpcode.REMOVE_DOOR.getValue());
            mplew.write(0);
            mplew.writeInt(oid);
        }
        return mplew.getPacket();
    }

    /**
     * Sends a request to remove Mir<br>
     *
     * @param chrid - Needs the specific Character ID
     * @return The packet
     */
    public static byte[] removeDragon(int chrid) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.REMOVE_DRAGON.getValue());
        mplew.writeInt(chrid);
        return mplew.getPacket();
    }

    public static byte[] removeGuildFromAlliance(MapleAlliance alliance, int expelledGuild, MapleClient c) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x10);
        mplew.writeInt(alliance.getId());
        mplew.writeMapleString(alliance.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleString(alliance.getRankTitle(i));
        }
        mplew.write(alliance.getGuilds().size());
        for (Integer guild : alliance.getGuilds()) {
            mplew.writeInt(guild);
        }
        mplew.writeInt(2);
        mplew.writeMapleString(alliance.getNotice());
        mplew.writeInt(expelledGuild);
        getGuildInfo(mplew, Server.getGuild(expelledGuild, c.getWorld(), null));
        mplew.write(0x01);
        return mplew.getPacket();
    }

    public static byte[] removeItemFromDuey(boolean remove, int Package) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PARCEL.getValue());
        mplew.write(0x17);
        mplew.writeInt(Package);
        mplew.write(remove ? 3 : 4);
        return mplew.getPacket();
    }

    /**
     * animation: 0 - expire<br/> 1 - without animation<br/> 2 - pickup<br/> 4 - explode<br/> cid is ignored for 0 and
     * 1
     *
     * @param oid
     * @param animation
     * @param cid
     * @return
     */
    public static byte[] removeItemFromMap(int oid, int animation, int cid) {
        return removeItemFromMap(oid, animation, cid, false, 0);
    }

    /**
     * animation: 0 - expire<br/> 1 - without animation<br/> 2 - pickup<br/> 4 - explode<br/> cid is ignored for 0 and
     * 1.<br /><br />Flagging pet as true will make a pet pick up the item.
     *
     * @param oid
     * @param animation
     * @param cid
     * @param pet
     * @param slot
     * @return
     */
    public static byte[] removeItemFromMap(int oid, int animation, int cid, boolean pet, int slot) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.REMOVE_ITEM_FROM_MAP.getValue());
        mplew.write(animation); // expire
        mplew.writeInt(oid);
        if (animation >= 2) {
            mplew.writeInt(cid);
            if (pet) {
                mplew.write(slot);
            }
        }
        return mplew.getPacket();
    }

    public static byte[] removeMapEffect() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.BLOW_WEATHER.getValue());
        mplew.write(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] removeMatchcardBox(MapleCharacter c) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] removeMessengerPlayer(int position) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MESSENGER.getValue());
        mplew.write(0x02);
        mplew.write(position);
        return mplew.getPacket();
    }

    public static byte[] removeMist(int oid) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.REMOVE_MIST.getValue());
        mplew.writeInt(oid);
        return mplew.getPacket();
    }

    /**
     * Removes a monster invisibility.
     *
     * @param life
     * @return
     */
    public static byte[] removeMonsterInvisibility(MapleMonster life) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(1);
        mplew.writeInt(life.getObjectId());
        return mplew.getPacket();
        //return spawnMonsterInternal(life, true, false, false, 0, false);
    }

    public static byte[] removeNPC(int oid) { //Make npc's invisible
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.REMOVE_NPC.getValue());
        mplew.writeInt(oid);
        return mplew.getPacket();
    }

    public static byte[] removeOmokBox(MapleCharacter c) {
        final MaplePacketWriter mplew = new MaplePacketWriter(7);
        mplew.writeShort(SendOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] removePlayerFromMap(int cid) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.REMOVE_PLAYER_FROM_MAP.getValue());
        mplew.writeInt(cid);
        return mplew.getPacket();
    }
    /*
     * Possible things for ENTRUSTED_SHOP_CHECK_RESULT
     * 0x0E = 00 = Renaming Failed - Can't find the merchant, 01 = Renaming succesful
     * 0x10 = Changes channel to the store (Store is open at Channel 1, do you want to change channels?)
     * 0x11 = You cannot sell any items when managing.. blabla
     * 0x12 = FKING POPUP LOL
     */

    public static byte[] removeQuestTimeLimit(final short quest) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(7);
        mplew.writeShort(1);//Position
        mplew.writeShort(quest);
        return mplew.getPacket();
    }

    /**
     * Gets a packet to remove a special map object.
     *
     * @param summon
     * @param animated Animated removal?
     * @return The packet removing the object.
     */
    public static byte[] removeSummon(MapleSummon summon, boolean animated) {
        final MaplePacketWriter mplew = new MaplePacketWriter(11);
        mplew.writeShort(SendOpcode.SUMMONED_LEAVE_FIELD.getValue());
        mplew.writeInt(summon.getOwner().getId());
        mplew.writeInt(summon.getObjectId());
        mplew.write(animated ? 4 : 1);
        return mplew.getPacket();
    }

    /**
     * Removes TV
     *
     * @return The Remove TV Packet
     */
    public static byte[] getMapleTvClearMessage() {
        final MaplePacketWriter mplew = new MaplePacketWriter(2);
        mplew.writeShort(SendOpcode.MAPLE_TV_CLEAR_MESSAGE.getValue());
        return mplew.getPacket();
    }

    /**
     * Sends a report response
     * <p>
     * Possible values for <code>mode</code>:<br> 0: You have succesfully reported the user.<br> 1: Unable to locate the
     * user.<br> 2: You may only report users 10 times a day.<br> 3: You have been reported to the GM's by a user.<br>
     * 4: Your request did not go through for unknown reasons. Please try again later.<br>
     *
     * @param mode The mode
     * @return Report Reponse packet
     */
    public static byte[] reportResponse(byte mode) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SUE_CHARACTER_RESULT.getValue());
        mplew.write(mode);
        return mplew.getPacket();
    }

    public static byte[] requestBuddylistAdd(int cidFrom, int cid, String nameFrom) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.BUDDYLIST.getValue());
        mplew.write(9);
        mplew.writeInt(cidFrom);
        mplew.writeMapleString(nameFrom);
        mplew.writeInt(cidFrom);
        mplew.writeAsciiString(StringUtil.getRightPaddedStr(nameFrom, '\0', 11));
        mplew.write(0x09);
        mplew.write(0xf0);
        mplew.write(0x01);
        mplew.writeInt(0x0f);
        mplew.writeNullTerminatedAsciiString("Default Group");
        mplew.writeInt(cid);
        return mplew.getPacket();
    }

    public static byte[] requestPin() {
        return pinOperation((byte) 4);
    }

    public static byte[] requestPinAfterFailure() {
        return pinOperation((byte) 2);
    }

    public static byte[] getForcedStatReset() {
        final MaplePacketWriter mplew = new MaplePacketWriter(2);
        mplew.writeShort(SendOpcode.FORCED_STAT_RESET.getValue());
        return mplew.getPacket();
    }

    public static byte[] retrieveFirstMessage() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.ENTRUSTED_SHOP_CHECK_RESULT.getValue()); // header.
        mplew.write(0x09);
        return mplew.getPacket();
    }

    public static byte[] rollSnowBall(boolean entermap, int state, MapleSnowball ball0, MapleSnowball ball1) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SNOWBALL_STATE.getValue());
        if (entermap) {
            mplew.skip(21);
        } else {
            mplew.write(state);// 0 = move, 1 = roll, 2 is down disappear, 3 is up disappear
            mplew.writeInt(ball0.getSnowmanHP() / 75);
            mplew.writeInt(ball1.getSnowmanHP() / 75);
            mplew.writeShort(ball0.getPosition());//distance snowball down, 84 03 = max
            mplew.write(-1);
            mplew.writeShort(ball1.getPosition());//distance snowball up, 84 03 = max
            mplew.write(-1);
        }
        return mplew.getPacket();
    }

    public static byte[] selectWorld(int world) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.LAST_CONNECTED_WORLD.getValue());
        mplew.writeInt(world);//According to GMS, it should be the world that contains the most characters (most active)
        return mplew.getPacket();
    }

    public static byte[] sendAutoHpPot(int itemId) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.AUTO_HP_POT.getValue());
        mplew.writeInt(itemId);
        return mplew.getPacket();
    }

    public static byte[] sendAutoMpPot(int itemId) {
        final MaplePacketWriter mplew = new MaplePacketWriter(6);
        mplew.writeShort(SendOpcode.AUTO_MP_POT.getValue());
        mplew.writeInt(itemId);
        return mplew.getPacket();
    }

    public static byte[] sendBrideWishList(List<Item> items) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.WEDDING_GIFT_RESULT.getValue());
        mplew.write(0x0A);
        mplew.writeLong(-1); // ?
        mplew.writeInt(0); // ?
        mplew.write(items.size());
        for (Item item : items) {
            addItemInfo(mplew, item, true);
        }
        return mplew.getPacket();
    }

    public static byte[] sendDojoAnimation(byte firstByte, String animation) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FIELD_EFFECT.getValue());
        mplew.write(firstByte);
        mplew.writeMapleString(animation);
        return mplew.getPacket();
    }

    public static byte[] sendDuey(byte operation, List<DueyPackages> packages) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PARCEL.getValue());
        mplew.write(operation);
        if (operation == 8) {
            mplew.write(0);
            mplew.write(packages.size());
            for (DueyPackages dp : packages) {
                mplew.writeInt(dp.getPackageId());
                mplew.writeAsciiString(dp.getSender());
                for (int i = dp.getSender().length(); i < 13; i++) {
                    mplew.write(0);
                }
                mplew.writeInt(dp.getMesos());
                mplew.writeLong(getTime(dp.sentTimeInMilliseconds()));
                mplew.writeLong(0); // Contains message o____o.
                for (int i = 0; i < 48; i++) {
                    mplew.writeInt(Randomizer.nextInt(Integer.MAX_VALUE));
                }
                mplew.writeInt(0);
                mplew.write(0);
                if (dp.getItem() != null) {
                    mplew.write(1);
                    addItemInfo(mplew, dp.getItem(), true);
                } else {
                    mplew.write(0);
                }
            }
            mplew.write(0);
        }
        return mplew.getPacket();
    }

    public static byte[] sendDueyMSG(byte operation) {
        return sendDuey(operation, null);
    }

    public static byte[] sendEngagementRequest(String name) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MARRIAGE_REQUEST.getValue()); //<name> has requested engagement. Will you accept this proposal?
        mplew.write(0);
        mplew.writeMapleString(name); // name
        mplew.writeInt(10); // playerid
        return mplew.getPacket();
    }

    public static byte[] sendFamilyInvite(int playerId, String inviter) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FAMILY_JOIN_REQUEST.getValue());
        mplew.writeInt(playerId);
        mplew.writeMapleString(inviter);
        return mplew.getPacket();
    }

    public static byte[] sendFamilyJoinResponse(boolean accepted, String added) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FAMILY_JOIN_REQUEST_RESULT.getValue());
        mplew.write(accepted ? 1 : 0);
        mplew.writeMapleString(added);
        return mplew.getPacket();
    }

    /**
     * Family Result Message
     * <p>
     * Possible values for <code>type</code>:<br> 67: You do not belong to the same family.<br> 69: The character you
     * wish to add as\r\na Junior must be in the same map.<br> 70: This character is already a Junior of another
     * character.<br> 71: The Junior you wish to add\r\nmust be at a lower rank.<br> 72: The gap between you and
     * your\r\njunior must be within 20 levels.<br> 73: Another character has requested to add this character.\r\nPlease
     * try again later.<br> 74: Another character has requested a summon.\r\nPlease try again later.<br> 75: The summons
     * has failed. Your current location or state does not allow a summons.<br> 76: The family cannot extend more than
     * 1000 generations from above and below.<br> 77: The Junior you wish to add\r\nmust be over Level 10.<br> 78: You
     * cannot add a Junior \r\nthat has requested to change worlds.<br> 79: You cannot add a Junior \r\nsince you've
     * requested to change worlds.<br> 80: Separation is not possible due to insufficient Mesos.\r\nYou will need %d
     * Mesos to\r\nseparate with a Senior.<br> 81: Separation is not possible due to insufficient Mesos.\r\nYou will
     * need %d Mesos to\r\nseparate with a Junior.<br> 82: The Entitlement does not apply because your level does not
     * match the corresponding area.<br>
     *
     * @param type The type
     * @return Family Result packet
     */
    public static byte[] sendFamilyMessage(int type, int mesos) {
        final MaplePacketWriter mplew = new MaplePacketWriter(6);
        mplew.writeShort(SendOpcode.FAMILY_RESULT.getValue());
        mplew.writeInt(type);
        mplew.writeInt(mesos);
        return mplew.getPacket();
    }

    public static byte[] sendGainRep(int gain, int mode) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FAMILY_FAMOUS_POINT_INC_RESULT.getValue());
        mplew.writeInt(gain);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static byte[] sendGroomWishlist() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MARRIAGE_REQUEST.getValue()); //<name> has requested engagement. Will you accept this proposal?
        mplew.write(9);
        return mplew.getPacket();
    }

    public static byte[] sendGuestTOS() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUEST_ID_LOGIN.getValue());
        mplew.writeShort(0x100);
        mplew.writeInt(Randomizer.nextInt(999999));
        mplew.writeLong(0);
        mplew.writeLong(getTime(-2));
        mplew.writeLong(getTime(System.currentTimeMillis()));
        mplew.writeInt(0);
        mplew.writeMapleString("http://maplesolaxia.com");
        return mplew.getPacket();
    }

    public static byte[] sendHammerData(int hammerUsed) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.VICIOUS_HAMMER.getValue());
        mplew.write(0x39);
        mplew.writeInt(0);
        mplew.writeInt(hammerUsed);
        return mplew.getPacket();
    }

    public static byte[] sendHammerMessage() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.VICIOUS_HAMMER.getValue());
        mplew.write(0x3D);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    /**
     * Sends a player hint.
     *
     * @param hint   The hint it's going to send.
     * @param width  How tall the box is going to be.
     * @param height How long the box is going to be.
     * @return The player hint packet.
     */
    public static byte[] sendHint(String hint, int width, int height) {
        if (width < 1) {
            width = hint.length() * 10;
            if (width < 40) {
                width = 40;
            }
        }
        if (height < 5) {
            height = 5;
        }
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_HINT.getValue());
        mplew.writeMapleString(hint);
        mplew.writeShort(width);
        mplew.writeShort(height);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] sendMTS(List<MTSItemInfo> items, int tab, int type, int page, int pages) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MTS_OPERATION.getValue());
        mplew.write(0x15); //operation
        mplew.writeInt(pages * 16); //testing, change to 10 if fails
        mplew.writeInt(items.size()); //number of items
        mplew.writeInt(tab);
        mplew.writeInt(type);
        mplew.writeInt(page);
        mplew.write(1);
        mplew.write(1);
        for (MTSItemInfo item : items) {
            addItemInfo(mplew, item.getItem(), true);
            mplew.writeInt(item.getID()); //id
            mplew.writeInt(item.getTaxes()); //this + below = price
            mplew.writeInt(item.getPrice()); //price
            mplew.writeInt(0);
            mplew.writeLong(getTime(item.getEndingDate()));
            mplew.writeMapleString(item.getSeller()); //account name (what was nexon thinking?)
            mplew.writeMapleString(item.getSeller()); //char name
            for (int j = 0; j < 28; j++) {
                mplew.write(0);
            }
        }
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] sendMesoLimit() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.TRADE_MONEY_LIMIT.getValue()); //Players under level 15 can only trade 1m per day
        return mplew.getPacket();
    }

    public static byte[] sendPolice() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FAKE_GM_NOTICE.getValue());
        mplew.write(0);//doesn't even matter what value
        return mplew.getPacket();
    }

    public static byte[] sendPolice(String text) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.DATA_CRC_CHECK_FAILED.getValue());
        mplew.writeMapleString(text);
        return mplew.getPacket();
    }

    public static byte[] sendRecommended(List<Pair<Integer, String>> worlds) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.RECOMMENDED_WORLD_MESSAGE.getValue());
        mplew.write(worlds.size());//size
        for (Pair<Integer, String> world : worlds) {
            mplew.writeInt(world.getLeft());
            mplew.writeMapleString(world.getRight() == null ? "" : world.getRight());
        }
        return mplew.getPacket();
    }

    public static byte[] sendSpouseChat(MapleCharacter wife, String msg, boolean spouse) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SPOUSE_CHAT.getValue());
        mplew.write(spouse ? 5 : 4);
        if (spouse) {
            mplew.writeMapleString(wife.getName());
        }
        mplew.write(spouse ? 5 : 1);
        mplew.writeMapleString(msg);
        return mplew.getPacket();
    }

    /**
     * Sends MapleTV
     *
     * @param chr      The character shown in TV
     * @param messages The message sent with the TV
     * @param type     The type of TV
     * @param partner  The partner shown with chr
     * @return the SEND_TV packet
     */
    public static byte[] getMapleTvSetMessage(MapleCharacter chr, String[] messages, int type, MapleCharacter partner) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MAPLE_TV_SET_MESSAGE.getValue());
        mplew.write(partner != null ? 3 : 1);
        mplew.write(type); //Heart = 2  Star = 1  Normal = 0
        addCharLook(mplew, chr, false);
        mplew.writeMapleString(chr.getName());
        if (partner != null) {
            mplew.writeMapleString(partner.getName());
        } else {
            mplew.writeShort(0);
        }
        for (int i = 0; i < messages.length; i++) {
            mplew.writeMapleString(messages[i].substring(0, 15));
        }
        mplew.writeInt(1337); // time limit shit lol 'Your thing still start in blah blah seconds'
        if (partner != null) {
            addCharLook(mplew, partner, false);
        }
        return mplew.getPacket();
    }

    public static byte[] sendYellowTip(String tip) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SET_WEEK_EVENT_MESSAGE.getValue());
        mplew.write(0xFF);
        mplew.writeMapleString(tip);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    private static void serializeMovementList(MaplePacketWriter w, List<LifeMovementFragment> moves) {
        w.write(moves.size());
        for (LifeMovementFragment move : moves) {
            move.serialize(w);
        }
    }

    /**
     * Gets a server message packet.
     *
     * @param message The message to convey.
     * @return The server message packet.
     */
    public static byte[] serverMessage(String message) {
        return serverMessage(4, (byte) 0, message, true, false, 0);
    }

    /**
     * Gets a server message packet.
     * <p>
     * Possible values for <code>type</code>:<br> 0: [Notice]<br> 1: Popup<br> 2: Megaphone<br> 3: Super Megaphone<br>
     * 4: Scrolling message at top<br> 5: Pink Text<br> 6: Lightblue Text<br> 7: BroadCasting NPC
     *
     * @param type          The type of the notice.
     * @param channel       The channel this notice was sent on.
     * @param message       The message to convey.
     * @param servermessage Is this a scrolling ticker?
     * @return The server notice packet.
     */
    private static byte[] serverMessage(int type, int channel, String message, boolean servermessage, boolean megaEar, int npc) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SERVERMESSAGE.getValue());
        mplew.write(type);
        if (type == 4 || servermessage) {
            mplew.write(1);
        }
        mplew.writeMapleString(message);
        if (type == 3) {
            mplew.write(channel - 1); // channel
            mplew.writeBoolean(megaEar);
        } else if (type == 6) {
            mplew.writeInt(0);
        } else if (type == 7) { // npc
            mplew.writeInt(npc);
        }
        return mplew.getPacket();
    }

    /**
     * Gets a server notice packet.
     * <p>
     * Possible values for <code>type</code>:<br> 0: [Notice]<br> 1: Popup<br> 2: Megaphone<br> 3: Super Megaphone<br>
     * 4: Scrolling message at top<br> 5: Pink Text<br> 6: Lightblue Text
     *
     * @param type    The type of the notice.
     * @param message The message to convey.
     * @return The server notice packet.
     */
    public static byte[] serverNotice(int type, String message) {
        return serverMessage(type, (byte) 0, message, false, false, 0);
    }

    /**
     * Gets a server notice packet.
     * <p>
     * Possible values for <code>type</code>:<br> 0: [Notice]<br> 1: Popup<br> 2: Megaphone<br> 3: Super Megaphone<br>
     * 4: Scrolling message at top<br> 5: Pink Text<br> 6: Lightblue Text
     *
     * @param type    The type of the notice.
     * @param message The message to convey.
     * @return The server notice packet.
     */
    public static byte[] serverNotice(int type, String message, int npc) {
        return serverMessage(type, 0, message, false, false, npc);
    }

    public static byte[] serverNotice(int type, int channel, String message) {
        return serverMessage(type, channel, message, false, false, 0);
    }

    public static byte[] serverNotice(int type, int channel, String message, boolean smegaEar) {
        return serverMessage(type, channel, message, false, smegaEar, 0);
    }

    public static byte[] sheepRanchClothes(int id, byte clothes) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHEEP_RANCH_CLOTHES.getValue());
        mplew.writeInt(id); //Character id
        mplew.write(clothes); //0 = sheep, 1 = wolf, 2 = Spectator (wolf without wool)
        return mplew.getPacket();
    }

    public static byte[] sheepRanchInfo(byte wolf, byte sheep) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHEEP_RANCH_INFO.getValue());
        mplew.write(wolf);
        mplew.write(sheep);
        return mplew.getPacket();
    }

    public static byte[] shopErrorMessage(int error, int type) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x0A);
        mplew.write(type);
        mplew.write(error);
        return mplew.getPacket();
    }

    /* 00 = /
     * 01 = You don't have enough in stock
     * 02 = You do not have enough mesos
     * 03 = Please check if your inventory is full or not
     * 05 = You don't have enough in stock
     * 06 = Due to an error, the trade did not happen
     * 07 = Due to an error, the trade did not happen
     * 08 = /
     * 0D = You need more items
     * 0E = CRASH; LENGTH NEEDS TO BE LONGER :O
     */
    public static byte[] shopTransaction(byte code) {
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.CONFIRM_SHOP_TRANSACTION.getValue());
        mplew.write(code);
        return mplew.getPacket();
    }

    public static byte[] showAllCharacter(int chars, int unk) {
        final MaplePacketWriter mplew = new MaplePacketWriter(11);
        mplew.writeShort(SendOpcode.VIEW_ALL_CHAR.getValue());
        mplew.write(1);
        mplew.writeInt(chars);
        mplew.writeInt(unk);
        return mplew.getPacket();
    }

    public static byte[] showAllCharacterInfo(int worldid, List<MapleCharacter> chars) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.VIEW_ALL_CHAR.getValue());
        mplew.write(0);
        mplew.write(worldid);
        mplew.write(chars.size());
        for (MapleCharacter chr : chars) {
            addCharEntry(mplew, chr, true);
        }
        return mplew.getPacket();
    }

    public static byte[] showBerserk(int cid, int skilllevel, boolean Berserk) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(1);
        mplew.writeInt(1320006);
        mplew.write(0xA9);
        mplew.write(skilllevel);
        mplew.write(Berserk ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] showBossHP(int templateID, int HP, int maxHP, byte tagColor, byte tagBgColor) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FIELD_EFFECT.getValue());
        mplew.write(5);
        mplew.writeInt(templateID);
        mplew.writeInt(HP);
        mplew.writeInt(maxHP);
        mplew.write(tagColor);
        mplew.write(tagBgColor);
        return mplew.getPacket();
    }

    public static byte[] showBoughtCashItem(Item item, int accountId) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x57);
        addCashItemInformation(mplew, item, accountId);

        return mplew.getPacket();
    }

    public static byte[] showBoughtCashPackage(List<Item> cashPackage, int accountId) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x89);
        mplew.write(cashPackage.size());

        for (Item item : cashPackage) {
            addCashItemInformation(mplew, item, accountId);
        }

        mplew.writeShort(0);

        return mplew.getPacket();
    }

    public static byte[] showBoughtCharacterSlot(short slots) {
        final MaplePacketWriter mplew = new MaplePacketWriter(5);
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x64);
        mplew.writeShort(slots);

        return mplew.getPacket();
    }

    public static byte[] showBoughtInventorySlots(int type, short slots) {
        final MaplePacketWriter mplew = new MaplePacketWriter(6);
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x60);
        mplew.write(type);
        mplew.writeShort(slots);

        return mplew.getPacket();
    }

    public static byte[] showBoughtQuestItem(int itemId) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x8D);
        mplew.writeInt(1);
        mplew.writeShort(1);
        mplew.write(0x0B);
        mplew.write(0);
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    public static byte[] showBoughtStorageSlots(short slots) {
        final MaplePacketWriter mplew = new MaplePacketWriter(5);
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x62);
        mplew.writeShort(slots);

        return mplew.getPacket();
    }

    public static byte[] showBuffeffect(int cid, int skillid, int effectid) {
        return showBuffeffect(cid, skillid, effectid, (byte) 3);
    }

    public static byte[] showBuffeffect(int cid, int skillid, int effectid, byte direction) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(effectid); //buff level
        mplew.writeInt(skillid);
        mplew.write(direction);
        mplew.write(1);
        mplew.writeLong(0);
        return mplew.getPacket();
    }

    public static byte[] showCash(MapleCharacter mc) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.QUERY_CASH_RESULT.getValue());

        mplew.writeInt(mc.getCashShop().getCash(1));
        mplew.writeInt(mc.getCashShop().getCash(2));
        mplew.writeInt(mc.getCashShop().getCash(4));

        return mplew.getPacket();
    }

    public static byte[] showCashInventory(MapleClient c) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x4B);
        mplew.writeShort(c.getPlayer().getCashShop().getInventory().size());

        for (Item item : c.getPlayer().getCashShop().getInventory()) {
            addCashItemInformation(mplew, item, c.getAccID());
        }

        mplew.writeShort(c.getPlayer().getStorage().getSlotCount());
        mplew.writeShort(c.getCharacterSlots());

        return mplew.getPacket();
    }

    /*
     * 00 = Due to an unknown error, failed
     * A4 = Due to an unknown error, failed + warpout
     * A5 = You don't have enough cash.
     * A6 = long as shet msg
     * A7 = You have exceeded the allotted limit of price for gifts.
     * A8 = You cannot send a gift to your own account. Log in on the char and purchase
     * A9 = Please confirm whether the character's name is correct.
     * AA = Gender restriction!
     * //Skipped a few
     * B0 = Wrong Coupon Code
     * B1 = Disconnect from CS because of 3 wrong coupon codes < lol
     * B2 = Expired Coupon
     * B3 = Coupon has been used already
     * B4 = Nexon internet cafes? lolfk
     *
     * BB = inv full
     * C2 = not enough mesos? Lol not even 1 mesos xD
     */
    public static byte[] showCashShopMessage(byte message) {
        final MaplePacketWriter mplew = new MaplePacketWriter(4);
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x5C);
        mplew.write(message);

        return mplew.getPacket();
    }

    public static byte[] showChair(int characterid, int itemid) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_CHAIR.getValue());
        mplew.writeInt(characterid);
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    public static byte[] showCombo(int count) {
        final MaplePacketWriter mplew = new MaplePacketWriter(6);
        mplew.writeShort(SendOpcode.SHOW_COMBO.getValue());
        mplew.writeInt(count);
        return mplew.getPacket();
    }

    public static byte[] showCouponRedeemedItem(int itemid) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());
        mplew.writeShort(0x49); //v72
        mplew.writeInt(0);
        mplew.writeInt(1);
        mplew.writeShort(1);
        mplew.writeShort(0x1A);
        mplew.writeInt(itemid);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] showEffect(String effect) {
        return environmentChange(effect, 3);
    }

    public static byte[] showEquipmentLevelUp() {
        return showSpecialEffect(15);
    }

    public static byte[] showEventInstructions() {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GMEVENT_INSTRUCTIONS.getValue());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] getUpdateFieldSpecificData(int team) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FIELD_SPECIFIC_DATA.getValue());
        if (team > -1) {
            mplew.write(team);   // 00 = red, 01 = blue
        }
        return mplew.getPacket();
    }

    public static byte[] showForeginCardEffect(int id) {
        final MaplePacketWriter mplew = new MaplePacketWriter(7);
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(id);
        mplew.write(0x0D);
        return mplew.getPacket();
    }

    public static byte[] showForeignEffect(int cid, int effect) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(effect);
        return mplew.getPacket();
    }

    public static byte[] showGainCard() {
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(0x0D);
        return mplew.getPacket();
    }

    public static byte[] showGiftSucceed(String to, CashItem item) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x5E); //0x5D, Couldn't be sent
        mplew.writeMapleString(to);
        mplew.writeInt(item.getItemId());
        mplew.writeShort(item.getCount());
        mplew.writeInt(item.getPrice());

        return mplew.getPacket();
    }

    public static byte[] showGifts(List<Pair<Item, String>> gifts) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x4D);
        mplew.writeShort(gifts.size());

        for (Pair<Item, String> gift : gifts) {
            addCashItemInformation(mplew, gift.getLeft(), 0, gift.getRight());
        }

        return mplew.getPacket();
    }

    public static byte[] showGuildInfo(MapleCharacter c) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x1A); //signature for showing guild info
        if (c == null) { //show empty guild (used for leaving, expelled)
            mplew.write(0);
            return mplew.getPacket();
        }
        MapleGuild g = c.getClient().getWorldServer().getGuild(c.getMGC());
        if (g == null) { //failed to read from DB - don't show a guild
            mplew.write(0);
            return mplew.getPacket();
        } else {
            c.setGuildRank(c.getGuildRank());
        }
        mplew.write(1); //bInGuild
        mplew.writeInt(g.getId());
        mplew.writeMapleString(g.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleString(g.getRankTitle(i));
        }
        Collection<MapleGuildCharacter> members = g.getMembers();
        mplew.write(members.size()); //then it is the size of all the members
        for (MapleGuildCharacter mgc : members) {//and each of their character ids o_O
            mplew.writeInt(mgc.getId());
        }
        for (MapleGuildCharacter mgc : members) {
            mplew.writeAsciiString(StringUtil.getRightPaddedStr(mgc.getName(), '\0', 13));
            mplew.writeInt(mgc.getJobId());
            mplew.writeInt(mgc.getLevel());
            mplew.writeInt(mgc.getGuildRank());
            mplew.writeInt(mgc.isOnline() ? 1 : 0);
            mplew.writeInt(g.getSignature());
            mplew.writeInt(mgc.getAllianceRank());
        }
        mplew.writeInt(g.getCapacity());
        mplew.writeShort(g.getLogoBG());
        mplew.write(g.getLogoBGColor());
        mplew.writeShort(g.getLogo());
        mplew.write(g.getLogoColor());
        mplew.writeMapleString(g.getNotice());
        mplew.writeInt(g.getGP());
        mplew.writeInt(g.getAllianceId());
        return mplew.getPacket();
    }

    public static byte[] showGuildRanks(int npcid, ResultSet rs) throws SQLException {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x49);
        mplew.writeInt(npcid);
        if (!rs.last()) { //no guilds o.o
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        mplew.writeInt(rs.getRow()); //number of entries
        rs.beforeFirst();
        while (rs.next()) {
            mplew.writeMapleString(rs.getString("name"));
            mplew.writeInt(rs.getInt("GP"));
            mplew.writeInt(rs.getInt("logo"));
            mplew.writeInt(rs.getInt("logoColor"));
            mplew.writeInt(rs.getInt("logoBG"));
            mplew.writeInt(rs.getInt("logoBGColor"));
        }
        return mplew.getPacket();
    }

    public static byte[] showInfo(String path) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(0x17);
        mplew.writeMapleString(path);
        mplew.writeInt(1);
        return mplew.getPacket();
    }

    public static byte[] showInfoText(String text) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(9);
        mplew.writeMapleString(text);
        return mplew.getPacket();
    }

    public static byte[] showIntro(String path) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(0x12);
        mplew.writeMapleString(path);
        return mplew.getPacket();
    }

    public static byte[] showItemLevelup() {
        return showSpecialEffect(15);
    }

    public static byte[] showItemUnavailable() {
        return getShowInventoryStatus(0xfe);
    }

    public static byte[] showMTSCash(MapleCharacter p) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MTS_OPERATION2.getValue());
        mplew.writeInt(p.getCashShop().getCash(4));
        mplew.writeInt(p.getCashShop().getCash(2));
        return mplew.getPacket();
    }

    public static byte[] showMagnet(int mobid, byte success) { // Monster Magnet
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_MAGNET.getValue());
        mplew.writeInt(mobid);
        mplew.write(success);
        mplew.skip(10); //Mmmk
        return mplew.getPacket();
    }

    public static byte[] showMonsterBookPickup() {
        return showSpecialEffect(14);
    }

    /**
     * @param oid
     * @param remhppercentage
     * @return
     */
    public static byte[] showMonsterHP(int oid, int remhppercentage) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_MONSTER_HP.getValue());
        mplew.writeInt(oid);
        mplew.write(remhppercentage);
        return mplew.getPacket();
    }

    public static byte[] showMonsterRiding(int cid, MapleMount mount) { //Gtfo with this, this is just giveForeignBuff
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        mplew.writeLong(MapleBuffStat.RIDE_VEHICLE.getValue()); //Thanks?
        mplew.writeLong(0);
        mplew.writeShort(0);
        mplew.writeInt(mount.getItemId());
        mplew.writeInt(mount.getSkillId());
        mplew.writeInt(0); //Server Tick value.
        mplew.writeShort(0);
        mplew.write(0); //Times you have been buffed
        return mplew.getPacket();
    }

    public static byte[] showNotes(ResultSet notes, int count) throws SQLException {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MEMO_RESULT.getValue());
        mplew.write(3);
        mplew.write(count);
        for (int i = 0; i < count; i++) {
            mplew.writeInt(notes.getInt("id"));
            mplew.writeMapleString(notes.getString("from") + " ");//Stupid nexon forgot space lol
            mplew.writeMapleString(notes.getString("message"));
            mplew.writeLong(getTime(notes.getLong("timestamp")));
            mplew.write(notes.getByte("fame"));//FAME :D
            notes.next();
        }
        return mplew.getPacket();
    }

    public static byte[] showOXQuiz(int questionSet, int questionId, boolean askQuestion) {
        final MaplePacketWriter mplew = new MaplePacketWriter(6);
        mplew.writeShort(SendOpcode.OX_QUIZ.getValue());
        mplew.write(askQuestion ? 1 : 0);
        mplew.write(questionSet);
        mplew.writeShort(questionId);
        return mplew.getPacket();
    }

    public static byte[] showOwnBerserk(int skilllevel, boolean Berserk) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(1);
        mplew.writeInt(1320006);
        mplew.write(0xA9);
        mplew.write(skilllevel);
        mplew.write(Berserk ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] showOwnBuffEffect(int skillid, int effectid) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(effectid);
        mplew.writeInt(skillid);
        mplew.write(0xA9);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] getLocalEffectPetLeveled(byte petSlot) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(4);
        mplew.write(0);
        mplew.write(petSlot);
        return mplew.getPacket();
    }

    public static byte[] showOwnRecovery(byte heal) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(0x0A);
        mplew.write(heal);
        return mplew.getPacket();
    }

    public static byte[] showPedigree(int chrid, Map<Integer, MapleFamilyEntry> members) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FAMILY_CHART_RESULT.getValue());
        //Hmmm xD
        return mplew.getPacket();
    }

    public static byte[] showPet(MapleCharacter chr, MaplePet pet, boolean remove, boolean hunger) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SPAWN_PET.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(chr.getPetIndex(pet));
        if (remove) {
            mplew.write(0);
            mplew.write(hunger ? 1 : 0);
        } else {
            addPetInfo(mplew, pet, true);
        }
        return mplew.getPacket();
    }

    public static byte[] getEffectPetLeveled(MapleCharacter chr, byte petSlot) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(4);
        mplew.write(0);
        mplew.write(petSlot);
        return mplew.getPacket();
    }

    public static byte[] showPlayerRanks(int npcid, ResultSet rs) throws SQLException {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x49);
        mplew.writeInt(npcid);
        if (!rs.last()) {
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        mplew.writeInt(rs.getRow());
        rs.beforeFirst();
        while (rs.next()) {
            mplew.writeMapleString(rs.getString("name"));
            mplew.writeInt(rs.getInt("level"));
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static byte[] showRecovery(int cid, byte amount) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(0x0A);
        mplew.write(amount);
        return mplew.getPacket();
    }

    /**
     * 6 = Exp did not drop (Safety Charms) 7 = Enter portal sound 8 = Job change 9 = Quest complete 10 = Recovery 14 =
     * Monster book pickup 15 = Equipment levelup 16 = Maker Skill Success 19 = Exp card [500, 200, 50]
     *
     * @param effect
     * @return
     */
    public static byte[] showSpecialEffect(int effect) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(effect);
        return mplew.getPacket();
    }

    public static byte[] showThread(int localthreadid, ResultSet threadRS, ResultSet repliesRS) throws SQLException, RuntimeException {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_BBS_PACKET.getValue());
        mplew.write(0x07);
        mplew.writeInt(localthreadid);
        mplew.writeInt(threadRS.getInt("postercid"));
        mplew.writeLong(getTime(threadRS.getLong("timestamp")));
        mplew.writeMapleString(threadRS.getString("name"));
        mplew.writeMapleString(threadRS.getString("startpost"));
        mplew.writeInt(threadRS.getInt("icon"));
        if (repliesRS != null) {
            int replyCount = threadRS.getInt("replycount");
            mplew.writeInt(replyCount);
            int i;
            for (i = 0; i < replyCount && repliesRS.next(); i++) {
                mplew.writeInt(repliesRS.getInt("replyid"));
                mplew.writeInt(repliesRS.getInt("postercid"));
                mplew.writeLong(getTime(repliesRS.getLong("timestamp")));
                mplew.writeMapleString(repliesRS.getString("content"));
            }
            if (i != replyCount || repliesRS.next()) {
                throw new RuntimeException(String.valueOf(threadRS.getInt("threadid")));
            }
        } else {
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static byte[] showWheelsLeft(int left) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(0x15);
        mplew.write(left);
        return mplew.getPacket();
    }

    public static byte[] showWishList(MapleCharacter mc, boolean update) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        if (update) {
            mplew.write(0x55);
        } else {
            mplew.write(0x4F);
        }

        for (int sn : mc.getCashShop().getWishList()) {
            mplew.writeInt(sn);
        }

        for (int i = mc.getCashShop().getWishList().size(); i < 10; i++) {
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    public static byte[] skillBookSuccess(MapleCharacter chr, int skillid, int maxlevel, boolean canuse, boolean success) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SKILL_LEARN_ITEM_RESULT.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(1);
        mplew.writeInt(skillid);
        mplew.writeInt(maxlevel);
        mplew.write(canuse ? 1 : 0);
        mplew.write(success ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] skillCancel(MapleCharacter from, int skillId) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CANCEL_SKILL_EFFECT.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(skillId);
        return mplew.getPacket();
    }

    public static byte[] skillCooldown(int sid, int time) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.COOLDOWN.getValue());
        mplew.writeInt(sid);
        mplew.writeShort(time);//Int in v97
        return mplew.getPacket();
    }

    public static byte[] skillEffect(MapleCharacter from, int skillId, int level, byte flags, int speed, byte direction) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SKILL_EFFECT.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(skillId);
        mplew.write(level);
        mplew.write(flags);
        mplew.write(speed);
        mplew.write(direction); //Mmmk
        return mplew.getPacket();
    }

    /**
     * Sends a Snowball Message<br>
     * <p>
     * Possible values for <code>message</code>:<br> 1: ... Team's snowball has passed the stage 1.<br> 2: ... Team's
     * snowball has passed the stage 2.<br> 3: ... Team's snowball has passed the stage 3.<br> 4: ... Team is attacking
     * the snowman, stopping the progress<br> 5: ... Team is moving again<br>
     *
     * @param message
     */
    public static byte[] snowballMessage(int team, int message) {
        final MaplePacketWriter mplew = new MaplePacketWriter(7);
        mplew.writeShort(SendOpcode.SNOWBALL_MESSAGE.getValue());
        mplew.write(team);// 0 is down, 1 is up
        mplew.writeInt(message);
        return mplew.getPacket();
    }

    /**
     * Gets a packet to spawn a door.
     *
     * @param oid  The door's object ID.
     * @param pos  The position of the door.
     * @param town
     * @return The remove door packet.
     */
    public static byte[] spawnDoor(int oid, Point pos, boolean town) {
        final MaplePacketWriter mplew = new MaplePacketWriter(11);
        mplew.writeShort(SendOpcode.SPAWN_DOOR.getValue());
        mplew.writeBoolean(town);
        mplew.writeInt(oid);
        mplew.writeLocation(pos);
        return mplew.getPacket();
    }

    public static byte[] spawnDragon(MapleDragon dragon) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SPAWN_DRAGON.getValue());
        mplew.writeInt(dragon.getOwner().getId());//objectid = owner id
        mplew.writeInt(dragon.getPosition().x);
        mplew.writeInt(dragon.getPosition().y);
        mplew.write(dragon.getStance());
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    /**
     * Handles monsters not being targettable, such as Zakum's first body.
     *
     * @param life   The mob to spawn as non-targettable.
     * @param effect The effect to show when spawning.
     * @return The packet to spawn the mob as non-targettable.
     */
    public static byte[] spawnFakeMonster(MapleMonster life, int effect) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(1);
        mplew.writeInt(life.getObjectId());
        mplew.write(5);
        mplew.writeInt(life.getId());
        mplew.skip(15);
        mplew.write(0x88);
        mplew.skip(6);
        mplew.writeLocation(life.getPosition());
        mplew.write(life.getStance());
        mplew.writeShort(0);//life.getStartFh()
        mplew.writeShort(life.getFh());
        if (effect > 0) {
            mplew.write(effect);
            mplew.write(0);
            mplew.writeShort(0);
        }
        mplew.writeShort(-2);
        mplew.write(life.getTeam());
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] spawnGuide(boolean spawn) {
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.SPAWN_GUIDE.getValue());
        if (spawn) {
            mplew.write(1);
        } else {
            mplew.write(0);
        }
        return mplew.getPacket();
    }

    /**
     * Gets a spawn monster packet.
     *
     * @param life     The monster to spawn.
     * @param newSpawn Is it a new spawn?
     * @return The spawn monster packet.
     */
    public static byte[] spawnHPQMonster(MapleMonster life, boolean newSpawn) {
        return spawnMonsterInternal(life, false, newSpawn, false, 0, false);
    }

    public static byte[] spawnHiredMerchant(HiredMerchant hm) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SPAWN_HIRED_MERCHANT.getValue());
        mplew.writeInt(hm.getOwnerId());
        mplew.writeInt(hm.getItemId());
        mplew.writeShort((short) hm.getPosition().getX());
        mplew.writeShort((short) hm.getPosition().getY());
        mplew.writeShort(0);
        mplew.writeMapleString(hm.getOwner());
        mplew.write(0x05);
        mplew.writeInt(hm.getObjectId());
        mplew.writeMapleString(hm.getDescription());
        mplew.write(hm.getItemId() % 10);
        mplew.write(new byte[]{1, 4});
        return mplew.getPacket();
    }

    public static byte[] spawnMist(int oid, int ownerCid, int skill, int level, MapleMist mist) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SPAWN_MIST.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(mist.isMobMist() ? 0 : mist.isPoisonMist() ? 1 : mist.isRecoveryMist() ? 4 : 2); // mob mist = 0, player poison = 1, smokescreen = 2, unknown = 3, recovery = 4
        mplew.writeInt(ownerCid);
        mplew.writeInt(skill);
        mplew.write(level);
        mplew.writeShort(mist.getSkillDelay()); // Skill delay
        mplew.writeInt(mist.getBox().x);
        mplew.writeInt(mist.getBox().y);
        mplew.writeInt(mist.getBox().x + mist.getBox().width);
        mplew.writeInt(mist.getBox().y + mist.getBox().height);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    /**
     * Gets a spawn monster packet.
     *
     * @param life     The monster to spawn.
     * @param newSpawn Is it a new spawn?
     * @return The spawn monster packet.
     */
    public static byte[] spawnMonster(MapleMonster life, boolean newSpawn) {
        return spawnMonsterInternal(life, false, newSpawn, false, 0, false);
    }

    /**
     * Gets a spawn monster packet.
     *
     * @param life     The monster to spawn.
     * @param newSpawn Is it a new spawn?
     * @param effect   The spawn effect.
     * @return The spawn monster packet.
     */
    public static byte[] spawnMonster(MapleMonster life, boolean newSpawn, int effect) {
        return spawnMonsterInternal(life, false, newSpawn, false, effect, false);
    }

    /**
     * Internal function to handler monster spawning and controlling.
     *
     * @param life              The mob to perform operations with.
     * @param requestController Requesting control of mob?
     * @param newSpawn          New spawn (fade in?)
     * @param aggro             Aggressive mob?
     * @param effect            The spawn effect to use.
     * @return The spawn/control packet.
     */
    private static byte[] spawnMonsterInternal(MapleMonster life, boolean requestController, boolean newSpawn, boolean aggro, int effect, boolean makeInvis) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        if (makeInvis) {
            mplew.writeShort(SendOpcode.SPAWN_MONSTER_CONTROL.getValue());
            mplew.write(0);
            mplew.writeInt(life.getObjectId());
            return mplew.getPacket();
        }
        if (requestController) {
            mplew.writeShort(SendOpcode.SPAWN_MONSTER_CONTROL.getValue());
            mplew.write(aggro ? 2 : 1);
        } else {
            mplew.writeShort(SendOpcode.SPAWN_MONSTER.getValue());
        }
        mplew.writeInt(life.getObjectId());
        mplew.write(life.getController() == null ? 5 : 1);
        mplew.writeInt(life.getId());
        mplew.skip(16); // temp_stat bitmask
        mplew.writeLocation(life.getPosition());
        mplew.write(life.getStance());
        mplew.writeShort(life.getFoothold());
        mplew.writeShort(life.getFh());
        if (effect > 0) {
            mplew.writeInt(effect);
        }
        mplew.write(newSpawn ? -2 : -1);
        mplew.write(life.getTeam());
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] spawnNPC(MapleNPC life) {
        final MaplePacketWriter mplew = new MaplePacketWriter(24);
        mplew.writeShort(SendOpcode.SPAWN_NPC.getValue());
        mplew.writeInt(life.getObjectId());
        mplew.writeInt(life.getId());
        mplew.writeShort(life.getPosition().x);
        mplew.writeShort(life.getCy());
        if (life.getF() == 1) {
            mplew.write(0);
        } else {
            mplew.write(1);
        }
        mplew.writeShort(life.getFh());
        mplew.writeShort(life.getRx0());
        mplew.writeShort(life.getRx1());
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] spawnNPCRequestController(MapleNPC life, boolean MiniMap) {
        final MaplePacketWriter mplew = new MaplePacketWriter(23);
        mplew.writeShort(SendOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
        mplew.write(1);
        mplew.writeInt(life.getObjectId());
        mplew.writeInt(life.getId());
        mplew.writeShort(life.getPosition().x);
        mplew.writeShort(life.getCy());
        if (life.getF() == 1) {
            mplew.write(0);
        } else {
            mplew.write(1);
        }
        mplew.writeShort(life.getFh());
        mplew.writeShort(life.getRx0());
        mplew.writeShort(life.getRx1());
        mplew.writeBoolean(MiniMap);
        return mplew.getPacket();
    }

    /**
     * Gets a packet spawning a player as a mapobject to other clients.
     *
     * @param player The character to spawn to other clients.
     * @return The spawn player packet.
     */
    public static byte[] getUserEnterField(MapleCharacter player) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SPAWN_PLAYER.getValue());
        mplew.writeInt(player.getId());
        mplew.write(player.getLevel());
        mplew.writeMapleString(player.getName());
        if (player.getGuildId() < 1) {
            mplew.writeMapleString("");
            mplew.write(new byte[6]);
        } else {
            MapleGuildSummary gs = player.getClient().getWorldServer().getGuildSummary(player.getGuildId(), player.getWorld());
            if (gs != null) {
                mplew.writeMapleString(gs.getName());
                mplew.writeShort(gs.getLogoBG());
                mplew.write(gs.getLogoBGColor());
                mplew.writeShort(gs.getLogo());
                mplew.write(gs.getLogoColor());
            } else {
                mplew.writeMapleString("");
                mplew.write(new byte[6]);
            }
        }
        mplew.write(new byte[16]); // fuck that; CTS encoding
        mplew.write(0);
        mplew.write(0);
        mplew.writeShort(player.getJob().getId());
        addCharLook(mplew, player, false);
        mplew.writeInt(player.getInventory(MapleInventoryType.CASH).countById(5110000));
        mplew.writeInt(player.getItemEffect());
        mplew.writeInt(ItemConstants.getInventoryType(player.getChair()) == MapleInventoryType.SETUP ? player.getChair() : 0);
        mplew.writeLocation(player.getPosition());
        mplew.write(player.getStance());
        mplew.writeShort(player.getFoothold());
        mplew.write(0);
        MaplePet[] pet = player.getPets();
        for (int i = 0; i < 3; i++) {
            if (pet[i] != null) {
                addPetInfo(mplew, pet[i], false);
            }
        }
        mplew.write(0); //end of pets
        if (player.getVehicle() == null) {
            mplew.writeInt(1); // mob level
            mplew.writeLong(0); // mob exp + tiredness
        } else {
            mplew.writeInt(player.getVehicle().getLevel());
            mplew.writeInt(player.getVehicle().getExp());
            mplew.writeInt(player.getVehicle().getTiredness());
        }
        if (player.getPlayerShop() != null && player.getPlayerShop().isOwner(player)) {
            if (player.getPlayerShop().hasFreeSlot()) {
                addAnnounceBox(mplew, player.getPlayerShop(), player.getPlayerShop().getVisitors().length);
            } else {
                addAnnounceBox(mplew, player.getPlayerShop(), 1);
            }
        } else {
            MapleMiniGame game = player.getMiniGame();
            if (game != null && game.isOwner(player)) {
                addAnnounceBox(mplew, game, game.getMode(), game.getPieceType(), game.hasFreeSlot() ? 1 : 2, game.isStarted());
            } else {
                mplew.write(0);
            }
        }
        if (player.getChalkboard() != null) {
            mplew.write(1);
            mplew.writeMapleString(player.getChalkboard());
        } else {
            mplew.write(0);
        }
        encodeRingData(mplew, null);
        encodeRingData(mplew, null);
        encodeMarriageData(mplew, player);
        mplew.skip(3);
        mplew.write(player.getTeam());//only needed in specific fields
        return mplew.getPacket();
    }

    public static byte[] spawnPlayerNPC(PlayerNPC npc) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
        mplew.write(1);
        mplew.writeInt(npc.getObjectId());
        mplew.writeInt(npc.getId());
        mplew.writeShort(npc.getPosition().x);
        mplew.writeShort(npc.getCY());
        mplew.write(1);
        mplew.writeShort(npc.getFH());
        mplew.writeShort(npc.getRX0());
        mplew.writeShort(npc.getRX1());
        mplew.write(1);
        return mplew.getPacket();
    }

    /**
     * Gets a packet to spawn a portal.
     *
     * @param townId   The ID of the town the portal goes to.
     * @param targetId The ID of the target.
     * @param pos      Where to put the portal.
     * @return The portal spawn packet.
     */
    public static byte[] spawnPortal(int townId, int targetId, Point pos) {
        final MaplePacketWriter mplew = new MaplePacketWriter(14);
        mplew.writeShort(SendOpcode.SPAWN_PORTAL.getValue());
        mplew.writeInt(townId);
        mplew.writeInt(targetId);
        if (pos != null) {
            mplew.writeLocation(pos);
        }
        return mplew.getPacket();
    }

    // is there a way to spawn reactors non-animated?
    public static byte[] spawnReactor(MapleReactor reactor) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        Point pos = reactor.getPosition();
        mplew.writeShort(SendOpcode.REACTOR_SPAWN.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.writeInt(reactor.getId());
        mplew.write(1);
        mplew.writeLocation(pos);
        mplew.write(0);
        mplew.writeMapleString("");
        return mplew.getPacket();
    }

    /**
     * Gets a packet to spawn a special map object.
     *
     * @param summon
     * @param animated Animated spawn?
     * @return The spawn packet for the map object.
     */
    public static byte[] spawnSummon(MapleSummon summon, boolean animated) {
        final MaplePacketWriter mplew = new MaplePacketWriter(25);
        mplew.writeShort(SendOpcode.SUMMONED_ENTER_FIELD.getValue());
        mplew.writeInt(summon.getOwner().getId());
        mplew.writeInt(summon.getObjectId());
        mplew.writeInt(summon.getSkill());
        mplew.write(0x0A); //v83
        mplew.write(summon.getSkillLevel());
        mplew.writeLocation(summon.getPosition());
        mplew.skip(3);
        mplew.write(summon.getMovementType().getValue()); // 0 = don't move, 1 = follow (4th mage summons?), 2/4 = only tele follow, 3 = bird follow
        mplew.write(summon.isPuppet() ? 0 : 1); // 0 and the summon can't attack - but puppets don't attack with 1 either ^.-
        mplew.write(animated ? 0 : 1);
        return mplew.getPacket();
    }

    public static byte[] startMapEffect(String msg, int itemid, boolean active) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.BLOW_WEATHER.getValue());
        mplew.write(active ? 0 : 1);
        mplew.writeInt(itemid);
        if (active) {
            mplew.writeMapleString(msg);
        }
        return mplew.getPacket();
    }

    /**
     * Gets a stop control monster packet.
     *
     * @param oid The ObjectID of the monster to stop controlling.
     * @return The stop control monster packet.
     */
    public static byte[] stopControllingMonster(int oid) {
        final MaplePacketWriter mplew = new MaplePacketWriter(7);
        mplew.writeShort(SendOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(0);
        mplew.writeInt(oid);
        return mplew.getPacket();
    }

    public static byte[] summonAttack(int cid, int summonSkillId, byte direction, Pair<Integer, Integer>[] allDamage) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SUMMONED_ATTACK.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(summonSkillId);
        mplew.write(direction);
        mplew.write(4);
        mplew.write(allDamage.length);
        for (Pair<Integer, Integer> pair : allDamage) {
            mplew.writeInt(pair.getLeft());
            mplew.write(6);
            mplew.writeInt(pair.getRight());
        }
        return mplew.getPacket();
    }

    public static byte[] summonSkill(int cid, int summonSkillId, int newStance) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SUMMONED_SKILL.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(summonSkillId);
        mplew.write(newStance);
        return mplew.getPacket();
    }

    public static byte[] takeFromCashInventory(Item item) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x68);
        mplew.writeShort(item.getPosition());
        addItemInfo(mplew, item, true);

        return mplew.getPacket();
    }

    public static byte[] getStorageTakeItem(MapleStorage storage, MapleInventoryType type) {
        MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.STORAGE.getValue());
        w.write(9);
        w.write(storage.getSlotCount());
        w.writeLong(2 << type.getType());
        Set<Item> items = storage.get(type);
        w.write(items.size());
        items.forEach(i -> addItemInfo(w, i, true));
        return w.getPacket();
    }

    public static byte[] getStoragePutItem(MapleStorage storage, MapleInventoryType type) {
        MaplePacketWriter w = new MaplePacketWriter(13 + (storage.size() * 100));
        w.writeShort(SendOpcode.STORAGE.getValue());
        w.write(13);
        w.write(storage.getSlotCount());
        w.writeLong(2 << type.getType());
        Set<Item> items = storage.get(type);
        w.write(items.size());
        items.forEach(i -> addItemInfo(w, i, true));
        return w.getPacket();
    }

    public static byte[] talkGuide(String talk) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.TALK_GUIDE.getValue());
        mplew.write(0);
        mplew.writeMapleString(talk);
        mplew.write(new byte[]{(byte) 0xC8, 0, 0, 0, (byte) 0xA0, (byte) 0x0F, 0, 0});
        return mplew.getPacket();
    }

    public static byte[] transferInventory(List<MTSItemInfo> items) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MTS_OPERATION.getValue());
        mplew.write(0x21);
        mplew.writeInt(items.size());
        if (!items.isEmpty()) {
            for (MTSItemInfo item : items) {
                addItemInfo(mplew, item.getItem(), true);
                mplew.writeInt(item.getID()); //id
                mplew.writeInt(item.getTaxes()); //taxes
                mplew.writeInt(item.getPrice()); //price
                mplew.writeInt(0);
                mplew.writeLong(getTime(item.getEndingDate()));
                mplew.writeMapleString(item.getSeller()); //account name (what was nexon thinking?)
                mplew.writeMapleString(item.getSeller()); //char name
                for (int i = 0; i < 28; i++) {
                    mplew.write(0);
                }
            }
        }
        mplew.write(0xD0 + items.size());
        mplew.write(new byte[]{-1, -1, -1, 0});
        return mplew.getPacket();
    }

    /**
     * @param type  - (0:Light&Long 1:Heavy&Short)
     * @param delay - seconds
     * @return
     */
    public static byte[] trembleEffect(int type, int delay) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.FIELD_EFFECT.getValue());
        mplew.write(1);
        mplew.write(type);
        mplew.writeInt(delay);
        return mplew.getPacket();
    }

    public static byte[] triggerReactor(MapleReactor reactor, int stance) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        Point pos = reactor.getPosition();
        mplew.writeShort(SendOpcode.REACTOR_HIT.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.write(reactor.getState());
        mplew.writeLocation(pos);
        mplew.writeShort(stance);
        mplew.write(0);
        mplew.write(5); // frame delay, set to 5 since there doesn't appear to be a fixed formula for it
        return mplew.getPacket();
    }

    public static byte[] trockRefreshMapList(MapleCharacter chr, boolean delete, boolean vip) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MAP_TRANSFER_RESULT.getValue());
        mplew.write(delete ? 2 : 3);
        if (vip) {
            mplew.write(1);
            List<Integer> map = chr.getVipTrockMaps();
            for (int i = 0; i < 10; i++) {
                mplew.writeInt(map.get(i));
            }
        } else {
            mplew.write(0);
            List<Integer> map = chr.getTrockMaps();
            for (int i = 0; i < 5; i++) {
                mplew.writeInt(map.get(i));
            }
        }
        return mplew.getPacket();
    }

    public static byte[] updateAllianceJobLevel(MapleCharacter mc) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x18);
        mplew.writeInt(mc.getGuild().getAllianceId());
        mplew.writeInt(mc.getGuildId());
        mplew.writeInt(mc.getId());
        mplew.writeInt(mc.getLevel());
        mplew.writeInt(mc.getJob().getId());
        return mplew.getPacket();
    }

    public static byte[] updateAreaInfo(int area, String info) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(0x0A); //0x0B in v95
        mplew.writeShort(area);//infoNumber
        mplew.writeMapleString(info);
        return mplew.getPacket();
    }

    public static byte[] updateAriantPQRanking(String name, int score, boolean empty) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.ARIANT_SCORE.getValue());
        mplew.write(empty ? 0 : 1);
        if (!empty) {
            mplew.writeMapleString(name);
            mplew.writeInt(score);
        }
        return mplew.getPacket();
    }

    public static byte[] updateBuddyCapacity(int capacity) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.BUDDYLIST.getValue());
        mplew.write(0x15);
        mplew.write(capacity);
        return mplew.getPacket();
    }

    public static byte[] updateBuddyChannel(int characterid, int channel) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.BUDDYLIST.getValue());
        mplew.write(0x14);
        mplew.writeInt(characterid);
        mplew.write(0);
        mplew.writeInt(channel);
        return mplew.getPacket();
    }

    public static byte[] updateBuddylist(Collection<BuddylistEntry> buddylist) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.BUDDYLIST.getValue());
        mplew.write(7);
        mplew.write(buddylist.size());
        for (BuddylistEntry buddy : buddylist) {
            if (buddy.isVisible()) {
                mplew.writeInt(buddy.getCharacterId()); // cid
                mplew.writeAsciiString(StringUtil.getRightPaddedStr(buddy.getName(), '\0', 13));
                mplew.write(0); // opposite status
                mplew.writeInt(buddy.getChannel() - 1);
                mplew.writeAsciiString(StringUtil.getRightPaddedStr(buddy.getGroup(), '\0', 13));
                mplew.writeInt(0);//mapid?
            }
        }
        for (int x = 0; x < buddylist.size(); x++) {
            mplew.writeInt(0);//mapid?
        }
        return mplew.getPacket();
    }

    public static byte[] getPlayerModified(MapleCharacter player) {
        return getPlayerModified(player, null, null);
    }

    public static byte[] getPlayerModified(MapleCharacter player, MapleRing coupleRing, MapleRing friendshipRing) {
        final MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.UPDATE_CHAR_LOOK.getValue());
        w.writeInt(player.getId());
        w.write(1);
        addCharLook(w, player, false);
        encodeRingData(w, coupleRing);
        encodeRingData(w, friendshipRing);
        encodeMarriageData(w, player);
        w.writeInt(0);
        return w.getPacket();
    }

    public static byte[] updateDojoStats(MapleCharacter chr, int belt) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(10);
        mplew.write(new byte[]{(byte) 0xB7, 4}); //?
        mplew.writeMapleString("pt=" + chr.getDojoPoints() + ";belt=" + belt + ";tuto=" + (chr.getFinishedDojoTutorial() ? "1" : "0"));
        return mplew.getPacket();
    }

    public static byte[] updateGP(int gid, int GP) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x48);
        mplew.writeInt(gid);
        mplew.writeInt(GP);
        return mplew.getPacket();
    }

    public static byte[] updateGender(byte gender) {
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.SET_GENDER.getValue());
        mplew.write(gender);
        return mplew.getPacket();
    }

    public static byte[] updateHiredMerchant(HiredMerchant hm, MapleCharacter chr) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(CommunityActions.UPDATE_MERCHANT.value);
        mplew.writeInt(chr.getMeso());
        mplew.write(hm.getItems().size());
        for (MaplePlayerShopItem item : hm.getItems()) {
            mplew.writeShort(item.getBundles());
            mplew.writeShort(item.getItem().getQuantity());
            mplew.writeInt(item.getPrice());
            addItemInfo(mplew, item.getItem(), true);
        }
        return mplew.getPacket();
    }

    public static byte[] updateInventorySlotLimit(int type, int newLimit) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.INVENTORY_GROW.getValue());
        mplew.write(type);
        mplew.write(newLimit);
        return mplew.getPacket();
    }

    public static byte[] updateMessengerPlayer(MapleMessengerCharacter member) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.MESSENGER.getValue());
        mplew.write(0x07);
        mplew.write(member.getPosition());
        addCharLook(mplew, member.getPlayer(), true);
        mplew.writeMapleString(member.getUsername());
        mplew.write(member.getChannelID());
        mplew.write(0x00);
        return mplew.getPacket();
    }

    public static byte[] updateMount(int charid, MapleMount mount, boolean levelup) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SET_TAMING_MOB_INFO.getValue());
        mplew.writeInt(charid);
        mplew.writeInt(mount.getLevel());
        mplew.writeInt(mount.getExp());
        mplew.writeInt(mount.getTiredness());
        mplew.write(levelup ? (byte) 1 : (byte) 0);
        return mplew.getPacket();
    }

    public static byte[] updateParty(int forChannel, MapleParty party, PartyOperation op, MaplePartyCharacter target) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        switch (op) {
            case DISBAND:
            case EXPEL:
            case LEAVE:
                mplew.write(0x0C);
                mplew.writeInt(40546);
                mplew.writeInt(target.getPlayerID());
                if (op == PartyOperation.DISBAND) {
                    mplew.write(0);
                    mplew.writeInt(party.getID());
                } else {
                    mplew.write(1);
                    if (op == PartyOperation.EXPEL) {
                        mplew.write(1);
                    } else {
                        mplew.write(0);
                    }
                    mplew.writeMapleString(target.getUsername());
                    addPartyStatus(forChannel, party, mplew, false);
                }
                break;
            case JOIN:
                mplew.write(0xF);
                mplew.writeInt(40546);
                mplew.writeMapleString(target.getUsername());
                addPartyStatus(forChannel, party, mplew, false);
                break;
            case SILENT_UPDATE:
            case LOG_ONOFF:
                mplew.write(0x7);
                mplew.writeInt(party.getID());
                addPartyStatus(forChannel, party, mplew, false);
                break;
            case CHANGE_LEADER:
                mplew.write(0x1B);
                mplew.writeInt(target.getPlayerID());
                mplew.write(0);
                break;
        }
        return mplew.getPacket();
    }


    public static byte[] updatePartyMemberHP(int cid, int curhp, int maxhp) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.UPDATE_PARTYMEMBER_HP.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(curhp);
        mplew.writeInt(maxhp);
        return mplew.getPacket();
    }

    /**
     * Gets an update for specified stats.
     *
     * @param stats The stats to update.
     * @return The stat update packet.
     */
    public static byte[] updatePlayerStats(List<Pair<MapleStat, Integer>> stats, MapleCharacter chr) {
        return updatePlayerStats(stats, false, chr);
    }

    /**
     * Gets an update for specified stats.
     *
     * @param stats        The list of stats to update.
     * @param itemReaction Result of an item reaction(?)
     * @return The stat update packet.
     */
    public static byte[] updatePlayerStats(List<Pair<MapleStat, Integer>> stats, boolean itemReaction, MapleCharacter chr) {
        stats.sort(Comparator.comparingInt(p -> p.getLeft().getValue()));

        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.STAT_CHANGED.getValue());
        mplew.write(itemReaction ? 1 : 0);
        int updateMask = 0;
        for (Pair<MapleStat, Integer> statupdate : stats) {
            updateMask |= statupdate.getLeft().getValue();
        }
        mplew.writeInt(updateMask);
        for (Pair<MapleStat, Integer> statupdate : stats) {
            if (statupdate.getLeft().getValue() >= 1) {
                if (statupdate.getLeft().getValue() == 0x1) {
                    mplew.writeShort(statupdate.getRight().shortValue());
                } else if (statupdate.getLeft().getValue() <= 0x4) {
                    mplew.writeInt(statupdate.getRight());
                } else if (statupdate.getLeft().getValue() < 0x20) {
                    mplew.write(statupdate.getRight().shortValue());
                } else if (statupdate.getLeft().getValue() == MapleStat.AVAILABLESP.getValue()) {
                    if (chr.getJob().isEvan()) {
                        encodeEvanSkillPoints(mplew, chr.getJob().getId());
                    } else {
                        mplew.writeShort(statupdate.getRight().shortValue());
                    }
                } else if (statupdate.getLeft().getValue() < 0xFFFF) {
                    mplew.writeShort(Math.min(statupdate.getRight().shortValue(), Short.MAX_VALUE));
                } else {
                    mplew.writeInt(statupdate.getRight());
                }
            }
        }
        return mplew.getPacket();
    }

    public static byte[] updateQuest(MapleQuestStatus q, boolean infoUpdate) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(infoUpdate ? q.getQuest().getInfoNumber() : q.getQuest().getId());
        if (infoUpdate) {
            mplew.write(1);
        } else {
            mplew.write(q.getStatus().getId());
        }

        mplew.writeMapleString(q.getQuestData());
        return mplew.getPacket();
    }

    public static byte[] updateQuestFinish(short quest, int npc, short nextquest) { //Check
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.UPDATE_QUEST_INFO.getValue()); //0xF2 in v95
        mplew.write(8);//0x0A in v95
        mplew.writeShort(quest);
        mplew.writeInt(npc);
        mplew.writeShort(nextquest);
        return mplew.getPacket();
    }

    public static byte[] updateQuestInfo(short quest, int npc) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(8); //0x0A in v95
        mplew.writeShort(quest);
        mplew.writeInt(npc);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] updateSkill(int skillid, int level, int masterlevel, long expiration) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.UPDATE_SKILLS.getValue());
        mplew.write(1);
        mplew.writeShort(1);
        mplew.writeInt(skillid);
        mplew.writeInt(level);
        mplew.writeInt(masterlevel);
        mplew.writeLong(getTime(expiration));
        mplew.write(4);
        return mplew.getPacket();
    }

    public static byte[] useChalkboard(MapleCharacter chr, boolean close) {
        final MaplePacketWriter mplew = new MaplePacketWriter();
        mplew.writeShort(SendOpcode.CHALKBOARD.getValue());
        mplew.writeInt(chr.getId());
        if (close) {
            mplew.write(0);
        } else {
            mplew.write(1);
            mplew.writeMapleString(chr.getChalkboard());
        }
        return mplew.getPacket();
    }

    private static void writeIntMask(final MaplePacketWriter mplew, Map<MonsterStatus, Integer> stats) {
        int firstmask = 0;
        int secondmask = 0;
        for (MonsterStatus stat : stats.keySet()) {
            if (stat.isFirst()) {
                firstmask |= stat.getValue();
            } else {
                secondmask |= stat.getValue();
            }
        }
        mplew.writeInt(firstmask);
        mplew.writeInt(secondmask);
    }

    private static void encodeBuffMask(final MaplePacketWriter mplew, Set<MapleBuffStat> stats) {
        int[] mask = new int[4];
        for (MapleBuffStat statup : stats) {
            mask[statup.getIndex()] |= statup.getValue();
        }
        for (int i = 3; i >= 0; i--) {
            mplew.writeInt(mask[i]);
        }
    }

    private static void encode16ByteMask(final MaplePacketWriter mplew, List<Pair<MapleDisease, Integer>> statups) {
        long firstmask = 0;
        long secondmask = 0;
        for (Pair<MapleDisease, Integer> statup : statups) {
            if (statup.getLeft().isFirst()) {
                firstmask |= statup.getLeft().getValue();
            } else {
                secondmask |= statup.getLeft().getValue();
            }
        }
        mplew.writeLong(firstmask);
        mplew.writeLong(secondmask);
    }

    public static byte[] wrongPic() {
        final MaplePacketWriter mplew = new MaplePacketWriter(3);
        mplew.writeShort(SendOpcode.CHECK_SPW_RESULT.getValue());
        mplew.write(0);
        return mplew.getPacket();
    }
}
