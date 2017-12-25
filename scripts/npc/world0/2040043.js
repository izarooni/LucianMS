var MPC = Java.type("tools.MaplePacketCreator");
/* izarooni */
//Does not warp you but instead gives you the no longer threat on planet message.
var status = 0;
var crystal = 4011023; 
var moveTo = 98;
// crystal has been changed into a compass item instead.

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendNext("Are you ready to leave for #bPlanet Lucian#k?\r\nYou'll have to pay me #b1 #z" + crystal + "##k before I teleport you. Transportation ain't cheap!\r\n\r\n- What is Planet Lucian?\r\n\Planet Lucian is a mysterious area located in space. The rumor says that the almighty #rKing Slime#k sometimes makes an appearance there. We will announce whenever we spot him making an appearance so look out in the chat.");
    } else if (status == 2) {
        var event = client.getWorldServer().getScheduledEvent("SouterSpace");
        if (event != null && event.isOpen() && !event.isFinished(client.getChannel())) {
            if (cm.haveItem(crystal, 1)) {
                event.registerPlayer(player);
                cm.warp(98);
                cm.haveItem(crystal, -1);
                cm.serverNotice(""+ cm.getPlayer().getName() +" in channel "+cm.getPlayer().getClient().getChannel() +" has used a compass to travel to Planet Lucian.");
                cm.dispose();
            } else {
                cm.sendOk("Didn't I just say you need #b1 #z" + crystal + "##k? Pay up!");
            }
        } else {
            cm.sendOk("Oh, well it seems the threat is no longer present on the planet. There's no need for you to go there right now");
        }
        cm.dispose();
    }
}