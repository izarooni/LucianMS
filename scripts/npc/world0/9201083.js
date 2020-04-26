
const MapleStat = Java.type('com.lucianms.client.MapleStat');
var status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }

    if (status == 1) {
        cm.sendSimple("I am the Chirithy item vendor. What would you like to receive? [#e#rBETA FEATURE ONLY#n#k]\r\n\r\n#d#L0#Give me 100 power elixir\r\n#L1#Give me 10000 NX\r\n#L2#Give me 10 VP\r\n#L3#Set my SP to 50");
    }
    if (status == 2) {
        if (selection == 0) {
            cm.gainItem(2000005, 100);
            cm.sendOk("I have given you 100 power elixir.");
        }
        if (selection == 1) {
            player.addPoints("nx", 10000);
            cm.sendOk("I have given you 10000 NX.");
        }
        if (selection == 2) {
            player.addPoints("vp", 10);
            cm.sendOk("I have given you 10 vote points.");
        }
        if (selection == 3) {
            player.setRemainingSp(50);
            player.updateSingleStat(MapleStat.AVAILABLESP, 50);
            cm.sendOk("I have set your SP to 50.");
        }
    }
}
