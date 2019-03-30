const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
const MaplePacketWriter = Java.type('com.lucianms.nio.send.MaplePacketWriter');
/* izarooni */
let status = 0;

function action(mode, type, selection) {
    let w = new MaplePacketWriter();
    w.writeShort(90);
    w.writeMapleString("massacre_miss");
    w.writeMapleString("0");
    // w.writeShort(128);
    // w.write(1); /// 0, 1 | add, remove
    // w.writeInt(0);
    // w.write(4); // layer probably
    // w.writeInt(0);
    player.announce(w.getPacket());
    player.dropMessage("Packet sent");
    cm.dispose();
}
