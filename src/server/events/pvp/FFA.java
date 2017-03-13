package server.events.pvp;

import client.MapleCharacter;
import net.server.channel.handlers.AbstractDealDamageHandler.AttackInfo;

public class FFA extends PVP {

	public FFA(MapleCharacter player) {
		super(player);
		this.mapId = 910000000;
		this.levelRequirement = 50;
		this.type = PVPType.FFA;
	}

	@Override
	public void doDamage(AttackInfo attack) {
		for (MapleCharacter target : pvper.getMap().getNearestPvpChar(pvper.getPosition(), getMaxDistance(),
				getMaxHeight(), pvper.getMap().getCharacters())) {
			if (canAttack(attack)) {
				if (target.isAlive() && pvper.getId() != target.getId()) {
					int damage = DamageBalancer(attack);
					monsterBomb(pvper, damage, target, pvper.getMap(), attack);
				}
			}
		}
	}

}
