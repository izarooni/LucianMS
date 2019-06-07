/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation version 3 as published by
the Free Software Foundation. You may not use, modify or distribute
this program under any other version of the GNU Affero General Public
License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.lucianms.server.maps;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.io.scripting.portal.PortalScriptManager;
import com.lucianms.server.MaplePortal;

import java.awt.*;

public class MapleGenericPortal implements MaplePortal {

    private String name;
    private String target;
    private Point position;
    private int targetmap;
    private int type;
    private boolean status = true;
    private int id;
    private String scriptName;
    private boolean portalState;
    private boolean disabled = false;

    public MapleGenericPortal(int type) {
        this.type = type;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Point getPosition() {
        return position;
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public void setPortalStatus(boolean newStatus) {
        this.status = newStatus;
    }

    @Override
    public boolean getPortalStatus() {
        return status;
    }

    @Override
    public int getTargetMapId() {
        return targetmap;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public String getScriptName() {
        return scriptName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPosition(Point position) {
        this.position = position;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setTargetMapId(int targetmapid) {
        this.targetmap = targetmapid;
    }

    @Override
    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    @Override
    public boolean enterPortal(MapleClient c) {
        MapleCharacter player = c.getPlayer();

        if (getScriptName() != null) {
            return PortalScriptManager.executePortalScript(c, this);
        } else if (getTargetMapId() != 999999999 && !disabled) {
            MapleMap dest;
            if (player.getEventInstance() != null) {
                dest = player.getEventInstance().getMapInstance(getTargetMapId());
            } else {
                dest = c.getChannelServer().getMap(getTargetMapId());
            }
            Point destLocation;
            MaplePortal portal = dest.getPortal(getTarget());
            if (portal == null) {
                destLocation = dest.getFootholds().getCenter();
            } else {
                destLocation = portal.getPosition();
            }
            player.changeMap(dest, destLocation);
            return true;
        }
        return false;
    }
}
