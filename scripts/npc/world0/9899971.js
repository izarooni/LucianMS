load('scripts/util_cquests.js');
const CQuests = Java.type("com.lucianms.cquest.CQuestBuilder");
/* izarooni */
const DestinationFieldID = 90000007;
const QuestID = 36;
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    let quest = player.getCustomQuest(QuestID);
    if (quest != null) {
        if (!quest.isCompleted()) {
            if (quest.checkRequirements()) {
                if (quest.complete(player)) {
                    player.changeMap(DestinationFieldID);
                    cm.dispose();
                } else {
                    cm.sendOk("Please make sure you have enough room in your inventory to receive rewards");
                    cm.dispose();
                }
            } else if (!DisplaySummary(CQuests.getMetaData(QuestID))) {
                cm.dispose();
            }
        } else {
            player.changeMap(DestinationFieldID);
            cm.dispose();
        }
    } else {
        CQuests.beginQuest(player, QuestID);
        cm.sendNext("Lets begin to erase the darkness from your heart, body and mind. Kill 15 #rHeartless#k.");
    }
    cm.dispose();
}