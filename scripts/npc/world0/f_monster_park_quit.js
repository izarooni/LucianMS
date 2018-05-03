var MonsterPark = Java.type("com.lucianms.features.MonsterPark");
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
        cm.sendNext("Are you sure you want to quit this Monster Park?");
    } else if (status == 2) {
        // get monster park event handler
        this.optional = player.getGenericEvents().stream().filter(function(e){
            return (e instanceof MonsterPark);
        }).findFirst();

        // if one exists in the player instance
        if (this.optional.isPresent()) {
            this.optional.get().unregisterPlayer(player);
        }
        cm.dispose();
    }
}
