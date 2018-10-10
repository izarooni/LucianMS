load('scripts/util_cquests.js');
const CQuests = Java.type("com.lucianms.cquest.CQuestBuilder");
const QuestID = [109, 110, 111];
/* izarooni */
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }

    let pquest;
    let testFor = function (q, nStatus) {
        if (q != null) {
            if (!q.isCompleted() && q.checkRequirements()) {
                status = nStatus;
            } else {
                if (status == 1) status = 3;
                else if (status == 6) status = 8;
            }
        }
    };

    if ((pquest = player.getCustomQuest(QuestID[0])) == null || !pquest.isCompleted()) {
        let metadata = CQuests.getMetaData(QuestID[0]);
        testFor(pquest, 6); // the status to jump to if true
        if (status == 1) { // quest beginning
            cm.sendNext(`Placeholder text 1 #b${metadata.getName()}#k`);
        } else if (status == 2) {
            cm.sendNext(`Placeholder text 2 #b${metadata.getName()}#k`);
        } else if (status == 3) { // quest progress
            DisplaySummary(metadata);
        } else if (status == 4) {
            cm.sendAcceptDecline("Will you give me a hand at completing this task?");
        } else if (status == 5) {
            CQuests.beginQuest(player, QuestID[0]);
            cm.dispose();
        } else if (status == 6) { // quest complete
            if (pquest.complete(player)) {
                cm.sendNext("I appreciate your efforts, I'll let you know if I need your assistance again.");
            } else {
                cm.sendOk("You must accept my rewards! Please clear up some space in your inventory and speak to me again.");
            }
        } else cm.dispose();
    } else if ((pquest = player.getCustomQuest(QuestID[1])) == null || !pquest.isCompleted()) {
        let metadata = CQuests.getMetaData(QuestID[1]);
        testFor(pquest, 6); // jump
        if (status == 1) { // quest beginning
            cm.sendNext(`Placeholder text 1 #b${metadata.getName()}#k`);
        } else if (status == 2) {
            cm.sendNext(`Placeholder text 2 #b${metadata.getName()}#k`);
        } else if (status == 3) { // quest progress
            DisplaySummary(metadata);
        } else if (status == 4) {
            cm.sendAcceptDecline("I don't know why their numbers are growing so much but this just means we'll have to move quickly.\r\nHelp me once again?");
        } else if (status == 5) {
            CQuests.beginQuest(player, QuestID[1]);
            cm.dispose();
        } else if (status == 6) { // quest complete
            if (pquest.complete(player)) {
                cm.sendNext("Very impressive! I suppose to may have my trust now");
            } else {
                cm.sendOk("You must accept my rewards! Please clear up some space in your inventory and speak to me again.");
            }
            cm.dispose();
        } else cm.dispose();
    } else if ((pquest = player.getCustomQuest(QuestID[2])) == null || !pquest.isCompleted()) {
        let metadata = CQuests.getMetaData(QuestID[2]);
        testFor(pquest, 6); // jump
        if (status == 1) { // quest begeining
            cm.sendNext(`Placeholder text 1 #b${metadata.getName()}#k`);
        } else if (status == 2) {
            cm.sendNext(`Placeholder text 2 #b${metadata.getName()}#k`);
        } else if (status == 3) { // quest progress
            DisplaySummary(metadata);
        } else if (status == 4) {
            cm.sendAcceptDecline("Where are they coming from? I have never seen so many appear at once...\r\nWe must put an end to this before it's too late");
        } else if (status == 5) {
            CQuests.beginQuest(player, QuestID[2]);
            cm.dispose();
        } else if (status == 6) { // quest complete
            if (pquest.complete(player)) {
                cm.sendNext("Thank you so much for helping me! I'll be sure to ask for help the next time I need it!");
            } else {
                cm.sendOk("You must accept my rewards! Please clear up some space in your inventory and speak to me again.");
            }
            cm.dispose();
        } else cm.dispose();
    } else {
        cm.sendOk("Have you spoken to #bNora#k?");
        cm.dispose();
    }
}

function isQuestCompleted(pquest) {
    if (pquest.isCompleted()) return true;
    if (pquest.checkRequirements()) {
        let completed = pquest.complete(player);
        if (completed)
            cm.sendOk("Thank you for helping me! I'll be sure to ask you first if I ever need help again!");
        else
            cm.sendOk("Please clear out some space in your inventory.\r\nYou must take my rewards!");
        cm.dispose();
        return completed;
    }
    return false;
}