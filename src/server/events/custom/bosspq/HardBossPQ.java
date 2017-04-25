package server.events.custom.bosspq;

import java.awt.Point;

import client.MapleCharacter;
import server.events.custom.AbstractBossPQ;

public class HardBossPQ extends AbstractBossPQ {

	int[] easyBosses = {7220003, 8220011, 8220010, 8800000, 8810000, 8810001, 8810003, 9001010, 9001009, 9300158};
	
	public HardBossPQ(MapleCharacter partyleader) {
		super(partyleader);
		this.bosses = easyBosses;
		this.nxWinnings = 12 * easyBosses.length;
		this.nxWinningsMultiplier = 2;
		this.minLevel = 100;
		this.points = 25;
		this.map = 100000000; // the map of the boss pq
		this.monsterSpawnLoc = new Point(-246, 274);
		this.spawnLoc = new Point(-216, 274);
		this.damageMultipier = 4;
		this.healthMultiplier = 4;
	}
	
}
