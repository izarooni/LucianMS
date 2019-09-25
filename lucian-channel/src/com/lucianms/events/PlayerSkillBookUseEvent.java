package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.Skill;
import com.lucianms.client.SkillFactory;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.events.PacketEvent;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MapleItemInformationProvider;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.util.Map;

/**
 * @author izarooni
 */
public class PlayerSkillBookUseEvent extends PacketEvent {

    private short slot;
    private int itemID;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.readInt();
        slot = reader.readShort();
        itemID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (!player.isAlive()) {
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        }
        Item toUse = player.getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse != null && toUse.getQuantity() == 1) {
            if (toUse.getItemId() != itemID) {
                return null;
            }
            Map<String, Integer> skilldata = MapleItemInformationProvider.getInstance().getSkillStats(toUse.getItemId(), player.getJob().getId());
            boolean canuse;
            boolean success = false;
            int skill = 0;
            int maxlevel = 0;
            if (skilldata == null) {
                return null;
            }
            Skill skill2 = SkillFactory.getSkill(skilldata.get("skillid"));
            if (skilldata.get("skillid") == 0) {
                canuse = false;
            } else if ((player.getSkillLevel(skill2) >= skilldata.get("reqSkillLevel") || skilldata.get("reqSkillLevel") == 0) && player.getMasterLevel(skill2) < skilldata.get("masterLevel")) {
                canuse = true;
                if (Randomizer.nextInt(101) < skilldata.get("success") && skilldata.get("success") != 0) {
                    success = true;

                    player.changeSkillLevel(skill2, player.getSkillLevel(skill2), Math.max(skilldata.get("masterLevel"), player.getMasterLevel(skill2)), -1);
                } else {
                    success = false;
                    //player.dropMessage("The skill book lights up, but the skill winds up as if nothing happened.");
                }
                MapleInventoryManipulator.removeFromSlot(getClient(), MapleInventoryType.USE, slot, (short) 1, false);
            } else {
                canuse = false;
            }
            player.getClient().announce(MaplePacketCreator.skillBookSuccess(player, skill, maxlevel, canuse, success));
        }
        return null;
    }
}
