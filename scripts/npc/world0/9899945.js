var MaplePacketCreator = Java.type("tools.MaplePacketCreator");
var LifeFactory = Java.type("com.lucianms.server.life.MapleLifeFactory");

/* izarooni */
var status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendNext("So Hey #h #. You finally have arrived. \r\n We have a lot to discuss. Please pay attention.");
    } else if (status == 2) {
        cm.sendNext("As you know the #revil organization#k has begun their attack on our worlds. Their mission is to eradicate all light from all the worlds and bring darkness to them. \r\n From every world they bring into darkness the stronger they become.");
    } else if (status == 3) {
        cm.sendNext("You and the other heroes are the only thing that can stop them. You must visit every world and protect it from #rdarkness#k.");
    } else if (status == 4) {
        cm.sendNext("It is currently unknown who the organizations leader is so be carefull! Every organization member has different forms of magic and attacks.");
    } else if (status == 5) {
        cm.sendNext("You must train and become stronger! We have to stop them before it is too late!");
    } else if (status == 6) {
        cm.sendNext("Oh..I forgot to mention that they are also on the lookout for a box that was hidden many, many years ago. It is unknown what is inside the box but you must find it before they do!");
    } else if (status == 6) {
        cm.sendNext("We have discussed enough..It is time..You must go and protect our worlds and please..Find that #rBox#k.\r\n\r\n I will give you a starting gift so you can become even stronger.\r\nHere take those crystals with you.");
        cm.gainItem(4260002, 3);
        cm.warp(993080002);
        cm.dispose();
    }
}