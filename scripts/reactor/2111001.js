function act() {
    if(rm.getPlayer().getEventInstance() != null){
		rm.getPlayer().getEventInstance().setProperty("summoned", "true");
		rm.getPlayer().getEventInstance().setProperty("canEnter", "false");
	}
    rm.changeMusic("Bgm06/FinalFight");
    rm.spawnFakeMonster(8800000);
    for (i=8800003; i<8800011; i++)
        rm.spawnMonster(i);
    rm.createMapMonitor(280030000,"ps00");
    rm.mapMessage(5, "Zakum is summoned by the force of Eye of Fire.");
}
