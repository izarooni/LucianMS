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
package net;

import com.lucianms.nio.RecvOpcode;
import net.server.channel.handlers.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Deprecated
public final class PacketProcessor {

    private final static Map<String, PacketProcessor> instances = new LinkedHashMap<>();
    private MaplePacketHandler[] handlers;

    private PacketProcessor() {
        int maxRecvOp = 0;
        for (RecvOpcode op : RecvOpcode.values()) {
            if (op.getValue() > maxRecvOp) {
                maxRecvOp = op.getValue();
            }
        }
        handlers = new MaplePacketHandler[maxRecvOp + 1];
    }

    public MaplePacketHandler getHandler(short packetId) {
        if (packetId > handlers.length) {
            return null;
        }
        MaplePacketHandler handler = handlers[packetId];
        if (handler != null) {
            return handler;
        }
        return null;
    }

    public void registerHandler(RecvOpcode code, MaplePacketHandler handler) {
        try {
            handlers[code.getValue()] = handler;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Error registering handler - " + code.name());
        }
    }

    public synchronized static PacketProcessor getProcessor(int world, int channel) {
        final String lolpair = world + " " + channel;
        PacketProcessor processor = instances.get(lolpair);
        if (processor == null) {
            processor = new PacketProcessor();
            processor.reset(channel);
            instances.put(lolpair, processor);
        }
        return processor;
    }

    public void reset(int channel) {
        handlers = new MaplePacketHandler[handlers.length];

        if (channel >= 0) {//login
            //CHANNEL HANDLERS
            registerHandler(RecvOpcode.USE_ITEMEFFECT, new UseItemEffectHandler());
            registerHandler(RecvOpcode.BBS_OPERATION, new BBSOperationHandler());
            registerHandler(RecvOpcode.MESSENGER, new MessengerHandler());
            registerHandler(RecvOpcode.CHECK_CASH, new TouchingCashShopHandler());
            registerHandler(RecvOpcode.SPAWN_PET, new SpawnPetHandler());
            registerHandler(RecvOpcode.PET_COMMAND, new PetCommandHandler());
            registerHandler(RecvOpcode.PET_FOOD, new PetFoodHandler());
            registerHandler(RecvOpcode.PET_LOOT, new PetLootHandler());
            registerHandler(RecvOpcode.AUTO_AGGRO, new AutoAggroHandler());
            registerHandler(RecvOpcode.MONSTER_BOMB, new MonsterBombHandler());
            registerHandler(RecvOpcode.CANCEL_DEBUFF, new CancelDebuffHandler());
            registerHandler(RecvOpcode.USE_SKILL_BOOK, new SkillBookHandler());
            registerHandler(RecvOpcode.SKILL_MACRO, new SkillMacroHandler());
            registerHandler(RecvOpcode.NOTE_ACTION, new NoteActionHandler());
            registerHandler(RecvOpcode.USE_MOUNT_FOOD, new UseMountFoodHandler());
            registerHandler(RecvOpcode.PET_AUTO_POT, new PetAutoPotHandler());
            registerHandler(RecvOpcode.TROCK_ADD_MAP, new TrockAddMapHandler());
            registerHandler(RecvOpcode.MOB_DAMAGE_MOB, new MobDamageMobHandler());
            registerHandler(RecvOpcode.REPORT, new ReportHandler());
            registerHandler(RecvOpcode.MONSTER_BOOK_COVER, new MonsterBookCoverHandler());
            registerHandler(RecvOpcode.AUTO_DISTRIBUTE_AP, new AutoAssignHandler());
            registerHandler(RecvOpcode.MAKER_SKILL, new MakerSkillHandler());
            registerHandler(RecvOpcode.ADD_FAMILY, new FamilyAddHandler());
            registerHandler(RecvOpcode.USE_FAMILY, new FamilyUseHandler());
            registerHandler(RecvOpcode.SCRIPTED_ITEM, new ScriptedItemHandler());
            registerHandler(RecvOpcode.BEHOLDER, new BeholderHandler());
            registerHandler(RecvOpcode.ALLIANCE_OPERATION, new AllianceOperationHandler());
            registerHandler(RecvOpcode.USE_SOLOMON_ITEM, new UseSolomonHandler());
            registerHandler(RecvOpcode.USE_GACHA_EXP, new UseGachaExpHandler());
            registerHandler(RecvOpcode.ACCEPT_FAMILY, new AcceptFamilyHandler());
            registerHandler(RecvOpcode.USE_DEATHITEM, new UseDeathItemHandler());
            registerHandler(RecvOpcode.USE_MAPLELIFE, new UseMapleLifeHandler());
            registerHandler(RecvOpcode.USE_CATCH_ITEM, new UseCatchItemHandler());
            registerHandler(RecvOpcode.PARTY_SEARCH_REGISTER, new PartySearchRegisterHandler());
            registerHandler(RecvOpcode.ITEM_SORT2, new ItemIdSortHandler());
            registerHandler(RecvOpcode.LEFT_KNOCKBACK, new LeftKnockbackHandler());
            registerHandler(RecvOpcode.SNOWBALL, new SnowballHandler());
            registerHandler(RecvOpcode.COCONUT, new CoconutHandler());
            registerHandler(RecvOpcode.FREDRICK_ACTION, new FredrickHandler());
            registerHandler(RecvOpcode.REMOTE_STORE, new RemoteStoreHandler());
            registerHandler(RecvOpcode.WEDDING_ACTION, new WeddingHandler());
        }
    }
}