package server.events.pvp;

import client.MapleCharacter;
import client.inventory.Equip;
import client.inventory.MapleInventoryType;
import client.inventory.MapleWeaponType;
import net.server.channel.handlers.AbstractDealDamageHandler;
import net.server.channel.handlers.CloseRangeDamageHandler;
import server.events.custom.GenericEvent;
import server.life.FakePlayer;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.annotation.PacketWorker;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author izarooni
 */
public class PlayerBattle extends GenericEvent {

    private final MapleCharacter attacker;
    private HashMap<Integer, Point> locations = new HashMap<>();

    private int damage = 0;
    private double distance = -1;

    /**
     * cache locations of every player in the field for final distance calculations
     *
     * @param attacker an attacking player
     */
    public PlayerBattle(MapleCharacter attacker) {
        registerAnnotationPacketEvents(this);
        this.attacker = attacker;
    }

    @PacketWorker
    public void onCloseRange(CloseRangeDamageHandler event) {
        MapleCharacter player = event.getClient().getPlayer();
        FakePlayer fPlayer = player.getFakePlayer();
        if (fPlayer == null) {
            return;
        }
        AbstractDealDamageHandler.AttackInfo attackInfo = event.getAttackInfo();
        collectTargets();
        calculateAttack(attackInfo).attackNeighbors();
    }

    /**
     * Create a copy of all players position in the current field
     */
    private void collectTargets() {
        if (!locations.isEmpty()) {
            locations.clear();
            locations = new HashMap<>();
        }
        attacker.getMap().getCharacters().stream() // cache locations of players in the current field
                .filter(player -> player.getId() != attacker.getId() && player.getGenericEvents().stream().anyMatch(g -> g instanceof PlayerBattle)) // find players register in a pvp event and exclude the attacker
                .forEach(player -> locations.put(player.getId(), player.getPosition().getLocation()));
        System.out.println(locations.size() + " targets initialized");
    }

    /**
     * Attack nearby players in the appropriate direction
     */
    private void attackNeighbors() {
        if (distance == -1) {
            throw new RuntimeException("Unable to attack neighboring targets; Distance not calculated");
        }
        final MapleMap map = attacker.getMap();
        final Point attackerLocation = attacker.getPosition().getLocation();
        final boolean facingLeft = attacker.isFacingLeft();

        HashMap<Integer, Point> neighbors = new HashMap<>(); // nearby targets

        for (Map.Entry<Integer, Point> entry : locations.entrySet()) { // all targets in the field
            Point targetLocation = entry.getValue();
            double lDistance = attackerLocation.distance(targetLocation);
            System.out.println("A<->T dist: " + lDistance);
            if (lDistance <= distance) { // target within attacking distance
                if ((attackerLocation.getX() <= targetLocation.getX() && !facingLeft)) { // (attacker)-> (target)
                    neighbors.put(entry.getKey(), entry.getValue());
                    System.out.println("attacker found (right)");
                } else if ((attackerLocation.getX() >= targetLocation.getX() && facingLeft)) { // (target) <-(attacker)
                    neighbors.put(entry.getKey(), entry.getValue());
                    System.out.println("attacker found (left)");
                }
            }
        }

        for (Map.Entry<Integer, Point> entry : neighbors.entrySet()) { // iterate nearby targets and display damage dealt
            map.broadcastMessage(MaplePacketCreator.damagePlayer(0, 100100, entry.getKey(), damage, 0, 0, false, 0, false, 0, 0, 0));
        }
    }

    /**
     * Calculate necessary attack data between the player and nearby targets via weapon, skill and stats
     *
     * @param attackInfo attack data
     * @return the PlayerBattle object for a more convenient use
     */
    private PlayerBattle calculateAttack(AbstractDealDamageHandler.AttackInfo attackInfo) {
        Equip weapon = (Equip) attacker.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
        if (weapon == null) {
            // not possible to attack when a weapon is not present
            return this;
        }
        // create a base distance by weapon type hitbox range
        int wt = MapleWeaponType.getWeaponType(weapon.getItemId());
        switch (wt) {
            case 33: // dagger
                distance = 74;
                break;
            default:
                System.out.println("Unhandled weapon type for distance calc'u: " + wt);
                break;
        }

        // add distance to base
        switch (attackInfo.skill) {
            default:
                System.out.println("Unhandled skill for distance calc'u: " + attackInfo.skill);
                break;
        }
        System.out.println("Distance calc'd: " + distance);

        for (List<Integer> list : attackInfo.allDamage.values()) {
            damage += list.stream().mapToInt(Integer::intValue).sum();
        }
        if (damage < 0) {
            damage = Integer.MAX_VALUE;
        }
        System.out.println("Damage calc'd: " + damage);
        return this;
    }
}
