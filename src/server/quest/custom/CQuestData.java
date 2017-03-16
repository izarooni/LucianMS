package server.quest.custom;

import client.MapleCharacter;
import server.quest.custom.reward.CQuestReward;
import tools.Pair;

import java.util.*;

/**
 * Holds information for a quest (i.e. killed monsters, items looted)
 *
 * @author izarooni
 */
public class CQuestData {

    private final int id;
    private final String name;
    private boolean completed = false;

    /**
     * Map of monster ID to kill requirement and kill progress
     */
    HashMap<Integer, Pair<Integer, Integer>> toKill = new HashMap<>();

    /**
     * Map of item ID to quantity requirement and quantity progress
     */
    HashMap<Integer, Pair<Integer, Integer>> toCollect = new HashMap<>();

    ArrayList<CQuestReward> rewards = new ArrayList<>();

    CQuestData(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Creates a copy of this quest objects with 0 progress on all requirements
     * <p>
     * Prevents overhead of reading and obtaining data from XML files
     * </p>
     *
     * @param player A player to begin the quest
     */
    CQuestData beginNew(MapleCharacter player) {
        CQuestData ret = new CQuestData(id, name);
        ret.rewards.addAll(rewards); // rewards don't have any changeable variables so we can use the same Objects
        // ID and requirement don't change but reset progress then add to new QuestData
        toKill.forEach((e, v) -> ret.toKill.put(e, new Pair<>(v.left, 0)));
        toCollect.forEach((e, v) -> ret.toCollect.put(e, new Pair<>(v.left, 0)));

        player.getCustomQuests().put(ret.getId(), ret);
        return ret;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<Integer, Pair<Integer, Integer>> getToKill() {
        return Collections.unmodifiableMap(toKill);
    }

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
        Pair<Integer, Integer> d = toKill.get(monsterId);
        if (d.right.equals(d.left)) {
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

    public Map<Integer, Pair<Integer, Integer>> getToCollect() {
        return Collections.unmodifiableMap(toCollect);
    }

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
        Pair<Integer, Integer> d = toCollect.get(itemId);
        if (d.right + quantity < 0) {
            // don't go negative
            d.right = 0;
        } else {
            // going over requirement is ok
            d.right += quantity;
        }
    }

    public List<CQuestReward> getRewards() {
        return Collections.unmodifiableList(rewards);
    }

    /**
     * Iterate each quest requirement and ensures all progress variables meet the paired requirement
     *
     * @return true if all progress variables meet their requirement, false otherwise
     */
    public boolean isCompleted() {
        // all pairs must meet their requirement
        if (!toCollect.values().stream().allMatch(p -> p.right >= p.left)) {
            // items can exceed their requirement
            return false;
        } else if (!toKill.values().stream().allMatch(p -> p.right.equals(p.left))) {
            // kills have a capacity so progress should never exceed the requirement
            return false;
        }
        return true;
    }

    /**
     * Complete the quest and give rewards to the specified player
     *
     * @param player player to give the quest rewards to
     * @return true if all rewards can be given at once, false otherwise
     */
    public boolean complete(MapleCharacter player) {
        if (player == null) {
            throw new IllegalArgumentException("Can't given rewards to nobody!");
        }
        if (rewards.stream().allMatch(r -> r.canAccept(player))) {
            completed = true;
            rewards.forEach(r -> r.give(player));
            return true;
        }
        return false;
    }
}
