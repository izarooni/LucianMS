package server.quest.custom.requirement;

import tools.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author izarooni
 */
public class CQuestKillRequirement extends HashMap<Integer, Pair<Integer, Integer>> implements CQuestRequirement {

    /**
     * Increment monster kill progress
     * <p>
     * Progress will never decrease below 0 and will max at the requirement
     * </p>
     *
     * @param monsterId ID of monster to progress
     * @param amount    amount to increment progress
     */
    public void addToKill(int monsterId, int amount) {
        Pair<Integer, Integer> d = get(monsterId);
        if (d == null || d.right.equals(d.left)) {
            return;
        }
        if (d.right + amount > d.left) { // progress meets requirement
            // don't exceed requirement
            d.right = d.left;
        } else if (d.right + amount < 0) { // subtract progress
            // don't go negative
            d.right = 0;
        } else {
            d.right += amount;
        }
    }
}
