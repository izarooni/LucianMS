function enter(pi) {
	pi.playPortalSound();
	let em = pi.getEventManager("KerningTrain");
    let eim = em.startInstance(pi.getPlayer());
	return true;
}
