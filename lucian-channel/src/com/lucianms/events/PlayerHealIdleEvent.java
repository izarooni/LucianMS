package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.SpamTracker;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author izaroooni
 */
public class PlayerHealIdleEvent extends PacketEvent {

    private int timestamp;
    private short incHealth;
    private short incMana;

    @Override
    public void processInput(MaplePacketReader reader) {
        timestamp = reader.readInt();
        reader.skip(4);
        incHealth = reader.readShort();
        incMana = reader.readShort();

        if (incHealth > 1000) {
            getLogger().warn("Abnormal slow health recovery value {} from {}", incHealth, getClient().getPlayer().getName());
            setCanceled(true);
        }
        if (incMana > 1000) {
            getLogger().warn("Abnormal slow mana recovery value {} from {}", incMana, getClient().getPlayer().getName());
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        SpamTracker.SpamData spamTracker = player.getSpamTracker(SpamTracker.SpamOperation.IdleHeal);
        if (spamTracker.testFor(1000) && spamTracker.getTriggers() > 10) {
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        }
        spamTracker.record();
        if (incHealth != 0) {
            int abHeal = 500;
            if (player.getMapId() == 105040401 || player.getMapId() == 105040402 || player.getMapId() == 809000101 || player.getMapId() == 809000201) {
                abHeal += 40; // Sleepywood sauna and showa spa...
            }
            player.addHP(incHealth);
            player.checkBerserk();
        }

        player.addMP(incMana);
        return null;
    }
}
