const em = cm.getEventManager("Ranmaru");
const instanceName = "Ranmaru-" + ch.getId();
const MinimumLevel = 160;
/* izarooni */
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        if (em != null) {
            let instance = em.getInstance(instanceName);
            if (cm.getParty() != null) {
                if (!cm.isLeader()) {
                    cm.sendOk("I'll only speak to your party leader.");
                    cm.dispose();
                    return;
                } else {
                    let iter = cm.getPartyMembers().iterator();
                    while (iter.hasNext()) {
                        let player = iter.next();
                        if (player.getLevel() < MinimumLevel) {
                            cm.sendOk(`I'll only battle people who are strong.\r\nMake sure all members of your party are at least level #b${MinimumLevel}#k.`);
                            cm.dispose();
                            return;
                        }
                    }
                }
            }
            if (instance == null) {
                cm.sendAcceptDecline("No time for chatter. Fight me."); 
            } else {
                cm.sendOk("I'm already in a fight. Wait your turn.");
                cm.dispose();
            }
        } else {
            cm.sendOk("Give me time to prepare.");
            cm.dispose();
        }
    } else if (status == 2) {
        if (cm.getParty() == null) {
            em.startInstance(player);
        } else {
            em.startInstance(cm.getParty(), player.getMap());
        }
        cm.dispose();
    }
}