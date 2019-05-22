package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.arcade.RPSGame;
import com.lucianms.nio.SendOpcode;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.nio.send.MaplePacketWriter;
import tools.Randomizer;

/**
 * @author izarooni
 * @author kevintjuh93
 */
public class RockPaperScissorsEvent extends PacketEvent {

    private byte operation;
    private byte choice;

    @Override
    public void processInput(MaplePacketReader reader) {
        operation = reader.readByte();
        switch (operation) {
            case 0x1:
                choice = reader.readByte();
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

        MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.RPS_GAME.getValue());
        switch (operation) {
            case 0x0: // start
            case 0x5: { // retry
                game.setSelection((byte) -1); // allow selection

                w.write(0x9); // action
                getClient().announce(w.getPacket());
                break;
            }
            case 0x1: { // select
                if (game.getSelection() == -1) {
                    byte npc = (byte) Randomizer.nextInt(3);
                    int round = game.getRound();
                    game.setSelection(choice);

                    w.write(0xb); // action
                    w.write(npc); // npc choice

                    if (choice == npc) {
                        game.setSelection((byte) -1);
                        w.write(round); // draw
                    } else if ((choice == 0 && npc == 1)  // P:Rock     Vs. C:Paper
                            || (choice == 1 && npc == 2)  // P:Paper    Vs. C:Scissors
                            || (choice == 2 && npc == 0)) // P:Scissors Vs  C:Rock
                    {
                        game.setRound(0);
                        w.write(-1); // NPC win
                    } else {
                        w.write(++round); // Player win
                    }
                    getClient().announce(w.getPacket());
                }
                break;
            }
            case 0x3: { // continue
                game.setSelection((byte) -1); // allow selection

                w.write(0xC); // action
                getClient().announce(w.getPacket());
                break;
            }
            case 0x4: { // exit
                player.setRPSGame(null);

                w.write(0xd); // action
                getClient().announce(w.getPacket());
                break;
            }
        }
        return null;
    }

    public static void startGame(MapleCharacter player) {
        player.setRPSGame(new RPSGame());

        MaplePacketWriter w = new MaplePacketWriter(5);
        w.writeShort(SendOpcode.RPS_GAME.getValue());
        w.write(8);
        w.writeInt(9000019);
        player.announce(w.getPacket());
    }
}
