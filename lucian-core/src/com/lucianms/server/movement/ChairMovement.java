package com.lucianms.server.movement;

import com.lucianms.nio.send.MaplePacketWriter;

import java.awt.*;

public class ChairMovement extends AbstractLifeMovement {
    private int unk;

    public ChairMovement(int type, Point position, int duration, int newState, int unk) {
        super(type, position, duration, newState);
        this.unk = unk;
    }

    @Override
    public void serialize(MaplePacketWriter w) {
        w.write(getType());
        w.writeLocation(getPosition());
        w.writeShort(getUnk());
        w.write(getStance());
        w.writeShort(getDuration());
    }

    @Override
    public LifeMovementFragment duplicate() {
        return new ChairMovement(getType(),
                getPosition().getLocation(),
                getDuration(),
                getStance(),
                getUnk());
    }

    public int getUnk() {
        return unk;
    }

    public void setUnk(int unk) {
        this.unk = unk;
    }
}

