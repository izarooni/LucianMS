load('scripts/util_cquests.js');
const CQuests = Java.type("com.lucianms.cquest.CQuestBuilder");
const QuestID = 112;
/* izarooni */
let status = 0;
let quest = player.getCustomQuest(QuestID);
let metadata = CQuests.getMetaData(QuestID);

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }

    if (quest != null) {
        if (quest.checkRequirements()) {
            status = 6;
        } else if (!quest.isCompleted()) {
            if (status == 1) status = 3;
            else if (status == 4) status = 8;
        }
    } else {
        let prequest = player.getCustomQuest(metadata.getPreQuestId());
        if (prequest == null || !prequest.isCompleted())
            status= 0;
    }

    if (status == 0) { // pre-quest status
        cm.sendOk("I don't need help. I'm very busy right now.");
        cm.dispose();
    } else if (status == 1) { // quest beginning
        cm.sendNext("Placeholder 1");
    } else if (status == 2) {
        cm.sendNext("Placeholder 2");
    } else if (status == 3) { // quest progress
        DisplaySummary(metadata);
    } else if (status == 4) {
        cm.sendAcceptDecline("uwu");
    } else if (status == 5) {
        CQuests.beginQuest(player, QuestID);
        cm.dispose();
    } else if (status == 6) { // complete
        cm.sendOk("Complete!");
        quest.complete(player);
        cm.dispose();
    } else cm.dispose();
}