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
            byte moveType = reader.readByte();
            switch (moveType) {
                case 0: // normal move
                case 5:
                case 17: { // Float
                    Point position = reader.readPoint();
                    Point velocity = reader.readPoint();
                    short foothold = reader.readShort();
                    byte stance = reader.readByte();
                    short duration = reader.readShort();
                    AbsoluteLifeMovement alm = new AbsoluteLifeMovement(moveType, position, duration, stance, velocity, foothold);
                    res.add(alm);

                    if (client != null) {
                        MapleCharacter player = client.getPlayer();
                        if (player != null) {
                            if (Math.abs(velocity.getX()) > 1500) {
                                Cheater.CheatEntry cheatEntry = player.getCheater().getCheatEntry(Cheats.FastWalk);
                                if (cheatEntry.testFor(1000 * 60)) {
                                    cheatEntry.announce(client, 1000 * 60, "'{}' is fast walking (speed: {})", player.getName(), velocity.getX());
                                }
                            }
                            if (stance == 13 && !player.getMap().isSwimEnabled()) {
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
                    Point position = reader.readPoint();
                    byte stance = reader.readByte();
                    short duration = reader.readShort();
                    res.add(new RelativeLifeMovement(moveType, position, duration, stance));
                    break;
                }
                case 3:
                case 4: // tele... -.-
                case 7: // assaulter
                case 8: // assassinate
                case 9: // rush
                case 11: { //chair
                    Point position = reader.readPoint();
                    short unk = reader.readShort();
                    byte stance = reader.readByte();
                    short duration = reader.readShort();
                    res.add(new TeleportMovement(moveType, position, duration, stance, unk));
                    break;
                }
                case 14:
                    reader.skip(9); // jump down (?)
                    break;
                case 10: // Change Equip
                    res.add(new ChangeEquip(reader.readByte()));
                    break;
                case 15: {
                    Point position = reader.readPoint();
                    Point velocity = reader.readPoint();
                    short unk = reader.readShort();
                    short fh = reader.readShort();
                    byte newState = reader.readByte();
                    short duration = reader.readShort();
                    res.add(new JumpDownMovement(moveType, position, duration, newState, velocity, unk, fh));
                    break;
                }
                case 21: {
                    reader.skip(3);
                    break;
                }
            }
        }
        return res;
    }

    public static void updatePosition(List<LifeMovementFragment> movement, AnimatedMapleMapObject target, int yoffset) {
        for (LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof AbsoluteLifeMovement) {
                    AbsoluteLifeMovement abs = (AbsoluteLifeMovement) move;
                    Point position = abs.getPosition();
                    position.y += yoffset;
                    target.setPosition(position);
                    target.setFoothold(abs.getFoothold());
                }
                target.setStance(((LifeMovement) move).getStance());
            }
        }
    }
}
