package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.lang.GProperties;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.life.FakePlayer;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.movement.AbstractLifeMovement;
import com.lucianms.server.movement.LifeMovementFragment;
import com.lucianms.server.movement.MovementPacketHelper;
import tools.Duplicable;
import tools.Functions;
import tools.MaplePacketCreator;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public final class PlayerMoveEvent extends PacketEvent {

    private List<LifeMovementFragment> movements;
    private Point clientPosition;

    @Override
    public void clean() {
        Functions.requireNotNull(movements, List::clear);
        movements = null;
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
            player.getMap().broadcastGMMessage(player, MaplePacketCreator.movePlayer(player.getId(), clientPosition, movements), false);
        } else {
            player.getMap().broadcastMessage(player, MaplePacketCreator.movePlayer(player.getId(), clientPosition, movements), false);
        }

        FakePlayer fPlayer = player.getFakePlayer();
        if (fPlayer != null && player.isAlive() && fPlayer.isFollowing()) {
            MovementPacketHelper.updatePosition(movements, fPlayer, 0);
            List<LifeMovementFragment> modified = movements.stream().map(Duplicable::duplicate).collect(Collectors.toList());
            modified.stream().filter(m -> m instanceof AbstractLifeMovement)
                    .map(m -> ((AbstractLifeMovement) m))
                    .forEach(m -> m.setDuration((int) (m.getDuration() * 1.3f)));
            player.getMap().broadcastMessage(fPlayer, MaplePacketCreator.movePlayer(fPlayer.getId(), clientPosition, modified), false);
            modified.clear();
        }

        if ((!player.isGM() || player.isDebug()) && ((boolean) player.getMap().getVariables().checkProperty(MapleMap.AUTO_KILL_TOGGLE, true))) {
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
