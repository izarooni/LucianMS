/* izarooni */
// npc: 9071000
// map: 951000000
var MonsterPark = Java.type("server.events.custom.MonsterPark");
var status = 0;
var minParticpants = 2;
var base = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();4
        return;
    } else {
        status++;
    }
    this.optional = player.getGenericEvents().stream().filter(function(e){
        return (e instanceof MonsterPark);
    }).findFirst();
    if (status == 1) {
        // skip
    } else if (status == 2) {
        var text = "Are you ready to enter the #b";
        switch (selection) {
            case 3: // zebra
                text += "zebra";
                base = 953050000;
                break;
            case 4: // leopard
                text += "leopard";
                break;
            case 2: // tiger
                text += "tiger";
                break;
            case 5: // extreme
                text += "extreme";
                break;
        }
        text += "#k monster park?"
        cm.sendNext(text);
    } else if (status == 3) {
        if (base == 0) {
            cm.sendOk("Sorry, this monster park is currently not availble.");
            cm.dispose();
        } else {
            var park = new MonsterPark(client.getWorld(), client.getChannel(), base);
            park.registerPlayer(player);
            cm.dispose();
        }
    }
}
