/* izarooni */
var status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (cm.getParty() == null) {
        cm.sendNext("Who are you? How did you get here?");
        cm.dispose();
        return;
    }
    if (this.event == null) {
        let leaderUsernarme = cm.getParty().getLeader().getUsername();
        this.event = cm.getEventManager("HenesysPQ").getInstance(`HenesysPQ_${leaderUsernarme}`);
        if (this.event == null) {
            cm.sendNext("Who are you? You don't belong here.");
            cm.dispose();
            return;
        }
    }
    if (status == 1) {
        if (this.event.getProperty("clear") == "true") {
            cm.sendNext("Mmmm ... this is delicious. Please come see me next time for more #b#t4001101##k. Have a safe trip home!");
        } else {
            var text = "Growl! I am Growlie, always ready to protect this place. What brought you here?#b";
            text += "\r\n#L0# Please tell me what this place is all about.#l";
            if (cm.isLeader()) {
                text += "\r\n#L1# I have brought #t4001101#.#l"
            }
            text += "\r\n#L2# I would like to leave this place.#l";
            cm.sendSimple(text);
        }
    } else if (status == 2) {
        if (this.event.getProperty("clear") == "true") {
            this.event.finishPQ();
            cm.dispose();
        } else {
            this.chosen = selection;
             if (this.chosen == 0) {
                cm.sendNext("This place can be best described as the prime spot where you can taste the delicious rice cakes made by Moon Bunny every full moon.");
            } else if (this.chosen == 1) {
                if (cm.haveItem(4001101, 10)) {
                    cm.sendNext("Oh... isn't this rice cake made by Moon Bunny? Please hand me the rice cake.");
                } else {
                    cm.sendOk("I advise you to check and make sure that you have indeed gathered up #b10 #t4001101#s#k.");
                    cm.dispose();
                }
            } else if (this.chosen == 2) {
                cm.sendYesNo("Are you sure you want to leave?");
            } else {
                cm.dispose();
            }
        }
    } else if (status == 3) {
        if (this.chosen == 0) {
            cm.sendNextPrev("Gather up the primrose seeds from the primrose leaves all over this area, and plant the seeds at the footing near the crescent moon to see the primrose bloom. There are 6 types of primroses, and all of them require different footings. It is imperative that the footing fits the seed of the flower.");
        } else if (this.chosen == 1) {
            cm.gainItem(4001101, -10);
            cm.givePartyExp("HenesysPQ");

            var em = cm.getEventManager("HenesysPQ");
            var eim = em.getInstance("HenesysPQ_" + cm.getParty().getLeader().getUsername());
            var map = eim.getMapInstance(cm.getPlayer().getMapId());
            map.setSummonState(false);
            map.killAllMonstersNotFriendly();
            map.broadcastMessage(Packages.tools.MaplePacketCreator.showEffect("quest/party/clear"));
            map.broadcastMessage(Packages.tools.MaplePacketCreator.playSound("Party1/Clear"));
            cm.sendNext("Mmmm ... this is delicious. Please come see me next time for more #b#t4001101##k. Have a safe trip home!");
        } else if (this.chosen == 2) {
            this.event.disbandParty();
            cm.dispose();
        }
    } else if (status == 4) {
        if (this.chosen == 0) {
            cm.sendNextPrev("When the flowers of primrose blooms, the full moon will rise, and that's when the Moon Bunnies will appear and start pounding the mill. Your task is to fight off the monsters to make sure that Moon Bunny can concentrate on making the best rice cake possible.");
        } else if (this.chosen == 1) {
            this.event.finishPQ();
            cm.dispose();
        }
    } else if (status == 5) {
        if (this.chosen == 0) {
            cm.sendNextPrev("I would like for you and your party members to cooperate and get me 10 rice cakes. I strongly advise you to get me the rice cakes within the allotted time.");
        }
    } else {
        cm.dispose();
    }
}
