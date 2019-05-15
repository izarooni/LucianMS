package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.MaplePet;
import com.lucianms.client.inventory.PetCommand;
import com.lucianms.client.inventory.PetDataFactory;
import com.lucianms.constants.ExpTable;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;
import tools.Randomizer;

/**
 * @author izarooni
 */
public class PlayerPetCommandEvent extends PacketEvent {

    private byte action;
    private int petID;

    @Override
    public void processInput(MaplePacketReader reader) {
        petID = reader.readInt();
        reader.readInt();
        reader.readByte();
        action = reader.readByte();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        byte petIndex = player.getPetIndex(petID);
        MaplePet pet;
        if (petIndex == -1) {
            return null;
        } else {
            pet = player.getPet(petIndex);
        }
        PetCommand petCommand = PetDataFactory.getPetCommand(pet.getItemId(), action);
        if (petCommand == null) {
            return null;
        }
        boolean success = false;
        if (Randomizer.nextInt(101) <= petCommand.getProbability()) {
            success = true;
            if (pet.getCloseness() < 30000) {
                int newCloseness = pet.getCloseness() + petCommand.getIncrease();
                if (newCloseness > 30000) {
                    newCloseness = 30000;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness >= ExpTable.getClosenessNeededForLevel(pet.getLevel())) {
                    pet.setLevel((byte) (pet.getLevel() + 1));
                    getClient().announce(MaplePacketCreator.showOwnPetLevelUp(player.getPetIndex(pet)));
                    player.getMap().broadcastMessage(MaplePacketCreator.showPetLevelUp(player, player.getPetIndex(pet)));
                }
            }
        }
        player.getMap().broadcastMessage(player, MaplePacketCreator.commandResponse(player.getId(), petIndex, action, success), true);
        return null;
    }
}
