const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
const MinimumLevel = 80;
const EventName = "Horntail-" + ch.getId();
const em = cm.getEventManager("HorntailFight");
/* izarooini */
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }

    let eim = em.getInstance(EventName);

    if (player.getLevel() < MinimumLevel) {
        cm.sendOk("You do not meet the criteria to take on Horntail");
        cm.dispose();
    } else if (em == null) {
        cm.sendOk("Horntail is having an existential crisis right now");
        cm.dispose();
    } else if (status == 1) {
        if (eim == null) {
            cm.sendSimple("Would you like to assemble a team to take on the mighty #rHorntail#k?#b"
            + "\r\n#L1#Lets get this going!#l"
            + "\r\n#L2#No, I think I'll wait a bit...#l");
        } else if (eim.getProperty("leader") == player.getName()) {
            if (eim.getProperty("registering") == "false") {
                cm.sendOk("Sorry, this expedition is already underway.");
                cm.dispose();
            } else {
                cm.sendSimple("What would you like to do?#b\r\n"
                + "\r\n#L1#View current Expedition members#l"
                + "\r\n#L2#Start the fight!#l"
                + "\r\n#L3#Stop the expedition.#l");
            }
        } else if (!eim.containsPlayer(player.getId())) {
            if (eim.getProperty("registering") == "false") {
                cm.sendOk("Sorry, this expedition is already underway. Registration is closed!");
                cm.dispose();
            } else if (eim.getProperty(`ban-${player.getId()}`) == player.getName()) {
                cm.sendOk(`Sorry, you've been banned from this expedition by #b${eim.getProperty("leader")}`);
                cm.dispose();
            } else {
                eim.addPlayer(player);
                eim.broadcastMessage(6, `${player.getName()} has joined the expedition!`);
                cm.sendOk("You have registered for the expedition successfully!");
                cm.dispose();
            }
        } else {
            cm.sendOk(`You must wait for your expedition leader #b${eim.getProperty("leader")}#k to begin`);
            cm.dispose();
        }
    } else if (status == 2) {
        if (eim != null) {
            if (selection == 1) { // view expedition members
                let content = "The following members make up your expedition (Click on them to expel them):#b\r\n";
                let oLength = content.length;
                eim.getPlayers().stream().filter(p => p.getId() != player.getId()).forEach(function (p) {
                    content += `\r\n#L${p.getId()}#${p.getName()}#l`;
                });
                if (oLength == content.length) { // to prevent sendSimple crash
                    cm.sendOk("There are no players to ban");
                    cm.dispose();
                } else cm.sendSimple(content);
            } else if (selection == 2) { // start the expedition
                player.getMap().broadcastMessage(MaplePacketCreator.removeClock());
                eim.getPlayers().forEach(p => eim.registerPlayer(p));
                eim.setProperty("registering", "false");
                cm.dispose();
            } else if (selection == 3) { // stop the expedition
                player.getMap().broadcastMessage(MaplePacketCreator.removeClock());
                cm.sendOk("The expedition has now ended. Sometimes the best strategy is to run away.");
                eim.dispose();
                cm.dispose();
            }
        } else {
            if (selection == 1) { // create expedition
                let eim = em.newInstance(EventName);
                em.startInstance(eim, player.getName());
                eim.addPlayer(player);
                player.getMap().broadcastMessage(MaplePacketCreator.serverNotice(6, `${player.getName()} has been declared the expedition captain. Please register for the expedition.`));
                eim.invokeFunction("closeRegistration", eim, player.getMap(), 1000 * 60 * 10);
                status = 0;
                action(1, 0, 0);
            } else cm.dispose();
        }
    } else if (status == 3) {
        let ban = eim.getPlayers().stream().filter(p => p.getId() == selection).findFirst().orElse(null);
        if (ban != null) {
            eim.setProperty(`ban-${ban.getId()}`, ban.getName());
            eim.unregisterPlayer(ban);
            cm.sendOk(`You have banned #b${ban.getName()}#k from the expedition`);
            status = 0;
        } else {
            cm.sendOk("Who?");
        }
    }
}