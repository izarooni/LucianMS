load('scripts/util_cquests.js');
const CQuestBuilder = Java.type('com.lucianms.cquest.CQuestBuilder');
/* izarooni */
let status = 0;
let quest = player.getCustomQuests().get(37);
let metadata = CQuestBuilder.getMetaData(37);

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
        cm.sendNext("Congratulations, you did it! That wasn't hard now, was it?", 1);
        quest.complete(player);
    } else {
        cm.warp(Packages.constants.ServerConstants.HOME_MAP);
        cm.dispose();
    }
}

function beginQuest() {
    if (status == 1) {
        cm.sendNext("This one can't be too difficult. Let's do it!", 1);
        cm.getPlayer().getMap().spawnMonsterOnGroudBelow(9895246, 527, 657); // initial spawn
    } else if (status >= 2 && status <= 4) {
        var text = "#FUI/UIWindow/Quest/summary#\r\n";
        if (status == 2) {
            var res = CQuestKills(metadata.getToKill());
            if (res != null) {
                cm.sendNext(text + res);
                if(cm.getPlayer().getMap().countMonster(9895246) === 0) {
                    cm.getPlayer().getMap().spawnMonsterOnGroudBelow(9895246, 527, 657); // respawn when not completed quest, but the monster is killed.
                }
            }  else {
                action(1, 0, 0);
            }
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
        CQuestBuilder.beginQuest(player, 37);
        cm.dispose();
    }
}
