load('scripts/util_wedding.js');
/* izarooni 
Mom and Dad
*/
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    let quest = player.getCustomQuest(POL_PARENTS);
    if (quest == null) {
        return BeginQuest();
    } else if (!quest.isCompleted()) {
        if (quest.checkRequirements()) {
            if (quest.complete(player)) {
                cm.sendOk("Here is our blessing! Cherish each other forever~");
            } else {
                cm.sendOk("Please make sure you have enough space in your inventory to receive our blessing");
            }
        } else {
            cm.sendOk("Have you visited Nana the Fairy in Orbis and Ludibrium? Bring us their Proof of Love and you may have our blessing\r\n\r\n");
            if (player.isDebug()) {
                quest.getToCollect().getItems().values().forEach(item => {
                    cm.gainItem(item.getItemId(), item.getRequirement());
                });
            }
        }
    } else {
        cm.sendOk("We have given you our blessing already! We know you love each other very much. Go ahead, have your wedding~");
    }
    cm.dispose();
}

function BeginQuest() {
    if (status == 1) {
        cm.sendYesNo("Hello my child. Are you sure you want to get married? I believe in love at first sight, but this is rather sudden... I don't think we are ready for this. Let's think about it. Do you really love this person?");
    } else if (status == 2) {
        CQuests.beginQuest(player, POL_ORBIS, true);
        CQuests.beginQuest(player, POL_LUDI, true);
        CQuests.beginQuest(player, POL_PARENTS, true);
        cm.sendNext("Okay then, we respect your decision. Please travel to #bOrbis#k and #bLudibrium#k and collect two more Proof of Love from #bNana the Love Fairy#k");
        cm.dispose();
    }
}