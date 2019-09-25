/*
 * @author BubblesDev, Rich for text
 */
const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
importPackage(Packages.tools);
const LifeFactory = Java.type("com.lucianms.server.life.MapleLifeFactory");

function start() {
    cm.sendOk("it's been long since I've spotted Aeonaxx around here. Keep an eye out, will you? \r\n\\r\n\#eWho is #rAeonaxx#k?\r\n\Aeonaxx is a dragon that sometimes makes his presence here. He is a creation from the Black Mage but he's harmless unless you aggro him. If you dare to take him on then go ahead. i won't protect you.");
	player.getMap().broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear3"));
    cm.dispose();
}