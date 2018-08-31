package com.lucianms.server.events.channel;

import client.MapleCharacter;
import client.MapleStat;
import com.lucianms.scheduler.TaskExecutor;
import net.PacketEvent;
import server.life.FakePlayer;
import server.movement.LifeMovementFragment;
import server.movement.MovementPacketHelper;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.awt.*;
import java.util.List;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public final class PlayerMoveEvent extends PacketEvent {

    private List<LifeMovementFragment> movements = null;
    private Point clientPosition;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        slea.skip(1);
        slea.skip(4);
        clientPosition = slea.readPos();
        movements = MovementPacketHelper.parse(getClient(), slea);
    }

    @Override
    public Object onPacket() {
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
                TaskExecutor.createTask(new Runnable() {
                    @Override
                    public void run() {
                        MovementPacketHelper.updatePosition(movements, fPlayer, 0);
                        fPlayer.getMap().broadcastMessage(fPlayer, MaplePacketCreator.movePlayer(fPlayer.getId(), movements), false);
                    }
                }, 100);
            }
        }
        if ((!player.isGM() || (player.isGM() && player.isDebug())) && player.getMap().getAutoKillPosition() != null) {
            if (player.getPosition().getY() >= player.getMap().getAutoKillPosition().getY()) {
                player.setHp(0);
                player.updateSingleStat(MapleStat.HP, 0);
            }
        }
        return null;
    }

    public List<LifeMovementFragment> getMovements() {
        return movements;
    }

    public Point getClientPosition() {
        return clientPosition;
    }
}
