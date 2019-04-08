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
            cm.sendNext("You are currently a member of the #b" + NameFromValue(pocc.getType().ordinal()) + "#k occupation");
            status = 2;
        }
    } else if (status == 2){
        cm.sendSimple("Each occupation has it's own attributes that may help with your endeavors. Which one would you like to learn about?\r\n#b"
            + "\r\n#L0#Trainer#l"
            + "\r\n#L1#Troll#l"
            + "\r\n#L2#Farmer#l"
            + "\r\n#L3#Looter#l");
    } else if (status == 3) {
        this.career = (selection > -1) ? selection : pocc.getType().ordinal();
        if (this.career == 0) {
            cm.sendNextPrev("The #bTrainer#k has a maximum of 10 levels. As a trainer, the experience gained is increased by 10% for each occupation level reached.");
        } else if (this.career == 1) {
            cm.sendNextPrev("The #bTroll#k has a maximum of 5 levels. Trolls have access to special commands for debuffing players (ie. stun, reverse, seduce and bomb) and a command that allows warping to other players. One command is unlocked at each level");
        } else if (this.career == 2) {
            cm.sendNextPrev("The #bFarmer#k has a maximum of 3 levels. Farmers have a meso rate that increases for each occupation level and a command that allows automatic conversion from mesos to the server currency");
        } else if (this.career == 3) {
            cm.sendNextPrev("The #bLooter#k has a maximum of 5 levels. When a Looter has a pet with them, each time you (the player) level, items around the pet will \"vacuumed\" meaning that items and mesos will be looted for you. Each occupation will increase the range that the vacuum can reach for items.");
        } else {
            cm.sendOk("What?");
            cm.dispose();
        }
    } else if (status == 4) {
        if (pocc != null) {
            cm.dispose();
            return;
        }
        cm.sendNext("Are you sure you have decided to become a #b" + NameFromValue(this.career) + "#k?");
    } else if (status == 5) {
        player.setOccupation(new Occupation(TypeFromValue(this.career)));
        cm.sendOk("You are now a #b" + NameFromValue(this.career) + "#k!");
        client.getWorldServer().broadcastMessage(5, "{} has decided to become a member of the {} occupation!", player.getName(), NameFromValue(this.career));
        cm.dispose();
    }
}

function TypeFromValue(n) {
    switch (n) {
        default: return null;
        case 0: return Occupation.Type.Pharaoh;
        case 1: return Occupation.Type.Undead;
        case 2: return Occupation.Type.Demon;
        case 3: return Occupation.Type.Human;
    }
}

function NameFromValue(n) {
    switch (n) {
        default: return null;
        case 0: return "Pharaoh";
        case 1: return "Undead";
        case 2: return "Demon";
        case 3: return "Human";
    }
}
