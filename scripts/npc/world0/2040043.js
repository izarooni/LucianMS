var MPC = Java.type("tools.MaplePacketCreator");
/* izarooni */
var status = 0;
var ID_Item = 4011023;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendNext("Are you ready to leave for #bPlanet Lucian#k?\r\nYou'll have to pay me #b1 #z" + ID_Item + "##k before I teleport you. Transportation ain't cheap!\r\n\r\n- What is Planet Lucian?\r\n\Planet Lucian is a mysterious area located in space. The rumor says that the almighty #rKing Slime#k sometimes makes an appearance there. We will announce whenever we spot him making an appearance so look out in the chat.");
    } else if (status == 2) {
        var event = client.getWorldServer().getScheduledEvent("SOuterSpace");
        if (event != null) {
            if (event.isOpen() && !event.isFinished(client.getChannel() - 1)) {
                if (cm.haveItem(ID_Item, 1)) {
                    event.registerPlayer(player);
                    cm.gainItem(ID_Item, -1);
                    client.getWorldServer().broadcastMessage(0, "{} in channel {} used a compass to travel to Planet Lucian", player.getName(), client.getChannel());
                    cm.dispose();
                } else {
                    cm.sendOk("Didn't I just say you need #b1 #z" + ID_Item + "##k? Pay up!");
                }
            } else {
                cm.sendOk("Oh, well it seems the threat is no longer present on the planet. There's no need for you to go there right now");
            }
        } else {
            cm.sendOk("An error occurred");
        }
        cm.dispose();
    }
}