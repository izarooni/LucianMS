const Occupation = Java.type("com.lucianms.client.meta.Occupation");
const ServerConstants = Java.type("com.lucianms.constants.ServerConstants");
const ChangeCost = 500;
/* izarooni */
let status = 0;
let pocc = player.getOccupation();
let requestChange = false;
if (player.isDebug()) {
    pocc = null;
}

function action(mode, type, selection) {
    if (mode < 1) {
        if (mode == 0 && status == 3) {
            if (pocc == null) {
                status--;
            } else {
                status = 1;
            }
        } else {
            cm.dispose();
            return;
        }
    } else {
        status++;
    }
    if (status == 1) {
        if (pocc == null) {
            cm.sendNext("You haven't decided on your occupation?\r\nThere's a few you can pick from, let's take a look");
        } else {
            cm.sendSimple("You are currently a member of the #b" + pocc.getType().name() + "#k occupation\r\n#b"
                + "\r\n#L0#Tell me about my occupation#l"
                + "\r\n#L1#I want to change my occupation#l");
            status = 2;
        }
    } else if (status == 2) {
        let content = "Each occupation has it's own attributes that may help with your endeavors. Which one would you like to learn about?\r\n#b";
        let types = Occupation.Type.values();
        for (let i = 4; i < types.length; i++) {
            content += `\r\n#L${i}#${types[i].name()}#l`;
        }
        cm.sendSimple(content);
    } else if (status == 3) {
        if (pocc != null && selection == 1) {
            status = 1;
            pocc = null;
            requestChange = true;
            action(1, 0, 0);
        } else if (pocc == null || selection == 0) {
            this.career = (pocc == null) ? TypeFromValue(selection) : pocc.getType();
            if (this.career == Occupation.Type.Trainer) {
                cm.sendNextPrev(`The #b${this.career.name()}#k has a maximum of 10 levels. As a trainer, the experience gained via killing monsters and bosses is increased by 10% for each occupation level reached.`);
            } else if (this.career == Occupation.Type.Troll) {
                cm.sendNextPrev(`The #b${this.career.name()}#k has a maximum of 5 levels. Occupation experience is gained by hanging around in the server home map. Trolls have access to special commands for debuffing players (ie. stun, reverse, seduce and bomb) and a command that allows warping to other players. One command is unlocked at each level`);
            } else if (this.career == Occupation.Type.Farmer) {
                cm.sendNextPrev(`The #b${this.career.name()}#k has a maximum of 3 levels. Occupation experience is gained by converting mesos to #b#t${ServerConstants.CURRENCY}##k. Farmers have a meso rate that increases for each occupation level and a command that allows automatic conversion from mesos to the server currency`);
            } else if (this.career == Occupation.Type.Looter) {
                cm.sendNextPrev(`The #b${this.career.name()}#k has a maximum of 5 levels. When a Looter has a pet with them, each time you (the player) level, items around the pet will \"vacuumed\" meaning that items and mesos will be looted for you. Each occupation will increase the range that the vacuum can reach for items. Each pet-vac proc will give you occupation experience`);
            } else {
                cm.sendOk("What?");
            }
            if (!requestChange) cm.dispose();
        }
    } else if (status == 4) {
        let content = "Are you sure you want to become a #b" + this.career.name() + "#k?";
        if (requestChange) {
            content += `\r\nIt will cost #b${ChangeCost} rebirth points#k to change your occupation. Are you sure you want to do this?`
        }
        cm.sendNext(content);
    } else if (status == 5) {
        if (requestChange) {
            if (player.getOccupation() != null && player.getOccupation().getType() == this.career) {
                cm.sendOk("You cannot change to an occupation that you already are.\r\nWhat do you think you're doing?!");
                return cm.dispose();
            } else if (player.getRebirthPoints() >= ChangeCost) {
                player.setRebirthPoints(player.getRebirthPoints() - ChangeCost);
                player.sendMessage(`You now have ${player.getRebirthPoints()} rebirth points`);
            } else {
                cm.sendOk("You do not have enough #brebirth points#k for this");
                return cm.dispose();
            }
        }
        player.setOccupation(new Occupation(this.career));
        cm.sendOk("You are now a #b" + this.career + "#k!");
        cm.dispose();
    }
}

function TypeFromValue(n) {
    try {
        return Occupation.Type.values()[n];
    } catch (ignore) {
        return undefined;
    }
}
