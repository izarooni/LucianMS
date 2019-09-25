var MaplePacketCreator = Java.type("tools.MaplePacketCreator");
/* izarooni
LudiPQ - Stage 7 Green Balloon */

var status = 0;
var nthStage = "7th";
var ID_Ticket = 4001022;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        this.eim = player.getEventInstance();
        var complete = this.eim.getProperty(nthStage + "-clear");
        var preamble = this.eim.getProperty(nthStage + "-preamble");

        if (preamble == null) {
            cm.sendNext("Hi. Welcome to the " + nthStage + " stage.");
            this.eim.setProperty(nthStage + "-preamble", "true"); // value does not matter
        } else {
            if (complete == null) {
                if (cm.isLeader()) {
                    if(player.getMap().getCharacters().size() < eim.getPlayers().size()) {
                        cm.sendOk("Please wait for all of your party members to get here.");
                    } else if(cm.haveItem(ID_Ticket, 3)){
                        cm.sendOk("Good job! you have collected all #b3 #t" + ID_Ticket + "##k's");

            			player.getMap().broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear"));
            			player.getMap().broadcastMessage(MaplePacketCreator.playSound("Party1/Clear"));
            			player.getMap().broadcastMessage(MaplePacketCreator.environmentChange("gate", 2));

            			cm.removeAll(ID_Ticket);
                        cm.givePartyExp("LudiPQ7th");
                        eim.setProperty(nthStage + "-clear","true");
                    } else {
                        cm.sendOk("Sorry you don't have all #b3 #t" + ID_Ticket + "#'s#k");
                    }
                } else {
                    cm.sendOk("Please tell your #bParty-Leader#k to come talk to me");
                }
            } else {
                cm.sendOk("Hurry, goto the next stage, the portal is open!");
            }
        }
        cm.dispose();
    }
}
