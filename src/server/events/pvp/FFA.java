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
		// TODO Auto-generated method stub
		
	}

}
