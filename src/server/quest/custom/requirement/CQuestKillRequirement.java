package server.quest.custom.requirement;

import tools.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author izarooni
 */
public class CQuestKillRequirement extends CQuestRequirement {

    private HashMap<Integer, Pair<Integer, Integer>> kills = new HashMap<>();

    public Pair<Integer, Integer> get(int monsterId) {
        return kills.get(monsterId);
    }

    public Map<Integer, Pair<Integer, Integer>> getKills() {
        return Collections.unmodifiableMap(kills);
    }

    public boolean isEmpty() {
        return kills.isEmpty();
    }

    /**
     * Adds a monster kill requirement
     *
     * @param monsterId an ID of a monster
     * @param amount    requirement amount to kill this monster
     */
    public void add(int monsterId, int amount) {
        if (kills.containsKey(monsterId)) {
            throw new RuntimeException(String.format("A monster (%d) already exists", monsterId));
        }
        kills.put(monsterId, new Pair<>(amount, 0));
    }

    /**
     * Increment monster kill progress
     * <p>
     * Progress will never decrease below 0 and will max at the requirement
     * </p>
     *
     * @param monsterId ID of monster to progress
     * @param inc       right to increment progress
     */
    public void incrementRequirement(int monsterId, int inc) {
        Pair<Integer, Integer> d = kills.get(monsterId);
        if (d == null || d.right.equals(d.left)) {
            return;
        }
        if (d.right + inc > d.left) { // progress meets requirement
            // don't exceed requirement
            d.right = d.left;
        } else if (d.right + inc < 0) { // subtract progress
            // don't go negative
            d.right = 0;
        } else {
            d.right += inc;
        }
    }
}
