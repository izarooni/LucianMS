var Integer = Java.type("java.lang.Integer");

function action(rm) {
    var em = rm.getEventManager("HenesysPQ");
    if (em != null) {
        var eim = em.getInstance("HenesysPQ_" + rm.getParty().getLeader().getName());
        if (eim != null) {
            var react = rm.getReactor().getMap().getReactorByName("fullmoon");
    		react.forceHitReactor(react.getState() + 1);

            var stage = Integer.parseInt(eim.getProperty("ActivatedPlants")) + 1;
            if (stage > 6) stage = 1; // not possible unless debugging
            eim.setProperty("ActivatedPlants", Integer.toString(stage));

    		if (stage == 6) {
    			rm.mapMessage(6, "Protect the Moon Bunny!!!");
    			var map = eim.getMapInstance(rm.getReactor().getMap().getId());
    			map.setSummonState(true);
    			map.spawnMonsterOnGroudBelow(9300061, -183, -433).setEventInstance(eim);
    		}
        } else {
            print("Event instance not found for player " + rm.getParty().getLeader().getName());
        }
    }
}
