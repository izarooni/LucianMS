const Occupation = Java.type("com.lucianms.client.meta.Occupation");
/* izarooni */
let status = 0;
let pocc = player.getOccupation();
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
            cm.sendNext("You are currently a member of the #b" + pocc.getType().name() + "#k occupation");
            status = 2;
        }
    } else if (status == 2){
        let content = "Each occupation has it's own attributes that may help with your endeavors. Which one would you like to learn about?\r\n#b";
        let types = Occupation.Type.values();
        for (let i = 4; i < types.length; i++) {
            content += `\r\n#L${i}#${types[i].name()}#l`;
        }
        cm.sendSimple(content);
    } else if (status == 3) {
        this.career = (selection > -1) ? TypeFromValue(selection) : pocc.getType();
        if (this.career == Occupation.Type.Trainer) {
            cm.sendNextPrev(`The #b${this.career.name()}#k has a maximum of 10 levels. As a trainer, the experience gained is increased by 10% for each occupation level reached.`);
        } else if (this.career == Occupation.Type.Troll) {
            cm.sendNextPrev(`The #b${this.career.name()}#k has a maximum of 5 levels. Trolls have access to special commands for debuffing players (ie. stun, reverse, seduce and bomb) and a command that allows warping to other players. One command is unlocked at each level`);
        } else if (this.career == Occupation.Type.Farmer) {
            cm.sendNextPrev(`The #b${this.career.name()}#k has a maximum of 3 levels. Farmers have a meso rate that increases for each occupation level and a command that allows automatic conversion from mesos to the server currency`);
        } else if (this.career == Occupation.Type.Looter) {
            cm.sendNextPrev(`The #b${this.career.name()}#k has a maximum of 5 levels. When a Looter has a pet with them, each time you (the player) level, items around the pet will \"vacuumed\" meaning that items and mesos will be looted for you. Each occupation will increase the range that the vacuum can reach for items.`);
        } else {
            cm.sendOk("What?");
            cm.dispose();
        }
    } else if (status == 4) {
        if (pocc != null) {
            cm.dispose();
            return;
        }
        cm.sendNext("Are you sure you have decided to become a #b" + this.career.name() + "#k?");
    } else if (status == 5) {
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
