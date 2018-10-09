const PlayerCreative = Java.type("com.lucianms.features.PlayerCreative");
/* izarooni */
let status = 0;
let creative = player.getGenericEvents().stream().filter(p => (p instanceof PlayerCreative)).findFirst().orElse(null);
if (creative != null) {
    player.getToggles().put("cmd_npc_access", true); // allow access to this npc only
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }

    player.getToggles().put("cmd_npc_access", false); // once script is executed, disable npc conversing

    if (creative != null) {
        if (status == 1) {
            cm.sendNext("Let me know when you're ready to leave.");
        } else {
            player.sendMessage(5, "You will be removed from the Sandbox momentarily.")
            creative.unregisterPlayer(player);
            cm.dispose();
        }
        return;
    }
    if (status == 1) {
        cm.sendNext("Did you see an equip that you think would look awesome on you but can't justify buying it until you see it on yourself?\r\nWelcome to #bSandbox#k! You may create and wear any equip you'd like if you have the item ID.");
    } else if (status == 2) {
        cm.sendSimple("Just let me know when you're ready and I can take you to the Sandbox.\r\n#b#L0#I'm ready to go!#l\r\n#L1#Give me a minute#l\r\n#L2#What can I do when I'm there?#l");
    } else if (status == 3) {
        if (selection == 0) {
            if (player.getTrade() != null) {
                cm.sendOk("Close your trade window before attempting to enter Sandbox.")
                cm.dispose();
            } else {
                creative = new PlayerCreative();
                creative.registerPlayer(player);
            }
        } else if (selection == 2) {
            cm.sendNext("Create items of course! This can be done via the #d@want <item_ID>#k command. For example, \"#b@want 1302000#k\" will create a #b#z1302000##k for you to pick up.\r\nEventually, you may have too many items on the ground, you can use #d@cleardrops#k should that ever happen.");
            status = 1;
            return;
        }
        cm.dispose();
    }
}
