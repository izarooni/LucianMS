package server.quest.custom.requirement;

import tools.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Pair of quest requirement variable (Pair.left) and progress variable (Pair.right)
 *
 * @author izarooni
 */
public class CQuestKillRequirement extends CQuestRequirement {

    /**
     * Increment monster kill progress
     * <p>
     * Progress will never decrease below 0 and will max at the requirement
     * </p>
     *
     * @param left ID of monster to progress
     * @param right    right to increment progress
     */
    public boolean incrementRequirement(int left, int right) {
        Pair<Integer, Integer> d = get(left);
        if (d == null || d.right.equals(d.left)) {
            return false;
        }
        if (d.right + right > d.left) { // progress meets requirement
            // don't exceed requirement
            d.right = d.left;
        } else if (d.right + right < 0) { // subtract progress
            // don't go negative
            d.right = 0;
        } else {
            d.right += right;
        }
        return d.right >= d.left;
    }
}
