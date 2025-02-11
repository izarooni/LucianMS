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
package com.lucianms.client.inventory;

import com.lucianms.client.MapleClient;
import com.lucianms.server.MapleItemInformationProvider;
import tools.MaplePacketCreator;
import tools.Pair;

import java.util.ArrayList;
import java.util.List;

public class Equip extends Item {

    public enum ScrollResult {
        FAIL, SUCCESS, CURSE
    }

    private byte upgradeSlots;
    private byte level, flag, itemLevel;
    private short str, dex, $int, luk, hp, mp, watk, matk, wdef, mdef, acc, avoid, hands, speed, jump, vicious;
    private float itemExp;
    private int ringid = -1;
    private int eliminations;
    private boolean wear;
    private boolean regalia;
    private boolean sandbox;

    public Equip(int id) {
        this(id, (short) 0);
    }

    public Equip(int id, short position) {
        this(id, position, 0);
    }

    public Equip(int id, short position, int upgradeSlots) {
        super(id, position, (short) 1);
        this.upgradeSlots = (byte) upgradeSlots;
        this.itemExp = 0;
        this.itemLevel = 1;
    }

    @Override
    public Item duplicate() {
        Equip ret = new Equip(getItemId(), getPosition(), getUpgradeSlots());
        ret.str = str;
        ret.dex = dex;
        ret.$int = $int;
        ret.luk = luk;
        ret.hp = hp;
        ret.mp = mp;
        ret.matk = matk;
        ret.mdef = mdef;
        ret.watk = watk;
        ret.wdef = wdef;
        ret.acc = acc;
        ret.avoid = avoid;
        ret.hands = hands;
        ret.speed = speed;
        ret.jump = jump;
        ret.flag = flag;
        ret.vicious = vicious;
        ret.upgradeSlots = upgradeSlots;
        ret.itemLevel = itemLevel;
        ret.itemExp = itemExp;
        ret.level = level;
        ret.log = new ArrayList<>(log);
        ret.setOwner(getOwner());
        ret.setExpiration(getExpiration());
        ret.setGiftFrom(getGiftFrom());
        return ret;
    }

    @Override
    public byte getFlag() {
        return flag;
    }

    @Override
    public byte getType() {
        return 1;
    }

    public void setStat(String stat, int value) {
        // a better way to do this? reflection maybe
        // is it efficient? no
        switch (stat) {
            case "str":
                this.str = (short) value;
                break;
            case "dex":
                this.dex = (short) value;
                break;
            case "int":
                this.$int = (short) value;
                break;
            case "luk":
                this.luk = (short) value;
                break;
            case "hp":
                this.hp = (short) value;
                break;
            case "mp":
                this.mp = (short) value;
                break;
            case "watk":
                this.watk = (short) value;
                break;
            case "matk":
                this.matk = (short) value;
                break;
            case "wdef":
                this.wdef = (short) value;
                break;
            case "mdef":
                this.mdef = (short) value;
                break;
            case "acc":
                this.acc = (short) value;
                break;
            case "avoid":
                this.avoid = (short) value;
                break;
            case "hands":
                this.hands = (short) value;
                break;
            case "speed":
                this.speed = (short) value;
                break;
            case "jump":
                this.jump = (short) value;
                break;
            case "vic":
                this.vicious = (short) value;
                break;
            case "upgrades":
                this.upgradeSlots = (byte) value;
                break;
        }
    }

    public byte getUpgradeSlots() {
        return upgradeSlots;
    }

    public short getStr() {
        return str;
    }

    public short getDex() {
        return dex;
    }

    public short getInt() {
        return $int;
    }

    public short getLuk() {
        return luk;
    }

    public short getHp() {
        return hp;
    }

    public short getMp() {
        return mp;
    }

    public short getWatk() {
        return watk;
    }

    public short getMatk() {
        return matk;
    }

    public short getWdef() {
        return wdef;
    }

    public short getMdef() {
        return mdef;
    }

    public short getAcc() {
        return acc;
    }

    public short getAvoid() {
        return avoid;
    }

    public short getHands() {
        return hands;
    }

    public short getSpeed() {
        return speed;
    }

    public short getJump() {
        return jump;
    }

    public short getVicious() {
        return vicious;
    }

    @Override
    public void setFlag(byte flag) {
        this.flag = flag;
    }

    public void setStr(short str) {
        this.str = str;
    }

    public void setDex(short dex) {
        this.dex = dex;
    }

    public void setInt(short _int) {
        this.$int = _int;
    }

    public void setLuk(short luk) {
        this.luk = luk;
    }

    public void setHp(short hp) {
        this.hp = hp;
    }

    public void setMp(short mp) {
        this.mp = mp;
    }

    public void setWatk(short watk) {
        this.watk = watk;
    }

    public void setMatk(short matk) {
        this.matk = matk;
    }

    public void setWdef(short wdef) {
        this.wdef = wdef;
    }

    public void setMdef(short mdef) {
        this.mdef = mdef;
    }

    public void setAcc(short acc) {
        this.acc = acc;
    }

    public void setAvoid(short avoid) {
        this.avoid = avoid;
    }

    public void setHands(short hands) {
        this.hands = hands;
    }

    public void setSpeed(short speed) {
        this.speed = speed;
    }

    public void setJump(short jump) {
        this.jump = jump;
    }

    public void setVicious(short vicious) {
        this.vicious = vicious;
    }

    public void setUpgradeSlots(byte upgradeSlots) {
        this.upgradeSlots = upgradeSlots;
    }

    public byte getLevel() {
        return level;
    }

    public void setLevel(byte level) {
        this.level = level;
    }

    public void gainLevel(MapleClient c, boolean timeless) {
        List<Pair<String, Integer>> stats = MapleItemInformationProvider.getInstance().getItemLevelupStats(getItemId(), itemLevel, timeless);
        for (Pair<String, Integer> stat : stats) {
            switch (stat.getLeft()) {
                case "incDEX":
                    dex += stat.getRight();
                    break;
                case "incSTR":
                    str += stat.getRight();
                    break;
                case "incINT":
                    $int += stat.getRight();
                    break;
                case "incLUK":
                    luk += stat.getRight();
                    break;
                case "incMHP":
                    hp += stat.getRight();
                    break;
                case "incMMP":
                    mp += stat.getRight();
                    break;
                case "incPAD":
                    watk += stat.getRight();
                    break;
                case "incMAD":
                    matk += stat.getRight();
                    break;
                case "incPDD":
                    wdef += stat.getRight();
                    break;
                case "incMDD":
                    mdef += stat.getRight();
                    break;
                case "incEVA":
                    avoid += stat.getRight();
                    break;
                case "incACC":
                    acc += stat.getRight();
                    break;
                case "incSpeed":
                    speed += stat.getRight();
                    break;
                case "incJump":
                    jump += stat.getRight();
                    break;
            }
        }
        this.itemLevel++;
        c.announce(MaplePacketCreator.showEquipmentLevelUp());
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.showForeignEffect(c.getPlayer().getId(), 15));
        c.getPlayer().forceUpdateItem(this);
    }

    public int getItemExp() {
        return (int) itemExp;
    }

    public void gainItemExp(MapleClient c, int gain, boolean timeless) {
        int expneeded = timeless ? (10 * itemLevel + 70) : (5 * itemLevel + 65);
        float modifier = 364 / expneeded;
        float exp = (expneeded / (1000000 * modifier * modifier)) * gain;
        itemExp += exp;
        if (itemExp >= 364) {
            itemExp = (itemExp - 364);
            gainLevel(c, timeless);
        } else {
            c.getPlayer().forceUpdateItem(this);
        }
    }

    public void setItemExp(int exp) {
        this.itemExp = exp;
    }

    public void setItemLevel(byte level) {
        this.itemLevel = level;
    }

    @Override
    public void setQuantity(short quantity) {
        throw new UnsupportedOperationException("Cannot change quantity of an Equip");
    }

    public void setUpgradeSlots(int i) {
        this.upgradeSlots = (byte) i;
    }

    public void setVicious(int i) {
        this.vicious = (short) i;
    }

    public int getRingId() {
        return ringid;
    }

    public void setRingId(int id) {
        this.ringid = id;
    }

    public boolean isWearing() {
        return wear;
    }

    public void wear(boolean yes) {
        wear = yes;
    }

    public boolean isRegalia() {
        return regalia;
    }

    public void setRegalia(boolean regalia) {
        this.regalia = regalia;
    }

    public boolean isSandbox() {
        return sandbox;
    }

    public void setSandbox(boolean sandbox) {
        this.sandbox = sandbox;
    }

    public byte getItemLevel() {
        return itemLevel;
    }

    public int getEliminations() {
        return eliminations;
    }

    public void setEliminations(int eliminations) {
        this.eliminations = eliminations;
    }
}