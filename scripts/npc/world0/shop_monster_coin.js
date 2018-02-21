load("scripts/npc/generic_shop.js");

var pointsType = 4011020;

try {
    var file = new java.io.File("resources/data-monster_coin.json");
    var content = Packages.org.apache.commons.io.FileUtils.readFileToString(file);
    var json = JSON.parse(content);

    for (var j in json) {
        items[j] = JSON.parse("[" + json[j] + "]");
    }
} catch (e) {
    broken = e.message.replace(/\\/g, "/");
}
