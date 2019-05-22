package com.lucianms.server.movement;

import com.lucianms.nio.send.MaplePacketWriter;

import java.awt.*;

public class JumpDownMovement extends AbstractLifeMovement {
    private Point pixelsPerSecond;
    private int unk;
    private int fh;

    public JumpDownMovement(int type, Point position, int duration, int newstate, Point pixelsPerSecond, int unk, int fh) {
        super(type, position, duration, newstate);
        this.pixelsPerSecond = pixelsPerSecond;
        this.unk = unk;
        this.fh = fh;
    }

    @Override
    public void serialize(MaplePacketWriter w) {
        w.write(getType());
        w.writeLocation(getPosition());
        w.writeLocation(getPixelsPerSecond());
        w.writeShort(getUnk());
        w.writeShort(getFH());
        w.write(getStance());
        w.writeShort(getDuration());
    }

    @Override
    public LifeMovementFragment duplicate() {
        return new JumpDownMovement(getType(),
                getPosition().getLocation(),
                getDuration(),
                getStance(),
                getPixelsPerSecond(),
                getUnk(),
                getFH());
    }

    public Point getPixelsPerSecond() {
        return pixelsPerSecond;
    }

    public void setPixelsPerSecond(Point wobble) {
        this.pixelsPerSecond = wobble;
    }

    public int getUnk() {
        return unk;
    }

    public void setUnk(int unk) {
        this.unk = unk;
    }

    public int getFH() {
        return fh;
    }

    public void setFH(int fh) {
        this.fh = fh;
    }
}
