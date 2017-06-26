package server.partyquest.carnival;

import client.MapleCharacter;
import net.server.world.MapleParty;

/**
 * @author izarooni
 */
public class MCarnivalTeam {

    private final int id;
    private final MapleParty party;

    private int availableCarnivalPoints = 0;
    private int totalCarnivalPoints = 0;
    private int availableSummons = 7;

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

    public int getAvailableSummons() {
        return availableSummons;
    }

    public void setAvailableSummons(int availableSummons) {
        this.availableSummons = availableSummons;
    }
}
