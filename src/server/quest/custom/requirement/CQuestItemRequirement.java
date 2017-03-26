package server.quest.custom.requirement;

import tools.Pair;

import java.util.HashMap;

/**
 * Pair of quest requirement variable (Pair.left) and progress variable (Pair.right)
 *
 * @author izarooni
 */
public class CQuestItemRequirement extends CQuestRequirement {

    /**
     * Increment an item collection progress.
     * <p>
     * Progress will never decrease below 0 but may increase above the requirement
     * </p>
     *
     * @param left   ID of item to progress
     * @param right amount to increment progress
     * @return true if the progress variable is larger than or equal to the requirement variable
     */
    @Override
    public boolean incrementRequirement(int left, int right) {
        Pair<Integer, Integer> d = get(left);
        if (d == null) {
            return false;
        }
        if (d.right + right < 0) {
            // don't go negative
            d.right = 0;
        } else {
            // going over requirement is ok
            d.right += right;
        }
        return d.right >= d.left;
    }
}
