package com.lucianms.server.movement;

import com.lucianms.nio.send.MaplePacketWriter;

import java.awt.*;

public class RelativeLifeMovement extends AbstractLifeMovement {

    public RelativeLifeMovement(int type, Point position, int duration, int stance) {
        super(type, position, duration, stance);
    }

    @Override
    public void serialize(MaplePacketWriter w) {
        w.write(getType());
        w.writeLocation(getPosition());
        w.write(getStance());
        w.writeShort(getDuration());
    }

    @Override
    public LifeMovementFragment duplicate() {
        return new RelativeLifeMovement(getType(),
                getPosition().getLocation(),
                getDuration(),
                getStance());
    }
}
