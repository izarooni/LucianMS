package server.events.custom.bosspq;

import java.awt.Point;

import client.MapleCharacter;
import server.events.custom.AbstractBossPQ;

public class HellBossPQ extends AbstractBossPQ {

	int[] easyBosses = {8170000, 8220012, 8820001, 8820002, 8820003, 8820004, 8820005, 8820006, 8830000, 8830001, 8830002, 9001014};
	
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
