package com.lucianms.features.carnival;

import com.lucianms.client.MapleCharacter;
import com.lucianms.server.world.MapleParty;

/**
 * @author izarooni
 */
public class MCarnivalTeam {

    private final int id;
    private final MapleParty party;

    private int availableCarnivalPoints;
    private int totalCarnivalPoints;
    private int summonedMonsters;
    private int summonedGuardians;

    public MCarnivalTeam(int id, MapleParty party) {
        this.id = id;
        this.party = party;
    }

    public int getId() {
        return id;
    }

    public MapleParty getParty() {
        return party;
    }

    public int getAvailableCarnivalPoints() {
        return availableCarnivalPoints;
    }

    public void setAvailableCarnivalPoints(int availableCarnivalPoints) {
        this.availableCarnivalPoints = availableCarnivalPoints;
    }

    public void addCarnivalPoints(MapleCharacter player, int carnivalPoints) {
        player.addCP(carnivalPoints);
        availableCarnivalPoints += carnivalPoints;
        totalCarnivalPoints += carnivalPoints;
    }

    public int getTotalCarnivalPoints() {
        return totalCarnivalPoints;
    }

    public void setTotalCarnivalPoints(int totalCarnivalPoints) {
        this.totalCarnivalPoints = totalCarnivalPoints;
    }

    public int getSummonedMonsters() {
        return summonedMonsters;
    }

    public void setSummonedMonsters(int summonedMonsters) {
        this.summonedMonsters = summonedMonsters;
    }

    public int getSummonedGuardians() {
        return summonedGuardians;
    }

    public void setSummonedGuardians(int summonedGuardians) {
        this.summonedGuardians = summonedGuardians;
    }
}
