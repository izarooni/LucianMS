package server.events.pvp;

import client.MapleCharacter;
import client.inventory.Equip;
import client.inventory.MapleInventoryType;
import client.inventory.MapleWeaponType;
import net.server.channel.handlers.AbstractDealDamageHandler;
import net.server.channel.handlers.CloseRangeDamageHandler;
import net.server.channel.handlers.MagicDamageHandler;
import net.server.channel.handlers.RangedAttackHandler;
import server.events.custom.GenericEvent;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.Randomizer;
import tools.annotation.PacketWorker;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author izarooni
 */
public class PlayerBattle extends GenericEvent {

    private final MapleCharacter attacker;
    private HashMap<Integer, Point> locations = new HashMap<>();
    private ReentrantLock lock = new ReentrantLock(true);

    private int damage = 0;
    private double cAttackRange = -1;
    private double fAttackRange = -1;

    /**
     * @param attacker an attacking player
     */
    public PlayerBattle(MapleCharacter attacker) {
        registerAnnotationPacketEvents(this);
        this.attacker = attacker;
    }

    @PacketWorker
    public void onCloseRangeAttack(CloseRangeDamageHandler event) {
        lock.lock();
        try {
            // reset previous calculations
            damage = 0;
            fAttackRange = 0;
            cAttackRange = 0;
            AbstractDealDamageHandler.AttackInfo attackInfo = event.getAttackInfo();
            collectTargets();
            calculateAttack(attackInfo);
            attackNeighbors();
        } finally {
            lock.unlock();
        }
    }

    @PacketWorker
    public void onFarRangeAttack(RangedAttackHandler event) {
        lock.lock();
        try {
            // reset previous calculations
            damage = 0;
            fAttackRange = 0;
            cAttackRange = 0;
            AbstractDealDamageHandler.AttackInfo attackInfo = event.getAttackInfo();
            collectTargets();
            calculateAttack(attackInfo);
            attackNeighbors();
        } finally {
            lock.unlock();
        }
    }

    @PacketWorker
    public void onMagicAttack(MagicDamageHandler event) {
        lock.lock();
        try {
            // reset previous calculations
            damage = 0;
            fAttackRange = 0;
            cAttackRange = 0;
            AbstractDealDamageHandler.AttackInfo attackInfo = event.getAttackInfo();
            collectTargets();
            calculateAttack(attackInfo);
            attackNeighbors();
        } finally {
            lock.unlock();
        }
    }

    /**
     * cache locations of every player in the field for final distance calculations
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
        if (cAttackRange == -1 && fAttackRange == -1) {
            return;
        }
        final MapleMap map = attacker.getMap();
        final Point attackerLocation = attacker.getPosition().getLocation();
        final boolean facingLeft = attacker.isFacingLeft();

        HashMap<Integer, Point> neighbors = new HashMap<>(); // nearby targets

        for (Map.Entry<Integer, Point> entry : locations.entrySet()) { // all targets in the field
            Point targetLocation = entry.getValue();
            double lDistance = attackerLocation.distance(targetLocation);
            System.out.println("A<->T dist: " + lDistance + " / Required: " + (cAttackRange == -1 ? fAttackRange : cAttackRange));
            if (lDistance <= (cAttackRange == -1 ? fAttackRange : cAttackRange)) { // target within attacking distance
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
     * todo convert ranges to JSON data for easier customization
     *
     * @param attackInfo attack data
     */
    private void calculateAttack(AbstractDealDamageHandler.AttackInfo attackInfo) {
        Equip weapon = (Equip) attacker.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
        if (weapon == null) {
            // not possible to attack when a weapon is not present
            return;
        }
        // create a base distance by weapon type hitbox range
        int wt = MapleWeaponType.getWeaponType(weapon.getItemId());
        switch (wt) {
            case 30: // one-handed sword
            case 31: // one-handed axe
            case 32: // one-handed mace
            case 42: // two-handed mace
            case 44: // polearm
                if (cAttackRange == 0) {
                    cAttackRange = 100;
                } else if (fAttackRange == 0) {
                    fAttackRange = 1000;
                }
                break;
            case 33: // dagger
            case 47: // claw
            case 48: // knuckle
                if (cAttackRange == 0) {
                    cAttackRange = 80;
                } else if (fAttackRange == 0) {
                    fAttackRange = 420;
                }
                break;
            case 37: // wand
            case 38: // staff
            case 49: // gun
                if (cAttackRange == 0) {
                    cAttackRange = 75;
                } else if (fAttackRange == 0) {
                    fAttackRange = 1000;
                }
                break;
            case 40: // two-handed sword
            case 45: // bow
            case 46: // crossbow
                if (cAttackRange == 0) {
                    cAttackRange = 90;
                } else if (fAttackRange == 0) {
                    fAttackRange = 420;
                }
                break;
            case 41: // two-handed axe
                if (cAttackRange == 0) {
                    cAttackRange = 115;
                } else if (fAttackRange == 0) {
                    fAttackRange = 1000;
                }
                break;
            case 43: // spear
                if (cAttackRange == 0) {
                    cAttackRange = 130;
                } else if (fAttackRange == 0) {
                    fAttackRange = 1000;
                }
                break;
            default:
                System.out.println("Unhandled weapon type for distance calc'u: " + wt);
                break;
        }

        // add distance to base
        switch (attackInfo.skill) {
            case 0: // no skill
                // do nothing
                break;
            case 9001001: // GM Dragon Roar
                cAttackRange = 500;
                fAttackRange = 500;
                break;
            case 4121003: // Taunt
                fAttackRange = 330;
            case 4121008: { // Ninja Storm
                // cancel attacks
                fAttackRange = -1;
                cAttackRange = -1;

                MapleMonster monster = MapleLifeFactory.getMonster(9300166);
                if (monster != null) {
                    MapleLifeFactory.SelfDestruction des = monster.getStats().getSelfDestruction();
                    if (des != null) {
                        des.setRemoveAfter(1);
                        attacker.getMap().spawnMonsterOnGroundBelow(monster, attacker.getPosition());
                        monster.sendDestroyData(attacker.getClient());
                    }
                }
                break;
            }
            default:
                System.out.println("Unhandled skill for distance calc'u: " + attackInfo.skill);
                break;
        }
        System.out.println("Distance calc'd: {c:" + cAttackRange + ",f:" + fAttackRange + "}");

        int nDamage = attacker.calculateMaxBaseDamage(attacker.getTotalWatk()) * attackInfo.allDamage.values().size();
        nDamage = (int) Math.sqrt(nDamage * 0.75);
        nDamage = Randomizer.rand((int) (nDamage - (nDamage * 0.85)), (int) (nDamage * 1.5));
        damage += nDamage;
        if (damage < 0) {
            damage = Integer.MAX_VALUE;
        }
        System.out.println("Damage calc'd: " + damage);
    }
}
