/* izarooni
Ludi PQ: 5th stage to 6th stage portal
*/

function enter(pi) {
	var eim = pi.getPlayer().getEventInstance()
	var target = eim.getMapInstance(922010600);
	if (eim.getProperty("5th-clear") != null) {
        pi.givePartyExp("LudiPQ5th");
		pi.getPlayer().changeMap(target, target.getPortal("st00"));
		return true
	} else {
		return false;
    }
}
