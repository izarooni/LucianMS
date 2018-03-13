function enter(pi) {
	pi.playPortalSound();
    let name = "KerningTrain_" + pi.getPlayer().getName();
	let em = pi.getEventManager("KerningTrain");
    em.removeInstance(name);
	em.newInstance(name);
	em.setProperty("player", pi.getPlayer().getName());
	em.startInstance(pi.getPlayer());
	return true;
}
