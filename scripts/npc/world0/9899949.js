load('scripts/util_cquests.js');
const CQuests = Java.type("com.lucianms.cquest.CQuestBuilder");
const QuestID = 113;
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
            status = 0;
    }

    if (status == 0) { // pre-quest status
        cm.sendOk("We need to find the noragami mask pieces and recreate it to stop this #edarkness#k#n from spreading even more!");
        cm.dispose();
    } else if (status == 1) { // quest begining
        cm.sendNext("I lost it! I lost the mask it! I'm so irresponsible how could I lose it...");
    } else if (status == 2) {
        cm.sendNext("Hey you! #b#h ##k, can you help me find the mask pieces? They are around here somewhere. I know it is!");
    } else if (status == 3) { // quest progress
        DisplaySummary(metadata);
    } else if (status == 4) {
        cm.sendAcceptDecline("I'm not gonna find the pieces anytime soon alone...");
    } else if (status == 5) {
        CQuests.beginQuest(player, QuestID);
        cm.dispose();
    } else if (status == 6) { // quest complete
        if (quest.isCompleted()) {
            cm.sendOk("Please..Do not use the mask irresponsible else you could turn the world against itself!");
        } else if (quest.complete(player)) {
            cm.sendOk("You found all the pieces? You did! Thank you so much I'll recreate the mask and you may have it, hehe.");
        } else {
            cm.sendOk("Check if your inventory is full, you must accept my reward!");
        }
        cm.dispose();
    } else cm.dispose();
}