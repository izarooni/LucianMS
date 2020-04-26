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
var expiration = 360000000000; //One hour in MS
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
        cm.openNpc(9901755, "dp-shop");
    }

    else if(status ==1){

        if(selection ==0){

            cm.sendSimple("Hello. What would you like?\r\n"
            + "\r\n#L0#Perma Lidium for 20 DP#l");
        }

        if(selection ==1){
            cm.openNpc(9901755, "dp-shop");
        }
    }

    else if(status == 2){

        var points = 20

        if(player.getClient().getDonationPoints() >= points && cm.canHold(4000000)){
            cm.createItemWithExpiration(4011008, expiration);
            player.getClient().setDonationPoints(player.getClient().getDonationPoints() - points);
            dialog = "Thank you! You now have " + player.getClient().getDonationPoints() + " points left.";
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