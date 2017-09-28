var BossPQ = Java.type("server.events.custom.BossPQ");
/* izarooni */
var status = 0;
var optional = player.getGenericEvents().stream().filter(function(g) {
    return (g instanceof BossPQ);
}).findFirst();

function action(mode, type, selection) {
    if (mode < 1 && optional.isPresent()) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        if (optional.isPresent()) {
            cm.sendNext("Are you sure you want to leave the Boss PQ?\r\nThere's no coming  back if you do");
        } else {
            cm.sendNext("Who are you?... Nevermind, I'll bring you home");
        }
    } else if (status == 2) {
        if (optional.isPresent()) {
            var pq = optional.get();
            pq.broadcastMessage(player.getName() + " has decided to leave");
            pq.unregisterPlayer(player);
         } else {
            player.changeMap(809);
         }
        cm.dispose();
    }
}