package client.arcade;

import client.MapleCharacter;
import client.inventory.MapleInventoryType;
import server.MapleInventoryManipulator;
import tools.MaplePacketCreator;

public class PickAHolic extends Arcade {

	int highscore = 0;
	int prevScore = getHighscore(arcadeId, player);
	
	public PickAHolic(MapleCharacter player) {
		super(player);
		this.arcadeId = 0;
		this.mapId = 677000002;
		this.rewardPerKill = 0.2;
		this.itemReward = 4310149;
		
	}

	@Override
	public boolean fail() {
		int itemId = 2100067;
		int quantity = player.getInventory(MapleInventoryType.USE).countById(itemId);
		
		if(quantity >= 1) {
			MapleInventoryManipulator.removeById(player.getClient(), MapleInventoryType.USE, itemId, quantity, false, false);
			
			player.changeMap(910000000, 0);
			player.announce(MaplePacketCreator.serverNotice(1, "Game Over!"));
			if(saveData(highscore)) {
				player.dropMessage(5, "[Game Over] Your new highscore for Loot-A-Holic is " + highscore);
			} else {
				player.dropMessage(5, "[Game Over] Your highscore for Loot-A-Holic remains at " + Arcade.getHighscore(arcadeId, player));
			}
			MapleInventoryManipulator.addById(player.getClient(), itemReward, (short) rewardPerKill);
			respawnManager = null;
			player.setArcade(null);
			return true;
		}
		return false;
	}

	@Override
	public void add() {
		int itemId = 4031544;
		int quantity = player.getInventory(MapleInventoryType.ETC).countById(itemId);
		
		if(quantity >= 1) {
			MapleInventoryManipulator.removeById(player.getClient(), MapleInventoryType.ETC, itemId, quantity, false, false);
			++highscore;
			player.announce(MaplePacketCreator.sendHint("#e[Loot-A-Holic]#n\r\nYou have looted " + ((prevScore < highscore) ?  "#g" : "#r") + highscore + "#k card(s)!", 300, 40));
		}
	}

	@Override
	public void onKill(int monster) {}

	@Override
	public void onHit(int monster) {}

	@Override
	public boolean onBreak(int reactor) {
		return false;
	}

}
