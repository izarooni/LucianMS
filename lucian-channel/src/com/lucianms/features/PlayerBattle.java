package com.lucianms.features;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.Equip;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.client.inventory.MapleWeaponType;
import com.lucianms.events.AbstractDealDamageEvent;
import com.lucianms.events.PlayerDealDamageMagicEvent;
import com.lucianms.events.PlayerDealDamageNearbyEvent;
import com.lucianms.events.PlayerDealDamageRangedEvent;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.nio.SendOpcode;
import com.lucianms.nio.send.MaplePacketWriter;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.maps.MapleMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author izarooni
 */
public class PlayerBattle extends GenericEvent {

    public static byte[] setKnockBackForce(boolean left, byte force) {
        MaplePacketWriter w = new MaplePacketWriter(4);
        w.writeShort(SendOpcode.SNOWBALL_MESSAGE.getValue());
        w.write(force);
        w.write(left ? 6 : 7);
        return w.getPacket();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerBattle.class);

    private MapleCharacter attacker;
    private HashMap<Integer, Point> locations = new HashMap<>();
    private ReentrantLock lock = new ReentrantLock(true);

    private boolean global = false;
    private int damage = 0;
    private double lastAttack = 0;
    private double cAttackRange = -1;
    private double fAttackRange = -1;

    public PlayerBattle() {
        registerAnnotationPacketEvents(this);
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        player.addGenericEvent(this);
        this.attacker = player;
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
    }

    @Override
    public boolean onPlayerDeath(Object sender, MapleCharacter player) {
        Optional<GenericEvent> pvp = player.getGenericEvents().stream().filter(g -> (g instanceof PlayerBattle)).findFirst();
        if (pvp.isPresent()) {
            player.removeGenericEvent(pvp.get());
            player.dropMessage("You are no longer PvPing");
        }
        return false;
    }

    @PacketWorker
    public void onCloseRangeAttack(PlayerDealDamageNearbyEvent event) {
        lock.lock();
        try {
            // set attack type
            fAttackRange = -1;
            cAttackRange = 0;
            AbstractDealDamageEvent.AttackInfo attackInfo = event.getAttackInfo();
            collectTargets();
            calculateAttack(attackInfo);
            attackNeighbors();
        } finally {
            lock.unlock();
        }
    }

    @PacketWorker
    public void onFarRangeAttack(PlayerDealDamageRangedEvent event) {
        lock.lock();
        try {
            // set attack type
            fAttackRange = 0;
            cAttackRange = -1;
            AbstractDealDamageEvent.AttackInfo attackInfo = event.getAttackInfo();
            collectTargets();
            calculateAttack(attackInfo);
            attackNeighbors();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Magic attacks will vary between close range and far range
     * <p>
     * Both variables {@code fAttackRange} and {@code cAttackRange} are excluded from calculations as ranges will solely rely on specific skill usage
     * </p>
     *
     * @param event the magic attack packet event
     */
    @PacketWorker
    public void onMagicAttack(PlayerDealDamageMagicEvent event) {
        lock.lock();
        try {
            // set attack type
            fAttackRange = -1;
            cAttackRange = -1;
            AbstractDealDamageEvent.AttackInfo attackInfo = event.getAttackInfo();
            collectTargets();
            calculateAttack(attackInfo);
            attackNeighbors();
        } finally {
            lock.unlock();
        }
    }

    public double getLastAttack() {
        return (System.currentTimeMillis() - lastAttack) / 1000;
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
                .filter(player -> player.getId() != attacker.getId() && player.getGenericEvents().stream().anyMatch(g -> g instanceof PlayerBattle) // find players register in a pvp event and exclude the attacker
                        && player.isAlive()) // make sure they're alive
                .forEach(player -> locations.put(player.getId(), player.getPosition().getLocation()));
        if (attacker.isDebug()) {
            attacker.sendMessage("{} targets initialized", locations.size());
        }
    }

    /**
     * Attack nearby players in the appropriate direction
     */
    private void attackNeighbors() {
        if ((cAttackRange == -1 && fAttackRange == -1) || locations.isEmpty()) {
            return;
        }
        final MapleMap map = attacker.getMap();
        final Point attackerLocation = attacker.getPosition().getLocation();
        final boolean facingLeft = attacker.isFacingLeft();

        HashMap<Integer, Point> neighbors = new HashMap<>(); // nearby targets

        for (Map.Entry<Integer, Point> entry : locations.entrySet()) { // all targets in the field
            Point targetLocation = entry.getValue();
            double lDistance = attackerLocation.distance(targetLocation);
            if (attacker.isDebug()) {
                attacker.sendMessage("A<->T dist: " + lDistance + " / Required: " + (cAttackRange == -1 ? fAttackRange : cAttackRange));
            }
            if (lDistance <= (cAttackRange == -1 ? fAttackRange : cAttackRange)) { // target within attacking distance
                if (fAttackRange > 0) {
                    if (Math.abs(attackerLocation.getY() - targetLocation.getY()) > 85) { // character height
                        continue;
                    }
                }
                if ((attackerLocation.getX() <= targetLocation.getX() && !facingLeft) || global) { // (attacker)-> (target)
                    neighbors.put(entry.getKey(), entry.getValue());
                } else if ((attackerLocation.getX() >= targetLocation.getX() && facingLeft) || global) { // (target) <-(attacker)
                    neighbors.put(entry.getKey(), entry.getValue());
                }
            }
        }

        if (!neighbors.isEmpty()) {
            lastAttack = System.currentTimeMillis();
            for (Map.Entry<Integer, Point> entry : neighbors.entrySet()) { // iterate nearby targets and display damage dealt
                MapleCharacter mPlayer = attacker.getMap().getCharacterById(entry.getKey());
                if (mPlayer != null) {
                    byte force = (byte) ((70 / mPlayer.getMaxHp()) * mPlayer.getHp());
                    if (attacker.isDebug()) {
                        attacker.sendMessage("applying {} force to {}", (150 + (15 * force)), mPlayer.getName());
                    }
                    mPlayer.announce(setKnockBackForce(!facingLeft, force));
                    mPlayer.announce(MaplePacketCreator.getSnowBallTouch());
                    attacker.announce(MaplePacketCreator.playSound("Cokeplay/Hit"));
                    mPlayer.addHP(-damage);
                    map.broadcastMessage(MaplePacketCreator.damagePlayer(0, 100100, entry.getKey(), damage, 0, 0, false, 0, false, 0, 0, 0));
                }
            }
        }
    }

    /**
     * Calculate necessary attack data between the player and nearby targets via weapon, skill and stats
     *
     * @param attackInfo attack data
     */
    private void calculateAttack(AbstractDealDamageEvent.AttackInfo attackInfo) {

        // reset previous calculations
        global = false;
        damage = 0;

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
                }
                if (fAttackRange == 0) {
                    fAttackRange = 1000;
                }
                break;
            case 33: // dagger
            case 47: // claw
            case 48: // knuckle
                if (cAttackRange == 0) {
                    cAttackRange = 90;
                }
                if (fAttackRange == 0) {
                    fAttackRange = 420;
                }
                break;
            case 37: // wand
            case 38: // staff
            case 49: // gun
                if (cAttackRange == 0) {
                    cAttackRange = 75;
                }
                if (fAttackRange == 0) {
                    fAttackRange = 1000;
                }
                break;
            case 40: // two-handed sword
            case 45: // bow
            case 46: // crossbow
                if (cAttackRange == 0) {
                    cAttackRange = 90;
                }
                if (fAttackRange == 0) {
                    fAttackRange = 420;
                }
                break;
            case 41: // two-handed axe
                if (cAttackRange == 0) {
                    cAttackRange = 115;
                }
                if (fAttackRange == 0) {
                    fAttackRange = 1000;
                }
                break;
            case 43: // spear
                if (cAttackRange == 0) {
                    cAttackRange = 130;
                }
                if (fAttackRange == 0) {
                    fAttackRange = 1000;
                }
                break;
            default:
                if (attacker.isDebug()) {
                    attacker.sendMessage("Unhandled weapon type for distance calculations: {}", wt);
                }
                break;
        }

        // add distance to base
        switch (attackInfo.skill) {
            case 0: // no skill
                // do nothing
                break;
            case 9001001: // GM Dragon Roar
                // skill is universal
                cAttackRange = 800;
                fAttackRange = 800;
                global = true;
                break;

            //region Job 222
            case 2221003:
                fAttackRange = 400;
                break;
            //endregion

            //region Job 212
            case 2121006:
            case 2121003:
                cAttackRange = 350;
                fAttackRange = 350;
                break;
            //endregion

            //region Job 111
            case 1111008:
                cAttackRange = 240;
                break;
            //endregion

            //region Job 112
            case 1121008:
                cAttackRange = 480;
                break;
            //endregion

            //region Job 312
            case 3101003: {
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
            //endregion

            //region Job 412
            case 4121003: // Taunt
                fAttackRange = 330;
                break;
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
            //endregion

            //region Job 420
            case 4201005:
                cAttackRange = 500;
                break;
            //endregion

            //region Job 422
            case 4221001:
                cAttackRange = 100;
                break;
            //endregion

            //region Job 500
            case 5001001:
                cAttackRange = 140;
                break;
            //endregion

            //region Job 512
            case 5121001:
                global = true;
                cAttackRange = 500;
                break;
            //endregion

            default:
                if (attacker.isDebug()) {
                    attacker.sendMessage("Unhandled skill for distance calculation: {}", attackInfo.skill);
                }
                break;
        }

        int damage = attacker.calculateMaxBaseDamage(attacker.getTotalWatk());
        String dString = Integer.toString(damage);
        int sub = (int) (Math.ceil(dString.length() / 2f) + 1);
        int tDamage = Integer.parseInt(dString.substring(0, Math.min(dString.length(), sub)));
        int min = Math.abs(tDamage - 10);
        int max = (tDamage + 25);
        int eDamage = Randomizer.rand(min, max);
        this.damage = eDamage;
        if (attacker.isGM()) {
            if (attacker.isDebug()) {
                attacker.sendMessage(":");
                attacker.sendMessage("Distance calculated: [c: {}, f: {}]", cAttackRange, fAttackRange);
                attacker.sendMessage("Weapon attack damage calculation: {}", damage);
                attacker.sendMessage("Extra damage randomizer: {}", eDamage);
            }
        }
    }
}
