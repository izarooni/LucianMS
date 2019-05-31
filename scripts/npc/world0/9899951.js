load('scripts/util_cquests.js');
const CQuests = Java.type("com.lucianms.cquest.CQuestBuilder");
const QuestID = [106, 107, 108];
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
                else if (status == 4) status = 7;
            }
        }
    };

    if ((pquest = player.getCustomQuest(QuestID[0])) == null || !pquest.isCompleted()) {
        let metadata = CQuests.getMetaData(QuestID[0]);
        testFor(pquest, 6); // the status to jump to if true
        if (status == 1) { // quest beginning
            cm.sendNext("I don't know what's going on..It all happened so fast. A dark portal of some sort, a guy with white spikey hair and yellow eyes and some magic? Now there's demons and evil wolves. We need your help!");
        } else if (status == 2) {
            cm.sendNext("Okay, so while you help me out saving this world I will try to think what the name of the mysterious guy was. Meanwhile help us kill the #rPhantom Wolves#k.");
        } else if (status == 3) { // quest progress
            DisplaySummary(metadata);
        } else if (status == 4) {
            cm.sendAcceptDecline("Will you give me a hand at completing this task?");
        } else if (status == 5) {
            CQuests.beginQuest(player, QuestID[0]);
            cm.dispose();
        } else if (status == 6) { // quest complete
            if (pquest.complete(player)) {
                cm.sendNext("Thank you so much for helping us! Umm..I also remembered something..He said something about Xeha....\r\n\r\nUmm..I cant remember the rest but while you continue helping us I will try to remember the rest!");
            } else {
                cm.sendOk("You must accept my rewards! Please clear up some space in your inventory and speak to me again.");
            }
        } else if (status == 7) {
            cm.sendOk("Remember, you can check your progress anytime using the < #d@quests#k > command!");
            cm.dispose();
        } else cm.dispose();
    } else if ((pquest = player.getCustomQuest(QuestID[1])) == null || !pquest.isCompleted()) {
        let metadata = CQuests.getMetaData(QuestID[1]);
        testFor(pquest, 6); // jump
        if  (status == 1) { // quest beginning
            cm.sendNext("Oh, I remember!! His name was #eXehanort#k#n, and I am very afraid that he has corrupted Kofuku to have opened a vent, there are so many phantom wolves that have appeared throughout this land.");
        } else if (status == 2) {
            cm.sendNext("Keep on eliminiating them while I find out more information!");
        } else if (status == 3) { // quest progress
            DisplaySummary(metadata);
        } else if (status == 4) {
            cm.sendAcceptDecline("I don't know why their numbers are growing so much but this just means we'll have to move quickly.\r\nHelp me once again?");
        } else if (status == 5) {
            CQuests.beginQuest(player, QuestID[1]);
            cm.dispose();
        } else if (status == 6) { // quest complete
            if (pquest.complete(player)) {
                cm.sendNext("Yes! You did it. We mustn't stop now though..Let's continue!");
            } else {
                cm.sendOk("You must accept my rewards! Please clear up some space in your inventory and speak to me again.");
            }
            cm.dispose();
        } else cm.dispose();
    } else if ((pquest = player.getCustomQuest(QuestID[2])) == null || !pquest.isCompleted()) {
        let metadata = CQuests.getMetaData(QuestID[2]);
        testFor(pquest, 6); // jump
        if (status == 1) { // quest begeining
            cm.sendNext("Are you even doing anything? I can't do anything when there are so many phantoms freely roaming.");
        } else if (status == 2) {
           action(1, 0, -1);
        } else if (status == 3) { // quest progress
            DisplaySummary(metadata);
        } else if (status == 4) {
            cm.sendAcceptDecline("It seems that a mysterious creature is in our world somewhere. #eXehanort#k#ncame from a dark portal right? Go search for a similiar one and stop this creature!\r\nI'll put an end to the remaining chaos before it's too late");
        } else if (status == 5) {
            CQuests.beginQuest(player, QuestID[2]);
            cm.dispose();
        } else if (status == 6) { // quest complete
            if (pquest.complete(player)) {
                cm.sendNext("So I found this weird sword key looking thingy and I was considering keeping it for myself but I think you deserve it instead!");
            } else {
                cm.sendOk("You must accept my rewards! Please clear up some space in your inventory and speak to me again.");
            }
            cm.dispose();
        } else cm.dispose();
    } else {
        cm.sendOk("You've done enough. Go speak to #bHiyori#k, she most likely needs assistance right now.");
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