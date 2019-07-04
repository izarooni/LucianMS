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
package com.lucianms.server;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.MapleJob;
import com.lucianms.client.SkillFactory;
import com.lucianms.client.inventory.*;
import com.lucianms.constants.EquipSlot;
import com.lucianms.constants.ItemConstants;
import com.lucianms.constants.skills.Assassin;
import com.lucianms.constants.skills.Gunslinger;
import com.lucianms.constants.skills.NightWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import provider.*;
import provider.tools.CharacterProvider;
import provider.wz.MapleDataType;
import tools.Pair;
import tools.Randomizer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormatSymbols;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author Matze
 */
public class MapleItemInformationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapleItemInformationProvider.class);
    private static final MapleItemInformationProvider instance = new MapleItemInformationProvider();
    private MapleDataProvider WzItem;
    private MapleDataProvider WzString;
    private MapleData StringCashImg;
    private MapleData StringConsumeImg;
    private MapleData CashEquipImg;
    private MapleData CashEtcImg;
    private MapleData CashInstallImg;
    private MapleData CashPetImg;

    private HashMap<Integer, Short> slotMaxCache = new HashMap<>();
    private HashMap<Integer, MapleStatEffect> itemEffects = new HashMap<>();
    private HashMap<Integer, Map<String, Integer>> itemDataCache = new HashMap<>();
    private HashMap<Integer, ModifierCoupon> coupons = new HashMap<>(20);
    private HashMap<Integer, Equip> equipCache = new HashMap<>();
    private HashMap<Integer, Double> priceCache = new HashMap<>();
    private HashMap<Integer, Integer> wholePriceCache = new HashMap<>();
    private HashMap<Integer, Integer> projectileWatkCache = new HashMap<>();
    private HashMap<Integer, String> nameCache = new HashMap<>();
    private HashMap<Integer, String> msgCache = new HashMap<>();
    private HashMap<Integer, Boolean> dropRestrictionCache = new HashMap<>();
    private HashMap<Integer, Boolean> pickupRestrictionCache = new HashMap<>();
    private HashMap<Integer, Integer> getMesoCache = new HashMap<>();
    private HashMap<Integer, Integer> monsterBookID = new HashMap<>();
    private HashMap<Integer, Boolean> onEquipUntradableCache = new HashMap<>();
    private HashMap<Integer, scriptedItem> scriptedItemCache = new HashMap<>();
    private HashMap<Integer, Boolean> karmaCache = new HashMap<>();
    private HashMap<Integer, Integer> triggerItemCache = new HashMap<>();
    private HashMap<Integer, Integer> expCache = new HashMap<>();
    private HashMap<Integer, Integer> levelCache = new HashMap<>();
    private HashMap<Integer, Pair<Integer, List<RewardItem>>> rewardCache = new HashMap<>();
    private ArrayList<Pair<Integer, String>> itemNameCache = new ArrayList<>();
    private HashMap<Integer, Boolean> consumeOnPickupCache = new HashMap<>();
    private HashMap<Integer, Boolean> isQuestItemCache = new HashMap<>();
    private HashMap<Integer, String> equipmentSlotCache = new HashMap<>();

    private MapleItemInformationProvider() {
        loadCardIdData();
        WzItem = MapleDataProviderFactory.getWZ("Item.wz");
        WzString = MapleDataProviderFactory.getWZ("String.wz");

        StringCashImg = WzString.getData("Cash.img");
        StringConsumeImg = WzString.getData("Consume.img");

        CashEquipImg = WzString.getData("Eqp.img");
        CashEtcImg = WzString.getData("Etc.img");
        CashInstallImg = WzString.getData("Ins.img");
        CashPetImg = WzString.getData("Pet.img");
    }

    public static MapleItemInformationProvider getInstance() {
        return instance;
    }

    private static short getRandStat(short defaultValue, int maxRange) {
        if (defaultValue == 0) {
            return 0;
        }
        int lMaxRange = (int) Math.min(Math.ceil(defaultValue * 0.1), maxRange);
        return (short) ((defaultValue - lMaxRange) + Math.floor(Randomizer.nextDouble() * (lMaxRange * 2 + 1)));
    }

    public void clearCache() {
        slotMaxCache.clear();
        itemEffects.clear();
        itemDataCache.clear();
        equipCache.clear();
        priceCache.clear();
        wholePriceCache.clear();
        projectileWatkCache.clear();
        nameCache.clear();
        msgCache.clear();
        dropRestrictionCache.clear();
        pickupRestrictionCache.clear();
        getMesoCache.clear();
        monsterBookID.clear();
        onEquipUntradableCache.clear();
        scriptedItemCache.clear();
        karmaCache.clear();
        triggerItemCache.clear();
        expCache.clear();
        levelCache.clear();
        rewardCache.clear();
        itemNameCache.clear();
        consumeOnPickupCache.clear();
        isQuestItemCache.clear();
        equipmentSlotCache.clear();
    }

    @Deprecated
    public List<Pair<Integer, String>> getAllItems() {
        if (!itemNameCache.isEmpty()) {
            return itemNameCache;
        }
        List<Pair<Integer, String>> itemPairs = new ArrayList<>();
        MapleData itemsData;
        itemsData = WzString.getData("Cash.img");
        for (MapleData itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "No-Name")));
        }
        itemsData = WzString.getData("Consume.img");
        for (MapleData itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "No-Name")));
        }
        itemsData = WzString.getData("Eqp.img").getChildByPath("Eqp");
        for (MapleData eqpType : itemsData.getChildren()) {
            for (MapleData itemFolder : eqpType.getChildren()) {
                itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "No-Name")));
            }
        }
        itemsData = WzString.getData("Etc.img").getChildByPath("Etc");
        for (MapleData itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "No-Name")));
        }
        itemsData = WzString.getData("Ins.img");
        for (MapleData itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "No-Name")));
        }
        itemsData = WzString.getData("Pet.img");
        for (MapleData itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "No-Name")));
        }
        return itemPairs;
    }

    public List<Pair<Integer, String>> getAllEtcItems() {
        if (!itemNameCache.isEmpty()) {
            return itemNameCache;
        }

        List<Pair<Integer, String>> itemPairs = new ArrayList<>();
        MapleData itemsData;

        itemsData = WzString.getData("Etc.img").getChildByPath("Etc");
        for (MapleData itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "No-Name")));
        }
        return itemPairs;
    }

    private MapleData getStringData(int itemId) {
        String cat = "null";
        MapleData theData;
        if (itemId >= 4000000 && itemId <= 4400000) {
            theData = CashEtcImg;
            cat = "Etc";
        } else if (itemId >= 5010000) {
            theData = StringCashImg;
        } else if (itemId >= 2000000 && itemId < 3000000) {
            theData = StringConsumeImg;
        } else if ((itemId >= 1010000 && itemId < 1040000) || (itemId >= 1122000 && itemId < 1123000) || (itemId >= 1142000 && itemId < 1143000)) {
            theData = CashEquipImg;
            cat = "Eqp/Accessory";
        } else if (itemId >= 1000000 && itemId < 1010000) {
            theData = CashEquipImg;
            cat = "Eqp/Cap";
        } else if (itemId >= 1102000 && itemId < 1103000) {
            theData = CashEquipImg;
            cat = "Eqp/Cape";
        } else if (itemId >= 1040000 && itemId < 1050000) {
            theData = CashEquipImg;
            cat = "Eqp/Coat";
        } else if (itemId >= 20000 && itemId < 22000) {
            theData = CashEquipImg;
            cat = "Eqp/Face";
        } else if (itemId >= 1080000 && itemId < 1090000) {
            theData = CashEquipImg;
            cat = "Eqp/Glove";
        } else if (itemId >= 30000 && itemId < 32000) {
            theData = CashEquipImg;
            cat = "Eqp/Hair";
        } else if (itemId >= 1050000 && itemId < 1060000) {
            theData = CashEquipImg;
            cat = "Eqp/Longcoat";
        } else if (itemId >= 1060000 && itemId < 1070000) {
            theData = CashEquipImg;
            cat = "Eqp/Pants";
        } else if (ItemConstants.isPetEquip(itemId)) {
            theData = CashEquipImg;
            cat = "Eqp/PetEquip";
        } else if (itemId >= 1112000 && itemId < 1120000) {
            theData = CashEquipImg;
            cat = "Eqp/Ring";
        } else if (itemId >= 1092000 && itemId < 1100000) {
            theData = CashEquipImg;
            cat = "Eqp/Shield";
        } else if (itemId >= 1070000 && itemId < 1080000) {
            theData = CashEquipImg;
            cat = "Eqp/Shoes";
        } else if (itemId >= 1900000 && itemId < 2000000) {
            theData = CashEquipImg;
            cat = "Eqp/Taming";
        } else if (itemId >= 1300000 && itemId < 1800000) {
            theData = CashEquipImg;
            cat = "Eqp/Weapon";
        } else if (itemId >= 4000000 && itemId < 5000000) {
            theData = CashEtcImg;
        } else if (itemId >= 3000000 && itemId < 4000000) {
            theData = CashInstallImg;
        } else if (itemId >= 5000000 && itemId < 5010000) {
            theData = CashPetImg;
        } else {
            return null;
        }
        if (cat.equalsIgnoreCase("null")) {
            return theData.getChildByPath(String.valueOf(itemId));
        } else {
            return theData.getChildByPath(cat + "/" + itemId);
        }
    }

    public boolean noCancelMouse(int itemId) {
        MapleData item = getItemData(itemId);
        return item != null && MapleDataTool.getIntConvert("info/noCancelMouse", item, 0) == 1;
    }

    private MapleData getItemData(int itemId) {
        String idStr = "0" + itemId;
        MapleDataDirectoryEntry root = WzItem.getRoot();
        for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
            for (MapleDataFileEntry iFile : topDir.getFiles()) {
                if (iFile.getName().equals(idStr.substring(0, 4) + ".img")) {
                    MapleData ret = WzItem.getData(topDir.getName() + "/" + iFile.getName());
                    if (ret == null) {
                        LOGGER.warn("Unable to find data node for item {}", itemId);
                        return null;
                    }
                    return ret.getChildByPath(idStr);
                } else if (iFile.getName().equals(idStr.substring(1) + ".img")) {
                    return WzItem.getData(topDir.getName() + "/" + iFile.getName());
                }
            }
        }
        root = CharacterProvider.getProvider().getRoot();
        for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
            for (MapleDataFileEntry iFile : topDir.getFiles()) {
                if (iFile.getName().equals(idStr + ".img")) {
                    return CharacterProvider.getProvider().getData(topDir.getName() + "/" + iFile.getName());
                }
            }
        }
        return null;
    }

    public short getSlotMax(MapleClient c, int itemId) {
        if (slotMaxCache.containsKey(itemId)) {
            return slotMaxCache.get(itemId);
        }
        short ret = 1;
        MapleData item = getItemData(itemId);
        if (item != null) {
            MapleData smEntry = item.getChildByPath("info/slotMax");
            if (smEntry == null) {
                if (ItemConstants.getInventoryType(itemId).getType() == MapleInventoryType.EQUIP.getType()) {
                    ret = 1;
                } else {
                    ret = 100;
                }
            } else {
                ret = (short) MapleDataTool.getInt(smEntry);
                if (ItemConstants.isThrowingStar(itemId)) {
                    if (c.getPlayer().getJob().isA(MapleJob.NIGHTWALKER1)) {
                        ret += c.getPlayer().getSkillLevel(SkillFactory.getSkill(NightWalker.CLAW_MASTERY)) * 10;
                    } else {
                        ret += c.getPlayer().getSkillLevel(SkillFactory.getSkill(Assassin.CLAW_MASTERY)) * 10;
                    }
                } else {
                    ret += c.getPlayer().getSkillLevel(SkillFactory.getSkill(Gunslinger.GUN_MASTERY)) * 10;
                }
            }
        }
        if (!ItemConstants.isRechargable(itemId)) {
            slotMaxCache.put(itemId, ret);
        }
        return ret;
    }

    public int getMeso(int itemId) {
        if (getMesoCache.containsKey(itemId)) {
            return getMesoCache.get(itemId);
        }
        MapleData item = getItemData(itemId);
        if (item == null) {
            return -1;
        }
        int pEntry;
        MapleData pData = item.getChildByPath("info/meso");
        if (pData == null) {
            return -1;
        }
        pEntry = MapleDataTool.getInt(pData);
        getMesoCache.put(itemId, pEntry);
        return pEntry;
    }

    public int getWholePrice(int itemId) {
        if (wholePriceCache.containsKey(itemId)) {
            return wholePriceCache.get(itemId);
        }
        MapleData item = getItemData(itemId);
        if (item == null) {
            return -1;
        }
        int pEntry;
        MapleData pData = item.getChildByPath("info/price");
        if (pData == null) {
            return -1;
        }
        pEntry = MapleDataTool.getInt(pData);
        wholePriceCache.put(itemId, pEntry);
        return pEntry;
    }

    public double getPrice(int itemId) {
        if (priceCache.containsKey(itemId)) {
            return priceCache.get(itemId);
        }
        MapleData item = getItemData(itemId);
        if (item == null) {
            return -1;
        }
        double pEntry;
        MapleData pData = item.getChildByPath("info/unitPrice");
        if (pData != null) {
            try {
                pEntry = MapleDataTool.getDouble(pData);
            } catch (Exception e) {
                pEntry = (double) MapleDataTool.getInt(pData);
            }
        } else {
            pData = item.getChildByPath("info/price");
            if (pData == null) {
                return -1;
            }
            pEntry = (double) MapleDataTool.getInt(pData);
        }
        priceCache.put(itemId, pEntry);
        return pEntry;
    }

    private String getEquipmentSlot(int itemId) {
        if (equipmentSlotCache.containsKey(itemId)) {
            return equipmentSlotCache.get(itemId);
        }

        MapleData item = getItemData(itemId);
        if (item == null) {
            return null;
        }

        MapleData info = item.getChildByPath("info");
        if (info == null) {
            return null;
        }
        String ret = MapleDataTool.getString("islot", info, "");
        equipmentSlotCache.put(itemId, ret);
        return ret;
    }

    public Map<String, Integer> getEquipStats(int itemId) {
        if (itemDataCache.containsKey(itemId)) {
            return itemDataCache.get(itemId);
        }
        Map<String, Integer> ret = new LinkedHashMap<>();
        MapleData item = getItemData(itemId);
        if (item == null) {
            return null;
        }
        MapleData info = item.getChildByPath("info");
        if (info == null) {
            return null;
        }
        for (MapleData data : info.getChildren()) {
            if (data.getName().startsWith("inc")) {
                int value;
                if (data.getType() == MapleDataType.SHORT) {
                    value = Short.parseShort(MapleDataTool.getString(data));
                } else {
                    value = MapleDataTool.getIntConvert(data);
                }
                ret.put(data.getName().substring(3), value);
            }
        }
        ret.put("reqJob", MapleDataTool.getInt("reqJob", info, 0));
        ret.put("reqLevel", MapleDataTool.getInt("reqLevel", info, 0));
        ret.put("reqDEX", MapleDataTool.getInt("reqDEX", info, 0));
        ret.put("reqSTR", MapleDataTool.getInt("reqSTR", info, 0));
        ret.put("reqINT", MapleDataTool.getInt("reqINT", info, 0));
        ret.put("reqLUK", MapleDataTool.getInt("reqLUK", info, 0));
        ret.put("reqPOP", MapleDataTool.getInt("reqPOP", info, 0));
        ret.put("cash", MapleDataTool.getInt("cash", info, 0));
        ret.put("tuc", MapleDataTool.getInt("tuc", info, 0));
        ret.put("cursed", MapleDataTool.getInt("cursed", info, 0));
        ret.put("success", MapleDataTool.getInt("success", info, 0));
        ret.put("fs", MapleDataTool.getInt("fs", info, 0));
        itemDataCache.put(itemId, ret);
        return ret;
    }

    public List<Integer> getScrollReqs(int itemId) {
        List<Integer> ret = new ArrayList<>();
        MapleData data = getItemData(itemId);
        if (data == null) {
            return null;
        }
        data = data.getChildByPath("req");
        if (data == null) {
            return null;
        }
        for (MapleData req : data.getChildren()) {
            ret.add(MapleDataTool.getInt(req));
        }
        return ret;
    }

    public MapleWeaponType getWeaponType(int itemId) {
        int cat = (itemId / 10000) % 100;
        MapleWeaponType[] type = {MapleWeaponType.SWORD1H, MapleWeaponType.GENERAL1H_SWING, MapleWeaponType.GENERAL1H_SWING, MapleWeaponType.DAGGER_OTHER, MapleWeaponType.NOT_A_WEAPON, MapleWeaponType.NOT_A_WEAPON, MapleWeaponType.NOT_A_WEAPON, MapleWeaponType.WAND, MapleWeaponType.STAFF, MapleWeaponType.NOT_A_WEAPON, MapleWeaponType.SWORD2H, MapleWeaponType.GENERAL2H_SWING, MapleWeaponType.GENERAL2H_SWING, MapleWeaponType.SPEAR_STAB, MapleWeaponType.POLE_ARM_SWING, MapleWeaponType.BOW, MapleWeaponType.CROSSBOW, MapleWeaponType.CLAW, MapleWeaponType.KNUCKLE, MapleWeaponType.GUN};
        if (cat < 30 || cat > 49) {
            return MapleWeaponType.NOT_A_WEAPON;
        }
        return type[cat - 30];
    }

    private boolean isCleanSlate(int scrollId) {
        return scrollId > 2048999 && scrollId < 2049004;
    }

    public Item scrollEquipWithId(Item equip, int scrollId, boolean usingWhiteScroll, boolean isGM) {
        if (equip instanceof Equip) {
            Equip nEquip = (Equip) equip;
            Map<String, Integer> stats = this.getEquipStats(scrollId);
            Map<String, Integer> eqstats = this.getEquipStats(equip.getItemId());
            if (((nEquip.getUpgradeSlots() > 0 || isCleanSlate(scrollId)) && Math.ceil(Math.random() * 100.0) <= stats.get("success")) || isGM) {
                short flag = nEquip.getFlag();
                switch (scrollId) {
                    case 2040727:
                        flag |= ItemConstants.SPIKES;
                        nEquip.setFlag((byte) flag);
                        return equip;
                    case 2041058:
                        flag |= ItemConstants.COLD;
                        nEquip.setFlag((byte) flag);
                        return equip;
                    case 2049000:
                    case 2049001:
                    case 2049002:
                    case 2049003:
                        if (nEquip.getLevel() + nEquip.getUpgradeSlots() < eqstats.get("tuc")) {
                            nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() + 1));
                        }
                        break;
                    case 2049100:
                    case 2049101:
                    case 2049102:
                        int inc = 1;
                        if (Randomizer.nextInt(2) == 0) {
                            inc = -1;
                        }
                        if (nEquip.getStr() > 0) {
                            nEquip.setStr((short) Math.max(0, (nEquip.getStr() + Randomizer.nextInt(6) * inc)));
                        }
                        if (nEquip.getDex() > 0) {
                            nEquip.setDex((short) Math.max(0, (nEquip.getDex() + Randomizer.nextInt(6) * inc)));
                        }
                        if (nEquip.getInt() > 0) {
                            nEquip.setInt((short) Math.max(0, (nEquip.getInt() + Randomizer.nextInt(6) * inc)));
                        }
                        if (nEquip.getLuk() > 0) {
                            nEquip.setLuk((short) Math.max(0, (nEquip.getLuk() + Randomizer.nextInt(6) * inc)));
                        }
                        if (nEquip.getWatk() > 0) {
                            nEquip.setWatk((short) Math.max(0, (nEquip.getWatk() + Randomizer.nextInt(6) * inc)));
                        }
                        if (nEquip.getWdef() > 0) {
                            nEquip.setWdef((short) Math.max(0, (nEquip.getWdef() + Randomizer.nextInt(6) * inc)));
                        }
                        if (nEquip.getMatk() > 0) {
                            nEquip.setMatk((short) Math.max(0, (nEquip.getMatk() + Randomizer.nextInt(6) * inc)));
                        }
                        if (nEquip.getMdef() > 0) {
                            nEquip.setMdef((short) Math.max(0, (nEquip.getMdef() + Randomizer.nextInt(6) * inc)));
                        }
                        if (nEquip.getAcc() > 0) {
                            nEquip.setAcc((short) Math.max(0, (nEquip.getAcc() + Randomizer.nextInt(6) * inc)));
                        }
                        if (nEquip.getAvoid() > 0) {
                            nEquip.setAvoid((short) Math.max(0, (nEquip.getAvoid() + Randomizer.nextInt(6) * inc)));
                        }
                        if (nEquip.getSpeed() > 0) {
                            nEquip.setSpeed((short) Math.max(0, (nEquip.getSpeed() + Randomizer.nextInt(6) * inc)));
                        }
                        if (nEquip.getJump() > 0) {
                            nEquip.setJump((short) Math.max(0, (nEquip.getJump() + Randomizer.nextInt(6) * inc)));
                        }
                        if (nEquip.getHp() > 0) {
                            nEquip.setHp((short) Math.max(0, (nEquip.getHp() + Randomizer.nextInt(6) * inc)));
                        }
                        if (nEquip.getMp() > 0) {
                            nEquip.setMp((short) Math.max(0, (nEquip.getMp() + Randomizer.nextInt(6) * inc)));
                        }
                        break;
                    default:
                        for (Entry<String, Integer> stat : stats.entrySet()) {
                            switch (stat.getKey()) {
                                case "STR":
                                    nEquip.setStr((short) (nEquip.getStr() + stat.getValue()));
                                    break;
                                case "DEX":
                                    nEquip.setDex((short) (nEquip.getDex() + stat.getValue()));
                                    break;
                                case "INT":
                                    nEquip.setInt((short) (nEquip.getInt() + stat.getValue()));
                                    break;
                                case "LUK":
                                    nEquip.setLuk((short) (nEquip.getLuk() + stat.getValue()));
                                    break;
                                case "PAD":
                                    nEquip.setWatk((short) (nEquip.getWatk() + stat.getValue()));
                                    break;
                                case "PDD":
                                    nEquip.setWdef((short) (nEquip.getWdef() + stat.getValue()));
                                    break;
                                case "MAD":
                                    nEquip.setMatk((short) (nEquip.getMatk() + stat.getValue()));
                                    break;
                                case "MDD":
                                    nEquip.setMdef((short) (nEquip.getMdef() + stat.getValue()));
                                    break;
                                case "ACC":
                                    nEquip.setAcc((short) (nEquip.getAcc() + stat.getValue()));
                                    break;
                                case "EVA":
                                    nEquip.setAvoid((short) (nEquip.getAvoid() + stat.getValue()));
                                    break;
                                case "Speed":
                                    nEquip.setSpeed((short) (nEquip.getSpeed() + stat.getValue()));
                                    break;
                                case "Jump":
                                    nEquip.setJump((short) (nEquip.getJump() + stat.getValue()));
                                    break;
                                case "MHP":
                                    nEquip.setHp((short) (nEquip.getHp() + stat.getValue()));
                                    break;
                                case "MMP":
                                    nEquip.setMp((short) (nEquip.getMp() + stat.getValue()));
                                    break;
                                case "afterImage":
                                    break;
                            }
                        }
                        break;
                }
                if (!isCleanSlate(scrollId)) {
                    if (!isGM) {
                        nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
                    }
                    nEquip.setLevel((byte) (nEquip.getLevel() + 1));
                }
            } else {
                if (!usingWhiteScroll && !isCleanSlate(scrollId)) {
                    nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
                }
                if (Randomizer.nextInt(101) < stats.get("cursed")) {
                    return null;
                }
            }
        }
        return equip;
    }

    public Equip getEquipById(int equipId) {
        if (equipCache.containsKey(equipId)) {
            return (Equip) equipCache.get(equipId).duplicate();
        }
        Map<String, Integer> stats = this.getEquipStats(equipId);
        if (ItemConstants.getInventoryType(equipId) != MapleInventoryType.EQUIP || stats == null) {
            return null;
        }
        Equip nEquip = new Equip(equipId);
        for (Entry<String, Integer> stat : stats.entrySet()) {
            if (stat.getKey().equals("STR")) {
                nEquip.setStr((short) stat.getValue().intValue());
            } else if (stat.getKey().equals("DEX")) {
                nEquip.setDex((short) stat.getValue().intValue());
            } else if (stat.getKey().equals("INT")) {
                nEquip.setInt((short) stat.getValue().intValue());
            } else if (stat.getKey().equals("LUK")) {
                nEquip.setLuk((short) stat.getValue().intValue());
            } else if (stat.getKey().equals("PAD")) {
                nEquip.setWatk((short) stat.getValue().intValue());
            } else if (stat.getKey().equals("PDD")) {
                nEquip.setWdef((short) stat.getValue().intValue());
            } else if (stat.getKey().equals("MAD")) {
                nEquip.setMatk((short) stat.getValue().intValue());
            } else if (stat.getKey().equals("MDD")) {
                nEquip.setMdef((short) stat.getValue().intValue());
            } else if (stat.getKey().equals("ACC")) {
                nEquip.setAcc((short) stat.getValue().intValue());
            } else if (stat.getKey().equals("EVA")) {
                nEquip.setAvoid((short) stat.getValue().intValue());
            } else if (stat.getKey().equals("Speed")) {
                nEquip.setSpeed((short) stat.getValue().intValue());
            } else if (stat.getKey().equals("Jump")) {
                nEquip.setJump((short) stat.getValue().intValue());
            } else if (stat.getKey().equals("MHP")) {
                nEquip.setHp((short) stat.getValue().intValue());
            } else if (stat.getKey().equals("MMP")) {
                nEquip.setMp((short) stat.getValue().intValue());
            } else if (stat.getKey().equals("tuc")) {
                nEquip.setUpgradeSlots((byte) stat.getValue().intValue());
            } else if (isDropRestricted(equipId)) {
                byte flag = nEquip.getFlag();
                flag |= ItemConstants.UNTRADEABLE;
                nEquip.setFlag(flag);
            } else if (stats.get("fs") > 0) {
                byte flag = nEquip.getFlag();
                flag |= ItemConstants.SPIKES;
                nEquip.setFlag(flag);
            }
            equipCache.put(equipId, nEquip);
        }
        return (Equip) nEquip.duplicate();
    }

    public Equip randomizeStats(Equip equip) {
        equip.setStr(getRandStat(equip.getStr(), 5));
        equip.setDex(getRandStat(equip.getDex(), 5));
        equip.setInt(getRandStat(equip.getInt(), 5));
        equip.setLuk(getRandStat(equip.getLuk(), 5));
        equip.setMatk(getRandStat(equip.getMatk(), 5));
        equip.setWatk(getRandStat(equip.getWatk(), 5));
        equip.setAcc(getRandStat(equip.getAcc(), 5));
        equip.setAvoid(getRandStat(equip.getAvoid(), 5));
        equip.setJump(getRandStat(equip.getJump(), 5));
        equip.setSpeed(getRandStat(equip.getSpeed(), 5));
        equip.setWdef(getRandStat(equip.getWdef(), 10));
        equip.setMdef(getRandStat(equip.getMdef(), 10));
        equip.setHp(getRandStat(equip.getHp(), 10));
        equip.setMp(getRandStat(equip.getMp(), 10));
        return equip;
    }

    public MapleStatEffect getItemEffect(int itemId) {
        MapleStatEffect ret = itemEffects.get(itemId);
        if (ret == null) {
            MapleData item = getItemData(itemId);
            if (item == null) {
                return null;
            }
            MapleData spec = item.getChildByPath("spec");
            ret = MapleStatEffect.loadItemEffectFromData(spec, itemId);
            itemEffects.put(itemId, ret);
        }
        return ret;
    }

    public int[][] getSummonMobs(int itemId) {
        MapleData data = getItemData(itemId);
        if (data == null) {
            return null;
        }
        int theInt = data.getChildByPath("mob").getChildren().size();
        int[][] mobs2spawn = new int[theInt][2];
        for (int x = 0; x < theInt; x++) {
            mobs2spawn[x][0] = MapleDataTool.getIntConvert("mob/" + x + "/id", data);
            mobs2spawn[x][1] = MapleDataTool.getIntConvert("mob/" + x + "/prob", data);
        }
        return mobs2spawn;
    }

    public int getWatkForProjectile(int itemId) {
        Integer atk = projectileWatkCache.get(itemId);
        if (atk != null) {
            return atk;
        }
        MapleData data = getItemData(itemId);
        atk = MapleDataTool.getInt("info/incPAD", data, 0);
        projectileWatkCache.put(itemId, atk);
        return atk;
    }

    public String getName(int itemId) {
        if (nameCache.containsKey(itemId)) {
            return nameCache.get(itemId);
        }
        try {
            MapleData strings = getStringData(itemId);
            if (strings == null) {
                return null;
            }
            String ret = MapleDataTool.getString("name", strings, null);
            nameCache.put(itemId, ret);
            return ret;
        } catch (NullPointerException e) {
            LOGGER.error("Unable to getName of item {}", itemId);
            return null;
        }
    }

    public String getMsg(int itemId) {
        if (msgCache.containsKey(itemId)) {
            return msgCache.get(itemId);
        }
        MapleData strings = getStringData(itemId);
        if (strings == null) {
            return null;
        }
        String ret = MapleDataTool.getString("msg", strings, null);
        msgCache.put(itemId, ret);
        return ret;
    }

    public boolean isDropRestricted(int itemId) {
        if (dropRestrictionCache.containsKey(itemId)) {
            return dropRestrictionCache.get(itemId);
        }
        MapleData data = getItemData(itemId);
        boolean bRestricted = MapleDataTool.getIntConvert("info/tradeBlock", data, 0) == 1;
        if (!bRestricted) {
            bRestricted = MapleDataTool.getIntConvert("info/accountSharable", data, 0) == 1;
        }
        if (!bRestricted) {
            bRestricted = MapleDataTool.getIntConvert("info/quest", data, 0) == 1;
        }
        dropRestrictionCache.put(itemId, bRestricted);
        return bRestricted;
    }

    public boolean isPickupRestricted(int itemId) {
        if (pickupRestrictionCache.containsKey(itemId)) {
            return pickupRestrictionCache.get(itemId);
        }
        MapleData data = getItemData(itemId);
        if (data == null) {
            return false;
        }
        boolean bRestricted = MapleDataTool.getIntConvert("info/only", data, 0) == 1;
        pickupRestrictionCache.put(itemId, bRestricted);
        return bRestricted;
    }

    public Map<String, Integer> getSkillStats(int itemId, double playerJob) {
        Map<String, Integer> ret = new LinkedHashMap<>();
        MapleData item = getItemData(itemId);
        if (item == null) {
            return null;
        }
        MapleData info = item.getChildByPath("info");
        if (info == null) {
            return null;
        }
        for (MapleData data : info.getChildren()) {
            if (data.getName().startsWith("inc")) {
                ret.put(data.getName().substring(3), MapleDataTool.getIntConvert(data));
            }
        }
        ret.put("masterLevel", MapleDataTool.getInt("masterLevel", info, 0));
        ret.put("reqSkillLevel", MapleDataTool.getInt("reqSkillLevel", info, 0));
        ret.put("success", MapleDataTool.getInt("success", info, 0));
        MapleData skill = info.getChildByPath("skill");
        int curskill;
        for (int i = 0; i < skill.getChildren().size(); i++) {
            curskill = MapleDataTool.getInt(Integer.toString(i), skill, 0);
            if (curskill == 0) {
                break;
            }
            if (curskill / 10000 == playerJob) {
                ret.put("skillid", curskill);
                break;
            }
        }
        ret.putIfAbsent("skillid", 0);
        return ret;
    }

    public List<Integer> petsCanConsume(int itemId) {
        List<Integer> ret = new ArrayList<>();
        MapleData data = getItemData(itemId);
        int curPetId;
        for (int i = 0; i < data.getChildren().size(); i++) {
            curPetId = MapleDataTool.getInt("spec/" + Integer.toString(i), data, 0);
            if (curPetId == 0) {
                break;
            }
            ret.add(curPetId);
        }
        return ret;
    }

    public boolean isQuestItem(int itemId) {
        if (isQuestItemCache.containsKey(itemId)) {
            return isQuestItemCache.get(itemId);
        }
        MapleData data = getItemData(itemId);
        boolean questItem = MapleDataTool.getIntConvert("info/quest", data, 0) == 1;
        isQuestItemCache.put(itemId, questItem);
        return questItem;
    }

    public int getQuestIdFromItem(int itemId) {
        MapleData data = getItemData(itemId);
        return MapleDataTool.getIntConvert("info/quest", data, 0);
    }

    private void loadCardIdData() {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT cardid, mobid FROM monstercarddata")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    monsterBookID.put(rs.getInt(1), rs.getInt(2));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to load monster cards from database", e);
        }
    }

    public int getCardMobId(int id) {
        return monsterBookID.get(id);
    }

    public boolean isUntradeableOnEquip(int itemId) {
        if (onEquipUntradableCache.containsKey(itemId)) {
            return onEquipUntradableCache.get(itemId);
        }
        boolean untradableOnEquip = MapleDataTool.getIntConvert("info/equipTradeBlock", getItemData(itemId), 0) > 0;
        onEquipUntradableCache.put(itemId, untradableOnEquip);
        return untradableOnEquip;
    }

    public scriptedItem getScriptedItemInfo(int itemId) {
        if (scriptedItemCache.containsKey(itemId)) {
            return scriptedItemCache.get(itemId);
        }
        if ((itemId / 10000) != 243) {
            return null;
        }
        scriptedItem script = new scriptedItem(MapleDataTool.getIntConvert("spec/npc", getItemData(itemId), 0), MapleDataTool.getString("spec/script", getItemData(itemId), ""), MapleDataTool.getIntConvert("spec/runOnPickup", getItemData(itemId), 0) == 1);
        scriptedItemCache.put(itemId, script);
        return scriptedItemCache.get(itemId);
    }

    public boolean isKarmaAble(int itemId) {
        if (karmaCache.containsKey(itemId)) {
            return karmaCache.get(itemId);
        }
        boolean bRestricted = MapleDataTool.getIntConvert("info/tradeAvailable", getItemData(itemId), 0) > 0;
        karmaCache.put(itemId, bRestricted);
        return bRestricted;
    }

    public int getStateChangeItem(int itemId) {
        if (triggerItemCache.containsKey(itemId)) {
            return triggerItemCache.get(itemId);
        } else {
            int triggerItem = MapleDataTool.getIntConvert("info/stateChangeItem", getItemData(itemId), 0);
            triggerItemCache.put(itemId, triggerItem);
            return triggerItem;
        }
    }

    public int getExpById(int itemId) {
        if (expCache.containsKey(itemId)) {
            return expCache.get(itemId);
        } else {
            int exp = MapleDataTool.getIntConvert("spec/exp", getItemData(itemId), 0);
            expCache.put(itemId, exp);
            return exp;
        }
    }

    public int getMaxLevelById(int itemId) {
        if (levelCache.containsKey(itemId)) {
            return levelCache.get(itemId);
        } else {
            int level = MapleDataTool.getIntConvert("info/maxLevel", getItemData(itemId), 256);
            levelCache.put(itemId, level);
            return level;
        }
    }

    public Pair<Integer, List<RewardItem>> getItemReward(int itemId) {//Thanks Celino, used some stuffs :)
        if (rewardCache.containsKey(itemId)) {
            return rewardCache.get(itemId);
        }
        int totalprob = 0;
        List<RewardItem> rewards = new ArrayList<>();
        for (MapleData child : getItemData(itemId).getChildByPath("reward").getChildren()) {
            RewardItem reward = new RewardItem();
            reward.itemid = MapleDataTool.getInt("item", child, 0);
            reward.prob = (byte) MapleDataTool.getInt("prob", child, 0);
            reward.quantity = (short) MapleDataTool.getInt("count", child, 0);
            reward.effect = MapleDataTool.getString("Effect", child, "");
            reward.worldmsg = MapleDataTool.getString("worldMsg", child, null);
            reward.period = MapleDataTool.getInt("period", child, -1);

            totalprob += reward.prob;

            rewards.add(reward);
        }
        Pair<Integer, List<RewardItem>> hmm = new Pair<>(totalprob, rewards);
        rewardCache.put(itemId, hmm);
        return hmm;
    }

    public boolean isConsumeOnPickup(int itemId) {
        if (consumeOnPickupCache.containsKey(itemId)) {
            return consumeOnPickupCache.get(itemId);
        }
        MapleData data = getItemData(itemId);
        if (data == null) {
            return false;
        }
        boolean consume = MapleDataTool.getIntConvert("spec/consumeOnPickup", data, 0) == 1;
        if (!consume) {
            consume = MapleDataTool.getIntConvert("specEx/consumeOnPickup", data, 0) == 1;
        }
        consumeOnPickupCache.put(itemId, consume);
        return consume;
    }

    public final boolean isTwoHanded(int itemId) {
        switch (getWeaponType(itemId)) {
            case GENERAL2H_SWING:
            case BOW:
            case CLAW:
            case CROSSBOW:
            case POLE_ARM_SWING:
            case SPEAR_STAB:
            case SWORD2H:
            case GUN:
            case KNUCKLE:
                return true;
            default:
                return false;
        }
    }


    public ModifierCoupon getCoupon(int itemID) {
        MapleData info = getItemData(itemID);
        if (info == null) {
            return null;
        } else if ((info = info.getChildByPath("info")) == null) {
            return null;
        }
        ModifierCoupon coupon = new ModifierCoupon();
        Map<Integer, Pair<Integer, Integer>> mTimes = new HashMap<>();
        Number nRate = (Number) info.getChildByPath("rate").getData();
        coupon.rate = nRate.floatValue();
        coupon.times = mTimes;
        for (MapleData times : info.getChildByPath("time").getChildren()) {
            String[] input = ((String) times.getData()).split(":");
            String month = input[0];
            String[] timeRange = input[1].split("-");
            int lBound = Integer.parseInt(timeRange[0]), uBound = Integer.parseInt(timeRange[1]);

            for (int i = 0; i < DateFormatSymbols.getInstance().getShortWeekdays().length; i++) {
                if (DateFormatSymbols.getInstance().getShortWeekdays()[i].equalsIgnoreCase(month)) {
                    mTimes.put(i, new Pair<>(lBound, uBound));
                }
            }
        }
        return coupon;
    }

    public boolean isCash(int itemId) {
        try {
            Map<String, Integer> stats = getEquipStats(itemId);
            return itemId / 1000000 == 5 || (stats != null && stats.get("cash") == 1);
        } catch (NullPointerException e) {
            LOGGER.error("Failed to check if item {} is cash", itemId, e);
            throw e;
        }
    }

    public Collection<Item> canWearEquipment(MapleCharacter chr, Collection<Item> items) {
        MapleInventory inv = chr.getInventory(MapleInventoryType.EQUIPPED);
        if (inv.checked()) {
            return items;
        }
        Collection<Item> itemz = new LinkedList<>();
        if (chr.getJob() == MapleJob.SUPERGM || chr.getJob() == MapleJob.GM) {
            for (Item item : items) {
                Equip equip = (Equip) item;
                equip.wear(true);
                itemz.add(item);
            }
            return itemz;
        }
        int tdex = chr.getDex(), tstr = chr.getStr(), tint = chr.getInt(), tluk = chr.getLuk(), fame = chr.getFame();
        if (chr.getJob() != MapleJob.SUPERGM || chr.getJob() != MapleJob.GM) {
            for (Item item : inv.list()) {
                Equip equip = (Equip) item;
                tdex += equip.getDex();
                tstr += equip.getStr();
                tluk += equip.getLuk();
                tint += equip.getInt();
            }
        }
        for (Item item : items) {
            Equip equip = (Equip) item;
            Map<String, Integer> equipStats = getEquipStats(equip.getItemId());
            if (equipStats == null) {
                continue;
            }
            int reqLevel = equipStats.get("reqLevel");
            if (reqLevel > chr.getLevel()) {
                continue;
            } else if (equipStats.get("reqDEX") > tdex) {
                continue;
            } else if (equipStats.get("reqSTR") > tstr) {
                continue;
            } else if (equipStats.get("reqLUK") > tluk) {
                continue;
            } else if (equipStats.get("reqINT") > tint) {
                continue;
            }
            int reqPOP = equipStats.get("reqPOP");
            if (reqPOP > 0) {
                if (equipStats.get("reqPOP") > fame) {
                    continue;
                }
            }
            equip.wear(true);
            itemz.add(equip);
        }
        inv.checked(true);
        return itemz;
    }

    public boolean canWearEquipment(MapleCharacter chr, Equip equip, int dst) {
        int id = equip.getItemId();

        String islot = getEquipmentSlot(id);

        if (islot != null && !islot.isEmpty() && !EquipSlot.getFromTextSlot(islot).isAllowed(dst, isCash(id))) {
            equip.wear(false);
            return false;
        }

        if (chr.getJob() == MapleJob.SUPERGM || chr.getJob() == MapleJob.GM) {
            equip.wear(true);
            return true;
        }

        int reqLevel = getEquipStats(equip.getItemId()).get("reqLevel");
        //Removed job check. Shouldn't really be needed.
        if (reqLevel > chr.getLevel()) {
            return false;
        } else if (getEquipStats(equip.getItemId()).get("reqDEX") > chr.getTotalDex()) {
            return false;
        } else if (getEquipStats(equip.getItemId()).get("reqLUK") > chr.getTotalLuk()) {
            return false;
        } else if (getEquipStats(equip.getItemId()).get("reqSTR") > chr.getTotalStr()) {
            return false;
        } else if (getEquipStats(equip.getItemId()).get("reqINT") > chr.getTotalInt()) {
            return false;
        }
        int reqPOP = getEquipStats(equip.getItemId()).get("reqPOP");
        if (reqPOP > 0) {
            if (getEquipStats(equip.getItemId()).get("reqPOP") > chr.getFame()) {
                return false;
            }
        }

        equip.wear(true);
        return true;
    }

    public ArrayList<Pair<Integer, String>> getItemDataByName(String name) {
        ArrayList<Pair<Integer, String>> ret = new ArrayList<>();
        for (Pair<Integer, String> itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
            if (itemPair.getRight().toLowerCase().contains(name.toLowerCase())) {
                ret.add(itemPair);
            }
        }
        return ret;
    }

    public List<Pair<String, Integer>> getItemLevelupStats(int itemId, int level, boolean timeless) {
        List<Pair<String, Integer>> list = new LinkedList<>();
        MapleData data = getItemData(itemId);
        MapleData data1 = data.getChildByPath("info").getChildByPath("level");
        if (data1 != null) {
            MapleData data2 = data1.getChildByPath("info").getChildByPath(Integer.toString(level));
            if (data2 != null) {
                for (MapleData da : data2.getChildren()) {
                    if (Math.random() < 0.9) {
                        if (da.getName().startsWith("incDEXMin")) {
                            list.add(new Pair<>("incDEX", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incDEXMax")))));
                        } else if (da.getName().startsWith("incSTRMin")) {
                            list.add(new Pair<>("incSTR", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incSTRMax")))));
                        } else if (da.getName().startsWith("incINTMin")) {
                            list.add(new Pair<>("incINT", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incINTMax")))));
                        } else if (da.getName().startsWith("incLUKMin")) {
                            list.add(new Pair<>("incLUK", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incLUKMax")))));
                        } else if (da.getName().startsWith("incMHPMin")) {
                            list.add(new Pair<>("incMHP", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incMHPMax")))));
                        } else if (da.getName().startsWith("incMMPMin")) {
                            list.add(new Pair<>("incMMP", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incMMPMax")))));
                        } else if (da.getName().startsWith("incPADMin")) {
                            list.add(new Pair<>("incPAD", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incPADMax")))));
                        } else if (da.getName().startsWith("incMADMin")) {
                            list.add(new Pair<>("incMAD", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incMADMax")))));
                        } else if (da.getName().startsWith("incPDDMin")) {
                            list.add(new Pair<>("incPDD", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incPDDMax")))));
                        } else if (da.getName().startsWith("incMDDMin")) {
                            list.add(new Pair<>("incMDD", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incMDDMax")))));
                        } else if (da.getName().startsWith("incACCMin")) {
                            list.add(new Pair<>("incACC", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incACCMax")))));
                        } else if (da.getName().startsWith("incEVAMin")) {
                            list.add(new Pair<>("incEVA", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incEVAMax")))));
                        } else if (da.getName().startsWith("incSpeedMin")) {
                            list.add(new Pair<>("incSpeed", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incSpeedMax")))));
                        } else if (da.getName().startsWith("incJumpMin")) {
                            list.add(new Pair<>("incJump", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incJumpMax")))));
                        }
                    }
                }
            }
        }

        return list;
    }

    public static final class RewardItem {

        public int itemid, period;
        public short prob, quantity;
        public String effect, worldmsg;
    }

    public class scriptedItem {

        private boolean runOnPickup;
        private int npc;
        private String script;

        public scriptedItem(int npc, String script, boolean rop) {
            this.npc = npc;
            this.script = script;
            this.runOnPickup = rop;
        }

        public int getNpc() {
            return npc;
        }

        public String getScript() {
            return script;
        }

        public boolean runOnPickup() {
            return runOnPickup;
        }
    }

    public static class ModifierCoupon {

        private float rate;
        /**
         * Stores the day of the week to range of hours of coupon availability
         */
        private Map<Integer, Pair<Integer, Integer>> times;

        @Override
        public String toString() {
            return String.format("ModifierCoupon{rate=%s, times=%s}", rate, times);
        }

        public float getRate() {
            return rate;
        }

        public Map<Integer, Pair<Integer, Integer>> getTimes() {
            return times;
        }
    }
}
