package com.lucianms.server.movement;

import java.awt.*;

public abstract class AbstractLifeMovement implements LifeMovement {

    private Point position;
    private int type;
    private int stance;
    private int duration;

    public AbstractLifeMovement(int type, Point position, int duration, int stance) {
        this.type = type;
        this.position = position;
        this.duration = duration;
        this.stance = stance;
    }

    @Override
    public int getType() {
        return this.type;
    }

    @Override
    public int getDuration() {
        return duration;
    }

    @Override
    public int getStance() {
        return stance;
    }

    @Override
    public Point getPosition() {
        return position;
    }
}
