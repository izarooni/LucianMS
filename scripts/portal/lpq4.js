/* izarooni
Ludi PQ: 4th stage to 5th stage portal
*/

function enter(pi) {
	var eim = pi.getPlayer().getEventInstance()
	var target = eim.getMapInstance(922010500);
	if (eim.getProperty("4th-clear") != null) {
        pi.givePartyExp("LudiPQ4th");
		pi.getPlayer().changeMap(target, target.getPortal("st00"));
		return true
	} else {
		return false;
    }
}
