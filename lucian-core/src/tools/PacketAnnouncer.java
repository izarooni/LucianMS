package tools;

import com.lucianms.client.MapleCharacter;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author izarooni
 */
public interface PacketAnnouncer {

    /**
     * This assumes a new collection is created per call. Do not return collections directly
     */
    Collection<MapleCharacter> getPlayers();

    default Collection<MapleCharacter> getPlayers(Predicate<MapleCharacter> condition) {
        // lul pls dont judge me
        Collection<MapleCharacter> players = getPlayers();
        List<MapleCharacter> filtered = players.stream().filter(condition).collect(Collectors.toList());
        players.clear();
        return filtered;
    }

    default void forEachPlayer(Consumer<MapleCharacter> action) {
        Collection<MapleCharacter> players = getPlayers();
        players.forEach(action);
        players.clear();
    }

    default void sendMessage(int type, String content, Object... args) {
        Collection<MapleCharacter> players = getPlayers();
        players.forEach(p -> p.sendMessage(5, content, args));
        players.clear();
    }

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
