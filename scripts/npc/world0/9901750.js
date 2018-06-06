let items = [
    1902011, 1902013, 1902014, 1902020, 1902021, 1902022, 1902023, 1902024, 1902025, 1902026, 1902027, 1902028, 1902031, 1902032, 1902033, 1902034, 1902035, 1902036, 1902037, 1902038, 1902036, 1902045, 1902059, 1902060, 1902061, 1912007, 1912009, 1912010, 1912013, 1912014, 1912015, 1912016, 1912017, 1912018, 1912019, 1912020, 1912021, 1912024, 1912025, 1902026, 1912027, 1912028, 1912029, 1912030, 1912031, 1912032, 1912038, 1912026, 1912052, 1912053, 1912054];
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
     if (status == 1) {
        cm.sendSimple("I sell mounts for #b5,000,00#k mesos each. Are you interested?\r\n#b#L0#I'll have a look#l");
    } else if (status == 2) {
            var selStr = "Let me know if you see something you'd like to buy!";
            for (var i = 0; i < items.length; i++){
                selStr += "\r\n#b#L" + i + "# #v" + items[i] + "# #t" + items[i] + "##l#k";
            }
            cm.sendSimple(selStr);
    } else if (status == 3) {
        if (cm.getMeso() < 5000000) {
            cm.sendOk("You do not have enough mesos.");
            cm.dispose();
        } else {
            cm.gainMeso(-5000000);
            cm.gainItem(items[selection], 1);
            cm.dispose();
        }
    }
}
