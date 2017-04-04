package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.arcade.RPSGame;
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
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {
        MapleCharacter player = client.getPlayer();
        RPSGame game = player.getRPSGame();
        if (game == null) {
            return;
        }
        byte action = slea.readByte();
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.RPS_GAME.getValue());
        switch (action) {
            case 0x0: // start
            case 0x5: { // retry
                mplew.write(0x9); // action
                client.announce(mplew.getPacket());
                game.setSelection((byte) -1); // allow selection
                break;
            }
            case 0x1: { // select
                if (game.getSelection() == -1) {
                    byte selection = slea.readByte();
                    byte npc = (byte) Randomizer.nextInt(3);
                    int round = game.getRound();
                    game.setSelection(selection);

                    mplew.write(0xb); // action
                    mplew.write(npc); // npc choice

                    if (selection == npc) {
                        mplew.write(round); // draw
                    } else if ((selection == 0 && npc == 1)  // P:Rock     Vs. C:Paper
                            || (selection == 1 && npc == 2)  // P:Paper    Vs. C:Scissors
                            || (selection == 2 && npc == 0)) // P:Scissors Vs  C:Rock
                    {
                        mplew.write(--round); // NPC win
                    } else {
                        mplew.write(++round); // Player win
                    }
                    client.announce(mplew.getPacket());
                }
                break;
            }
            case 0x3: { // continue
                mplew.write(0xC); // action
                client.announce(mplew.getPacket());
                break;
            }
            case 0x4: { // exit
                mplew.write(0xd); // action
                client.announce(mplew.getPacket());
                player.setRPSGame(null);
                break;
            }
        }
    }

    public static void startGame(MapleCharacter player) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(5);
        mplew.writeShort(SendOpcode.RPS_GAME.getValue());
        mplew.write(8);
        mplew.writeInt(9000019);
        player.announce(mplew.getPacket());
        player.setRPSGame(new RPSGame());
    }
}
