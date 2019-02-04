/*

    Author: Lucasdieswagger @ discord

 */

//TODO test

importPackage(Packages.com.lucianms.client.arcade);

 var sections = {};
 var method = null;
 var status = 0;
 var text = "";


function start() {
    action(1, 0, 0);
}

sections["Play Mob Rush (Current highscore: "] = function(mode, type, selection) {
    if(status >= 2) {
        cm.getPlayer().setArcade(new MobRush(cm.getPlayer()));
        var minigame = cm.getPlayer().getArcade();
        if(minigame != null) {
            minigame.start();
        }
    cm.dispose();
    }
};

sections["How to play"] = function(mode, type, selection) {
    if(status >= 2) {
        cm.sendOk("When playing Mob Rush, you should be aware of the following.\r\n\r\n#r1. Keep the monster count under 90\r\n2. Don't die!\r\n3. You have 2.5 minutes.\r\n\r\n#kThe point is to survive and kill as many monsters as you can, good luck!");
        cm.dispose();
    }
};

sections["Highscores"] = function(mode, type, selection) {
    if(status >= 2) {
        var fuck = Arcade.getTop(5);
        cm.sendOk("This is the current top 50 of Mob Rush \r\n\r\n" + (fuck == null ? "#rThere are no highscores yet..#" : fuck));
        cm.dispose();
    }
};


function action(mode, type, selection) {
    if (mode === -1) {
        cm.dispose();
        return;
    } else if (mode === 0) {
        status--;
        if (status === 0) {
            cm.dispose();
            return;
        }
    } else {
        status++;
    }
    if (status === 1) {
        method = null;
            text = "Are you interested in playing Mob Rush? It's pretty cool!\r\n";
        var i = 0;
        for (var s in sections) {
            text += "\r\n#b#L" + (i++) + "#" + s + (i == 1 ? (Arcade.getHighscore(5, cm.getPlayer())) + ")" : "") + "#l#k";
        }
        cm.sendSimple(text);
    } else {
        if (method == null) {
            method = sections[get(selection)];
        }
        method(mode, type, selection);
    }
}


function get(index) {
    var i = 0;
    for (var s in sections) {
        if (i === index)
            return s;
        i++;
    }
    return null;
}
