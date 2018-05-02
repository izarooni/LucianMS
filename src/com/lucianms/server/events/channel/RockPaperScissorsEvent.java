package com.lucianms.server.events.channel;

import client.MapleCharacter;
import client.arcade.RPSGame;
import net.PacketEvent;
import net.SendOpcode;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @author izarooni
 * @author kevintjuh93
 */
public class RockPaperScissorsEvent extends PacketEvent {

    private byte operation;
    private byte choice;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        operation = slea.readByte();
        switch (operation) {
            case 0x1:
                choice = slea.readByte();
                break;
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        RPSGame game = player.getRPSGame();
        if (game == null) {
            player.setRPSGame((game = new RPSGame()));
        }

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.RPS_GAME.getValue());
        switch (operation) {
            case 0x0: // start
            case 0x5: { // retry
                game.setSelection((byte) -1); // allow selection

                mplew.write(0x9); // action
                getClient().announce(mplew.getPacket());
                break;
            }
            case 0x1: { // select
                if (game.getSelection() == -1) {
                    byte npc = (byte) Randomizer.nextInt(3);
                    int round = game.getRound();
                    game.setSelection(choice);

                    mplew.write(0xb); // action
                    mplew.write(npc); // npc choice

                    if (choice == npc) {
                        game.setSelection((byte) -1);
                        mplew.write(round); // draw
                    } else if ((choice == 0 && npc == 1)  // P:Rock     Vs. C:Paper
                            || (choice == 1 && npc == 2)  // P:Paper    Vs. C:Scissors
                            || (choice == 2 && npc == 0)) // P:Scissors Vs  C:Rock
                    {
                        game.setRound(0);
                        mplew.write(-1); // NPC win
                    } else {
                        mplew.write(++round); // Player win
                    }
                    getClient().announce(mplew.getPacket());
                }
                break;
            }
            case 0x3: { // continue
                game.setSelection((byte) -1); // allow selection

                mplew.write(0xC); // action
                getClient().announce(mplew.getPacket());
                break;
            }
            case 0x4: { // exit
                player.setRPSGame(null);

                mplew.write(0xd); // action
                getClient().announce(mplew.getPacket());
                break;
            }
        }
        return null;
    }

    public static void startGame(MapleCharacter player) {
        player.setRPSGame(new RPSGame());

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(5);
        mplew.writeShort(SendOpcode.RPS_GAME.getValue());
        mplew.write(8);
        mplew.writeInt(9000019);
        player.announce(mplew.getPacket());
    }
}
