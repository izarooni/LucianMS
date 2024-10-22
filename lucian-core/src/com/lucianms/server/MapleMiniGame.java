/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.lucianms.server;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.server.maps.AbstractMapleMapObject;
import com.lucianms.server.maps.MapleMapObjectType;
import tools.MaplePacketCreator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Matze
 */
public class MapleMiniGame extends AbstractMapleMapObject {

    private final byte mode;
    private MapleCharacter owner;
    private MapleCharacter visitor;
    private String GameType = null;
    private int[] piece = new int[250];
    private List<Integer> list4x3 = new ArrayList<>();
    private List<Integer> list5x4 = new ArrayList<>();
    private List<Integer> list6x5 = new ArrayList<>();
    private String description;
    private String password;
    private int loser = 1;
    private int piecetype;
    private int firstslot;
    private int visitorpoints;
    private int ownerpoints;
    private int matchestowin;
    private boolean started;

    public MapleMiniGame(byte mode, MapleCharacter owner, String description, String password) {
        this.mode = mode;
        this.owner = owner;
        this.description = description;
        this.password = password;
    }

    public byte getMode() {
        return mode;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean hasFreeSlot() {
        return visitor == null;
    }

    public boolean isOwner(MapleCharacter c) {
        return owner.equals(c);
    }

    public void addVisitor(MapleCharacter challenger) {
        visitor = challenger;
        if (GameType.equals("omok")) {
            this.getOwner().getClient().announce(MaplePacketCreator.getMiniGameNewVisitor(challenger, 1));
            this.getOwner().getMap().broadcastMessage(MaplePacketCreator.addOmokBox(owner, this));
        }
        if (GameType.equals("matchcard")) {
            this.getOwner().getClient().announce(MaplePacketCreator.getMatchCardNewVisitor(challenger, 1));
            this.getOwner().getMap().broadcastMessage(MaplePacketCreator.addMatchCardBox(owner, this));
        }
    }

    public void removeVisitor(MapleCharacter challenger) {
        if (visitor == challenger) {
            started = false;
            visitor = null;
            this.getOwner().getClient().announce(MaplePacketCreator.getMiniGameRemoveVisitor());
            if (GameType.equals("omok")) {
                this.getOwner().getMap().broadcastMessage(MaplePacketCreator.addOmokBox(owner, this));
            }
            if (GameType.equals("matchcard")) {
                this.getOwner().getMap().broadcastMessage(MaplePacketCreator.addMatchCardBox(owner, this));
            }
        }
    }

    public boolean isVisitor(MapleCharacter challenger) {
        return visitor == challenger;
    }

    public void broadcastToVisitor(final byte[] packet) {
        if (visitor != null) {
            visitor.getClient().announce(packet);
        }
    }

    public int getFirstSlot() {
        return firstslot;
    }

    public void setFirstSlot(int type) {
        firstslot = type;
    }

    public void setOwnerPoints() {
        started = false;
        ownerpoints++;
        if (ownerpoints + visitorpoints == matchestowin) {
            if (ownerpoints == visitorpoints) {
                this.broadcast(MaplePacketCreator.getMatchCardTie(this));
            } else if (ownerpoints > visitorpoints) {
                this.broadcast(MaplePacketCreator.getMatchCardOwnerWin(this));
            } else {
                this.broadcast(MaplePacketCreator.getMatchCardVisitorWin(this));
            }
            ownerpoints = 0;
            visitorpoints = 0;
        }
    }

    public void setVisitorPoints() {
        visitorpoints++;
        if (ownerpoints + visitorpoints == matchestowin) {
            started = false;
            if (ownerpoints > visitorpoints) {
                this.broadcast(MaplePacketCreator.getMiniGameOwnerWin(this));
            } else if (visitorpoints > ownerpoints) {
                this.broadcast(MaplePacketCreator.getMiniGameVisitorWin(this));
            } else {
                this.broadcast(MaplePacketCreator.getMiniGameTie(this));
            }
            getOwner().getMap().broadcastMessage(MaplePacketCreator.addMatchCardBox(getOwner(), this));
            ownerpoints = 0;
            visitorpoints = 0;
        }
    }

    public int getPieceType() {
        return piecetype;
    }

    public void setPieceType(int type) {
        piecetype = type;
    }

    public String getGameType() {
        return GameType;
    }

    public void setGameType(String game) {
        GameType = game;
        if (game.equals("matchcard")) {
            if (matchestowin == 6) {
                for (int i = 0; i < 6; i++) {
                    list4x3.add(i);
                    list4x3.add(i);
                }
            } else if (matchestowin == 10) {
                for (int i = 0; i < 10; i++) {
                    list5x4.add(i);
                    list5x4.add(i);
                }
            } else {
                for (int i = 0; i < 15; i++) {
                    list6x5.add(i);
                    list6x5.add(i);
                }
            }
        }
    }

    public void shuffleList() {
        if (matchestowin == 6) {
            Collections.shuffle(list4x3);
        } else if (matchestowin == 10) {
            Collections.shuffle(list5x4);
        } else {
            Collections.shuffle(list6x5);
        }
    }

    public int getCardId(int slot) {
        int cardid;
        if (matchestowin == 6) {
            cardid = list4x3.get(slot - 1);
        } else if (matchestowin == 10) {
            cardid = list5x4.get(slot - 1);
        } else {
            cardid = list6x5.get(slot - 1);
        }
        return cardid;
    }

    public int getMatchesToWin() {
        return matchestowin;
    }

    public void setMatchesToWin(int type) {
        matchestowin = type;
    }

    public int getLoser() {
        return loser;
    }

    public void setLoser(int type) {
        loser = type;
    }

    public void broadcast(final byte[] packet) {
        if (owner.getClient() != null && owner.getClient().getSession() != null) {
            owner.getClient().announce(packet);
        }
        broadcastToVisitor(packet);
    }

    public void chat(MapleClient c, String chat) {
        broadcast(MaplePacketCreator.getPlayerShopChat(c.getPlayer(), chat, isOwner(c.getPlayer())));
    }

    public void sendOmok(MapleClient c) {
        c.announce(MaplePacketCreator.getMiniGame(c, this, isOwner(c.getPlayer())));
    }

    public void sendMatchCard(MapleClient c) {
        c.announce(MaplePacketCreator.getMatchCard(c, this, isOwner(c.getPlayer()), getPieceType()));
    }

    public MapleCharacter getOwner() {
        return owner;
    }

    public MapleCharacter getVisitor() {
        return visitor;
    }

    public void setPiece(int move1, int move2, int type, MapleCharacter chr) {
        int slot = move2 * 15 + move1 + 1;
        if (piece[slot] == 0) {
            piece[slot] = type;
            this.broadcast(MaplePacketCreator.getMiniGameMoveOmok(this, move1, move2, type));
            for (int y = 0; y < 15; y++) {
                for (int x = 0; x < 11; x++) {
                    if (searchCombo(x, y, type)) {
                        if (this.isOwner(chr)) {
                            this.broadcast(MaplePacketCreator.getMiniGameOwnerWin(this));
                            this.setLoser(0);
                        } else {
                            this.broadcast(MaplePacketCreator.getMiniGameVisitorWin(this));
                            this.setLoser(1);
                        }
                        for (int y2 = 0; y2 < 15; y2++) {
                            for (int x2 = 0; x2 < 15; x2++) {
                                int slot2 = (y2 * 15 + x2 + 1);
                                piece[slot2] = 0;
                            }
                        }
                    }
                }
            }
            for (int y = 0; y < 15; y++) {
                for (int x = 4; x < 15; x++) {
                    if (searchCombo2(x, y, type)) {
                        if (this.isOwner(chr)) {
                            this.broadcast(MaplePacketCreator.getMiniGameOwnerWin(this));
                            this.setLoser(0);
                        } else {
                            this.broadcast(MaplePacketCreator.getMiniGameVisitorWin(this));
                            this.setLoser(1);
                        }
                        for (int y2 = 0; y2 < 15; y2++) {
                            for (int x2 = 0; x2 < 15; x2++) {
                                int slot2 = (y2 * 15 + x2 + 1);
                                piece[slot2] = 0;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean searchCombo(int x, int y, int type) {
        int slot = y * 15 + x + 1;
        for (int i = 0; i < 5; i++) {
            if (piece[slot + i] == type) {
                if (i == 4) {
                    return true;
                }
            } else {
                break;
            }
        }
        for (int j = 15; j < 17; j++) {
            for (int i = 0; i < 5; i++) {
                if (piece[slot + i * j] == type) {
                    if (i == 4) {
                        return true;
                    }
                } else {
                    break;
                }
            }
        }
        return false;
    }

    private boolean searchCombo2(int x, int y, int type) {
        int slot = y * 15 + x + 1;
        for (int j = 14; j < 15; j++) {
            for (int i = 0; i < 5; i++) {
                if (piece[slot + i * j] == type) {
                    if (i == 4) {
                        return true;
                    }
                } else {
                    break;
                }
            }
        }
        return false;
    }

    public String getDescription() {
        return description;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
    }

    @Override
    public void sendDestroyData(MapleClient client) {
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.MINI_GAME;
    }
}
