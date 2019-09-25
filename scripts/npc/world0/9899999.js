load("nashorn:mozilla_compat.js");
load("scripts/util_imports.js");
var cqb = Java.type("com.lucianms.cquest.CQuestBuilder");

importPackage(Packages.com.lucianms.server.quest.custom.reward);

/* izarooni */
var DAILY_PROP = "npc.daily_quest_npc.quest_id";
var status = 0;
var quest_id = 0;

if (System.getProperties().getProperty(DAILY_PROP) == null) {
    quest_id = Randomizer.rand(100, 105); // currently there is only 6 avaialble quests
    System.getProperties().setProperty(DAILY_PROP, quest_id);
} else {
    // Integer#parseInt my fucking ass javascript fuck off
    quest_id = java.lang.Integer.parseInt(System.getProperties().getProperty(DAILY_PROP));
}

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
    this.quest = player.getCustomQuest(quest_id);
    this.meta = cqb.getMetaData(quest_id);
    if (status == 1) {
        if (this.quest == null) {
            cm.sendNext("#eQuest Name : #b" + this.meta.getName() + "#k#n\r\nHey #h #, are you interested in today's daily quest? Every daily quest is different in its level difficulty.");
        } else {
            this.finished = this.quest.checkRequirements();
            if (this.quest.isCompleted()) {
                var timestamp = parseInt(this.quest.getCompletion());
                if (this.quest.isDaily()) {
                    var nextDay = timestamp + (1000 * 3600 * 24);
                    if (Date.now() >= nextDay) {
                        player.getCustomQuests().remove(quest_id);
                        status = 0;
                        cm.sendNext("Give me a moment to prepare this quest for you...");
                    } else {
                        cm.sendOk("You've already completed this quest at server time\r\n#b" + new Date(timestamp) + "#k\r\nCome back tomorrow for a new quest!");
                        cm.dispose();
                    }
                }
            } else if (this.finished) {
                cm.sendNext("You're done? That's what I'm talking about!");
            } else {
                cm.sendNext("Oh hey! Not quite done yet, huh?\r\nIf you need, I can remind you of what you need to do");
            }
        }
    } else if (status == 2) {
        if (this.quest == null || !this.finished) {
            var kills = this.meta.getToKill();
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
                cm.sendOk("That's all for now, see you tomorrow!~");
            } else {
                cm.sendOk("You need to make room in your inventory before you receive your rewards");
            }
            cm.dispose();
        }
    } else if (status == 3) {
        if (this.quest == null || !this.finished) {
            var collects = this.meta.getToCollect();
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
        var rewards = this.meta.getRewards();
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
        cqb.beginQuest(player, quest_id);
        cm.sendOk("Alright! I knew you would do it!\r\nCome speak to me again once you're finished everything, or if you forget what you're supposed to do~");
        cm.dispose();
    }
}
