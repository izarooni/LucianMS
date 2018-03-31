/* izarooni */
// npc: 9071000
// map: 951000000
var MonsterPark = Java.type("server.events.custom.MonsterPark");
var status = 0;
var minParticpants = 2;

var areas = {
    zebra: [["Auto Security Area", 953020000, 100], ["Clandestine Ruins", 953090000, 110], ["Dangerously Isolated Forest", 953080000, 120]],
    leopard: [["Dead Tree Forest", 954010000, 130], ["Dragon Nest", 954030000, 140], ["Forbidden Time", 953050000, 150]],
    tiger: [["Mossy Tree Forest", 953030000,160], ["Secret Pirate Hideout", 953060000, 170], ["Sky Forest Training Center", 953040000, 180]],
    extreme: [["Temple of Oblivion", 954040000, 190]]
};

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
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
        if (!player.isGM()) {
            if (cm.getParty() == null || cm.getPartyMembers().size() <= minParticpants) {
                cm.sendOk("You must be in a party with at least #b" + minParticpants + " members#k to enter ths monster park");
                cm.dispose();
                return;
            } else if (!cm.isLeader()) {
                cm.sendOk("Only your party leader may decide when to enter");
                cm.dispose();
                return;
            }
        }
        var area = getAreaFromValue(selection);
        var text = "\r\n#b"

        var range = getAreaLevelRange(area);
        if (player.getLevel() > range.max || player.getLevel() < range.min) {
            cm.sendOk("You are not within the recommended level range of this Monster Park.\r\n\t#b- Level Range: " + range.min + " ~ " + range.max);
            cm.dispose();
            return;
        } else {
            for (var i = 0; i < area.length; i++) {
                text += "\r\n#L" + area[i][1] + "#" + area[i][0] + " (Lv." + area[i][2] + ")#l";
            }
        }

        cm.sendSimple(text);
    } else if (status == 3) {
        var level = getAreaLevelFromMapId(selection);
        if (player.getLevel() < level) {
            cm.sendOk("You must be at least #blevel " + level + "#k to enter this area");
        } else {
            var park = new MonsterPark(client.getWorld(), client.getChannel(), selection);
            if (cm.getParty() != null) {
                cm.getPartyMembers().forEach(function(member) {
                    if (member.getMapId() == player.getMapId()) {
                        park.registerPlayer(member);
                    }
                });
            } else if (player.isGM()) {
                park.registerPlayer(player);
            }
        }
        cm.dispose();
    }
}

function getAreaLevelFromMapId(m){
    for (var a in areas) {
        var area =  areas[a];
        for (var i = 0; i < area.length; i++) {
            if (area[i][1] == m) {
                return areas[a][i][2];
            }
        }
    }
    return null;
}

function getAreaFromValue(n) {
    print(n)
    switch (n) {
        case 2: return areas.tiger;
        case 3: return areas.zebra;
        case 4: return areas.leopard;
        case 5 : return areas.extreme;
    }
}

function getAreaLevelRange(area) {
    var range = {};
    range.min = area[0][2];
    range.max = area[area.length - 1][2];

     // extreme monster park special case
    if (range.min == range.max && range.min == 190) range.max = 200;

    return range;
}