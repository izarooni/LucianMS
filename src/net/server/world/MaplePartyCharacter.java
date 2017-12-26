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
package net.server.world;

import client.MapleCharacter;
import client.MapleJob;
import server.maps.MapleDoor;

import java.util.ArrayList;

public class MaplePartyCharacter {

    private String name;
    private int id;
    private int level;
    private int channel, world;
    private int jobid;
    private int mapid;
    private ArrayList<MapleDoor> door = new ArrayList<>();
    private boolean online;
    private MapleJob job;
    private MapleCharacter character;

    public MaplePartyCharacter(MapleCharacter player) {
        this.character = player;
        this.name = player.getName();
        this.level = player.getLevel();
        this.channel = player.getClient().getChannel();
        this.world = player.getWorld();
        this.id = player.getId();
        this.jobid = player.getJob().getId();
        this.mapid = player.getMapId();
        this.online = true;
        this.job = player.getJob();
        this.door.addAll(player.getDoors());
    }

    public MaplePartyCharacter() {
        this.name = "";
    }

    public MapleCharacter getPlayer() {
        return character;
    }

    public MapleJob getJob() {
        return job;
    }

    public int getLevel() {
        return level;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public int getMapId() {
        return mapid;
    }

    public void setMapId(int mapid) {
        this.mapid = mapid;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public int getJobId() {
        return jobid;
    }

    public void updateDoor(MapleDoor door) {
        this.door.add(door);
    }

    public ArrayList<MapleDoor> getDoors() {
        return door;
    }

    public int getWorld() {
        return world;
    }
}
