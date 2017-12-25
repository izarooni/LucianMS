/* izarooni */
var status = 0;
var P_MinLevel = 35;
var P_MinMemberCount = 1;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendSimple("#e<Party Quest: Dimensional Schism>#n\r\n\r\nYou can't go any higher because of the extremely dangerous creatures above. Would you like to collaborate with party members to complete the quest? If so, please have your #bparty leader#k talk to me.\r\n#b"
            + "\r\n#L0#I want to participate in the party quest."
            + "\r\n#L2#I want to receive the Broken Glasses."
            + "\r\n#L3#I would like to hear more details.");
    } else if (status == 2) {
        if (selection == 0) {
			if (cm.getParty() == null) {
				cm.sendOk("You can participate in the party quest only if you are in a party.");
            } else if (!cm.isLeader()) {
                cm.sendOk("Only the party leader may decide when to enter.");
			} else if (!player.isGM() && cm.getParty().getMembers().size() < P_MinMemberCount ) {
				cm.sendOk("You cannot participate in the quest, because you do not have at least 3 party members.");
			} else {
                if (cm.getPartyMembers() < P_MinMemberCount) {
                    cm.sendYesNo("You cannot participate in the quest, because you do not have at least 3 party members. If you're having trouble finding party members, try Party Search.");
                } else {
                    if (!player.isGM()) {
                        cm.getPartyMembers().forEach(function(chr) {
                            if (chr.getLevel() < P_MinLevel) {
                                cm.sendOk("One or more members of your party are below the minimum level requirement needed to participate.");
                                cm.dispose();
                                return;
                            } else if (chr.getMapId() == player.getMapId()) {
                                cm.sendOk("Make sure all party members are currently in the same map as you before entering.");
                                cm.dispose();
                                return;
                            }
                        });
                    }
    				var em = cm.getEventManager("LudiPQ");
    				if (em == null) {
    					cm.sendOk("The Ludibrium PQ has encountered an error. Please report this on the forums, and with a screenshot.");
    				} else {
    					var prop = em.getProperty("LPQOpen");
    					if (prop.equals("true")) {
    						em.startInstance(cm.getParty(), player.getMap());
    						cm.removeAll(4001022);
    						cm.removeAll(4001023);
    					} else {
    						cm.sendOk("Another party has already entered the #rParty Quest#k in this channel. Please try another channel, or wait for the current party to finish.");
    					}
    				}
                }
            }
		} else if (selection == 1) {
			cm.sendOk("Try using a Super Megaphone or asking your buddies or guild to join!");
		} else if (selection == 2) {
			cm.sendNext("I am offering 1 #i1022073:# #bBroken Glasses#k for every 20 times you help me. If you help me #b" + brokenGlassesCount + " more times, you can receive Broken Glasses.#k");
		} else {
			cm.sendOk("#e<Party Quest: Dimensional Schism>#n\r\nA Dimensional Schism has appeared in #b#m220000000#!#k We desperately need brave adventurers who can defeat the intruding monsters. Please, party with some dependable allies to save #m220000000#! You must pass through various stages by defeating monsters and solving quizzes, and ultimately defeat #r#o9300012##k.\r\n - #eLevel#n: 30 or above #r(Recommended Level: 60 ~ 69)#k\r\n - #eTime Limit#n: 20 min\r\n - #eNumber of Players#n: 3 to 6\r\n - #eReward#n: #i1022073:# Broken Glasses #b(obtained every 20 time(s) you participate)#k\r\n                      Various Use, Etc, and Equip items");
		}
        cm.dispose();
    }
}
