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
    let testFor = function(q, nStatus) {
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
            cm.sendNext(`Have you noticed the influx of #bflying#k phantoms in the area? They are not my doing but I cannot let them kill Yato before I do!`);
        } else if (status == 2) {
            cm.sendNext(`I need you to defeat some as they are interfering with my work`);
        } else if (status == 3) { // quest progress
            DisplaySummary(metadata);
        } else if (status == 4) {
            cm.sendAcceptDecline("Will you give me a hand at completing this task?");
        } else if (status == 5) {
            CQuests.beginQuest(player, QuestID[0]);
            cm.dispose();
        } else if (status == 6) { // quest complete
            if (pquest.complete(player)) {
                cm.sendNext("I appreciate your efforts, very well done for a rookie.");
            } else {
                cm.sendOk("You must accept my rewards! Please clear up some space in your inventory and speak to me again.");
            }
        } else cm.dispose();
    } else if ((pquest = player.getCustomQuest(QuestID[1])) == null || !pquest.isCompleted()) {
        let metadata = CQuests.getMetaData(QuestID[1]);
        testFor(pquest, 6); // jump
        if (status == 1) { // quest beginning
            cm.sendNext(`Well done but you are still very weak. Power is everything so you need to defeat more.`);
        } else if (status == 2) {
            action(1, 0, 0);
        } else if (status == 3) { // quest progress
            DisplaySummary(metadata);
        } else if (status == 4) {
            cm.sendAcceptDecline("This should be a piece of cake. Don't come back until you complete this");
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
            cm.sendNext(`We are not fully done yet..I still see more demons`);
        } else if (status == 2) {
            cm.sendNext(`I can feel the demons aura close to me..I sense that you have some kind of anger inside you from a past incident. Take it out on the demons.`);
        } else if (status == 3) { // quest progress
            DisplaySummary(metadata);
        } else if (status == 4) {
            cm.sendAcceptDecline("Where do they coming from? It must have been this white haired guy that came by recently...\r\nWe must stop the demons before it is too late!");
        } else if (status == 5) {
            CQuests.beginQuest(player, QuestID[2]);
            cm.dispose();
        } else if (status == 6) { // quest complete
            if (pquest.complete(player)) {
                cm.sendNext("I suppose you might become something one day..hmm, take my mask as your reward, rookie.");
            } else {
                cm.sendOk("You must accept my rewards! Please clear up some space in your inventory and speak to me again.");
            }
            cm.dispose();
        } else cm.dispose();
    } else {
        cm.sendOk("Have you spoken to #bNora#k yet?");
        cm.dispose();
    }
}

function isQuestCompleted(pquest) {
    if (pquest.isCompleted()) return true;
    if (pquest.checkRequirements()) {
        let completed = pquest.complete(player);
        if (completed)
            cm.sendOk("Your power is..something different.");
        else
            cm.sendOk("Please clear out some space in your inventory.\r\nYou must take my rewards!");
        cm.dispose();
        return completed;
    }
    return false;
}