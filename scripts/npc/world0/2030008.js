/* Adobis
 * 
 * El Nath: The Door to Zakum (211042300)
 * 
 * Zakum Quest NPC 
 
 * Custom Quest 100200 = whether you can do Zakum
 * Custom Quest 100201 = Collecting Gold Teeth <- indicates it's been started
 * Custom Quest 100203 = Collecting Gold Teeth <- indicates it's finished
 * 
 * 4031061 = Piece of Fire Ore - stage 1 reward
 * 4031062 = Breath of Fire    - stage 2 reward
 * 4001017 = Eye of Fire       - stage 3 reward
 * 4000082 = Zombie's Gold Tooth (stage 3 req)
*/

const MinimumLevel = 50;
const Maps = { Entrance: 211042300, BreathOfLava1: 280020000}
const Items = { Ore: 4031061, Breath: 4031062, Tooth: 4000082, Eye: 4001017 };

let status = 0;
let stage;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        if (player.getLevel() >= MinimumLevel) {
            if (cm.isQuestCompleted(100200) && !cm.isQuestStarted(100200)) {
                cm.startQuest(100200);
                cm.sendOk("You want to be permitted to do the Zakum Dungeon Quest?  Well, I, #bAdobis#k... judge you to be suitable.  You should be safe roaming around the dungeon.  Just be careful...");
                cm.dispose();
            } else if (cm.isQuestStarted(100201)) {
                cm.sendNext("Have you got the items I asked for?  This ain't no charity.");
            } else {
                cm.sendSimple("Beware, for the power of olde has not been forgotten... #b"
                + "\r\n#L0#Enter the Unknown Dead Mine (Stage 1)#l" // ZakumPQ
                + "\r\n#L1#Face the Breath of Lava (Stage 2)#l" // Jump Quest
                + "\r\n#L2#Forging the Eyes of Fire (Stage 3)#l" // Golden Tooth
                );
            }
        } else {
            cm.sendOk("Please come back to me when you've become stronger.  I've seen a few adventurers in my day, and you're far too weak to complete my tasks.");
            cm.dispose();
        }
    } else if (status == 2) {
        if (cm.isQuestStarted(100201)) {
            if (cm.haveItem(Items.Ore, 1) && cm.haveItem(Items.Breath, 1) && cm.haveItem(Items.Tooth, 30)) {
                cm.gainItem(Items.Ore, -1);
                cm.gainItem(Items.Breath, -1);
                cm.gainItem(Items.Tooth, -30);
                cm.gainItem(Items.Eye, 5);
                cm.sendNext("Thank you for the teeth!  Next time you see me, I'll be blinging harder than #rJaws#k! Goodbye and good luck!");
                cm.completeQuest(100201);
                cm.completeQuest(100200);
                cm.dispose();
            } else {
                cm.sendNext("You shtill didn't get me my teef! Howsh a man shupposhed to conshentrate wifout teef?");
                cm.dispose();
            }
        } else if (selection == 0) {
            if (cm.getParty() == null) {
                cm.sendNext("Please talk to me again when you have formed a party.");
                cm.dispose();
            } else if (!cm.isLeader()) {
                cm.sendNext("Please have the leader of your party speak with me.");
                cm.dispose();
            } else {
                let party = cm.getPartyMembers();
                mapId = cm.getPlayer().getMapId();
                let allowEntrance = party.stream().noneMatch(p => p.getLevel() < MinimumLevel);
                if (allowEntrance) {
                    var em = cm.getEventManager("ZakumPQ");
                    if (em == null) {
                        cm.sendOk("This trial is currently under construction.");
                    } else {
                        em.startInstance(cm.getParty(), cm.getPlayer().getMap());
                        cm.removeFromParty(4001015);
                        cm.removeFromParty(4001018);
                        cm.removeFromParty(4001016);
                    }
                } else {
                    cm.sendNext("Please make sure all of your members are qualified to begin my trials...");
                }
                cm.dispose();
            }
        } else if (selection == 1) {
            stage = 1;
            if (cm.haveItem(Items.Ore) && !cm.haveItem(Items.Breath)) {
                cm.sendYesNo("Would you like to attempt the #bBreath of Lava#k?  If you fail, there is a very real chance you will die.");
            } else {
                if (cm.haveItem(Items.Breath)) {
                    cm.sendNext("You've already got the #bBreath of Lava#k, you don't need to do this stage.");
                } else {
                    cm.sendNext("Please complete the earlier trials first.");
                }
            }
        } else if (selection == 2) {
            stage = 2;
            if (cm.isQuestCompleted(100201) && cm.haveItem(Items.Ore) && cm.haveItem(Items.Breath)) {
                cm.sendYesNo("If you want more #bEyes of Fire#k, you need to bring me the same #b30 Zombie's Lost Gold Tooth#k.  Turns out gold dentures don't last long, and I need a new one.\r\nDo you have those teeth for me?");
            } else if (cm.haveItem(Items.Ore) && cm.haveItem(Items.Breath)) {
                cm.sendYesNo("Okay, you've completed the earlier trials.  Now, with a little hard work I can get you the #bseeds of Zakum#k necessary to enter combat.  But first, my teeths are not as good as they used to be.  You ever seen a dentist in Maple Story?  Well, I heard the Miner Zombies have gold teeth.  I'd like you to collect #b30 Zombie's Lost Gold Tooth#k so I can build myself some dentures.  Then I'll be able to get you the items you desire.\r\nRequired:\r\n#i4000082##b x 30");
            } else {
                cm.sendNext("Please complete the earlier trials before attempting this one.");
            }
        }
    } else if (status == 2) {
        if (stage == 1) {
            cm.warp(Maps.BreathOfLava1);
            cm.dispose();
        } else if (stage == 2) {
            if (!cm.isQuestStarted(100201)) {
                if (cm.haveItem(Items.Ore, 1) && cm.haveItem(Items.Breath, 1) && cm.haveItem(Items.Tooth, 30)) {
                    cm.gainItem(Items.Ore, -1);
                    cm.gainItem(Items.Breath, -1);
                    cm.gainItem(Items.Tooth, -30);
                    cm.gainItem(Items.Eye, 5);
                    cm.sendNext("Thank you for the teeth!  Next time you see me, I'll be blinging harder than #rJaws#k!  Goodbye and good luck!");
                    cm.completeQuest(100201);
                    cm.completeQuest(100200);
                    cm.dispose();
                }
                else {
                    cm.sendNext("You don't have any teeth yet!  Don't try to pull a fast one on me.");
                    cm.dispose();
                }
            } else {
                cm.startQuest(100201);
                cm.dispose();
            }
        }
    }
}
