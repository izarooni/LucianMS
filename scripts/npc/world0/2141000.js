load('scripts/util_imports.js');
/* izarooni */
let status = 0;
const ID_RewardBox = 2022336;
const items = {
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
    if (status == 1) {
        let jcategory = getJobCategory(cm.getJobId());
        if (jcategory != null) {
            let sub = items[jcategory];
            let i = 0;
            let obtained = false;
            for (let l in sub) {
                let level = parseInt(l.split("-")[1]);
                if (getLevelReward() <= (i++) && level <= player.getLevel()) {
                    let item = new Packages.client.inventory.Item(ID_RewardBox, 0, 1);
                    item.setOwner("Level " + level + " - Reward Box")
                    if (!(obtained = InventoryModifier.addFromDrop(client, item, true))) {
                        player.dropMessage(1, "Make room in your USE inventory before receiving your reward box.");
                    } else {
                        cm.sendOk("Here is your level " + level + " reward box! Open it from your USE inventory!");
                        setLevelReward(getLevelReward() + 1);
                    }
                    break;
                }
            }
            if (!obtained) {
                cm.sendOk("You do not have any reward boxes to claim.");
            }
        } else {
            cm.sendOk("Unfortunately your job is currently not supported to accept level rewards.");
        }
        cm.dispose();
    }
}

function getJobCategory(n) {
    let j = Math.floor(n / 100);
    switch (j) {
        case 1: return "warrior";
        case 2: return "magician";
        case 3: return "bowman";
        case 4: return "thief";
        case 5: return "pirate";
    }
}

function getLevelReward() {
    let ps = Database.getConnection().prepareStatement("select level_reward from characters where id = ?");
    ps.setInt(1, player.getId());
    let rs = ps.executeQuery();
    if (rs.next()) {
        return rs.getInt("level_reward");
    }
    rs.close();
    ps.close();
    return -1;
}

function setLevelReward(n) {
    let ps = Database.getConnection().prepareStatement("update characters set level_reward = ? where id = ?");
    ps.setInt(1, n);
    ps.setInt(2, player.getId());
    ps.executeUpdate();
    ps.close();
}
