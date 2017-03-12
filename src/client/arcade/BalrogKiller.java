package client.arcade;

import java.awt.Point;

import client.MapleCharacter;
import client.MapleStat;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import tools.MaplePacketCreator;

public class BalrogKiller extends Arcade {


	int highscore = 0, balrog = 9500140;
	int currExp = player.getExp();
	int prevScore = getHighscore(arcadeId, player);
	
	
	public BalrogKiller(MapleCharacter player) {
		super(player);
		this.arcadeId = 4;
		this.mapId = 677000003;
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean fail() {
		
		player.setHp(player.getMaxHp());
		player.setExp(currExp);
		player.updateSingleStat(MapleStat.EXP, currExp);
		player.updateSingleStat(MapleStat.HP, player.getMaxHp());
		player.changeMap(910000000, 0);
		player.announce(MaplePacketCreator.serverNotice(1, "Game Over!"));
		if(saveData(highscore)) {
			player.dropMessage(5, "[Game Over] Your new highscore for Balrog Killer is " + highscore);
		} else {
			player.dropMessage(5, "[Game Over] Your highscore for Balrog Killer remains at " + Arcade.getHighscore(arcadeId, player));
		}
		player.setArcade(null);
		return true;
	}

	@Override
	public void add() {
		currExp = player.getExp();
		++highscore;
		player.announce(MaplePacketCreator.sendHint("#e[Balrog Killer]#n\r\nYou have killed " + ((prevScore < highscore) ?  "#g" : "#r") + highscore + "#k balrog(s)!", 300, 40));
		MapleMonster toSpawn = MapleLifeFactory.getMonster(9500140);
		toSpawn.setHp(350000);
		player.getMap().spawnMonsterOnGroudBelow(toSpawn, new Point(206, 35));
	}

	@Override
	public void onKill(int monster) {
		if(monster == balrog) add();

	}

	@Override
	public void onHit(int monster) {
		player.setHp(player.getHp() - 5000);
		if (player.getHp() < 1) {
			fail();
		}

	}

	@Override
	public boolean onBreak(int reactor) {
		return false;
	}

}
