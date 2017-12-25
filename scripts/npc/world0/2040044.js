/* izarooni
Ludi PQ: 8th stage to 9th stage portal
*/

function enter(pi) {
	var eim = pi.getPlayer().getEventInstance()
	var target = eim.getMapInstance(922010900);
	if (eim.getProperty("8th-clear") != null) {
        pi.givePartyExp("LudiPQ8th");
		pi.getPlayer().changeMap(target, target.getPortal("st00"));
		return true
	} else {
		return false;
    }
}
