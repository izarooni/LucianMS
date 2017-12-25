/* izarooni
Ludi PQ: 1st stage to 2nd stage portal
*/

function enter(pi) {
	var eim = pi.getPlayer().getEventInstance()
	var target = eim.getMapInstance(922010200);
	if (eim.getProperty("1st-clear") != null) {
        pi.givePartyExp("LudiPQ1st");
		pi.getPlayer().changeMap(target, target.getPortal("st00"));
		return true
	} else {
		return false;
    }
}
