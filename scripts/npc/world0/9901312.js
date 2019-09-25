var status = 0;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    }
    else {
        if (mode == 0 && status == 0) {
            cm.dispose();
            return;
        }
        if (mode == 1)
            status++;
        else
            status--;
        if (status == 0) {
            cm.sendSimple("Hello there! I am the local blacksmither.\r\n \r\nHow does blacksmithing work?\r\n#eKilling certain bosses will drop specific crystals. I can use those crystals to blacksmith strong items here.#k\r\n \r\n [#eMake sure to have space in your inventory, #bEquip/ETC#k]\r\n \r\n #bBy doing a super Rebirth 4 times and exchanging your stats into 4 SuperRB rings.#e#d" +
                "#k\r\n#L80##r Craft 5000 #i4004000# into a Shark Tooth Set" +
                "#k\r\n#L81##r Craft 5000 #i4004001# into a LionHeart Set" +
                "#k\r\n#L82##r Craft 5000 #i4004002# into a Raven Horn Set" +
                "#k\r\n#L83##r Craft 5000 #i4004003# into a Falcon Wing Set" +
                "#k\r\n#L84##r Craft 5000 #i4004004# into a Dragon Tail Set" +
                "#k\r\n#L85##r Craft 20 #i1112413# into a SweetWater Set");
        } else if (status == 1) {
            if (selection == 80) { //Shark Tooh Set
                if (cm.haveItem(4004000, 5000)) {
                    cm.gainItem(1003176, 1);
                    cm.gainItem(1052318, 1);
                    cm.gainItem(1072489, 1);
                    cm.gainItem(1082299, 1);
                    cm.gainItem(1102279, 1);
                    cm.gainItem(1492085, 1);
                    cm.gainItem(1492085, 1);
                    cm.gainItem(1482084, 1);
                    cm.gainItem(4004000, -5000);
                    cm.sendOk("You successfully used 5000 #b#v4004000##ks and crafted a Shark Tooth Set!!");
                } else
                    cm.sendOk("Sorry, you don't have 5000 #b#v4004000##ks!");
                cm.dispose();
            } else if (selection == 81) { //LionHeart Set
                if (cm.haveItem(4004001, 5000)) {
                    cm.gainItem(1003172, 1);
                    cm.gainItem(1052314, 1);
                    cm.gainItem(1072485, 1);
                    cm.gainItem(1082295, 1);
                    cm.gainItem(1102275, 1);
                    cm.gainItem(1302152, 1);
                    cm.gainItem(1432086, 1);
                    cm.gainItem(1442116, 1);
                    cm.gainItem(4004001, -5000);
                    cm.sendOk("You successfully used 5000 #b#v4004001##ks and crafted a LionHeart Set!!");
                } else
                    cm.sendOk("Sorry, you don't have 5000 #b#v4004001##ks");
                cm.dispose();
            } else if (selection == 82) { //Raven Horn Set
                if (cm.haveItem(4004002, 5000)) {
                    cm.gainItem(1003175, 1);
                    cm.gainItem(1052317, 1);
                    cm.gainItem(1072488, 1);
                    cm.gainItem(1082298, 1);
                    cm.gainItem(1102278, 1);
                    cm.gainItem(1332130, 1);
                    cm.gainItem(1472122, 1);
                    cm.gainItem(4004002, -5000);
                    cm.sendOk("You successfully used 5000 #b#v4004002##ks and crafted a Raven Horn Set!!");
                } else
                    cm.sendOk("Sorry, you don't have 5000 #b#v4004002##ks");
                cm.dispose();
            } else if (selection == 83) { //Falcon Wing Set
                if (cm.haveItem(4004003, 5000)) {
                    cm.gainItem(1003174, 1);
                    cm.gainItem(1052316, 1);
                    cm.gainItem(1072487, 1);
                    cm.gainItem(1082297, 1);
                    cm.gainItem(1102277, 1);
                    cm.gainItem(1452111, 1);
                    cm.gainItem(1462099, 1);
                    cm.gainItem(4004003, -5000);
                    cm.sendOk("You successfully used 5000 #b#v4004003##ks and crafted a Falcon Wing Set!!");
                } else
                    cm.sendOk("Sorry, you don't have 5000 #b#v4004003##ks");
                cm.dispose();
            } else if (selection == 84) { //Dragon Tail Set
                if (cm.haveItem(4004004, 5000)) {
                    cm.gainItem(1003173, 1);
                    cm.gainItem(1052315, 1);
                    cm.gainItem(1072486, 1);
                    cm.gainItem(1082296, 1);
                    cm.gainItem(1102276, 1);
                    cm.gainItem(1372084, 1);
                    cm.gainItem(1382104, 1);
                    cm.gainItem(4004004, -5000);
                    cm.sendOk("You successfully used 5000 #b#v4004004##ks and crafted a Dragon Tail Set!!");
                } else
                    cm.sendOk("Sorry, you don't have 5000 #b#v4004004##ks");
                cm.dispose();
            } else if (selection == 85) { //SweetWater set
                if (cm.haveItem(1112413, 20)) {
                    cm.gainItem(1003976, 1);
                    cm.gainItem(1052669, 1);
                    cm.gainItem(1072870, 1);
                    cm.gainItem(1082556, 1);
                    cm.gainItem(1102623, 1);
                    cm.gainItem(1112413, -20);
                    cm.sendOk("You successfully used 20 #b#v1112413##ks and crafted a SweetWater Set!!");
                } else
                    cm.sendOk("Sorry, you don't have 20 #b#v1112413##ks");
                cm.dispose();
            }
        }
    }
}