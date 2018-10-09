load("scripts/util_imports.js");
load("scripts/util_cquests.js");
const CQuests = Java.type("com.lucianms.cquest.CQuestBuilder");
/* izarooni */
let status = 0;
let quests = [2, [3, 20]];

let available = [];
let in_progress = [];
let completed = [];

for (let i = 0; i < quests.length; i++) {
    let obj = quests[i];
    if (obj instanceof Array) {
        for (var a = obj[0]; a <= obj[1]; a++) {
            appendQuest(a);
        }
    } else if (typeof obj == "number") {
        appendQuest(obj);
    }
}

function appendQuest(questId) {
    let quest = player.getCustomQuest(questId);
    if (quest == null) {
        let meta = CQuests.getMetaData(questId);
        // check if the player has the pre-quest completed
        let pQuestId = meta.getPreQuestId();
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
        for (let i = 0; i < in_progress.length; i++) {
            let meta = CQuests.getMetaData(in_progress[i]);
            text += "\r\n#L" + in_progress[i] + "# #FUI/UIWindow/Quest/icon4#  " + meta.getName() + "#l";
        }
        text += "\r\n";
        for (let i = 0; i < available.length; i++) {
            let meta = CQuests.getMetaData(available[i]);
            text += "\r\n#L" + available[i] + "# #FUI/UIWindow/Quest/icon0#  " + meta.getName() + "#l";
        }
        text += "\r\n";
        for (let i = 0; i < completed.length; i++) {
            let meta = CQuests.getMetaData(completed[i]);
            text += "\r\n#L" + completed[i] + "# #FUI/UIWindow/Quest/icon1#  " + meta.getName() + "#l";
        }
        cm.sendSimple(text);
    } else {
        if (this.questId == null) this.questId = selection;
        RedirectQuest(this.questId);
    }
}

function RedirectQuest(questId) {
    if (in_progress.indexOf(questId) > -1) {
        let quest = player.getCustomQuest(questId);
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
    } else if (available.indexOf(questId) > -1) {
        Available(questId);
    } else if (completed.indexOf(questId) > -1) {
        cm.sendNext("You helped me out a lot, I'll never forget it. Thanks!");
        this.questId = null;
        status = 0;
    }
}

function Available(questId) {
    let meta = CQuests.getMetaData(this.questId);
    if (status >= 2 && status <= 4) {
        let text = "#FUI/UIWindow/Quest/summary#\r\n";
        if (status == 2) {
            var res = CQuestKills(meta.getToKill());
            if (res != null) {
                text += res;
            } else {
                action(1, 0, 0);
                return;
            }
        } else if (status == 3) {
            var res = CQuestCollect(meta.getToCollect());
            if (res != null) {
                text += res;
            } else {
                action(1, 0, 0);
                return;
            }
        } else if (status == 4) {
            var res = CQuestRewards(meta.getRewards());
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
