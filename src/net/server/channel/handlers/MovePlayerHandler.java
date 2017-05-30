package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleStat;
import net.PacketHandler;
import server.TimerManager;
import server.life.FakePlayer;
import server.movement.LifeMovementFragment;
import server.movement.MovementPacketHelper;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.List;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public final class MovePlayerHandler extends PacketHandler {

    private List<LifeMovementFragment> movements = null;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        slea.skip(9);
        movements = MovementPacketHelper.parse(getClient(), slea);
    }

    @Override
    public void onPacket() {
        MapleCharacter player = getClient().getPlayer();

        if (movements != null && !movements.isEmpty()) {
            MovementPacketHelper.updatePosition(movements, player, 0);
            player.getMap().movePlayer(player, player.getPosition());
            if (player.isHidden()) {
                player.getMap().broadcastGMMessage(player, MaplePacketCreator.movePlayer(player.getId(), movements), false);
            } else {
                player.getMap().broadcastMessage(player, MaplePacketCreator.movePlayer(player.getId(), movements), false);
            }

            final FakePlayer fPlayer = player.getFakePlayer();
            if (player.isAlive() && fPlayer != null && fPlayer.isFollowing()) {
                TimerManager.getInstance().schedule(new Runnable() {
                    @Override
                    public void run() {
                        MovementPacketHelper.updatePosition(movements, fPlayer, 0);
                        fPlayer.getMap().broadcastMessage(fPlayer, MaplePacketCreator.movePlayer(fPlayer.getId(), movements), false);
                    }
                }, 100);
            }
        }
        if (player.getMap().getAutoKillPosition() != null) {
            if (player.getPosition().getY() >= player.getMap().getAutoKillPosition().getY()) {
                player.setHp(0);
                player.updateSingleStat(MapleStat.HP, 0);
            }
        }
    }

    public List<LifeMovementFragment> getMovements() {
        return movements;
    }
}
