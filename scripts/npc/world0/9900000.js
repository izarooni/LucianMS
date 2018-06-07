
var status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendOk("#dYou view me anytime using the < @jobs > command!\r\n\r\n                                             #bJob List#k\r\n\r\n#eHero#k - Uchiha\r\n#ePaladin#k - Rashoumon\r\n#eDark Knight#k - Dragoon\r\n#eDawn Warrior#k - Android\r\n\r\n#eArchmage (Fire/Poison)#k - Phantom\r\n#eArchmage (Ice/Lightning)#k - Luminous\r\n#eBishop#k - Evan\r\n\r\n#eBowmaster#k - Mercedes\r\n#eMarksman#k - Marksman\r\n\r\n#eNight Lord#k - Shade\r\n#eShadower#k - Dual Blader\r\n\r\n#eBuccaneer#k - Pink Bean\r\n#eCorsair#k - Mechanic\r\n\r\n#eAran#k - Aran");
        cm.dispose();
    }
}
