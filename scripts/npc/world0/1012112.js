var status = 0;

var ID_RiceCake = 4001101;
var ID_RiceHat = 1002798;

var Min_Members = 3;
var Min_Level = 10;

var M_Park = 100000200;
var M_Stage = 910010000;
var M_ExitSuccess = 910010100;
var M_Fail = 910010400;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (player.getMapId() == M_Park) Entrance(selection);
    else if (player.getMapId() == M_ExitSuccess || player.getMapId() == M_Fail) EndStage(selection);
}

function Entrance(selection) {
    if (cm.getParty() == null || !cm.isLeader()) {
        if (status == 1) {
            cm.sendNext("Hi there! I'm Tory. This place is covered with mysterious aura of the full moon, and no one person can enter here by him/herself.");
        } else if (status == 2) {
            cm.sendOk("If you'd like to enter here, the leader of your party will have to talk to me. Talk to your party leader about this.");
            cm.dispose();
        }
    } else {
        if (status == 1) {
            cm.sendNext("I'm Tory. Inside here is a beautiful hill where the primrose blooms. There's a tiger that lives in the hill, Growlie, and he seems to be looking for something to eat.");
        } else if (status == 2) {
            cm.sendSimple("Would you like to head over to the hill of primrose and join forces with your party members to help Growlie out?\r\n#b#L0# Yes, I will go.#l");
        } else if (status == 3) {
            var members = cm.getPartyMembers();
            var available = 0;
            members.forEach(function(chr) {
                if (chr.getMapId() == player.getMapId()) available++;
                if (chr.getLevel() < Min_Level) {
                    cm.sendOk("A member of your party does not meet the level requirement.");
                    cm.dispose();
                    return;
                }
            });
            if (available < cm.getParty().getMembers().size()) {
                cm.sendOk("A member of your party is not present in the map.");
            } else if (!player.isGM() && available < Min_Members) {
                cm.sendOk("You will need at least #b" + Min_Members + " party members#k to enter.");
            } else {
                var em = cm.getEventManager("HenesysPQ");
                if (em != null) {
                    var open = em.getProperty("HPQOpen");
                    if (open == "true") {
                        var toRemove = [4001095, 4001096, 4001097, 4001098, 4001099, 4001100, 4001101];
                        for (var i = 0; i < toRemove; i++) cm.removePartyItems(toRemove[i]);
                        em.setProperty("HPQOpen", "false");
                        em.setProperty("LeaderName", player.getName());
                        em.startInstance(cm.getParty(), player.getMap());
                    } else {
                        cm.sendOk("Someone is already attempting the PQ. Please wait for them to finish, or find another channel.");
                    }
                } else {
                    cm.sendOk("This party quest is currently unavailable.");
                }
            }
            cm.dispose();
        }
    }
}

function EndStage(selection) {
    if (status == 1) {
        var text = "I appreciate you giving some rice cakes for the hungry Growlie. It looks like you have nothing else to do now. Would you like to leave this place?#b";
        if (player.getMapId() == M_ExitSuccess) {
            text += "\r\n#L0#I want to give you the rest of my rice cakes.#l"
        }
        text += "\r\n#L1#Yes, please get me out of here.#l";
        cm.sendSimple(text);
    } else if (status == 2) {
        if (selection == 0) {
            if (player.getRiceCakes() >= 20) {
                if (cm.hasItem(ID_RiceHat)) {
                    cm.sendNext("Do you like the hat I gave you? I ate so much of your rice cake that I will have to say no to your offer of rice cake for a little while.");
                    cm.dispose();
                } else if (cm.canHold(ID_RiceHat)) {
                    cm.sendYesNo("I appreciate the thought, but I am okay now. I still have some of the rice cakes you gave me stored at home. To show you my appreciation, I prepared a small gift for you. Would you like to accept it?");
                } else {
                    player.ropMessage(1, "EQUIP inventory full.");
                    cm.dispose();
                }
            } else {
                this.pquantity = player.getItemQuantity(ID_RiceCake, false);
                if (this.pquantity == 0) {
                    cm.sendOk("You do not have an rice cakes to give me");
                    cm.dispose();
                } else {
                    cm.sendGetNumber("How many rice cakes are you going to give me?", this.pquantity, 1, this.pquantity);
                }
            }
        } else if (selection == 1) {
            cm.removeHPQItems();
            cm.warp(M_Park);
            cm.dispose();
        }
    } else if (status == 3) {
        if (selection > 0 && selection <= this.pquantity) {
            player.setRiceCakes(player.getRiceCakes() + selection);
            cm.sendOk("Thank you for the " + selection + " rice cakes!! I really appreciate it!\r\nI now have " + player.getRiceCakes() + " rice cakes");
        }
        cm.dispose();
    }
}
