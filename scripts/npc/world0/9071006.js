/* izarooni */
// npc: 9071000
var MonsterPark = Java.type("server.events.custom.MonsterPark");
var status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        if (cm.getParty() == null) {
            cm.sendOk("So you want to enter the Monster Park? Well I can't let you enter without a party");
            cm.dispose();
        } else if (cm.isLeader()) {
             cm.sendNext("Whenever you're ready you may enter");
        } else {
            cm.sendOk("Only your party leader can decide when to enter");
            cm.dispose();
        }
    } else if (status == 2) {
        this.optional = player.getGenericEvents().stream().filter(function(e){
            return e instanceof MonsterPark;
        }).findFirst();
        if (optional.isPresent()) {
            cm.sendYesNo("You are already registered in a Monster Park!\r\nWould you like to leave?");
        } else {
            var park = new MonsterPark(client.getWorld(), client.getChannel(), 953050000);
            park.registerPlayer(player);
            cm.dispose();
        }
    } else if (status == 3) {
        if (this.optional.isPresent()) {
            this.optional.get().unregisterPlayer(player);
            cm.dispose();
        }
    }
}
