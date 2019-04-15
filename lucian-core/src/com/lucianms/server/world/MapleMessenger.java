package com.lucianms.server.world;

import com.lucianms.client.MapleCharacter;
import tools.Functions;
import tools.MaplePacketCreator;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author izarooni
 */
public class MapleMessenger extends SocialGroup<MapleMessengerCharacter> {

    private static final AtomicInteger UniqueID = new AtomicInteger(1);
    public static final int MaximumUsers = 3;

    private int ID;

    public MapleMessenger(MapleCharacter player) {
        super(3);
        ID = UniqueID.getAndIncrement();
        addMember(player);
    }

    @Override
    public Collection<MapleCharacter> getPlayers() {
        return values().stream().filter(m -> m.getPlayer() != null)
                .map(SocialMember::getPlayer).collect(Collectors.toList());
    }

    public int getID() {
        return ID;
    }

    public boolean addMember(MapleCharacter player) {
        if (size() == MaximumUsers) {
            return false;
        }
        MapleMessengerCharacter member = new MapleMessengerCharacter(player, size());
        player.announce(MaplePacketCreator.joinMessenger(member.getPosition()));
        forEachMember(others -> {
            others.getPlayer().announce(MaplePacketCreator.addMessengerPlayer(member));
            player.announce(MaplePacketCreator.addMessengerPlayer(others));
        });
        put(player.getId(), member);
        return true;
    }

    public void removeMember(MapleCharacter player) {
        MapleMessengerCharacter remove = remove(player.getId());
        sendPacket(MaplePacketCreator.removeMessengerPlayer(remove.getPosition()));
        Functions.requireNotNull(remove, SocialMember::dispose);
    }

    public int getLowestPosition() {
        for (int index = 0; index < 3; index++) {
            final int position = index;
            if (values().stream().noneMatch(m -> m.getPosition() == position)) {
                return position;
            }
        }
        return -1;
    }

    public int getPositionByName(String name) {
        MapleMessengerCharacter member = find(p -> p.getUsername().equalsIgnoreCase(name));
        return member == null ? -1 : member.getPosition();
    }
}

