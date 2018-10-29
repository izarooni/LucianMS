package com.lucianms.server.movement;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.autoban.Cheater;
import com.lucianms.client.autoban.Cheats;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.maps.AnimatedMapleMapObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class MovementPacketHelper {

    public static List<LifeMovementFragment> parse(MapleClient client, MaplePacketReader reader) {
        List<LifeMovementFragment> res = new ArrayList<>();
        byte numCommands = reader.readByte();
        for (byte i = 0; i < numCommands; i++) {
            byte command = reader.readByte();
            switch (command) {
                case 0: // normal move
                case 5:
                case 17: { // Float
                    short xpos = reader.readShort();
                    short ypos = reader.readShort();
                    short xwobble = reader.readShort();
                    short ywobble = reader.readShort();
                    short unk = reader.readShort();
                    byte newstate = reader.readByte();
                    short duration = reader.readShort();
                    AbsoluteLifeMovement alm = new AbsoluteLifeMovement(command, new Point(xpos, ypos), duration, newstate);
                    alm.setUnk(unk);
                    alm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    res.add(alm);

                    if (client != null) {
                        MapleCharacter player = client.getPlayer();
                        if (player != null) {
                            if (Math.abs(xwobble) > 1500) {
                                Cheater.CheatEntry cheatEntry = player.getCheater().getCheatEntry(Cheats.FastWalk);
                                if (cheatEntry.testFor(1000 * 60)) {
                                    cheatEntry.announce(client, 1000 * 60, "'{}' is fast walking (speed: {})", player.getName(), xwobble);
                                }
                            }
                            if (newstate == 13 && !player.getMap().isSwimEnabled()) {
                                Cheater.CheatEntry cheatEntry = player.getCheater().getCheatEntry(Cheats.Swim);
                                if (cheatEntry.testFor(1000 * 60)) {
                                    cheatEntry.announce(client, 1000 * 60, "'{}' is swimming in a swim disabled map {}", player.getName(), player.getMapId());
                                }
                            }
                        }
                    }
                    break;
                }
                case 1:
                case 2:
                case 6: // fj
                case 12:
                case 13: // Shot-jump-back thing
                case 16: // Float
                case 18:
                case 19: // Springs on maps
                case 20: // Aran Combat Step
                case 22: {
                    short xpos = reader.readShort();
                    short ypos = reader.readShort();
                    byte newstate = reader.readByte();
                    short duration = reader.readShort();
                    RelativeLifeMovement rlm = new RelativeLifeMovement(command, new Point(xpos, ypos), duration, newstate);
                    res.add(rlm);
                    break;
                }
                case 3:
                case 4: // tele... -.-
                case 7: // assaulter
                case 8: // assassinate
                case 9: // rush
                case 11: //chair
                {
                    //                case 14: {
                    short xpos = reader.readShort();
                    short ypos = reader.readShort();
                    short xwobble = reader.readShort();
                    short ywobble = reader.readShort();
                    byte newstate = reader.readByte();
                    TeleportMovement tm = new TeleportMovement(command, new Point(xpos, ypos), newstate);
                    tm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    res.add(tm);
                    break;
                }
                case 14:
                    reader.skip(9); // jump down (?)
                    break;
                case 10: // Change Equip
                    res.add(new ChangeEquip(reader.readByte()));
                    break;
                case 15: {
                    short xpos = reader.readShort();
                    short ypos = reader.readShort();
                    short xwobble = reader.readShort();
                    short ywobble = reader.readShort();
                    short unk = reader.readShort();
                    short fh = reader.readShort();
                    byte newstate = reader.readByte();
                    short duration = reader.readShort();
                    JumpDownMovement jdm = new JumpDownMovement(command, new Point(xpos, ypos), duration, newstate);
                    jdm.setUnk(unk);
                    jdm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    jdm.setFH(fh);
                    res.add(jdm);
                    break;
                }
                case 21: {
                    reader.skip(3);
                    break;
                }
                default:
                    System.out.println("Unhandled Case:" + command);
                    return null;
            }
        }
        return res;
    }

    public static void updatePosition(List<LifeMovementFragment> movement, AnimatedMapleMapObject target, int yoffset) {
        for (LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof AbsoluteLifeMovement) {
                    Point position = move.getPosition();
                    position.y += yoffset;
                    target.setPosition(position);
                }
                target.setStance(((LifeMovement) move).getNewstate());
            }
        }
    }
}
