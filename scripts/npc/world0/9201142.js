load("scripts/npc/generic_shop.js");

const pointsType = 4031203;

dialog.intro = "Happy #rHalloween#k! Do you have candies for me? You can find them by killing monsters.";
dialog.first = "#b{PointsName}#k are good. Trade them now!\r\n You currently have #b{PointsQuantity} {PointsName}#l";
dialog.second = "Are you ready to spend some candy?";

try {
    let file = new java.io.File("resources/data-candy.json");
    let content = Packages.org.apache.commons.io.FileUtils.readFileToString(file);
    let json = JSON.parse(content);

    for (let j in json) {
        items[j] = JSON.parse("[" + json[j] + "]");
    }
} catch (e) {
    broken = e.message.replace(/\\/g, "/");
}
