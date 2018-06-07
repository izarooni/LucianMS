load('scripts/util_cquests.js');
const CQuestBuilder = Java.type('com.lucianms.cquest.CQuestBuilder');
/* izarooni */
let status = 0;
let quest = player.getCustomQuests().get(36);
let metadata = CQuestBuilder.getMetaData(36);

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (quest == null) beginQuest();
    else completeQuest();
}

function completeQuest() {
    if (!quest.checkRequirements()) {
        cm.sendOk("Have you forgotten what to do? You can check all of your quests via the #d< @quests >#k command!");
        cm.dispose();
    } else if (!quest.isCompleted()) {
        cm.sendOk("Look at you go! You're a natural at this.\r\nLet me do one more thing for you before you start your adventure.\r\nSpeak to me when you're ready");
        quest.complete(player);
        cm.dispose();
    } else {
        if (status == 1) {
            cm.sendNext("Are you ready for your next task?");
        } else if (status == 2) {
            cm.warp(90000004);
            cm.openNpc(9899984);
        }
    }
}

function beginQuest() {
    if (status == 1) {
        cm.sendNext("Hey there newbie! I know it's pretty difficult starting fresh so let me help you get a little stronger.", 1);
    } else if (status >= 2 && status <= 4) {
        var text = "#FUI/UIWindow/Quest/summary#\r\n";
        if (status == 2) {
            var res = CQuestKills(metadata.getToKill());
            if (res != null) cm.sendNext(text + res);
            else action(1, 0, 0);
        } else if (status == 3) {
            var res = CQuestCollect(metadata.getToCollect());
            if (res != null) cm.sendNext(text + res);
            else action(1, 0, 0);
        } else if (status == 4) {
            var res = CQuestRewards(metadata.getRewards());
            if (res != null) cm.sendNext(text + res);
            else action(1, 0, 0);
        }
    } else if (status == 5) {
        cm.sendAcceptDecline("Let me know when you're ready to take on this task!");
    } else if (status == 6) {
        CQuestBuilder.beginQuest(player, 36);
        cm.dispose();
    }
}
