var MPC = Java.type("tools.MaplePacketCreator");
/* izarooni */
var status = 0;
var crystal = 4260002; 

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendNext("Are you ready to leave for #bPlanet Lucian#k?\r\nYou'll have to pay me #b2 #z" + crystal + "##k before I teleport you. Transportation ain't cheap!");
    } else if (status == 2) {
        var event = client.getWorldServer().getScheduledEvent("SouterSpace");
        if (event != null && event.isOpen() && !event.isFinished(client.getChannel())) {
            if (cm.haveItem(crystal, 2)) {
                event.registerPlayer(player);
            } else {
                cm.sendOk("Didn't I just say you need #b2 #z" + crystal + "##k? Pay up!");
            }
        } else {
            cm.sendOk("Oh, well it seems the threat is no longer present on the planet. There's no need for you to go there right now");
        }
        cm.dispose();
    }
}