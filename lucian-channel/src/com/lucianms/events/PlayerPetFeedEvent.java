package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.SpamTracker;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.client.inventory.MaplePet;
import com.lucianms.constants.ExpTable;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleInventoryManipulator;
import tools.MaplePacketCreator;
import tools.Randomizer;

/**
 * @author izarooni
 */
public class PlayerPetFeedEvent extends PacketEvent {

    private short slot;
    private int itemID;

    @Override
    public void processInput(MaplePacketReader reader) {
        slot = reader.readShort();
        itemID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        SpamTracker.SpamData spamTracker = player.getSpamTracker(SpamTracker.SpamOperation.PetFeeding);
        if (spamTracker.testFor(500)) {
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        }
        spamTracker.record();

        if (player.getNoPets() == 0) {
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        }
        int previousFullness = 100;
        byte slot = 0;
        MaplePet[] pets = player.getPets();
        for (byte i = 0; i < 3; i++) {
            if (pets[i] != null) {
                if (pets[i].getFullness() < previousFullness) {
                    slot = i;
                    previousFullness = pets[i].getFullness();
                }
            }
        }
        MaplePet pet = player.getPet(slot);
        Item use = player.getInventory(MapleInventoryType.USE).getItem(slot);
        if (use == null || (itemID / 10000) != 212 || use.getItemId() != itemID) {
            return null;
        }
        boolean gainCloseness = false;
        if (Randomizer.nextInt(101) > 50) {
            gainCloseness = true;
        }
        if (pet.getFullness() < 100) {
            int newFullness = pet.getFullness() + 30;
            if (newFullness > 100) {
                newFullness = 100;
            }
            pet.setFullness(newFullness);
            if (gainCloseness && pet.getCloseness() < 30000) {
                int newCloseness = pet.getCloseness() + 1;
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
            player.getMap().broadcastMessage(MaplePacketCreator.commandResponse(player.getId(), slot, 0, true));
        } else {
            if (gainCloseness) {
                int newCloseness = pet.getCloseness() - 1;
                if (newCloseness < 0) {
                    newCloseness = 0;
                }
                pet.setCloseness(newCloseness);
                if (pet.getLevel() > 1 && newCloseness < ExpTable.getClosenessNeededForLevel(pet.getLevel())) {
                    pet.setLevel((byte) (pet.getLevel() - 1));
                }
            }
            player.getMap().broadcastMessage(MaplePacketCreator.commandResponse(player.getId(), slot, 0, false));
        }
        MapleInventoryManipulator.removeFromSlot(getClient(), MapleInventoryType.USE, slot, (short) 1, false);

        Item petz = player.getInventory(MapleInventoryType.CASH).getItem(pet.getPosition());
        if (petz == null) { //Not a real fix but fuck it you know?
            return null;
        }

        player.forceUpdateItem(petz);
        return null;
    }
}
