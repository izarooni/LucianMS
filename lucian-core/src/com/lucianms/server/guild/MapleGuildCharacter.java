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
package com.lucianms.server.guild;

import com.lucianms.client.MapleCharacter;

public class MapleGuildCharacter {
    private int level;
    private int id;
    private int world, channel;
    private int jobid;
    private int guildrank;
    private int guildid;
    private int allianceRank;
    private boolean online;
    private String name;

    public MapleGuildCharacter(MapleCharacter c) {
        this.name = c.getName();
        this.level = c.getLevel();
        this.id = c.getId();
        this.channel = c.getClient().getChannel();
        this.world = c.getWorld();
        this.jobid = c.getJob().getId();
        this.guildrank = c.getGuildRank();
        this.guildid = c.getGuildId();
        this.online = true;
        this.allianceRank = c.getAllianceRank();
    }

    public MapleGuildCharacter(int _id, int _lv, String _name, int _channel, int _world, int _job, int _rank, int _gid, boolean _on, int _allianceRank) {
        this.level = _lv;
        this.id = _id;
        this.name = _name;
        if (_on) {
            this.channel = _channel;
            this.world = _world;
        }
        this.jobid = _job;
        this.online = _on;
        this.guildrank = _rank;
        this.guildid = _gid;
        this.allianceRank = _allianceRank;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int l) {
        level = l;
    }

    public int getId() {
        return id;
    }

    public void setChannel(int ch) {
        channel = ch;
    }

    public int getChannel() {
        return channel;
    }

    public int getWorld() {
        return world;
    }

    public int getJobId() {
        return jobid;
    }

    public void setJobId(int job) {
        jobid = job;
    }

    public int getGuildId() {
        return guildid;
    }

    public void setGuildId(int gid) {
        guildid = gid;
    }

    public void setGuildRank(int rank) {
        guildrank = rank;
    }

    public int getGuildRank() {
        return guildrank;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean f) {
        online = f;
    }

    public String getName() {
        return name;
    }

    public void setAllianceRank(int rank) {
        allianceRank = rank;
    }

    public int getAllianceRank() {
        return allianceRank;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MapleGuildCharacter)) {
            return false;
        }
        MapleGuildCharacter o = (MapleGuildCharacter) other;
        return (o.getId() == id && o.getName().equals(name));
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 19 * hash + this.id;
        hash = 19 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
}
