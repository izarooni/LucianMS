/* izarooni
Ludi PQ: 6th stage to 7th stage portal
*/

function enter(pi) {
	var eim = pi.getPlayer().getEventInstance()
	var target = eim.getMapInstance(922010700);

    pi.givePartyExp("LudiPQ6th");
	pi.getPlayer().changeMap(target, target.getPortal("st00"));
	return true
}
