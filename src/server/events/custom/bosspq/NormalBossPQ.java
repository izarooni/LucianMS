package server.events.custom.bosspq;

import java.awt.Point;

import client.MapleCharacter;
import server.events.custom.AbstractBossPQ;

public class NormalBossPQ extends AbstractBossPQ {

	int[] easyBosses = {8510000, 6090001, 6220000, 6300005, 8500001, 9300012, 9300028, 9300151, 9300206};
	
	public NormalBossPQ(MapleCharacter partyleader) {
		super(partyleader);
		this.bosses = easyBosses;
		this.nxWinnings = 7 * easyBosses.length;
		this.nxWinningsMultiplier = 1;
		this.minLevel = 80;
		this.points = 25;
		this.map = 100000000; // the map of the boss pq
		this.monsterSpawnLoc = new Point(-246, 274);
		this.spawnLoc = new Point(-216, 274);
		this.damageMultipier = 2;
		this.healthMultiplier = 2;
	}
	
}
