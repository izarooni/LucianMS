const MapleExpeditionType = Java.type('com.lucianms.server.expeditions.MapleExpeditionType');
 
function start() {
    cm.sendYesNo("If you leave now, you won't be able to return. Are you sure you want to leave?");
}

function action(mode, type, selection) {
	var scarga = MapleExpeditionType.SCARGA;
    var expedition = cm.getExpedition(scarga);
    if (mode < 1)
        cm.dispose();
    else {
        if (cm.getPlayer().getMap().getCharacters().size() < 2){
            cm.getPlayer().getMap().killAllMonsters();
            cm.getPlayer().getMap().resetReactors();
			if (expedition != null){
				cm.endExpedition(expedition);
			}
        }
        if (cm.getPlayer().getEventInstance() != null)
            cm.getPlayer().getEventInstance().removePlayer(cm.getPlayer());
        else
            cm.warp(551030100);
        cm.dispose();
    }
}