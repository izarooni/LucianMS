/* izarooni
Ludi PQ: 7th stage to 8th stage portal
*/

function enter(pi) {
	var eim = pi.getPlayer().getEventInstance()
	var target = eim.getMapInstance(922010800);
	if (eim.getProperty("7th-clear") != null) {
        pi.givePartyExp("LudiPQ7th");
		pi.getPlayer().changeMap(target, target.getPortal("st00"));
		return true
	} else {
		return false;
    }
}
