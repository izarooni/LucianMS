var StringUtil = Java.type("tools.StringUtil");
/* izarooni */
// get a list of monsters to kill, and the kill progression
function CQuestPKills(map) {
    var text = "#eKill the following monsters#n#b";
    map.forEach(function(k, v) {
        text += "\r\n" + v.getRight() + " / " + v.getLeft() + " of #o" + k + "#";
    });
    return text;
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
    if (map == null || (map.get("exp").isEmpty() && map.get("meso").isEmpty() && map.get("items").isEmpty())) return null;
    var text = "#FUI/UIWindow/QuestIcon/4/0#\r\n\r\n";

    var total = 0;
    map.get("exp").forEach(function(r) {
        total += r.getExp();
    });
    if (total > 0) {
        text += "\t#e #FUI/UIWindow/QuestIcon/8/0#  #b" + StringUtil.formatNumber(total) + "#k\r\n";
    }

    total = 0;
    map.get("meso").forEach(function(r) {
        total += r.getMeso();
    });
    if (total > 0) {
        text += "\t#e #FUI/UIWindow/QuestIcon/7/0#  #b" + StringUtil.formatNumber(total) + "#k\r\n";
    }

    if (!map.get("items").isEmpty()) {
        text += "\r\n#eItems#n#b"
        map.get("items").forEach(function(r) {
            text += "\r\n\t" + r.getQuantity() + " of #z" + r.getItemId() + "#";
        });
    }
    return text;
}
