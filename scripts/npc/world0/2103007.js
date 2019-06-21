load("scripts/npc/generic_shop.js");

dialog.intro = "Hey #h #, I trade #bParty Quest Points#k for various items. You can obtain these points by completing PQs found in the #bParty Quest Entrance#k map < #d@go pq#k >.";
var pointsType = "PQ points";

try {
    let file = new java.io.File("resources/data-pq.json");
    let content = Packages.org.apache.commons.io.FileUtils.readFileToString(file);
    let json = JSON.parse(content);

    for (let j in json) {
        items[j] = JSON.parse("[" + json[j] + "]");
    }
} catch (e) {
    broken = e.message.replace(/\\/g, "/");
}
