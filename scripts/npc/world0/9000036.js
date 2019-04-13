/*Maple Weapon Upgrade NPC
Made by Cesar for Morphine Network servers
NPC ID - 9120023*/

var status = 0;

var leaf = 4001126; 

var warrior = 0;
var bowman = 0;
var thief = 0;
var mage = 0;
var pirate = 0;
var prank = 0;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {

         
         if (mode == -1) {
        cm.dispose();
    
    }else if (mode == 0){
        cm.dispose();

    }else{
        if (mode == 1)
            status++;
        else
            status--;
				if (status == 0) {
					cm.sendNext("Agent E Here Reporting for duty. \r\nI'm the Chirithy's #rMaple Weapon#k upgrader! \r\nI'll try to do my best in upgrading your gear to full power.");
                }
				if (status == 1) {
					cm.sendSimple("What would you like to do Agent #h # \r\n#L0##bUpgrade my weapon#l\r\n#L1#What do I need to upgrade my weapon?#l\r\n#L2#Nevermind!#l");
				}
				else if (status == 2) {
					if (selection == 0) {
						cm.sendSimple("#L0##bWarrior Maple Weapons#l\r\n#L1#Archer Maple Weapons#l\r\n#L2#Thief Maple Weapons#l\r\n#L3#Magician Maple Weapons#l\r\n#L4#Pirate Maple Weapons#l\r\n#L5#GM Items#l");
					}
					else if (selection == 1) {
						cm.sendOk("Well aren't you the curious one! My main purpose to upgrade your #rMaple Weapon#k to the next level, but for you, I might just do something extra *wink*. \r\n\r\nI can upgrade level 35 Maple Weapons to level 43 Maple Weapons, and from level 43 Maple Weapons to level 64 Maple Weapons. I can't however upgrade you level 35 Maple Weapons to level 64 Maple Weapons. It's just impossible for me to do so! \r\n\r\nYou will need to bring mesos and #b#z4001126##k according to the upgrade you want me to perform. #b#z4001126##k can be found anywhere! I might also need other items, but I'm not entirely sure yet! I need to check my 'How to upgrade weapons for Dummies' book! So just keep hunting until you have enough! Hehe ^_^");
						cm.dispose();
					}
					else if (selection == 2) {
						cm.sendOk("Imma kill you in your sleep! Have a nice day! ^_^");
						cm.dispose();
					}
				}
				else if (status == 3) {
					if (selection == 0) {
                        cm.sendSimple("PICK SOMETHING ALREADY!!! *glares* \r\n#L0#To make a #v1302030#, you will need #i1302020#, #i4001126# #rx600#k , and #b500k Mesoss#k.#l\r\n#L1#To make a #i1412011#, you will need #i1302020#, #i4001126# #rx600#k , and #b500k Mesoss#k.#l\r\n#L2#To make a #i1422014#, you will need #i1302020#, #i4001126# #rx600#k , and #b500k Mesoss#k.#l\r\n#L3#To make a #i1432012#, you will need #i1302020#, #i4001126# #rx600#k , and #b500k Mesoss#k.#l\r\n#L4#To make a #i1442024#, you will need #i1302020#, #i4001126# #rx600#k , and #b500k Mesoss#k.#l\r\n#L5#To make a #i1302064#, you will need #i1302030#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l\r\n#L6#To make a #i1312032#, you will need #i1412011#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l\r\n#L7#To make a #i1322054#, you will need #i1422014#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l\r\n#L8#To make a #i1402039#, you will need #i1302030#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l\r\n#L9#To make a #i1412027#, you will need #i1412011#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l\r\n#L10#To make a #i1422029#, you will need #i1422014#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l\r\n#L11#To make a #i1432040#, you will need #i1432012#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l\r\n#L12#To make a #i1442051#, you will need #i1442024#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l\r\n#L13#To make a #i1092046#, you will need #i1092030#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l");
						warrior = 1;
					}
					if (selection == 1) {
                        cm.sendSimple("PICK SOMETHING ALREADY!!! *glares* \r\n#L0#To make a #i1452022#, you will need #i1452016#, #i4001126# #rx600#k , and #b500k Mesoss#k.#l\r\n#L1#To make a #i1452045#, you will need #i1452022#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l\r\n#L2#To make a #i1462019#, you will need #i1462014#, #i4001126# #rx600#k , and #b500k Mesoss#k.#l\r\n#L3#To make a #i1462040#, you will need #i1462019#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l");
						bowman = 1;
					}
					if (selection == 2) {
                        cm.sendSimple("PICK SOMETHING ALREADY!!! *glares* \r\n#L0#To make a #i1472032#, you will need #i1472030#, #i4001126# #rx600#k , and #b500k Mesoss#k.#l\r\n#L1#To make a #i1472055#, you will need #i1472032#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l\r\n#L2#To make a #i1332025#, you will need #i1472030#, #i4001126# #rx600#k , and #b500k Mesoss#k.#l\r\n#L3#To make a #i1332055#, you will need #i1332025#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l\r\n#L4#To make a #i1332056#, you will need #i1332025#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l\r\n#L5#To make a #i1092047#, you will need #i1092030#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l");
						thief = 1;
					}
					if (selection == 3) {
                        cm.sendSimple("PICK SOMETHING ALREADY!!! *glares* \r\n#L0#To make a #i1382012#, you will need #i1382009#, #i4001126# #rx60#k , and #b500k Mesoss#k.#l\r\n#L1#To make a #i1372034#, you will need #i1382012#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l\r\n#L2#To make a #i1382039#, you will need #i1382012#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l\r\n#L3#To make a #i1092045#, you will need #i1092030#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l");
						mage = 1;
					}
					if (selection == 4) {
                        cm.sendSimple("PICK SOMETHING ALREADY!!! *glares* \r\n#L0#To make a #i1482021#, you will need #i1482020#, #i4001126# #rx600#k , and #b500k Mesoss#k.#l\r\n#L1#To make a #i1482022#, you will need #i1482021#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l\r\n#L2#To make a #i1492021#, you will need #i1492020#, #i4001126# #rx600#k , and #b500k Mesoss#k.#l\r\n#L3#To make a #i1492022#, you will need #i1492021#, #i4001126# #rx600#k , and #b5mil Mesoss#k.#l");
						pirate = 1;
					}
					if (selection == 5) {
                        cm.sendSimple("YOU SHOULD CONSIDER YOUR SELF LUCKY THAT I SNUCK THIS IN HERE! Pick some before I get found out and lose my job. >_> <_< >_> \r\n#L0#Since Im going to get fired for this, its free. Have a #i1002140# and a #i1042003#, cant forget the #i1062007# and last but not least! A #i1322013##l");
						prank = 1;
					}
				}
			else if (status == 4) {
                if (warrior == 1) {
					if (selection == 0) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1302020)) && (cm.getMeso() >= 500000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1302020,-1);
						cm.gainMeso(-500000);
						cm.gainItem(1302030,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 1) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1302020)) && (cm.getMeso() >= 500000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1302020,-1);
						cm.gainMeso(-500000);
						cm.gainItem(1412011,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 2) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1302020)) && (cm.getMeso() >= 500000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1302020,-1);
						cm.gainMeso(-500000);
						cm.gainItem(1422014,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 3) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1302020)) && (cm.getMeso() >= 500000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1302020,-1);
						cm.gainMeso(-500000);
						cm.gainItem(1432012,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 4) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1302020)) && (cm.getMeso() >= 500000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1302020,-1);
						cm.gainMeso(-500000);
						cm.gainItem(1442024,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 5) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1302030)) && (cm.getMeso() >= 5000000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1302030,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1302064,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 6) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1412011)) && (cm.getMeso() >= 5000000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1412011,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1312032,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 7) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1422014)) && (cm.getMeso() >= 5000000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1422014,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1322054,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 8) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1302030)) && (cm.getMeso() >= 5000000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1302030,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1402039,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 9) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1412011)) && (cm.getMeso() >= 5000000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1412011,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1412027,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 10) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1422014)) && (cm.getMeso() >= 5000000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1422014,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1422029,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 11) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1432012)) && (cm.getMeso() >= 5000000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1432012,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1432040,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 12) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1442024)) && (cm.getMeso() >= 5000000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1442024,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1442051,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 13) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1092030)) && (cm.getMeso() >= 2500000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1092030,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1092046,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}
					}
                }
                else if (bowman == 1) {
					if (selection == 0) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1452016)) && (cm.getMeso() >= 500000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1452016,-1);
						cm.gainMeso(-500000);
						cm.gainItem(1452022,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 1) {
						if ((cm.haveItem(leaf, 600)) && (cm.haveItem(1452022)) && (cm.getMeso() >= 5000000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1452022,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1452045,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 2) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1462014)) && (cm.getMeso() >= 500000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1462014,-1);
						cm.gainMeso(-500000);
						cm.gainItem(1462019,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 3) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1462019)) && (cm.getMeso() >= 5000000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1462019,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1462040,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}
					}
                }
                else if (thief == 1) {
					if (selection == 0) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1472030)) && (cm.getMeso() >= 500000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1472030,-1);
						cm.gainMeso(-500000);
						cm.gainItem(1472032,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 1) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1472032)) && (cm.getMeso() >= 5000000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1472032,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1472055,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 2) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1472030)) && (cm.getMeso() >= 500000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1472030,-1);
						cm.gainMeso(-500000);
						cm.gainItem(1332025,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 3) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1332025)) && (cm.getMeso() >= 5000000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1332025,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1332055,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 4) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1332025)) && (cm.getMeso() >= 5000000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1332025,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1332056,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 5) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1092030)) && (cm.getMeso() >= 2500000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1092030,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1092047,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}
					}
                }
				else if (mage == 1) {
					if (selection == 0) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1382009)) && (cm.getMeso() >= 500000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1382009,-1);
						cm.gainMeso(-500000);
						cm.gainItem(1382012,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 1) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1382012)) && (cm.getMeso() >= 5000000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1382012,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1372034,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 2) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1382012)) && (cm.getMeso() >= 5000000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1382012,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1382039,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 3) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1092030)) && (cm.getMeso() >= 2500000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1092030,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1092045,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}
					}
				}
				else if (pirate == 1) {
					if (selection == 0) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1482020)) && (cm.getMeso() >= 500000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1482020,-1);
						cm.gainMeso(-500000);
						cm.gainItem(1482021,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 1) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1482021)) && (cm.getMeso() >= 5000000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1482021,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1482022,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 2) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1492020)) && (cm.getMeso() >= 500000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1492020,-1);
						cm.gainMeso(-500000);
						cm.gainItem(1492021,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}}
					else if (selection == 3) {
						if ((cm.haveItem(leaf, 600)) & (cm.haveItem(1492021)) && (cm.getMeso() >= 5000000 )) {
						cm.gainItem(leaf, -600);
						cm.gainItem(1492021,-1);
						cm.gainMeso(-5000000);
						cm.gainItem(1492022,1);
						}
						else {
						cm.sendOk("You don't have the proper items Agent");
						cm.dispose();
						}
					}		
				}else if (prank == 1) {
						if (selection == 0) {
						cm.playSound("Cokeplay/Failed");
						cm.sendYesNo("Are you stupid or something? You think I'll actually give you GM items!? lulululululululululu");
						prank = 2;
						}
				else {
						cm.dispose();
				}
					}
			}else if (status == 5) {
				if (prank == 2) {

					cm.sendOk("*Takes out a chainsaw* YOU SURE YOU WANT TO TEST ME!?");
					cm.dispose();
				}
			}
		}
	}