load('scripts/util_imports.js');
/* izarooni */
var status = 0;
var ID_RewardBox = 2022336;
var items = {
    "warrior": {
        "lvl-30": [1052613, 1003864, 1082343, 1102563, 1012377, 1122253, 1132229, 1302124],
        "lvl-60": [1082526, 1052614,1003563, 1302126, 1442084, 1402068],
        "lvl-70": [1302012, 1442030],
        "lvl-90": [1302100, 1402050, 1432011 ],
        "lvl-110": [1302312, 1442251, 1052929, 1004492, 1082647, 1102828, 1073057, 1012524, 1122306, 1132287],
        "lvl-120": [1052299, 1003154, 1082285, 1072471, 1102262, 1302193, 1442156, 1402131]
    },

    "thief": {
        "lvl-30": [1052613, 1003864, 1082343, 1102563, 1012377, 1122253, 1132229, 1472093, 1332133],
        "lvl-60": [1082526, 1052614, 1003563, 1472095, 1332094],
        "lvl-70": [1332064, 1472062],
        "lvl-90": [1332135, 1472074],
        "lvl-110": [1332049, 1472244, 1052929, 1004492, 1082647, 1102828, 1073057, 1012524, 1122306, 1132287],
        "lvl-120": [1052302, 1003157, 1082288, 1072474, 1102265, 1472161, 1332170]
    },

    "bowman": {
        "lvl-30": [1052613, 1003864, 1082343, 1102563, 1012377, 1122253, 1132229, 1452114, 1462102],
        "lvl-60": [1082526,  1052614, 1003563, 1452079, 1462040],
        "lvl-70": [1452031, 1462027],
        "lvl-90": [1462104, 1452116],
        "lvl-110": [1462222, 1452235, 1052929, 1004492, 1082647, 1102828, 1073057, 1012524, 1122306, 1132287],
        "lvl-120": [1052301, 1003156, 1082287, 1072473, 1102264, 1452149, 1462139]
    },

    "pirate": {
        "lvl-30": [1052613, 1003864, 1082343, 1102563, 1012377, 1122253, 1132229, 1482041, 1492042],
        "lvl-60": [1082526,  1052614, 1003563, 1482043, 1492044],
        "lvl-70": [1492142, 1482124],
        "lvl-90": [1482089, 1492106],
        "lvl-110": [1492209, 1482199, 1052929, 1004492, 1082647, 1102828, 1073057, 1012524, 1122306, 1132287],
        "lvl-120": [1052303, 1003158, 1082289, 1072475, 1102266, 1482122, 1492122]
    },

    "magician": {
        "lvl-30": [1052613, 1003864, 1082343, 1102563, 1012377, 1122253, 1132229, 1382109, 1372058],
        "lvl-60": [1082526,  1052614, 1003563, 1372056, 1382076],
        "lvl-70": [1382121, 1372038],
        "lvl-90": [1382111, 1372118],
        "lvl-110": [1372204, 1382242, 1052929, 1004492, 1082647, 1102828, 1073057, 1012524, 1122306, 1132287],
        "lvl-120": [1052300, 1003155, 1082286, 1072472, 1102263, 1372119, 1382145]
    }
};

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (selection > -1) {
        var item = player.getInventory(InventoryType.USE).getItem(selection);
        if (item != null && !item.getOwner().isEmpty() && item.getItemId() == ID_RewardBox) {
            var level = parseInt(item.getOwner().split(" ")[1]);
            if (isNaN(level)) {
                cm.sendOk("What's this? Where did you get this reward box?");
                cm.dispose();
                return;
            }
            var rewards = items[getJobCategory(cm.getJobId())]["lvl-" + level];
            for (var i = 0; i < rewards.length; i++) {
                if (!InventoryModifier.checkSpace(client, rewards[i], 1, "")) {
                    player.sendMessage(1, "Make more room in your EQUIP inventory before receiving your rewards");
                    cm.dispose();
                    return;
                }
            }
            for (var i = 0; i < rewards.length; i++) {
                cm.gainItem(rewards[i], 1);
            }
            cm.gainItem(item.getItemId(), -1);
            setLevelReward(getLevelReward() + 1);
            cm.sendOk("Enjoy your new equips!~");
        } else {
            player.sendMessage("Error: Unable to find reward box");
        }
        cm.dispose();
    }
}

function getJobCategory(n) {
    var j = Math.floor(n / 100);
    switch (j) {
        case 1: return "warrior";
        case 2: return "magician";
        case 3: return "bowman";
        case 4: return "thief";
        case 5: return "pirate";
    }
}

function getLevelReward() {
    let con = Database.getConnection();
    try {
        var ps = con.prepareStatement("select level_reward from characters where id = ?");
        ps.setInt(1, player.getId());
        var rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("level_reward");
        }
        rs.close();
        ps.close();
    } finally { con.close(); }
    return -1;
}

function setLevelReward(n) {
    let con = Database.getConnection();
    try {
        var ps = con.prepareStatement("update characters set level_reward = ? where id = ?");
        ps.setInt(1, n);
        ps.setInt(2, player.getId());
        ps.executeUpdate();
        ps.close();
    } finally { con.close(); }
}
