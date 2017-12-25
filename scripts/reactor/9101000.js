function act() {
    rm.spawnMonster(9300061, 1, 0, 0); // (0, 0) is temp position
    rm.getClient().getMap().startMapEffect("Protect the Moon Bunny that's pounding the mill, and gather up 10 Moon Bunny's Rice Cakes!", 5120016, 7000);
    rm.getClient().getMap().broadcastMessage(MaplePacketCreator.bunnyPacket()); // Protect the Moon Bunny!
    rm.getClient().getMap().broadcastMessage(MaplePacketCreator.showHPQMoon());
    rm.getClient().getMap().showAllMonsters();
}
