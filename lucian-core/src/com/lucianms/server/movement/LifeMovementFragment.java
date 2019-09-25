package com.lucianms.server.movement;

import com.lucianms.nio.send.MaplePacketWriter;
import tools.Duplicable;

import java.awt.*;

public interface LifeMovementFragment extends Duplicable<LifeMovementFragment> {

    void serialize(MaplePacketWriter w);

    Point getPosition();
}
