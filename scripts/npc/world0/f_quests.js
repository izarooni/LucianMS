load("scripts/util_cquests.js");
var CQuests = Java.type("com.lucianms.cquest.CQuestBuilder");
/* izarooni */
var status = 0;
var in_progress = [];
var completed = [];

player.getCustomQuests().values().forEach(function(cq) {
    if (cq.isCompleted()) {
        completed.push(cq);
    } else {
        in_progress.push(cq);
    }
});

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendSimple("#b\r\n#L0#View in progress quests#l \r\n #L1#View completed quests#l");
    } else if (status == 2) {
        this.sub = selection;
        if (this.sub == 0) {
            if (in_progress.length > 0) {
                var text = "\r\n#b";
                for (var i = 0; i < in_progress.length; i++) {
                    var quest = in_progress[i];
                    text += "\r\n#L" + quest.getId() + "#" + quest.getName() + "#l";
                }
                cm.sendSimple(text);
            } else {
                cm.sendNext("\t\t\t\t\t\t#FUI/UIWindow/Quest/notice1#");
                status = 0;
            }
        } else if (this.sub == 1) {
            if (completed.length > 0) {
                var text = "\r\n#b";
                for (var i = 0; i < completed.length; i++) {
                    var quest = completed[i];
                    text += "\r\n\t" + quest.getName();
                }
                cm.sendNext(text);
                status = 0;
            } else {
                cm.sendNext("\t\t\t\t\t\t#FUI/UIWindow/Quest/notice2#");
                status = 0;
            }
        }
    } else {
        if (this.questId == null) this.questId = selection;
        if (this.sub == 0) Progress(this.questId);
    }
}

function Progress(questId) {
    var metadata = CQuests.getMetaData(questId);
    if (status >= 3 && status <= 5) {
        var text = "#FUI/UIWindow/Quest/summary#\r\n";
        if (status == 3) {
            var res = CQuestPKills(player.getCustomQuest(questId).getToKill().getKills());
            if (res != null) {
                cm.sendNext(text + res);
                return;
            }
        } else if (status == 4) {
            var res = CQuestCollect(metadata.getToCollect());
            if (res != null) {
                cm.sendNext(text + res);
                return;
            }
        } else if (status == 5) {
            var res = CQuestRewards(metadata.getRewards());
            if (res != null) {
                cm.sendNext(text + res);
                return;
            }
        }
        action(1, 0, 0);
    } else {
        status = 0;
        action(1, 0, 0);
    }
}
