package server.events.custom.bosspq;

import java.awt.Point;

import client.MapleCharacter;
import server.events.custom.AbstractBossPQ;

public class HellBossPQ extends AbstractBossPQ {

	int[] easyBosses = {100100, 100100, 100100, 100100, 100100, 100100};
	
	public HellBossPQ(MapleCharacter partyleader) {
		super(partyleader);
		this.bosses = easyBosses;
		this.nxWinnings = 20 * easyBosses.length;
		this.nxWinningsMultiplier = 1;
		this.minLevel = 150;
		this.points = 75;
		this.map = 100000000; // the map of the boss pq
		this.monsterSpawnLoc = new Point(-246, 274);
		this.spawnLoc = new Point(-216, 274);
		this.damageMultipier = 10;
		this.healthMultiplier = 10;
	}
	
}
