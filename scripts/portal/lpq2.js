/* izarooni
Ludi PQ: 2nd stage to 3rd stage portal
*/

function enter(pi) {
	var eim = pi.getPlayer().getEventInstance()
	var target = eim.getMapInstance(922010300);
	if (eim.getProperty("2nd-clear") != null) {
        pi.givePartyExp("LudiPQ2nd");
		pi.getPlayer().changeMap(target, target.getPortal("st00"));
		return true
	} else {
		return false;
    }
}
