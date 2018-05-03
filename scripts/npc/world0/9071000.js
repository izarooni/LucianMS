/* izarooni */
// npc: 9071000
// map: 951000000
var MonsterPark = Java.type("com.lucianms.features.MonsterPark");
var status = 0;
var minParticpants = 2;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    this.optional = player.getGenericEvents().stream().filter(function(e){
        return (e instanceof MonsterPark);
    }).findFirst();
    if (status == 1) {
        if (!this.optional.isPresent()) {
            cm.sendSimple("Do you think you're strong enough to enter the Monster Park?\r\n#b"
                + "\r\n#L0#Tell me more about this place#l");
        } else {
            cm.sendYesNo("Seems you are already in a Monster Park.\r\nWould you like to leave?");
        }
    } else if (status == 2) {
        if (this.optional.isPresent()) {
            this.optional.get().unregisterPlayer(player);
            cm.dispose();
        } else if (selection == 0) {
            cm.sendNext("There are 6 linked maps in the Monster Park that all contain monsters.\r\nKilling these specific monsters give a bonus #b25% EXP#k. To procceed to the next map, all monsters must be killed");
            cm.dispose();
        }
        cm.dispose();
    }
}
