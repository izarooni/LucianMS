load("nashorn:mozilla_compat.js");
load("scripts/util_imports.js");
var cqb = Java.type("com.lucianms.cquest.CQuestBuilder");

importPackage(Packages.server.quest.custom.reward);

/* izarooni */
var status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        if (status == 5 && mode == 0) {
            cm.sendOk("No problem! Just let me know if you change your mind");
        }
        cm.dispose();
        return;
    } else {
        status++;
    }
    this.quest = player.getCustomQuest(1);
    if (status == 1) {
        if (this.quest == null) {
            cm.sendNext("#eQuest Name : #b" + cqb.getName(1) + "#k#n\r\nHey #h #, would you mind helping me out clear out some of the irregulars that are inside this forest?");
        } else {
            this.finished = this.quest.checkRequirements();
            if (this.quest.isCompleted()) { // quest completed and 'handed in'
                cm.sendOk("Thanks for helping me out!");
                cm.dispose();
            } else if (this.finished) { // quest is compelted but not 'handed in'
                cm.sendNext("You're done? That's what I'm talking about!");
            } else {
                cm.sendNext("Oh hey! Not quite done yet, huh?\r\nIf you need, I can remind you of what you need to do");
            }
        }
    } else if (status == 2) {
        if (this.quest == null || !this.finished) {
            var kills = cqb.getToKill(1);
            if (!kills.isEmpty()) {
                var text = "#FUI/UIWindow/Quest/summary#\r\n";
                text += "#eKill the following monsters#n#b";
                kills.forEach(function(k, v) {
                    text += "\r\n" + v + " of #o" + k + "#";
                });
                cm.sendNext(text);
            } else {
                action(1, 0, 0);
            }
        } else if (this.finished) {
            if (this.quest.complete(player)) {
                cm.sendOk("That's all for now!~");
            } else {
                cm.sendOk("You need to make room in your inventory for your rewards.");
            }
            cm.dispose();
        }
    } else if (status == 3) {
        if (this.quest == null || !this.finished) {
            var collects = cqb.getToCollect(1);
            if (!collects.isEmpty()) {
                var text = "#FUI/UIWindow/Quest/summary#\r\n";
                text += "#eCollect the following items#n#b";
                collects.forEach(function(k, v) {
                    text += "\r\n" + v.getRequirement() + " of #z" + k + "#";
                });
                cm.sendNext(text);
            } else {
                action(1, 0, 0);
            }
        }
    } else if (status == 4) {
        var text = "#FUI/UIWindow/Quest/reward#\r\n\r\n";
        var rewards = cqb.getRewards(1);
        var total = 0;
        rewards.get("exp").forEach(function(r) {
            total += r.getExp();
        });
        if (total > 0) {
            text += "#eTotal EXP: #b" + StringUtil.formatNumber(total) + "#k\r\n";
        }

        total = 0;
        rewards.get("meso").forEach(function(r) {
            total += r.getMeso();
        });
        if (total > 0) {
            text += "#eTotal Meso: #b" + StringUtil.formatNumber(total) + "#k\r\n";
        }

        if (!rewards.get("items").isEmpty()) {
            text += "#eItems#n#b"
            rewards.get("items").forEach(function(r) {
                text += "\r\n" + r.getQuantity() + " of #z" + r.getItemId() + "#";
            });
        }
        cm.sendNext(text);
        if (this.finished != null && !this.finished) {
            cm.dispose();
            return;
        }
    } else if (status == 5) {
        cm.sendAcceptDecline("What do you think? Are you up for this quest?");
    } else if (status == 6) {
        cqb.beginQuest(player, 1);
        cm.sendOk("Alright! I knew you would do it!\r\nCome speak to me again once you're finished everything, or if you forget what you're supposed to do~");
        cm.dispose();
    }
}
