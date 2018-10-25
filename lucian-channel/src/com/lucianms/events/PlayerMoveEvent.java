package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.lang.GProperties;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.life.FakePlayer;
import com.lucianms.server.movement.LifeMovementFragment;
import com.lucianms.server.movement.MovementPacketHelper;
import tools.MaplePacketCreator;

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
    public void clean() {
        movements.clear();
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.skip(1);
        reader.skip(4);
        clientPosition = reader.readPoint();
        movements = MovementPacketHelper.parse(getClient(), reader);
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();

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

        if ((!player.isGM() || (player.isGM() && player.isDebug()))) {
            GProperties<Point> akp = player.getMap().getAutoKillPositions();
            Point position;
            if ((position = akp.get("left")) != null && clientPosition.x <= position.x) {
                player.setHpMp(0);
            }
            if ((position = akp.get("right")) != null && clientPosition.x >= position.x) {
                player.setHpMp(0);
            }
            if ((position = akp.get("up")) != null && clientPosition.y <= position.y) {
                player.setHpMp(0);
            }
            if ((position = akp.get("down")) != null && clientPosition.y >= position.y) {
                player.setHpMp(0);
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
