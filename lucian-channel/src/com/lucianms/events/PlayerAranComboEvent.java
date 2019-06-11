package com.lucianms.events;

import com.lucianms.client.MapleBuffStat;
import com.lucianms.client.MapleCharacter;
import com.lucianms.client.SkillFactory;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.client.inventory.MapleWeaponType;
import com.lucianms.constants.skills.Aran;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.BuffContainer;
import com.lucianms.server.MapleStatEffect;
import tools.MaplePacketCreator;

import java.util.Map;

/**
 * @author izarooni
 */
public class PlayerAranComboEvent extends PacketEvent {

    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        int skillID = player.getJob().getId() == 2000 ? Aran.TUTORIAL_SKILL_COMBO : Aran.COMBO_ABILITY;
        if (player.getSkillLevel(skillID) == 0) {
            return null;
        }
        Item weapon = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
        if (weapon != null) {
            int weaponType = MapleWeaponType.getWeaponType(weapon.getItemId());
            if (weaponType != 44) { // pole arm only
                return null;
            }
        }
        short localCombo = player.getCombo();
        player.setCombo(++localCombo);
        getClient().announce(MaplePacketCreator.showCombo(localCombo));
        if (localCombo % 10 == 0 && localCombo / 10 <= 10) {
            MapleStatEffect effect = SkillFactory.getSkill(skillID).getEffect(player);
            long currentTime = System.currentTimeMillis();
            BuffContainer container = new BuffContainer(effect, null, currentTime, localCombo);
            Map<MapleBuffStat, BuffContainer> comboAbilityBuff = Map.of(MapleBuffStat.COMBO_ABILITY_BUFF, container);
            getClient().announce(MaplePacketCreator.setTempStats(comboAbilityBuff));
            player.registerEffect(effect, comboAbilityBuff, currentTime, null);
        }
        return null;
    }
}
