const Occupation = Java.type("com.lucianms.client.meta.Occupation");
/* izarooni */
let status = 0;
let pocc = player.getOccupation();

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
            cm.sendNext("You haven't decided on your occupation?\r\nThere's a few you can pick from, let me tell you about them");
        } else {
            cm.sendNext("You are currently a member of the #b" + NameFromValue(pocc.getType().ordinal()) + "#k occupation");
            status = 2;
        }
    } else if (status == 2){
        cm.sendSimple("There's #bPharoah, Undead, Demon and Human#k and each one has it's own attributes that may help with your endeavors. Which one would you like to learn about?\r\n#b"
            + "\r\n#L0#Pharoah#l"
            + "\r\n#L1#Undead#l"
            + "\r\n#L2#Demon#l"
            + "\r\n#L3#Human#l");
    } else if (status == 3) {
        this.career = (selection > -1) ? selection : pocc.getType().ordinal();
        if (this.career == 0) {
            cm.sendNextPrev("The #bPharaoh#k has a higher EXP rate (1+), but a lower meso & drop rate (-1)\r\n\r\n\t#bGood for people with no life");
        } else if (this.career == 1) {
            cm.sendNextPrev("The #bUndead#k gains HP each time a monster is attacked and does not lose EXP upon death\r\n\r\n\t#bGood for bossing & leeching");
        } else if (this.career == 2) {
            cm.sendNextPrev("The #bDemon#k is immune to all diseases from monsters\r\n\r\n\t#bGood for bossing");
        } else if (this.career == 3) {
            cm.sendNextPrev("The #bHuman#k has a higher loot rate and meso rate (1+)\r\n\r\n\t#bGood for farming")
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
