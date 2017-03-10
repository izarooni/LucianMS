package client.arcade;

import client.MapleCharacter;
import tools.MaplePacketCreator;

public class CrowOnly extends Arcade {

	int highscore = 0;
	int monsterId = 9400000;
	boolean touched = false;
	
	public CrowOnly(MapleCharacter player) {
		super(player);
		this.mapId = 677000008;
		this.arcadeId = 3;
	}

	@Override
	public boolean fail() {
		if(touched) {
		player.setArcade(null);
		player.changeMap(910000000, 0);
		saveData(highscore);
		player.announce(MaplePacketCreator.serverNotice(1, "Game Over!"));
		if(saveData(highscore)) {
			player.dropMessage(5, "[Game Over] Your new highscore for Crow Only is " + highscore);
		} else {
			player.dropMessage(5, "[Game Over] Your highscore for Crow Only remains at " + Arcade.getHighscore(arcadeId, player));
		}
		}
		return true;
	}

	@Override
	public void add() {
		++highscore;
		player.announce(MaplePacketCreator.sendHint("#e[Crow only]#n\r\nYou have killed #r" + highscore + "#k crow(s)!", 300, 40));
		
	}

	@Override
	public void onKill(int monster) {
		if(monster == monsterId) {
			add();
		}
	}

	@Override
	public void onHit(int monster) {
		if(monster == monsterId) {
			touched = true;
			fail();
		}
		
	}

	@Override
	public boolean onBreak(int reactor) {
		return false;
	}

}
