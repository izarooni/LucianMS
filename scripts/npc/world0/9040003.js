load('scripts/util_gpq.js');
/* izarooni 
Sharen III's Soul, Sharenian: Sharen III's Grave (990000700)
*/
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    let eim = player.getEventInstance();
    if (eim == null) {
        if (status == 1) cm.sendNext("How did I get here?");
        else {
            cm.warp(nFieldEnding);
            cm.dispose();
        }
        return;
    }
    let gate = player.getMap().getReactorByName("ghostgate");
    if (status == 1) {
        if (gate.getState() == 1 || eim.isLeader(player)) {
            cm.sendNext("After what I thought would be an immortal sleep, I have finally found someone that will save Sharenian. I can truly rest in peace now.");
            if (gate.getState() == 1 || !eim.isLeader(player)) return cm.dispose();
        } else {
            cm.sendOk("I need the leader of your party to speak with me, nobody else.");
            cm.dispose();
        }
    } else if (status == 2) {
        cm.getGuild().gainGP(30);
        gate.hitReactor(client);
        cm.showEffect("quest/party/clear");
        cm.playSound("Party1/Clear");
        cm.dispose();
    }
}