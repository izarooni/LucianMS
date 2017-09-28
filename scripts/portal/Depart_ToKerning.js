function enter(pi) {
	pi.playPortalSound();
	var train = pi.getEventManager("KerningTrain");
	train.newInstance("KerningTrain");
	train.setProperty("player", cm.getPlayer().getName());
	train.startInstance(cm.getPlayer());
	return true;
}