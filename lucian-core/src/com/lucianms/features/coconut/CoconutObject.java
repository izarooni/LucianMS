package com.lucianms.features.coconut;

import java.awt.*;

/**
 * @author izarooni
 */
public class CoconutObject {

    private final String name;
    private final Point location;
    private boolean canHit;
    private byte result;
    private byte state;

    public CoconutObject(String name, Point location) {
        this.name = name;
        this.location = location;

        result = 1;
    }

    public String getName() {
        return name;
    }

    public Point getLocation() {
        return location.getLocation(); // immutable
    }

    public boolean isCanHit() {
        return canHit;
    }

    public void setCanHit(boolean canHit) {
        this.canHit = canHit;
    }

    public byte getResult() {
        return result;
    }

    public void setResult(byte result) {
        this.result = result;
    }

    public byte getState() {
        return state;
    }

    public void setState(byte state) {
        this.state = state;
    }
}
