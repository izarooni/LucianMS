package com.lucianms.server.maps;


import java.awt.*;

public abstract class AbstractMapleMapObject implements MapleMapObject {

    private Point position = new Point();
    private int objectId;

    @Override
    public abstract MapleMapObjectType getType();

    @Override
    public Point getPosition() {
        return position.getLocation();
    }

    @Override
    public void setPosition(Point position) {
        this.position.x = position.x;
        this.position.y = position.y;
    }

    @Override
    public int getObjectId() {
        return objectId;
    }

    @Override
    public void setObjectId(int id) {
        this.objectId = id;
    }

    @Override
    public void nullifyPosition() {
        this.position = null;
    }
}
