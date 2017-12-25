/* izarooni
Ludi PQ: 3rd stage to 4th stage portal
*/

function enter(pi) {
	var eim = pi.getPlayer().getEventInstance()
	var target = eim.getMapInstance(922010400);
	if (eim.getProperty("3rd-clear") != null) {
        pi.givePartyExp("LudiPQ3rd");
		pi.getPlayer().changeMap(target, target.getPortal("st00"));
		return true
	} else {
		return false;
    }
}
