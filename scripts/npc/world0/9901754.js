/*
load("scripts/npc/generic_shop.js");

var pointsType = "vote points";

try {
    var file = new java.io.File("resources/data-vp.json");
    var content = Packages.org.apache.commons.io.FileUtils.readFileToString(file);
    var json = JSON.parse(content);

    for (var j in json) {
        items[j] = JSON.parse("[" + json[j] + "]");
    }
} catch (e) {
    broken = e.message.replace(/\\/g, "/");
}

*/
var expiration = 3600000; //One hour in MS
const cost = [3,4,6,8,10,13,16];
function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }

    if (status == 0) {
            cm.sendSimple("Hello. I'm the Chirithy #bVote Points#k NPC.\r\n"
            + "\r\n#d#L0#I would like to buy Lidium(AutoRB) with my VP#l"
            + "\r\n#L1#Open the VP Shop!#l");

    }

    else if(status ==1){

        if(selection ==0){

            cm.sendSimple("Hello. What would you like?\r\n"
            + "\r\n#d#L0#16 Hours for 3 VP#l"
            + "\r\n#d#L1#24 Hours for 4 VP#l"
            + "\r\n#d#L2#36 Hours for 6 VP#l"
            + "\r\n#d#L3#48 Hours for 8 VP#l"
            + "\r\n#d#L4#72 Hours for 10 VP#l"
            + "\r\n#d#L5#96 Hours for 13 VP#l"
            + "\r\n#L6#One Week for 16 VP#l");
        }

        if(selection ==1){
            cm.openNpc(9901754, "vp-shop");
        }
    }

    else if(status == 2){
        switch(selection){
            case 0:
                expiration*=16;
                break;
            case 1:
                expiration*=24;
                break;
            case 2:
                expiration*=36;
                break;
            case 3:
                expiration*=48;
                break;
            case 4:
                expiration*=72;
                break;
            case 5:
                expiration*=96;
                break;
            case 6:
                expiration*=168;
                break;
            default:
                break;
        }

        var points = cost[selection];

        if(player.getClient().getVotePoints() >= points && cm.canHold(4000000)){
            cm.createItemWithExpiration(4011008, expiration);
            player.getClient().setVotePoints(player.getClient().getVotePoints() - points);
            dialog = "Thank you! You now have " + player.getClient().getVotePoints() + " points left.";
            cm.sendOk(dialog);
            cm.dispose();
        }

        else {

            dialog = "You don't have enough points or not enough room.";
            cm.sendOk(dialog);
            cm.dispose();
        }

    }
    
    

}