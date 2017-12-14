load("scripts/util_imports.js");
load("scripts/util_cquests.js");
var CQuests = Java.type("server.quest.custom.CQuestBuilder");
/* izarooni */
var status = 0;
var quests = [
    2, // maple collector
    [3, 20], // evolve ring
    21, // golems
    22,  // just toys
    23, // castle crusader
    24, // from the past
    27, // beginners luck
    32, // dark cave
    33, // mystery in space
    34 // snowy area
  ];

var available = [];
var in_progress = [];
var completed = [];

for (var i = 0; i < quests.length; i++) {
    var obj = quests[i];
    if (obj instanceof Array) {
        for (var a = obj[0]; a <= obj[1]; a++) {
            appendQuest(a);
        }
    } else if (typeof obj == "number") {
        appendQuest(obj);
    }
}

function appendQuest(questId) {
    var meta = CQuests.getMetaData(questId);
    if (meta == null) {
        return;
    }
    var quest = player.getCustomQuest(questId);
    if (quest == null) {
        // check if the player has the pre-quest completed
        var pQuestId = meta.getPreQuestId();
        if (pQuestId == -1) {
            available.push(questId);
        } else {
            // ignore quests that can't be accepted
            // due to a pre-quest not being compelted
            quest = player.getCustomQuest(pQuestId);
            if (quest != null && quest.isCompleted()) {
                available.push(questId);
            }
        }
    } else if (quest.isCompleted()) {
        completed.push(quest.getId());
    } else {
        in_progress.push(quest.getId());
    }
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        var text = "Got some free time? I have some things I need completed\r\n#b";
        for (var i = 0; i < in_progress.length; i++) {
            var name = CQuests.getMetaData(in_progress[i]).getName();
            text += "\r\n#L" + in_progress[i] + "# #FUI/UIWindow/Quest/icon4#  " + name + "#l";
        }
        text += "\r\n";
        for (var i = 0; i < available.length; i++) {
            var name = CQuests.getMetaData(available[i]).getName();
            text += "\r\n#L" + available[i] + "# #FUI/UIWindow/Quest/icon0#  " + name + "#l";
        }
        text += "\r\n";
        for (var i = 0; i < completed.length; i++) {
            var name = CQuests.getMetaData(completed[i]).getName();
            text += "\r\n#L" + completed[i] + "# #FUI/UIWindow/Quest/icon1#  " + name + "#l";
        }
        cm.sendSimple(text);
    } else {
        if (this.questId == null) {
            this.questId = selection;
            this.metadata = CQuests.getMetaData(this.questId);
        }
        RedirectQuest();
    }
}

function RedirectQuest() {
    if (in_progress.indexOf(this.questId) > -1) {
        var quest = player.getCustomQuest(this.questId);
        if (quest.checkRequirements()) {
            if (quest.complete(player)) {
                cm.sendOk("You did it #b#h ##k! Thanks for all your hard work!");
            } else {
                cm.sendOk("You need to make room in your inventory before you receive your rewards");
            }
        } else {
            cm.sendOk("Not done yet? The sooner the better and don't you worry, you'll get your reward!");
        }
        cm.dispose();
    } else if (available.indexOf(this.questId) > -1) {
        Available();
    } else if (completed.indexOf(this.questId) > -1) {
        cm.sendNext("You helped me out a lot, I'll never forget it. Thanks!");
        this.questId = null;
        status = 0;
    }
}

function Available() {
    if (player.getLevel() < this.metadata.getMinimumLevel()) {
        cm.sendOk("Unfortunately you must be at least #blevel " + this.metadata.getMinimumLevel() + "#k to accept this quest");
        cm.dispose();
        return;
    }
    if (status >= 2 && status <= 4) {
        var text = "#FUI/UIWindow/Quest/summary#\r\n";
        if (status == 2) {
            var res = CQuestKills(this.metadata.getToKill());
            if (res != null) {
                text += res;
            } else {
                action(1, 0, 0);
                return;
            }
        } else if (status == 3) {
            var res = CQuestCollect(this.metadata.getToCollect());
            if (res != null) {
                text += res;
            } else {
                action(1, 0, 0);
                return;
            }
        } else if (status == 4) {
            var res = CQuestRewards(this.metadata.getRewards());
            if (res != null) {
                text = res;
            } else {
                action(1, 0, 0);
                return;
            }
        }
        cm.sendNext(text);
    } else if (status == 5) {
        cm.sendAcceptDecline("Would you complete these tasks for me?");
    } else if (status == 6) {
        CQuests.beginQuest(player, this.questId);
        cm.sendOk("Thanks!\r\nI'll be here when you're done");
        cm.dispose();
    }
}
