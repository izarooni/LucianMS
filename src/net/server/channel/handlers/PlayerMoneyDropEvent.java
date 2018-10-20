package net.server.channel.handlers;

import client.MapleCharacter;
import client.SpamTracker;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerMoneyDropEvent extends PacketEvent {

    private int money;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.readInt();
        money = reader.readInt();
        if (money > 50000 || money < 10 || money > getClient().getPlayer().getMeso()) {
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (!player.isAlive()) {
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        }
        SpamTracker.SpamData spammer = player.getSpamTracker(SpamTracker.SpamOperation.MoneyDrop);
        if (spammer.testFor(500)) {
            player.sendMessage(5, "You are doing that too fast!");
            return null;
        }
        spammer.record();
        player.gainMeso(-money, false, true, false);
        player.getMap().spawnMesoDrop(money, player.getPosition(), player, player, true, (byte) 2);
        return null;
    }
}