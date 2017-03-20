package server.quest.custom.requirement;

import tools.Pair;

import java.util.HashMap;

/**
 * @author izarooni
 */
public class CQuestItemRequirement extends HashMap<Integer, Pair<Integer, Integer>> implements CQuestRequirement {

    /**
     * Increment an item collection progress.
     * <p>
     * Progress will never decrease below 0 but may increase above the requirement
     * </p>
     *
     * @param itemId   ID of item to progress
     * @param quantity amount to increment progress
     */
    public void addToCollect(int itemId, int quantity) {
        Pair<Integer, Integer> d = get(itemId);
        if (d.right + quantity < 0) {
            // don't go negative
            d.right = 0;
        } else {
            // going over requirement is ok
            d.right += quantity;
        }
    }
}
