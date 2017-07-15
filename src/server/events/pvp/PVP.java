package server.events.pvp;

import java.util.HashMap;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleJob;
import client.MapleStat;
import net.server.channel.handlers.AbstractDealDamageHandler;
import net.server.guild.MapleGuild;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleMap;
import tools.MaplePacketCreator;

public abstract class PVP {
	

	protected HashMap<String, Integer> PVPers;
	protected int levelRequirement, mapId;
	protected PVPType type;
	protected MapleCharacter pvper;
	
	private int maxDistance, maxHeight;

	protected boolean facingLeft, facingRight;

	public PVP(MapleCharacter player) {
		this.pvper = player;
		PVPers = new HashMap<String, Integer>();
	}

	public boolean join() {
		if (pvper.getLevel() >= levelRequirement) {
			if (!(PVPers.containsValue(pvper.getId()))) {
				PVPers.put(type.toString().toLowerCase(), pvper.getId());
				pvper.changeMap(mapId);
			}
		}

		return false;
	}
	
	
	public boolean canAttack(AbstractDealDamageHandler.AttackInfo attack) {
		for(MapleCharacter targets : pvper.getMap().getNearestPvpChar(pvper.getPosition(),(double) getMaxDistance(), (double) getMaxHeight(), pvper.getMap().getCharacters())) {
			return PVPers.containsValue(targets.getId());
		}
		return false;
	}
	
	public abstract void doDamage(AbstractDealDamageHandler.AttackInfo attack);
	
	
	public enum PVPType {
		FFA, TEAM, GUILD, GENDER;
	}

	public boolean isMeleeAttack(AbstractDealDamageHandler.AttackInfo attack) {
		switch (attack.skill) {
		case 1001004: // Power Strike
		case 1001005: // Slash Blast
		case 4001334: // Double Stab
		case 4201005: // Savage Blow
		case 1111004: // Panic: Axe
		case 1111003: // Panic: Sword
		case 1311004: // Dragon Fury: Pole Arm
		case 1311003: // Dragon Fury: Spear
		case 1311002: // Pole Arm Crusher
		case 1311005: // Sacrifice
		case 1311001: // Spear Crusher
		case 1121008: // Brandish
		case 1221009: // Blast
		case 1121006: // Rush
		case 1221007: // Rush
		case 1321003: // Rush
		case 4221001: // Assassinate
			return true;
		}
		return false;
	}

	public boolean isRangeAttack(AbstractDealDamageHandler.AttackInfo attack) {
		switch (attack.skill) {
		case 2001004: // Energy Bolt
		case 2001005: // Magic Claw
		case 3001004: // Arrow Blow
		case 3001005: // Double Shot
		case 4001344: // Lucky Seven
		case 2101004: // Fire Arrow
		case 2101005: // Poison Brace
		case 2201004: // Cold Beam
		case 2301005: // Holy Arrow
		case 4101005: // Drain
		case 2211002: // Ice Strike
		case 2211003: // Thunder Spear
		case 3111006: // Strafe
		case 3211006: // Strafe
		case 4111005: // Avenger
		case 4211002: // Assaulter
		case 2121003: // Fire Demon
		case 2221006: // Chain Lightning
		case 2221003: // Ice Demon
		case 2111006: // Element Composition F/P
		case 2211006: // Element Composition I/L
		case 2321007: // Angel's Ray
		case 3121003: // Dragon Pulse
		case 3121004: // Hurricane
		case 3221003: // Dragon Pulse
		case 3221001: // Piercing
		case 3221007: // Sniping
		case 4121003: // Showdown taunt
		case 4121007: // Triple Throw
		case 4221007: // Boomerang Step
		case 4221003: // Showdown taunt
		case 4111004: // Shadow Meso
			return true;
		}
		return false;
	}

	public boolean isAoeAttack(AbstractDealDamageHandler.AttackInfo attack) {
		switch (attack.skill) {
		case 2201005: // Thunderbolt
		case 3101005: // Arrow Bomb : Bow
		case 3201005: // Iron Arrow : Crossbow
		case 1111006: // Coma: Axe
		case 1111005: // Coma: Sword
		case 1211002: // Charged Blow
		case 1311006: // Dragon Roar
		case 2111002: // Explosion
		case 2111003: // Poison Mist
		case 2311004: // Shining Ray
		case 3111004: // Arrow Rain
		case 3111003: // Inferno
		case 3211004: // Arrow Eruption
		case 3211003: // Blizzard (Sniper)
		case 4211004: // Band of Thieves
		case 1221011: // Sanctuary Skill
		case 2121001: // Big Bang
		case 2121007: // Meteo
		case 2121006: // Paralyze
		case 2221001: // Big Bang
		case 2221007: // Blizzard
		case 2321008: // Genesis
		case 2321001: // Big Bang
		case 4121004: // Ninja Ambush
		case 4121008: // Ninja Storm knockback
		case 4221004: // Ninja Ambush
			return true;
		}
		return false;
	}

	public void getDirection(AbstractDealDamageHandler.AttackInfo attack) {
		if (isAoeAttack(attack)) {
			setFacingLeft(true);
			setFacingRight(true);
		} else if (isMeleeAttack(attack) || isRangeAttack(attack)) {
			setFacingRight((!(attack.direction <= 0 && attack.stance <= 0)));
			setFacingLeft((attack.direction <= 0 && attack.stance <= 0));
		}
	}

	public int DamageBalancer(AbstractDealDamageHandler.AttackInfo attack) {
		int pvpDamage = 0;
		boolean isAoe = false;
		if (attack.skill == 0) {
			pvpDamage = 100;
			maxDistance = 130;
			maxHeight = 35;
		} else if (isMeleeAttack(attack)) {
			maxDistance = 130;
			maxHeight = 45;
			isAoe = false;
			if (attack.skill == 4201005) {
				pvpDamage = (int) (Math.floor(Math.random() * (75 - 5) + 5));
			} else if (attack.skill == 1121008) {
				pvpDamage = (int) (Math.floor(Math.random() * (320 - 180) + 180));
				maxHeight = 50;
			} else if (attack.skill == 4221001) {
				pvpDamage = (int) (Math.floor(Math.random() * (200 - 150) + 150));
			} else if (attack.skill == 1121006 || attack.skill == 1221007 || attack.skill == 1321003) {
				pvpDamage = (int) (Math.floor(Math.random() * (200 - 80) + 80));
			} else {
				pvpDamage = (int) (Math.floor(Math.random() * (600 - 250) + 250));
			}
		} else if (isRangeAttack(attack)) {
			maxDistance = 300;
			maxHeight = 40;
			isAoe = false;
			if (attack.skill == 4201005) {
				pvpDamage = (int) (Math.floor(Math.random() * (75 - 5) + 5));
			} else if (attack.skill == 4121007) {
				pvpDamage = (int) (Math.floor(Math.random() * (60 - 15) + 15));
			} else if (attack.skill == 4001344 || attack.skill == 2001005) {
				pvpDamage = (int) (Math.floor(Math.random() * (195 - 90) + 90));
			} else if (attack.skill == 4221007) {
				pvpDamage = (int) (Math.floor(Math.random() * (350 - 180) + 180));
			} else if (attack.skill == 3121004 || attack.skill == 3111006 || attack.skill == 3211006) {
				maxDistance = 450;
				pvpDamage = (int) (Math.floor(Math.random() * (50 - 20) + 20));
			} else if (attack.skill == 2121003 || attack.skill == 2221003) {
				pvpDamage = (int) (Math.floor(Math.random() * (600 - 300) + 300));
			} else {
				pvpDamage = (int) (Math.floor(Math.random() * (400 - 250) + 250));
			}
		} else if (isAoeAttack(attack)) {
			maxDistance = 350;
			maxHeight = 350;
			isAoe = true;
			if (attack.skill == 2121001 || attack.skill == 2221001 || attack.skill == 2321001
					|| attack.skill == 2121006) {
				maxDistance = 175;
				maxHeight = 175;
				pvpDamage = (int) (Math.floor(Math.random() * (350 - 180) + 180));
			} else {
				pvpDamage = (int) (Math.floor(Math.random() * (700 - 300) + 300));
			}
		}
		return pvpDamage;
	}
	
	public void monsterBomb(MapleCharacter player, int pvpDamage, MapleCharacter attackedPlayers, MapleMap map, AbstractDealDamageHandler.AttackInfo attack) { 
        //level balances 
        if (attackedPlayers.getLevel() > player.getLevel() + 25) { 
                pvpDamage *= 1.35; 
        } else if (attackedPlayers.getLevel() < player.getLevel() - 25) { 
                pvpDamage /= 1.35; 
        } else if (attackedPlayers.getLevel() > player.getLevel() + 100) { 
                pvpDamage *= 1.50; 
        } else if (attackedPlayers.getLevel() < player.getLevel() - 100) { 
                pvpDamage /= 1.50; 
        } 
        //class balances 
        if (player.getJob().equals(MapleJob.MAGICIAN)) { 
                pvpDamage *= 1.20; 
        } 

        //buff modifiers 
Integer mguard = attackedPlayers.getBuffedValue(MapleBuffStat.MAGIC_GUARD); 
Integer mesoguard = attackedPlayers.getBuffedValue(MapleBuffStat.MESOGUARD); 
if (mguard != null) { 
    int mploss = (int) (pvpDamage / .5); 
                pvpDamage *= .70; 
    if (mploss > attackedPlayers.getMp()) { 
                        pvpDamage /= .70; 
        attackedPlayers.cancelBuffStats(MapleBuffStat.MAGIC_GUARD); 
    } else { 
                        attackedPlayers.setMp(attackedPlayers.getMp() - mploss); 
                        attackedPlayers.updateSingleStat(MapleStat.MP, attackedPlayers.getMp()); 
                } 
} else if (mesoguard != null) { 
    int mesoloss = (int) (pvpDamage * .80); 
                pvpDamage *= .80; 
    if(mesoloss > attackedPlayers.getMeso()) { 
                                pvpDamage /= .80; 
            attackedPlayers.cancelBuffStats(MapleBuffStat.MESOGUARD); 
    } else { 
            attackedPlayers.gainMeso(-mesoloss, false); 
    } 
        } 

        //set up us teh bonmb 
        //training thingy = 9409000 
        MapleMonster pvpMob = MapleLifeFactory.getMonster(9400711); 
        map.spawnMonsterOnGroundBelow(pvpMob, attackedPlayers.getPosition()); 
        for (int attacks = 0; attacks < attack.numDamage; attacks++) { 
        	map.broadcastMessage(MaplePacketCreator.damagePlayer(attack.numDamage, pvpMob.getId(), attackedPlayers.getId(), pvpDamage, attacks, attacks, isAoeAttack(attack), getMaxDistance(), isAoeAttack(attack), getMaxDistance(), getMaxDistance(), getMaxDistance()));
        	attackedPlayers.addHP(-pvpDamage); 
        } 
        int attackedDamage = pvpDamage * attack.numDamage; 
        attackedPlayers.getClient().getSession().write(MaplePacketCreator.serverNotice(5, player.getName() + " has hit you for " + attackedDamage + " damage!")); 
        map.killMonster(pvpMob, player, false); 

        //rewards 
        if (attackedPlayers.getHp() <= 0 && !attackedPlayers.isAlive()) { 
                int expReward = attackedPlayers.getLevel() * 100; 
                int gpReward = (int) (Math.floor(Math.random() * (200 - 50) + 50)); 
                //if (player.getPvpKills() * .25 >= player.getPvpDeaths()) { 
                //        expReward *= 20; 
                //} 
                player.gainExp(expReward, true, false); 
                if (player.getGuildId() != 0 && player.getGuildId() != attackedPlayers.getGuildId()) { 
                        try { 
                                MapleGuild guild = player.getGuild(); 
                                guild.gainGP(gpReward); 
                        } catch (Exception e) {} 
                } 
                //player.gainPvpKill(); 
                player.getClient().getSession().write(MaplePacketCreator.serverNotice(6, "You've killed " + attackedPlayers.getName() + "!! You've gained a pvp kill!")); 
                //attackedPlayers.gainPvpDeath(); 
                attackedPlayers.getClient().getSession().write(MaplePacketCreator.serverNotice(6, player.getName() + " has killed you!")); 
        } 
} 

	public boolean isFacingLeft() {
		return facingLeft;
	}

	public void setFacingLeft(boolean facingLeft) {
		this.facingLeft = facingLeft;
	}

	public boolean isFacingRight() {
		return facingRight;
	}

	public void setFacingRight(boolean facingRight) {
		this.facingRight = facingRight;
	}

	public int getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance(int maxDistance) {
		this.maxDistance = maxDistance;
	}

	public int getMaxHeight() {
		return maxHeight;
	}

	public void setMaxHeight(int maxHeight) {
		this.maxHeight = maxHeight;
	}

	public PVPType getPVPType() {
		return type;
	}
}
