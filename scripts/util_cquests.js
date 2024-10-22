StringUtil = Java.type("tools.StringUtil");
const SummaryIcon     = "#FUI/UIWindow/Quest/summary#";
const InProgressIcon  = "#FUI/UIWindow/UtilDlgEx/list0";
const AvailableIcon   = "#FUI/UIWindow/UtilDlgEx/list1#";
const CanCompleteIcon = "#FUI/UIWindow/UtilDlgEx/list3#";
const LockedIcon      = "#FUI/UIWindow/ItemProtector/Icon/0#";
/* izarooni */
// get a list of monsters to kill, and the kill progression
function CQuestPKills(map) {
    var text = "#eKill the following monsters#n#b";
    map.forEach(function(k, v) {
        text += "\r\n" + v.getRight() + " / " + v.getLeft() + " of #o" + k + "#";
    });
    return text;
}

function DisplaySummary(metadata) {
    this.originalStatus = this.originalStatus || status;
    this.internalStatus = this.internalStatus + 1 || 1;
    status = this.originalStatus - 1;

    let content = `${SummaryIcon}\r\n`;
    if (this.internalStatus == 1) {
        let append = CQuestKills(metadata.getToKill());
        if (append == null) return DisplaySummary(metadata);
        else cm.sendNext(content = (content + append));
    } else if (this.internalStatus == 2) {
        let append = CQuestCollect(metadata.getToCollect());
        if (append == null) return DisplaySummary(metadata);
        else cm.sendNext(content = (content + append))
    } else if (this.internalStatus == 3) {
        let append = CQuestRewards(metadata.getRewards());
        if (append == null) return DisplaySummary(metadata);
        else cm.sendNext(content = (content + append));
    } else if (this.internalStatus == 4) { // no rewards?
        status = this.originalStatus;
        action(1, 0, -1);
        return false;
    }
    return true;
}

// get a list of monsters to kill
function CQuestKills(map) {
    if (map == null || map.isEmpty()) return null;
    var text = "#eKill the following monsters#n#b";
    map.forEach(function(k, v) {
        text += "\r\n" + v + " of #o" + k + "#";
    });
    return text;
}

// get a list of items to collect
function CQuestCollect(map) {
    if (map == null || map.isEmpty()) return null;
    var text = "#eCollect the following items#n#b";
    map.forEach(function(k, v) {
        text += "\r\n" + v.getRequirement() + " of #z" + k + "#";
    });
    return text;
}

// get a list of rewards
function CQuestRewards(map) {
    if (map == null || (map.get("expp").isEmpty() && map.get("exp").isEmpty() && map.get("meso").isEmpty() && map.get("items").isEmpty())) return null;
    
    let text = "#FUI/UIWindow/QuestIcon/4/0#\r\n\r\n";
    let total = 0;
    map.get("exp").forEach(function (r) {
        total += r.getExp();
    });
    map.get("expp").forEach(function (r) {
        total += r.getExp(player);
    });

    // mesos rewards
    if (total > 0) {
        text += "\t#e #FUI/UIWindow/QuestIcon/8/0#  #b" + StringUtil.formatNumber(total) + "#k\r\n";
    }
    total = 0;
    map.get("meso").forEach(function(r) {
        total += r.getMeso();
    });

    // items rewards
    if (total > 0) {
        text += "\t#e #FUI/UIWindow/QuestIcon/7/0#  #b" + StringUtil.formatNumber(total) + "#k\r\n";
    }
    if (!map.get("items").isEmpty()) {
        text += "\r\n#eItems#n#b"
        map.get("items").forEach(function(r) {
            text += "\r\n\t" + r.getQuantity() + " of #z" + r.getItemId() + "#";
        });
    }
    map.values().forEach(a => a.clear());
    map.clear();
    return text;
}
