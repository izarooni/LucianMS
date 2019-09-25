package com.lucianms.server.maps;

import com.lucianms.client.MapleClient;

import java.awt.*;


public interface MapleMapObject {

    int getObjectId();

    void setObjectId(int id);

    MapleMapObjectType getType();

    Point getPosition();

    void setPosition(Point position);

    void sendSpawnData(MapleClient client);

    void sendDestroyData(MapleClient client);

    void nullifyPosition();
}