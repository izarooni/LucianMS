package server.events.custom;

import java.awt.Point;
import java.io.File;

import client.MapleCharacter;
import net.server.world.MaplePartyCharacter;
import provider.MapleDataProviderFactory;
import server.TimerManager;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleMapFactory;
import tools.MaplePacketCreator;

public class AbstractBossPQ {

	private int round = 1;
	private int points;
	private int[] bosses;
	private int map;
	private int nxWinnings;
	private int nxWinningsMultiplier;
	private static final int RETURN_MAP = 240070101;
	private int minLevel = 0;
	private int healthMultiplier = 1, damageMultipier = 1;
	
	private MapleCharacter leader;
	
	public AbstractBossPQ(MapleCharacter partyleader, int points, int map, int[] bosses, int nxWinnings, int nxWinningsMultiplier, int minLevel, int healthMultiplier, int damageMultiplier) {
		this.leader = partyleader;
		this.points = points;
		this.map = map;
		this.bosses = bosses;
		this.nxWinnings = nxWinnings;
		this.nxWinningsMultiplier = nxWinningsMultiplier;
		this.minLevel = minLevel;
		this.healthMultiplier = healthMultiplier;
		this.damageMultipier = damageMultiplier;
	}
	
	public boolean nextRound() {
			if(round >= bosses.length) {
				if(leader.getParty() != null && leader.getParty().getMembers().size() > 1) {
					for(MaplePartyCharacter party : leader.getParty().getMembers()) {
						party.getPlayer().dropMessage(5, "You completed the Boss Arena and received " + (nxWinnings * nxWinningsMultiplier * 2) + " nexon cash");
						party.getPlayer().getCashShop().gainCash(1, nxWinnings * nxWinningsMultiplier * 2);
						leader.changeMap(RETURN_MAP);
						leader.setBossPQ(null);
					}
				} else {
					leader.dropMessage(5, "You completed the Boss Arena and received " + (nxWinnings * nxWinningsMultiplier * 2) + " nexon cash");
					leader.getCashShop().gainCash(1, nxWinnings * nxWinningsMultiplier * 2);
					leader.changeMap(RETURN_MAP);
					leader.setBossPQ(null);
				}
			} else {
				TimerManager.getInstance().schedule(() -> {
					MapleMonster monster = MapleLifeFactory.getMonster(bosses[round]);
					monster.setHp(monster.getHp() * healthMultiplier);
					monster.setMp(monster.getMp() * healthMultiplier);
					monster.setVenomMulti(damageMultipier);
					monster.setBoss(true);
					leader.getMap().spawnMonsterOnGroudBelow(monster, new Point(leader.getPosition().x + 150, leader.getPosition().y));
					leader.getMap().broadcastGMMessage(MaplePacketCreator.earnTitleMessage("Boss Arena: round " + round));
					//leader.getMap().broadcastGMMessage(MaplePacketCreator.message);
					if(leader.getParty() != null && leader.getParty().getMembers().size() > 1) {
						for(MaplePartyCharacter party : leader.getParty().getMembers()) {
							if(round > 1) {
								party.getPlayer().dropMessage(6, "You defeated a boss, you gained " + (nxWinnings * nxWinningsMultiplier) + " nexon cash");
								party.getPlayer().getCashShop().gainCash(1, nxWinnings * nxWinningsMultiplier);
							}
							++round;
						}
					} else {
						if(round > 1) {
							leader.dropMessage(6, "You defeated a boss, you gained " + (nxWinnings * nxWinningsMultiplier) + " nexon cash");
							leader.getCashShop().gainCash(1, nxWinnings * nxWinningsMultiplier);
						}
						++round;
					}
				}, 8000);
				return true;
		}
		return false;
	}
	
	public void start() {
		MapleMapFactory factory = new MapleMapFactory(MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Map.wz")), MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String.wz")), leader.getWorld(), leader.getClient().getChannel());
		if(leader.getParty() != null && leader.getParty().getMembers().size() > 1) {
			for(MaplePartyCharacter party : leader.getParty().getMembers()) {
				party.getPlayer().changeMap(factory.getMap(map), factory.getMap(map).getPortal(0));
				party.getPlayer().dropMessage(6, "Boss Arena round 1 is starting in 8 seconds..");
				nextRound();
			}
		} else {
			leader.changeMap(factory.getMap(map), factory.getMap(map).getPortal(0));
			nextRound();
		}
	}
	
	public boolean allMembersMinLevel() {
		if(leader.getParty() != null) {
			for(MaplePartyCharacter player : leader.getParty().getMembers()) {
				if(player.getPlayer().getLevel() <= minLevel) {
					return false;
				}
			}
		} else {
			return false;
		}
		return true;
	}

	public int getRound() {
		return round;
	}

	public void setRound(int round) {
		this.round = round;
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public int[] getBosses() {
		return bosses;
	}

	public void setBosses(int[] bosses) {
		this.bosses = bosses;
	}

	public int getMap() {
		return map;
	}

	public void setMap(int map) {
		this.map = map;
	}

	public int getNxWinnings() {
		return nxWinnings;
	}

	public void setNxWinnings(int nxWinnings) {
		this.nxWinnings = nxWinnings;
	}

	public int getNxWinningsMultiplier() {
		return nxWinningsMultiplier;
	}

	public void setNxWinningsMultiplier(int nxWinningsMultiplier) {
		this.nxWinningsMultiplier = nxWinningsMultiplier;
	}
	
	public void setMinLevel(int level) {
		this.minLevel = level;
	}
	
	public int getMinLevel() {
		return minLevel;
	}
	
	
	
}
