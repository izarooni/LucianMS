package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.MaplePet;
import com.lucianms.client.inventory.PetCommand;
import com.lucianms.constants.ExpTable;
import com.lucianms.nio.receive.MaplePacketReader;
import provider.tools.PetDataProvider;
import tools.MaplePacketCreator;
import tools.Randomizer;

/**
 * @author izarooni
 */
public class PlayerPetCommandEvent extends PacketEvent {

    private byte interaction;
    private int petID;

    @Override
    public void processInput(MaplePacketReader reader) {
        petID = reader.readInt();
        reader.readInt();
        reader.readByte();
        interaction = reader.readByte();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        byte petSlot = player.getPetIndex(petID);
        if (petSlot == -1) {
            return null;
        }
        MaplePet pet = player.getPet(petSlot);
        PetCommand cmd = PetDataProvider.getPetCommand(pet.getItemId(), interaction);
        if (cmd == null) {
            return null;
        }
        boolean failed = Randomizer.nextInt(100) >= cmd.getProbability();
        if (!failed) {
            int closeness = pet.getCloseness();
            if (closeness < 30000) {
                int sum = Math.min(30000, closeness + cmd.getIncrease());
                pet.setCloseness(sum);
                byte petLevel = pet.getLevel();
                if (sum >= ExpTable.getClosenessNeededForLevel(petLevel)) {
                    pet.setLevel(++petLevel);
                    getClient().announce(MaplePacketCreator.getLocalEffectPetLeveled(player.getPetIndex(pet)));
                    player.getMap().sendPacketCheckHidden(player, MaplePacketCreator.getEffectPetLeveled(player, petSlot));
                }
            }
        }
        player.getMap().sendPacketCheckHidden(player, MaplePacketCreator.getPetActionCommand(player.getId(), petSlot, (byte) 0, interaction, failed));
        return null;
    }
}
