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
        cm.sendOk("Darkness is a dangerous weapon in this world..");
        cm.dispose();
    } else if (status == 1) { // quest beginning
        cm.sendNext("I feel weird..I can't control..I..I need your help. This man with yellow eyes did something to me, he stared at me and suddenly I began to feel weird.");
    } else if (status == 2) {
        cm.sendNext("I can only feel #edarkness#k#n inside. Please, help me!");
    } else if (status == 3) { // quest progress
        DisplaySummary(metadata);
    } else if (status == 4) {
        cm.sendAcceptDecline("I have my faith in you.");
    } else if (status == 5) {
        CQuests.beginQuest(player, QuestID);
        cm.dispose();
    } else if (status == 6) { // complete
        cm.sendOk("I cannot thank you enough! Try to find this weird man, I do not know his name but we need to stop him!");
        quest.complete(player);
        cm.dispose();
    } else cm.dispose();
}