package com.lucianms.server.world;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.meta.Union;
import tools.Disposable;
import tools.MaplePacketCreator;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author izarooni
 */
public class MapleParty extends SocialGroup<MaplePartyCharacter> implements Disposable {

    private static final AtomicInteger UniqueID = new AtomicInteger(1);
    public static final int MaximumUsers = 6;

    private final int ID;
    private int leaderPlayerID;

    public MapleParty(MapleCharacter player) {
        super(6);
        ID = UniqueID.getAndIncrement();
        leaderPlayerID = player.getId();

        player.setPartyID(ID);
        put(player.getId(), new MaplePartyCharacter(player));
    }

    @Override
    public Collection<MapleCharacter> getPlayers() {
        return values().stream().filter(m -> m.getPlayer() != null)
                .map(SocialMember::getPlayer).collect(Collectors.toList());
    }

    @Override
    public void dispose() {
        Collection<MapleCharacter> players = getPlayers();
        players.forEach(p -> p.setPartyID(0));
        players.clear();
        clear();
    }

    public int getID() {
        return ID;
    }

    public int getLeaderPlayerID() {
        return leaderPlayerID;
    }

    public MaplePartyCharacter getLeader() {
        return get(getLeaderPlayerID());
    }

    public void setLeader(int playerID) {
        MaplePartyCharacter member = get(playerID);
        if (member != null) {
            leaderPlayerID = playerID;
        }
        throw new NullPointerException();
    }

    public void addMember(MapleCharacter player) {
        player.setPartyID(ID);
        MaplePartyCharacter member = new MaplePartyCharacter(player);
        put(player.getId(), member);
        sendPacket(MaplePacketCreator.updateParty(player.getClient().getChannel(), this, PartyOperation.JOIN, member));
    }

    public void removeMember(MapleCharacter player, boolean expelled) {
        player.setPartyID(0);
        MaplePartyCharacter remove = remove(player.getId());
        if (remove != null) {
            sendPacket(MaplePacketCreator.updateParty(player.getClient().getChannel(), this, expelled ? PartyOperation.EXPEL : PartyOperation.LEAVE, remove));
        }
    }

    public int unionBonus(String union) {
        int bonus = 0;
        for (MapleCharacter partymember : this.getPlayers()) {
            if (union.equalsIgnoreCase(partymember.getUnion().getName())) {
                bonus ++;
            }
        }
        return bonus;
    }

    public boolean hasUnion(String union) {
        for (MapleCharacter partymember : this.getPlayers()) {
            if (union.equalsIgnoreCase(partymember.getUnion().getName())) {
                return true;
            }
        }
        return false;
    }

}
