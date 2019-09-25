package com.lucianms.server.movement;

import com.lucianms.nio.send.MaplePacketWriter;

import java.awt.*;

public class TeleportMovement extends AbsoluteLifeMovement {

    public TeleportMovement(int type, Point position, int duration, int newState, int unk) {
        super(type, position, duration, newState, new Point(), unk);
    }

    @Override
    public LifeMovementFragment duplicate() {
        return new TeleportMovement(getType(),
                getPosition().getLocation(),
                getDuration(),
                getStance(),
                getFoothold());
    }

    @Override
    public void serialize(MaplePacketWriter w) {
        w.write(getType());
        w.writeLocation(getPosition());
        w.writeShort(getFoothold());
        w.write(getStance());
        w.writeShort(getDuration());
    }
}
