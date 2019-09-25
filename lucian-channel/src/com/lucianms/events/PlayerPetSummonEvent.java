package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.client.inventory.MaplePet;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleInventoryManipulator;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.MaplePacketCreator;

import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author izarooni
 */
public class PlayerPetSummonEvent extends PacketEvent {

    private static MapleDataProvider dataRoot = MapleDataProviderFactory.getWZ(new File(System.getProperty("wzpath") + "/Item.wz"));

    private byte slot;
    private boolean leader;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.readInt();
        slot = reader.readByte();
        reader.readByte();
        leader = reader.readByte() == 1;
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MaplePet pet = player.getInventory(MapleInventoryType.CASH).getItem(slot).getPet();
        if (pet == null) {
            return null;
        }
        int petid = pet.getItemId();
        if (petid == 5000028 || petid == 5000047) //Handles Dragon AND Robos
        {
            if (player.haveItem(petid + 1)) {
                player.dropMessage(5, "You can't hatch your " + (petid == 5000028 ? "Dragon egg" : "Robo egg") + " if you already have a Baby " + (petid == 5000028 ? "Dragon." : "Robo."));
                getClient().announce(MaplePacketCreator.enableActions());
                return null;
            } else {
                int evolveid = MapleDataTool.getInt("info/evol1", dataRoot.getData("Pet/" + petid + ".img"));
                int petId = MaplePet.createPet(evolveid);
                if (petId == -1) {
                    return null;
                }
                try (Connection con = getClient().getWorldServer().getConnection()) {
                    try (PreparedStatement ps = con.prepareStatement("DELETE FROM pets WHERE `petid` = ?")) {
                        ps.setInt(1, pet.getUniqueId());
                        ps.executeUpdate();
                    }
                } catch (SQLException ex) {
                    getLogger().error("Unable to create a database connection: {}", ex.getMessage());
                }
                long expiration = player.getInventory(MapleInventoryType.CASH).getItem(slot).getExpiration();
                MapleInventoryManipulator.removeById(getClient(), MapleInventoryType.CASH, petid, (short) 1, false, false);
                MapleInventoryManipulator.addById(getClient(), evolveid, (short) 1, null, petId, expiration);
                getClient().announce(MaplePacketCreator.enableActions());
                return null;
            }
        }
        if (player.getPetIndex(pet) != -1) {
            player.unequipPet(pet, true);
        } else {
            if (player.getSkillLevel(8) == 0 && player.getPet(0) != null) {
                player.unequipPet(player.getPet(0), false);
            }
            if (leader) {
                player.shiftPetsRight();
            }
            Point pos = player.getPosition();
            pos.y -= 12;
            pet.setPos(pos);
            pet.setFh(player.getMap().getFootholds().findBelow(pet.getPos()).getId());
            pet.setStance(0);
            pet.setSummoned(true);
            player.addPet(pet);
            player.getMap().broadcastMessage(player, MaplePacketCreator.showPet(player, pet, false, false), true);
            getClient().announce(MaplePacketCreator.petStatUpdate(player));
            getClient().announce(MaplePacketCreator.enableActions());
//            chr.startFullnessSchedule(PetDataFactory.getHunger(pet.getItemId()), pet, chr.getPetIndex(pet));
        }
        return null;
    }
}
