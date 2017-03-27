package net.server.channel.handlers;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.SendOpcode;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @author izarooni
 * @author kevintjuh93
 */
public class RockPaperScissorsHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte action = slea.readByte();
        MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();
        writer.writeShort(SendOpcode.RPS_GAME.getValue());
        switch (action) {
            case 0x0: // start
            case 0x5: { // retry
                writer.write(0x9);
                c.announce(writer.getPacket());
                break;
            }
            case 0x1: { // select
                byte choice = slea.readByte();
                System.out.println(choice);
                byte npc = (byte) Randomizer.nextInt(3);
                writer.write(0xb);
                writer.write(npc);
                writer.write(0);
                c.announce(writer.getPacket());
                break;
            }
            case 0x3: { // continue
                writer.write(0xC);
                c.announce(writer.getPacket());
                break;
            }
            case 0x4: { // exit
                writer.write(0xd);
                c.announce(writer.getPacket());
                break;
            }
        }
    }
}
