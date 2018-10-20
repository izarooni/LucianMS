package net.server.channel.handlers;

import client.MapleCharacter;
import client.autoban.Cheater;
import client.autoban.Cheats;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;

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
        Cheater.CheatEntry entry = player.getCheater().getCheatEntry(Cheats.HealOvertime);

        if (System.currentTimeMillis() - entry.latestOperationTimestamp < 200) {
            entry.spamCount++;
            return null;
        } else {
            entry.spamCount = 0;
        }
        entry.latestOperationTimestamp = System.currentTimeMillis();

        if (incHealth != 0) {
            int abHeal = 500;
            if (player.getMapId() == 105040401 || player.getMapId() == 105040402 || player.getMapId() == 809000101 || player.getMapId() == 809000201) {
                abHeal += 40; // Sleepywood sauna and showa spa...
            }
            if (incHealth > abHeal) {
                entry.cheatCount++;
                entry.latestCheatTimestamp = System.currentTimeMillis();
                entry.announce(getClient(), 5000, "{} now has {} cheat points for fast healing", player.getName(), entry.cheatCount);
            }
            player.addHP(incHealth);
            player.checkBerserk();
        }

        player.addMP(incMana);
        return null;
    }
}
