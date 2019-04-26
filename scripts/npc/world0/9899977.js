/* izarooni */
const MStats = Java.type('com.lucianms.server.life.MapleMonsterStats');
const Currency = 4000313;
const SummonAmount = 10;
const Monsters = [
    new MonsterEntry(8500001, {hp:20000000}),
    new MonsterEntry(8510000, {hp:24000000}),
    new MonsterEntry(9400014, {hp:30000000}),
    new MonsterEntry(9400121, {hp:70000000}),
    new MonsterEntry(9400112, {hp:35000000}),
    new MonsterEntry(9400113, {hp:45000000}),
    new MonsterEntry(9400300, {hp:123000000}),
];

let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        return cm.dispose();
    } else {
        status++;
    }
    if (status == 1) {
        let content = `#h #, I see that you are just a dear farmer trying to get by. Well... I have a big fortune that I want to expand and I need #b3 #z${Currency}##k. If you have some, I will use my magic and summon bosses before you!\r\n#b`;
        for (let i = 0; i < Monsters.length; i++) {
            let entry = Monsters[i];
            content += `\r\n#L${i}##o${entry.ID}##l`;
        }
        cm.sendSimple(content);
    } else if (status == 2) {
        let entry = Monsters[selection];
        if (entry != undefined) {
            if (cm.haveItem(Currency, 3)) {
                cm.gainItem(Currency, -3, true);
                for (let i = 0; i < SummonAmount; i++) {
                    let monster = cm.getMonsterLifeFactory(entry.ID);
                    let nmstats = new MStats();
                    nmstats.setHp(entry.options.hp);
                    monster.setOverrideStats(nmstats);
                    player.getMap().spawnMonsterOnGroundBelow(monster, player.getPosition());
                }
            } else {
                cm.sendOk(`You do not have #b3 #z${Currency}##k to give me?\r\nWell then I can't follow up on your request`);
            }
        }
        cm.dispose();
    }
}

function MonsterEntry(ID, options) {
    this.ID = ID;
    this.options = options;
}