package tools;

import com.lucianms.client.MapleCharacter;

import java.util.Collection;

/**
 * @author izarooni
 */
public interface PacketAnnouncer {

    Collection<MapleCharacter> getPlayers();

    default void sendPacket(byte[] packet) {
        Collection<MapleCharacter> players = getPlayers();
        players.forEach(p -> p.announce(packet));
        players.clear();
    }

    default void sendPacket(byte[] packet, MapleCharacter exclude) {
        Collection<MapleCharacter> players = getPlayers();
        players.stream().filter(p -> p.getId() != exclude.getId()).forEach(p -> p.announce(packet));
        players.clear();
    }
}
