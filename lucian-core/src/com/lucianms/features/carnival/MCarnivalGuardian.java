package com.lucianms.features.carnival;

import java.awt.*;

/**
 * @author izarooni
 */
public class MCarnivalGuardian {

    private int team;
    private final Point location;
    private final boolean flipped;

    public MCarnivalGuardian(Point location, boolean flipped) {
        this.location = location;
        this.flipped = flipped;
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    public Point getLocation() {
        return location.getLocation();
    }

    public boolean isFlipped() {
        return flipped;
    }
}
