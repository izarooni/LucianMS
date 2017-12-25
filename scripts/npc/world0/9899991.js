load("scripts/util_imports.js");
var MaplePacketCreator = Java.type("tools.MaplePacketCreator");
/* izarooni */
var status = 0;
var coins = {
    heads: [
        "#FMob/8643003/stand/0#",
        "#FMob/8643004/stand/0#"
    ],
    tails: [
        "#FMob/8643001/stand/0#",
        "#FMob/8643002/stand/0#"
    ]
};

// randomized image?
var hImage = coins.heads[Math.floor(Math.random() * coins.heads.length)];
var tImage = coins.tails[Math.floor(Math.random() * coins.tails.length)];
var hTitle = "heads";
var tTitle = "tails"
var result = Math.floor(Math.random() * 2);

{ // yea im lazy
    var hBase = 3991000 - 97;
    var tBase = 3991026 - 97;

    var temp = "";
    for (var i = 0; i < hTitle.length; i++) {
        var code = hTitle.charCodeAt(i);
        temp += "#v" + (hBase + code) + "#";
    }
    hTitle = temp;
    temp = "";
    for (var i = 0; i < tTitle.length; i++) {
        var code = tTitle.charCodeAt(i);
        temp += "#v" + (tBase + code) + "#";
    }
    tTitle = temp;
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendGetNumber("Want to bet some mesos on a coin flip?", 1000, 1000, player.getMeso());
    } else if (status == 2) {
        if (selection < 1000) {
            status = 0;
            cm.sendNext("You must put at least #b1,000 mesos#k");
        } else if (selection > player.getMeso()) {
            status = 0;
            cm.sendNext("Insufficient funds");
        } else {
            this.cash_in = selection;
            var text = "#eYou are cashing in #b" + StringUtil.formatNumber(this.cash_in) + " mesos#k\r\nWhich one will you pick?\r\n";
            text += "#L0#" + hTitle + "#l#L1#" + tTitle + "#l";
            text += "\r\n#L0#" + hImage + "#l#L1#" + tImage + "#l";
            cm.sendSimple(text);
        }
    } else if (status == 3) {
        var text = "And the winner is...\r\n";
        if (result == 0) {
            text += hTitle + "\r\n" + hImage;
        } else {
            text += tTitle + "\r\n" + tImage;
        }
        cm.sendOk(text);
        player.gainMeso(-this.cash_in, false);
        if (selection == result) {
            player.announce(MaplePacketCreator.showEffect("quest/carnival/win"));
            reward(this.cash_in);

            var achieve = player.getAchievement("Win both casino games");
            achieve.setCasino1Completed(true);
            Java.type("scripting.Achievements").testFor(player, -1);
        } else {
            player.announce(MaplePacketCreator.showEffect("quest/carnival/lose"));
            player.dropMessage("[LOSE] You lost " + StringUtil.formatNumber(this.cash_in) + " mesos");
        }
        cm.dispose();
    }
}

function reward(meso) {
    var rMeso = java.lang.Integer.parseInt(Math.floor(meso * 1.15));
    player.dropMessage("[WIN] you obtained " + StringUtil.formatNumber(rMeso) + " mesos");
    player.gainMeso(rMeso, false);
}