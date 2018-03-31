const MonsterPark = Java.type("server.events.custom.MonsterPark");

/* izarooni */
var status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        this.optional = player.getGenericEvents().stream().filter(function(e){
            return (e instanceof MonsterPark);
        }).findFirst();
        if (this.optional.isPresent()) {
            cm.openNpc(9071000, "f_monster_park_quit");
        } else {
            cm.sendOk("uhhh");
            cm.dispose();
        }
    }
}
